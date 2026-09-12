package com.captain.allowanceos

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.programs.MemoProgram
import com.solana.programs.SystemProgram
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.toUnsignedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.bitcoinj.base.Base58
import java.security.MessageDigest

internal const val MWA_IDENTITY_URI = "https://0xcaptain888.github.io/allowance-os/"
internal const val MWA_ICON_RELATIVE_URI = "favicon-ao-v017.svg"

data class AuditEvent(
    val createdAt: Long,
    val kind: String,
    val state: AllowanceState,
    val amount: Double = 0.0,
    val message: String,
    val signature: String = "",
    val serviceId: String = "",
)

enum class WalletSessionState {
    DISCONNECTED,
    CONNECTED,
    REAUTH_REQUIRED,
}

enum class JudgeRunStage {
    IDLE,
    RESETTING,
    VERIFIED,
    BLOCKED,
    FROZEN,
    LIVE_PROOF,
    PROGRAM_MATRIX,
    WALLET_APPROVAL,
    COMPLETE,
    FAILED,
}

data class RestoredWalletSession(
    val state: WalletSessionState,
    val publicKey: String,
    val accountLabel: String,
)

fun resolveRestoredWalletSession(
    storedPublicKey: String,
    storedAccountLabel: String,
    authToken: String?,
): RestoredWalletSession = when {
    storedPublicKey.isBlank() -> RestoredWalletSession(WalletSessionState.DISCONNECTED, "", "")
    authToken.isNullOrBlank() -> RestoredWalletSession(WalletSessionState.REAUTH_REQUIRED, "", "")
    else -> RestoredWalletSession(WalletSessionState.CONNECTED, storedPublicKey, storedAccountLabel)
}

data class AllowanceUiState(
    val loading: Boolean = false,
    val loadingMessage: String = "",
    val walletAddress: String = "",
    val walletLabel: String = "",
    val walletSessionState: WalletSessionState = WalletSessionState.DISCONNECTED,
    val solBalance: Double? = null,
    val allowanceState: AllowanceState = AllowanceState.IDLE,
    val decisionReason: String = "Connect a Devnet wallet or replay a policy outcome.",
    val signature: String = "",
    val error: String = "",
    val requestedAmount: Double = 1.0,
    val periodSpent: Double = 0.0,
    val merchantTrusted: Boolean = true,
    val evidencePresent: Boolean = true,
    val auditEvents: List<AuditEvent> = emptyList(),
    val proofCheckLoading: Boolean = false,
    val proofCheckPassed: Boolean? = null,
    val proofCheckSlot: Long? = null,
    val proofCheckConfirmation: String = "",
    val proofCheckedSignature: String = "",
    val proofCheckMessage: String = "",
    val programCheckLoading: Boolean = false,
    val programCheckPassed: Boolean? = null,
    val programCheckMessage: String = "",
    val programSpentInPeriod: ULong = 0uL,
    val programBlockedRejected: Boolean = false,
    val programFrozenPersisted: Boolean = false,
    val programRevokedPersisted: Boolean = false,
    val programSettlementVerified: Boolean = false,
    val programSourceTokenRaw: ULong = 0uL,
    val programMerchantTokenRaw: ULong = 0uL,
    val selectedServiceId: String = CommercialCatalog.DEFAULT_ID,
    val requestId: String = "req_alphabrief_mobile_001",
    val requestNonce: Long = 1,
    val requestExpiresAt: Long = 0,
    val deliveryEvidenceHash: String = "",
    val alphaBriefUnlocked: Boolean = false,
    val replayRejected: Boolean = false,
    val alphaBriefLiveProofSynced: Boolean = false,
    val locallyPaused: Boolean = false,
    val v2StateLoading: Boolean = false,
    val v2State: DelegatedAllowanceSnapshot? = null,
    val v2StateMessage: String = "",
    val v2ControlSignature: String = "",
    val actionFeedback: String = "",
    val judgeRunStage: JudgeRunStage = JudgeRunStage.IDLE,
    val judgeRunMessage: String = "",
    val commercialSettlementVerified: Boolean? = null,
    val commercialSettlementLamports: Long = 0L,
    val commercialMerchantBalanceBefore: Long = 0L,
    val commercialMerchantBalanceAfter: Long = 0L,
)

class AllowanceViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("allowance_os", 0)
    private val secureSession = SecureSessionStore(application)
    val preferredChinese: Boolean
        get() = preferences.getBoolean(KEY_LANGUAGE_CHINESE, false)
    val hasCompletedOnboarding: Boolean
        get() = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)

    init {
        // Migrate pre-v0.11 installs that stored the MWA token in plaintext.
        preferences.getString(KEY_AUTH_TOKEN_LEGACY, null)?.takeIf { it.isNotBlank() }?.let {
            runCatching { secureSession.saveAuthToken(it) }
            preferences.edit().remove(KEY_AUTH_TOKEN_LEGACY).apply()
        }
    }

    private var activePolicy = CommercialCatalog.byId(
        preferences.getString(KEY_SELECTED_SERVICE, CommercialCatalog.DEFAULT_ID).orEmpty(),
    ).toPolicy()
    val policy: AllowancePolicy get() = activePolicy
    val policyHash: String get() = PolicyEngine.policyHash(activePolicy)
    private val rpc = DevnetRpc()
    private val loadedAuthToken = secureSession.loadAuthToken()
    private val storedPublicKey = preferences.getString(KEY_PUBLIC_KEY, "").orEmpty()
    private val storedAccountLabel = preferences.getString(KEY_ACCOUNT_LABEL, "").orEmpty()
    private val restoredSession = resolveRestoredWalletSession(
        storedPublicKey = storedPublicKey,
        storedAccountLabel = storedAccountLabel,
        authToken = loadedAuthToken,
    ).also { restored ->
        if (restored.state != WalletSessionState.CONNECTED) {
            if (storedPublicKey.isNotBlank()) clearStoredPublicIdentity()
            if (!loadedAuthToken.isNullOrBlank()) secureSession.clearAuthToken()
        }
    }
    private val restoredAuthToken = loadedAuthToken.takeIf {
        restoredSession.state == WalletSessionState.CONNECTED
    }
    private val walletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse(MWA_IDENTITY_URI),
            // MWA requires iconRelativeUri to be relative to identityUri. An absolute
            // icon URI makes compatible wallets reject authorization before opening.
            iconUri = Uri.parse(MWA_ICON_RELATIVE_URI),
            identityName = "Allowance OS",
        ),
    ).apply {
        blockchain = Solana.Devnet
        authToken = restoredAuthToken
    }

    private val _state = MutableStateFlow(
        AllowanceUiState(
            walletAddress = restoredSession.publicKey,
            walletLabel = restoredSession.accountLabel,
            walletSessionState = restoredSession.state,
            error = if (restoredSession.state == WalletSessionState.REAUTH_REQUIRED) {
                "The saved wallet session could not be restored. Reauthorize the wallet to continue."
            } else "",
            signature = preferences.getString(KEY_LAST_SIGNATURE, "").orEmpty(),
            auditEvents = loadEvents(),
            selectedServiceId = preferences.getString(KEY_SELECTED_SERVICE, CommercialCatalog.DEFAULT_ID)
                ?: CommercialCatalog.DEFAULT_ID,
            alphaBriefLiveProofSynced = preferences.getBoolean(KEY_ALPHABRIEF_SYNCED, false),
            locallyPaused = preferences.getBoolean(KEY_LOCAL_PAUSED, false),
            commercialSettlementVerified = preferences.getBoolean(KEY_COMMERCIAL_SETTLEMENT_VERIFIED, false),
            commercialSettlementLamports = preferences.getLong(KEY_COMMERCIAL_SETTLEMENT_LAMPORTS, 0L),
            commercialMerchantBalanceBefore = preferences.getLong(KEY_COMMERCIAL_MERCHANT_BALANCE_BEFORE, 0L),
            commercialMerchantBalanceAfter = preferences.getLong(KEY_COMMERCIAL_MERCHANT_BALANCE_AFTER, 0L),
        ),
    )
    val state: StateFlow<AllowanceUiState> = _state

    fun connect(sender: ActivityResultSender) {
        viewModelScope.launch {
            setLoading()
            try {
                when (val result = withTimeout(WALLET_OPERATION_TIMEOUT_MS) { walletAdapter.connect(sender) }) {
                    is TransactionResult.Success -> {
                        val account = result.authResult.accounts.first()
                        val publicKey = SolanaPublicKey(account.publicKey)
                        try {
                            persistConnection(
                                publicKey = publicKey.base58(),
                                accountLabel = account.accountLabel.orEmpty(),
                                authToken = result.authResult.authToken,
                            )
                        } catch (error: Exception) {
                            clearConnection()
                            fail(
                                "Wallet authorization succeeded, but the encrypted reconnect session could not be saved. Reconnect and try again.",
                                "WALLET_SESSION_STORAGE_ERROR",
                            )
                            return@launch
                        }
                        _state.update {
                            it.copy(
                                loading = false,
                                walletAddress = publicKey.base58(),
                                walletLabel = account.accountLabel.orEmpty(),
                                walletSessionState = WalletSessionState.CONNECTED,
                                solBalance = runCatching { rpc.balance(publicKey) }.getOrNull(),
                                error = "",
                                decisionReason = "MWA authorization succeeded on Solana Devnet.",
                                actionFeedback = "Wallet authorized on Solana Devnet.",
                            )
                        }
                        logEvent("WALLET_CONNECTED", AllowanceState.IDLE, message = "MWA wallet authorization succeeded")
                    }

                    is TransactionResult.NoWalletFound -> fail(result.message, "WALLET_ERROR")
                    is TransactionResult.Failure -> fail(result.message, "WALLET_ERROR")
                }
            } catch (error: TimeoutCancellationException) {
                fail("Wallet did not respond within 90 seconds. The app is unlocked; try again when the wallet is ready.", "WALLET_TIMEOUT")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error.message ?: "Wallet connection was interrupted", "WALLET_ERROR")
            }
        }
    }

    fun selectCommercialTemplate(id: String) {
        val template = CommercialCatalog.byId(id)
        activePolicy = template.toPolicy()
        preferences.edit().putString(KEY_SELECTED_SERVICE, template.id).apply()
        _state.update {
            it.copy(
                selectedServiceId = template.id,
                allowanceState = AllowanceState.IDLE,
                requestedAmount = template.perCharge,
                periodSpent = 0.0,
                merchantTrusted = true,
                evidencePresent = true,
                signature = "",
                error = "",
                decisionReason = "${template.name} template applied locally. Review the policy before wallet authorization.",
                actionFeedback = "${template.name} policy loaded. Spend history is isolated to this service.",
            )
        }
        logEvent("TEMPLATE_APPLIED", AllowanceState.IDLE, message = "${template.name} commercial allowance template applied")
    }

    fun setPreferredChinese(value: Boolean) {
        preferences.edit().putBoolean(KEY_LANGUAGE_CHINESE, value).apply()
    }

    fun completeOnboarding() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, true).apply()
    }

    fun resetInteractiveDemo() {
        preferences.edit().putBoolean(KEY_LOCAL_PAUSED, false).apply()
        _state.update {
            it.copy(
                loading = false,
                loadingMessage = "",
                allowanceState = AllowanceState.IDLE,
                decisionReason = "Demo reset. Choose VERIFIED, BLOCKED, or FROZEN, or adjust the policy controls.",
                requestedAmount = policy.perChargeCap,
                periodSpent = 0.0,
                merchantTrusted = true,
                evidencePresent = true,
                alphaBriefUnlocked = false,
                replayRejected = false,
                locallyPaused = false,
                error = "",
                actionFeedback = "Interactive policy demo reset to a testable baseline.",
            )
        }
        logEvent("INTERACTIVE_DEMO_RESET", AllowanceState.IDLE, message = "Local pause cleared and policy demo restored to a testable baseline")
    }

    fun runVerified() = evaluate(
        amount = policy.perChargeCap,
        merchant = policy.merchant,
        evidence = "service-delivery-sha256",
        respectLocalPause = false,
    )

    fun runBlocked() = evaluate(
        amount = policy.perChargeCap + maxOf(1.0, policy.perChargeCap),
        merchant = policy.merchant,
        evidence = "service-delivery-sha256",
        respectLocalPause = false,
    )

    fun runFrozen() = evaluate(
        amount = policy.perChargeCap,
        merchant = "merchant:lookalike",
        evidence = "service-delivery-sha256",
        respectLocalPause = false,
    )

    fun runJudgeDemo(sender: ActivityResultSender) {
        if (_state.value.judgeRunStage in JUDGE_RUN_ACTIVE_STAGES) return
        viewModelScope.launch {
            try {
                val template = CommercialCatalog.byId(CommercialCatalog.DEFAULT_ID)
                activePolicy = template.toPolicy()
                preferences.edit()
                    .putString(KEY_SELECTED_SERVICE, template.id)
                    .putBoolean(KEY_LOCAL_PAUSED, false)
                    .apply()
                _state.update {
                    it.copy(
                        selectedServiceId = template.id,
                        locallyPaused = false,
                        allowanceState = AllowanceState.IDLE,
                        requestedAmount = template.perCharge,
                        periodSpent = 0.0,
                        merchantTrusted = true,
                        evidencePresent = true,
                        signature = "",
                        error = "",
                        proofCheckPassed = null,
                        programCheckPassed = null,
                        commercialSettlementVerified = null,
                        commercialSettlementLamports = 0L,
                        commercialMerchantBalanceBefore = 0L,
                        commercialMerchantBalanceAfter = 0L,
                        judgeRunStage = JudgeRunStage.RESETTING,
                        judgeRunMessage = "Safe baseline restored. Running deterministic policy checks…",
                        actionFeedback = "Judge Run started. Local replay, public Devnet verification, and one wallet-approved settlement are clearly separated.",
                    )
                }
                logEvent("JUDGE_RUN_STARTED", AllowanceState.IDLE, message = "Judge Run reset local pause and selected the AlphaBrief testable baseline")
                delay(450)

                evaluate(template.perCharge, policy.merchant, "service-delivery-sha256", 0.0, respectLocalPause = false)
                _state.update { it.copy(judgeRunStage = JudgeRunStage.VERIFIED, judgeRunMessage = "VERIFIED: budget, merchant, and delivery evidence passed locally.") }
                delay(550)

                evaluate(template.perCharge + maxOf(1.0, template.perCharge), policy.merchant, "service-delivery-sha256", 0.0, respectLocalPause = false)
                _state.update { it.copy(judgeRunStage = JudgeRunStage.BLOCKED, judgeRunMessage = "BLOCKED: an oversized request stopped before the wallet opened.") }
                delay(550)

                evaluate(template.perCharge, "merchant:lookalike", "service-delivery-sha256", 0.0, respectLocalPause = false)
                _state.update { it.copy(judgeRunStage = JudgeRunStage.FROZEN, judgeRunMessage = "FROZEN: merchant identity drift was contained with zero funds moved.") }
                delay(550)

                _state.update { it.copy(judgeRunStage = JudgeRunStage.LIVE_PROOF, judgeRunMessage = "Checking the published AlphaBrief settlement and bad-output freeze through Devnet RPC…") }
                val historicalSettlement = rpc.signatureStatus(AlphaBriefLiveEvidence.SETTLEMENT_SIGNATURE)
                val historicalFreeze = rpc.signatureStatus(AlphaBriefLiveEvidence.FREEZE_SIGNATURE)
                check(historicalSettlement.succeeded && historicalSettlement.confirmation in CONFIRMED_STATUSES) {
                    "Published AlphaBrief settlement is not confirmed successfully"
                }
                check(historicalFreeze.succeeded && historicalFreeze.confirmation in CONFIRMED_STATUSES) {
                    "Published AlphaBrief freeze is not confirmed successfully"
                }
                _state.update { it.copy(alphaBriefLiveProofSynced = true) }
                logEvent("JUDGE_RUN_LIVE_PROOF_VERIFIED", AllowanceState.VERIFIED, message = "RPC independently confirmed the published AlphaBrief settlement and freeze")

                _state.update { it.copy(judgeRunStage = JudgeRunStage.PROGRAM_MATRIX, judgeRunMessage = "Checking Program state transitions and SPL-token balance deltas…") }
                val matrix = rpc.programMatrix()
                check(matrix.passed) { matrix.message }
                _state.update {
                    it.copy(
                        programCheckPassed = true,
                        programCheckMessage = matrix.message,
                        programSpentInPeriod = matrix.spentInPeriod,
                        programBlockedRejected = matrix.blockedRejected,
                        programFrozenPersisted = matrix.frozenPersisted,
                        programRevokedPersisted = matrix.revokedPersisted,
                        programSettlementVerified = matrix.settlementVerified,
                        programSourceTokenRaw = matrix.sourceTokenRaw,
                        programMerchantTokenRaw = matrix.merchantTokenRaw,
                    )
                }
                delay(350)

                evaluate(template.perCharge, policy.merchant, AlphaBriefLiveEvidence.ACCEPTED_EVIDENCE_HASH, 0.0, respectLocalPause = false)
                _state.update {
                    it.copy(
                        judgeRunStage = JudgeRunStage.WALLET_APPROVAL,
                        judgeRunMessage = "All read-only checks passed. Confirm one 0.00001 SOL Devnet AlphaBrief micro-settlement in Solflare.",
                    )
                }
                submitAlphaBriefSettlement(sender, fromJudgeRun = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                failJudgeRun(error.message ?: "Judge Run could not complete")
            }
        }
    }

    fun runAlphaBriefDelivery() {
        if (blockIfLocallyPaused(policy.perChargeCap)) return
        val now = System.currentTimeMillis()
        val envelope = ChargeEnvelope(
            requestId = "req_alphabrief_mobile_$now",
            nonce = now,
            requestedAtMillis = now,
            expiresAtMillis = now + 5 * 60_000,
            evidenceHash = AlphaBriefReference.evidenceHash,
            evidenceUri = AlphaBriefReference.EVIDENCE_URI,
        )
        val decision = PolicyEngine.evaluateRequest(policy, policy.perChargeCap, policy.merchant, envelope, now)
        _state.update {
            it.copy(
                allowanceState = decision.state,
                decisionReason = decision.reason,
                requestedAmount = policy.perChargeCap,
                requestId = envelope.requestId,
                requestNonce = envelope.nonce,
                requestExpiresAt = envelope.expiresAtMillis,
                deliveryEvidenceHash = envelope.evidenceHash,
                alphaBriefUnlocked = decision.state == AllowanceState.VERIFIED,
                replayRejected = false,
            )
        }
        logEvent("ALPHABRIEF_UNLOCKED", decision.state, policy.perChargeCap, "AlphaBrief content hash verified and report unlocked")
    }

    fun replayAlphaBriefEvidence() {
        if (blockIfLocallyPaused(policy.perChargeCap)) return
        val now = System.currentTimeMillis()
        val envelope = ChargeEnvelope(
            requestId = "req_alphabrief_replay_$now",
            nonce = now + 1,
            requestedAtMillis = now,
            expiresAtMillis = now + 5 * 60_000,
            evidenceHash = AlphaBriefReference.evidenceHash,
            evidenceUri = AlphaBriefReference.EVIDENCE_URI,
        )
        val decision = PolicyEngine.evaluateRequest(
            policy, policy.perChargeCap, policy.merchant, envelope, now,
            usedEvidenceHashes = setOf(AlphaBriefReference.evidenceHash),
        )
        _state.update {
            it.copy(
                allowanceState = decision.state,
                decisionReason = decision.reason,
                requestId = envelope.requestId,
                requestNonce = envelope.nonce,
                requestExpiresAt = envelope.expiresAtMillis,
                deliveryEvidenceHash = envelope.evidenceHash,
                replayRejected = decision.state == AllowanceState.BLOCKED,
            )
        }
        logEvent("REPLAY_REJECTED", decision.state, policy.perChargeCap, decision.reason)
    }

    fun syncAlphaBriefLiveProof(notificationsEnabled: Boolean = false) {
        if (_state.value.alphaBriefLiveProofSynced) {
            setActionFeedback("AlphaBrief public settlement and freeze proofs are already synced.")
            return
        }
        viewModelScope.launch {
            setLoading("Verifying AlphaBrief settlement and freeze on Solana Devnet…")
            runCatching {
                val settlement = rpc.signatureStatus(AlphaBriefLiveEvidence.SETTLEMENT_SIGNATURE)
                val freeze = rpc.signatureStatus(AlphaBriefLiveEvidence.FREEZE_SIGNATURE)
                check(settlement.succeeded && settlement.confirmation in setOf("CONFIRMED", "FINALIZED")) {
                    "The recorded AlphaBrief settlement is not confirmed successfully"
                }
                check(freeze.succeeded && freeze.confirmation in setOf("CONFIRMED", "FINALIZED")) {
                    "The recorded AlphaBrief freeze is not confirmed successfully"
                }
            }.onSuccess {
                preferences.edit().putBoolean(KEY_ALPHABRIEF_SYNCED, true).apply()
                logEvent(
                    "ALPHABRIEF_LIVE_SETTLED",
                    AllowanceState.VERIFIED,
                    AlphaBriefLiveEvidence.PRICE,
                    "RPC-confirmed Devnet proof: independently verified delivery settled through delegated v2.",
                    AlphaBriefLiveEvidence.SETTLEMENT_SIGNATURE,
                    serviceId = CommercialCatalog.DEFAULT_ID,
                )
                logEvent(
                    "ALPHABRIEF_BAD_OUTPUT_FROZEN",
                    AllowanceState.FROZEN,
                    0.0,
                    "RPC-confirmed Devnet proof: bad output froze the allowance; published evidence records zero token movement.",
                    AlphaBriefLiveEvidence.FREEZE_SIGNATURE,
                    serviceId = CommercialCatalog.DEFAULT_ID,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        loadingMessage = "",
                        alphaBriefLiveProofSynced = true,
                        deliveryEvidenceHash = AlphaBriefLiveEvidence.ACCEPTED_EVIDENCE_HASH,
                        allowanceState = AllowanceState.FROZEN,
                        decisionReason = "RPC-confirmed AlphaBrief settlement and bad-output freeze proofs are linked to this device timeline.",
                        actionFeedback = if (notificationsEnabled) {
                            "AlphaBrief proofs synced and device notifications sent."
                        } else {
                            "AlphaBrief proofs synced. Notifications are off, but the in-app timeline is complete."
                        },
                    )
                }
                if (notificationsEnabled) {
                    AllowanceNotifications.notify(
                        getApplication(),
                        1501,
                        "AlphaBrief delivered and settled",
                        "Independent verification passed. The RPC-confirmed Devnet v2 settlement is now in your timeline.",
                    )
                    AllowanceNotifications.notify(
                        getApplication(),
                        1502,
                        "Allowance frozen after bad output",
                        "A later AlphaBrief result failed verification. The published freeze path moved zero tokens.",
                    )
                }
            }.onFailure { error ->
                fail(error.message ?: "Unable to verify the AlphaBrief public proofs", "ALPHABRIEF_PROOF_SYNC_ERROR")
            }
        }
    }

    fun toggleLocalPause() {
        val paused = !_state.value.locallyPaused
        preferences.edit().putBoolean(KEY_LOCAL_PAUSED, paused).apply()
        _state.update {
            it.copy(
                locallyPaused = paused,
                decisionReason = if (paused) {
                    "Local safety pause enabled. New app-side requests are stopped; this is not an onchain pause transaction."
                } else {
                    "Local safety pause disabled. Onchain state remains unchanged."
                },
                actionFeedback = if (paused) {
                    "Local safety pause is ON. New app-side requests will be blocked."
                } else {
                    "Local safety pause is OFF. App-side requests may run again."
                },
            )
        }
        logEvent(
            if (paused) "LOCAL_SAFETY_PAUSED" else "LOCAL_SAFETY_RESUMED",
            AllowanceState.IDLE,
            message = if (paused) "New local requests paused; no onchain transaction broadcast" else "Local requests resumed; no onchain transaction broadcast",
        )
        AllowanceNotifications.notify(
            getApplication(),
            1503,
            if (paused) "Allowance requests paused locally" else "Allowance requests resumed locally",
            if (paused) "Allowance OS will stop new app-side requests. Onchain allowance state is unchanged." else "Local request processing is active again.",
        )
    }

    fun inspectDelegatedV2() {
        viewModelScope.launch {
            _state.update { it.copy(v2StateLoading = true, v2StateMessage = "Reading the deployed v2 allowance from Solana Devnet…") }
            runCatching { rpc.delegatedV2Allowance() }
                .onSuccess { snapshot ->
                    _state.update {
                        it.copy(
                            v2StateLoading = false,
                            v2State = snapshot,
                            v2StateMessage = "RPC confirmed the deployed v2 allowance state.",
                        )
                    }
                    logEvent("V2_STATE_INSPECTED", if (snapshot.revoked || snapshot.frozen) AllowanceState.FROZEN else AllowanceState.VERIFIED, message = "RPC read the delegated v2 allowance state")
                }
                .onFailure { error ->
                    _state.update { it.copy(v2StateLoading = false, v2StateMessage = error.message ?: "Unable to read the delegated v2 allowance") }
                }
        }
    }

    fun pauseDelegatedV2(sender: ActivityResultSender) = submitDelegatedV2Control(
        sender = sender,
        action = "PAUSE",
        instruction = DelegatedAllowanceV2.pause(
            DelegatedAllowanceV2.PROGRAM_ID,
            DelegatedAllowanceV2.AUTHORITY,
            DelegatedAllowanceV2.ALLOWANCE_ACCOUNT,
        ),
    )

    fun unpauseDelegatedV2(sender: ActivityResultSender) = submitDelegatedV2Control(
        sender = sender,
        action = "UNPAUSE",
        instruction = DelegatedAllowanceV2.unpause(
            DelegatedAllowanceV2.PROGRAM_ID,
            DelegatedAllowanceV2.AUTHORITY,
            DelegatedAllowanceV2.ALLOWANCE_ACCOUNT,
        ),
    )

    fun revokeDelegatedV2(sender: ActivityResultSender) = submitDelegatedV2Control(
        sender = sender,
        action = "REVOKE",
        instruction = DelegatedAllowanceV2.revoke(
            DelegatedAllowanceV2.PROGRAM_ID,
            DelegatedAllowanceV2.AUTHORITY,
            DelegatedAllowanceV2.ALLOWANCE_ACCOUNT,
            DelegatedAllowanceV2.SOURCE_TOKEN_ACCOUNT,
        ),
    )

    private fun submitDelegatedV2Control(
        sender: ActivityResultSender,
        action: String,
        instruction: com.solana.transaction.TransactionInstruction,
    ) {
        val connected = _state.value.walletAddress
        if (connected != DelegatedAllowanceV2.AUTHORITY) {
            val message = "This live control requires the allowance authority wallet ${DelegatedAllowanceV2.AUTHORITY}. Connected wallet does not control this Devnet evidence account."
            _state.update { it.copy(v2StateMessage = message, error = message) }
            logEvent("V2_CONTROL_BLOCKED", AllowanceState.BLOCKED, message = message)
            return
        }
        viewModelScope.launch {
            setLoading("Review the $action v2 transaction in your wallet…")
            try {
                when (val result = withTimeout(WALLET_OPERATION_TIMEOUT_MS) { walletAdapter.transact(sender) { authorization ->
                val signer = SolanaPublicKey(authorization.accounts.first().publicKey)
                check(signer.base58() == DelegatedAllowanceV2.AUTHORITY) {
                    "Wallet account changed; expected the delegated v2 authority account"
                }
                val transaction = Message.Builder()
                    .setRecentBlockhash(rpc.latestBlockhash())
                    .addInstruction(instruction)
                    .build()
                    .toUnsignedTransaction()
                Base58.encode(signAndSendTransactions(arrayOf(transaction.serialize())).signatures.first())
                } }) {
                is TransactionResult.Success -> {
                    val account = result.authResult.accounts.first()
                    runCatching {
                        persistConnection(
                            publicKey = SolanaPublicKey(account.publicKey).base58(),
                            accountLabel = account.accountLabel.orEmpty(),
                            authToken = result.authResult.authToken,
                        )
                    }.onFailure {
                        clearConnection()
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMessage = "",
                            walletAddress = SolanaPublicKey(account.publicKey).base58(),
                            walletLabel = account.accountLabel.orEmpty(),
                            walletSessionState = WalletSessionState.CONNECTED,
                            v2ControlSignature = result.payload,
                            v2StateMessage = "$action broadcast successfully. Reading the resulting state…",
                            error = "",
                        )
                    }
                    logEvent("V2_${action}_BROADCAST", AllowanceState.VERIFIED, message = "Live delegated v2 $action transaction broadcast", signature = result.payload)
                    inspectDelegatedV2()
                }
                    is TransactionResult.NoWalletFound -> fail(result.message, "V2_WALLET_ERROR")
                    is TransactionResult.Failure -> fail(result.message, "V2_CONTROL_ERROR")
                }
            } catch (error: TimeoutCancellationException) {
                fail("Wallet did not respond within 90 seconds. The app is unlocked; review the v2 action and try again.", "V2_WALLET_TIMEOUT")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error.message ?: "The v2 control broadcast was interrupted", "V2_CONTROL_ERROR")
            }
        }
    }

    fun dailyHabits(): DailyHabitsSnapshot = DailyHabitsEngine.snapshot(
        events = _state.value.auditEvents,
        service = CommercialCatalog.byId(_state.value.selectedServiceId),
    )

    fun weeklySafetyReport(): String = DailyHabitsEngine.weeklyReport(dailyHabits(), _state.value.locallyPaused)

    fun runDailySafetyCheck(notificationsEnabled: Boolean = false) {
        val habits = dailyHabits()
        val next = habits.upcomingCharges.first()
        if (notificationsEnabled) {
            AllowanceNotifications.notify(
                getApplication(),
                1510,
                "Upcoming allowance request",
                "${next.serviceName} may request ${next.amount} ${next.token}; delivery evidence is required before settlement.",
            )
            if (habits.budgetPressure) {
                AllowanceNotifications.notify(
                    getApplication(),
                    1511,
                    "Allowance budget needs review",
                    "This week's spend plus the next request is approaching the active policy threshold.",
                )
            }
            if (habits.merchantAnomaly) {
                AllowanceNotifications.notify(
                    getApplication(),
                    1512,
                    "Merchant anomaly detected",
                    "A recent FROZEN or identity-mismatch event needs review before the next payment.",
                )
            }
        }
        _state.update {
            it.copy(
                actionFeedback = if (notificationsEnabled) {
                    "Daily safety check complete. The in-app report and Android notification were updated."
                } else {
                    "Daily safety check complete. Notifications are off; the in-app report is still updated."
                },
            )
        }
        logEvent("DAILY_SAFETY_CHECK", AllowanceState.IDLE, message = "Upcoming charge, budget pressure, and merchant anomalies checked locally")
    }

    fun evaluateCustom(amount: Double, merchantTrusted: Boolean, evidencePresent: Boolean, periodSpent: Double = 0.0) = evaluate(
        amount = amount,
        merchant = if (merchantTrusted) policy.merchant else "merchant:lookalike",
        evidence = if (evidencePresent) "service-delivery-sha256" else "",
        periodSpent = periodSpent,
    )

    private fun evaluate(amount: Double, merchant: String, evidence: String, periodSpent: Double = 0.0) {
        evaluate(amount, merchant, evidence, periodSpent, respectLocalPause = true)
    }

    private fun evaluate(
        amount: Double,
        merchant: String,
        evidence: String,
        periodSpent: Double = 0.0,
        respectLocalPause: Boolean,
    ) {
        if (respectLocalPause && blockIfLocallyPaused(amount)) return
        val decision = PolicyEngine.evaluate(policy, amount, merchant, evidence, periodSpent)
        _state.update {
            it.copy(
                allowanceState = decision.state,
                decisionReason = decision.reason,
                signature = "",
                error = "",
                requestedAmount = amount,
                periodSpent = periodSpent,
                merchantTrusted = merchant == policy.merchant,
                evidencePresent = evidence.isNotBlank(),
            )
        }
        logEvent("POLICY_DECISION", decision.state, amount, decision.reason)
    }

    fun refreshBalance() {
        val address = _state.value.walletAddress
        if (address.isBlank()) return
        viewModelScope.launch {
            setLoading("Refreshing Devnet balance…")
            runCatching { rpc.balance(SolanaPublicKey(Base58.decode(address))) }
                .onSuccess { balance ->
                    _state.update {
                        it.copy(
                            loading = false,
                            solBalance = balance,
                            error = "",
                            actionFeedback = "Devnet balance refreshed successfully.",
                        )
                    }
                }
                .onFailure { error -> fail(error.message ?: "Unable to refresh Devnet balance", "BALANCE_ERROR") }
        }
    }

    fun publishDevnetProof(sender: ActivityResultSender) {
        val current = _state.value
        if (blockIfLocallyPaused(current.requestedAmount)) return
        val preflight = PolicyEngine.evaluate(
            policy = policy,
            amount = current.requestedAmount,
            merchant = if (current.merchantTrusted) policy.merchant else "merchant:lookalike",
            evidence = if (current.evidencePresent) "service-delivery-sha256" else "",
            periodSpent = current.periodSpent,
        )
        if (preflight.state != AllowanceState.VERIFIED) {
            _state.update { it.copy(allowanceState = preflight.state, decisionReason = preflight.reason, signature = "") }
            logEvent("POLICY_DECISION", preflight.state, current.requestedAmount, preflight.reason)
            return
        }

        viewModelScope.launch { submitAlphaBriefSettlement(sender, fromJudgeRun = false) }
    }

    private suspend fun submitAlphaBriefSettlement(sender: ActivityResultSender, fromJudgeRun: Boolean) {
        val merchant = SolanaPublicKey.from(AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT)
        var merchantBalanceBefore = 0L
        setLoading("Policy and delivery evidence passed. Approve the AlphaBrief Devnet micro-settlement in your wallet.")
        try {
            merchantBalanceBefore = rpc.balanceLamports(merchant)
            when (val result = withTimeout(WALLET_OPERATION_TIMEOUT_MS) { walletAdapter.transact(sender) { authorization ->
                val publicKey = SolanaPublicKey(authorization.accounts.first().publicKey)
                check(publicKey != merchant) { "Buyer and AlphaBrief merchant must be different wallets" }
                val memo = listOf(
                    "ALLOWANCE_OS",
                    "ALPHABRIEF_SETTLEMENT",
                    policy.allowanceId,
                    "policy=$policyHash",
                    "merchant=${AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT}",
                    "lamports=${AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS}",
                    "evidence=${AlphaBriefLiveEvidence.ACCEPTED_EVIDENCE_HASH}",
                ).joinToString("|")

                val transaction = Message.Builder()
                    .setRecentBlockhash(rpc.latestBlockhash())
                    .addInstruction(
                        SystemProgram.transfer(
                            fromPublicKey = publicKey,
                            toPublickKey = merchant,
                            lamports = AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS,
                        ),
                    )
                    .addInstruction(MemoProgram.publishMemo(publicKey, memo))
                    .build()
                    .toUnsignedTransaction()

                val signatureBytes = signAndSendTransactions(arrayOf(transaction.serialize()))
                    .signatures
                    .first()
                Base58.encode(signatureBytes)
            } }) {
                is TransactionResult.Success -> {
                    val account = result.authResult.accounts.first()
                    val publicKey = SolanaPublicKey(account.publicKey)
                    try {
                        persistConnection(
                            publicKey = publicKey.base58(),
                            accountLabel = account.accountLabel.orEmpty(),
                            authToken = result.authResult.authToken,
                        )
                    } catch (error: Exception) {
                        clearConnection()
                        preferences.edit().putString(KEY_LAST_SIGNATURE, result.payload).apply()
                        _state.update { it.copy(signature = result.payload) }
                        val message = "The settlement was broadcast, but the encrypted reconnect session could not be saved. Verify the signature, then reconnect the wallet."
                        if (fromJudgeRun) failJudgeRun(message) else fail(message, "WALLET_SESSION_STORAGE_ERROR")
                        return
                    }

                    val status = awaitSuccessfulSignature(result.payload)
                    val merchantBalanceAfter = rpc.balanceLamports(merchant)
                    val merchantDelta = merchantBalanceAfter - merchantBalanceBefore
                    check(merchantDelta >= AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS) {
                        "Transaction finalized, but the expected merchant balance increase was not observed"
                    }
                    val refreshedBalance = rpc.balance(publicKey)
                    preferences.edit()
                        .putString(KEY_LAST_SIGNATURE, result.payload)
                        .putBoolean(KEY_COMMERCIAL_SETTLEMENT_VERIFIED, true)
                        .putLong(KEY_COMMERCIAL_SETTLEMENT_LAMPORTS, AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS)
                        .putLong(KEY_COMMERCIAL_MERCHANT_BALANCE_BEFORE, merchantBalanceBefore)
                        .putLong(KEY_COMMERCIAL_MERCHANT_BALANCE_AFTER, merchantBalanceAfter)
                        .apply()
                    _state.update {
                        it.copy(
                            loading = false,
                            loadingMessage = "",
                            walletAddress = publicKey.base58(),
                            walletLabel = account.accountLabel.orEmpty(),
                            walletSessionState = WalletSessionState.CONNECTED,
                            solBalance = refreshedBalance,
                            allowanceState = AllowanceState.VERIFIED,
                            decisionReason = "Policy and delivery evidence passed; the wallet-approved AlphaBrief Devnet micro-settlement finalized and the merchant balance increased.",
                            signature = result.payload,
                            error = "",
                            proofCheckLoading = false,
                            proofCheckPassed = true,
                            proofCheckSlot = status.slot,
                            proofCheckConfirmation = status.confirmation,
                            proofCheckedSignature = result.payload,
                            proofCheckMessage = "RPC confirmed the settlement and the merchant balance delta.",
                            commercialSettlementVerified = true,
                            commercialSettlementLamports = AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_LAMPORTS,
                            commercialMerchantBalanceBefore = merchantBalanceBefore,
                            commercialMerchantBalanceAfter = merchantBalanceAfter,
                            judgeRunStage = if (fromJudgeRun) JudgeRunStage.COMPLETE else it.judgeRunStage,
                            judgeRunMessage = if (fromJudgeRun) {
                                "Complete: local safety matrix, public Program proof, wallet-approved settlement, and independent RPC verification all passed."
                            } else it.judgeRunMessage,
                            actionFeedback = "AlphaBrief Devnet settlement finalized and independently verified.",
                        )
                    }
                    logEvent(
                        "ALPHABRIEF_MOBILE_SETTLED",
                        AllowanceState.VERIFIED,
                        message = "Wallet-approved 0.00001 SOL AlphaBrief settlement finalized; merchant balance delta verified by RPC",
                        signature = result.payload,
                        serviceId = CommercialCatalog.DEFAULT_ID,
                    )
                    if (fromJudgeRun) {
                        logEvent("JUDGE_RUN_COMPLETED", AllowanceState.VERIFIED, message = "All simulated and live Judge Run stages passed", signature = result.payload)
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    if (fromJudgeRun) failJudgeRun(result.message) else fail(result.message, "WALLET_ERROR")
                }
                is TransactionResult.Failure -> {
                    if (fromJudgeRun) failJudgeRun(result.message) else fail(result.message, "WALLET_ERROR")
                }
            }
        } catch (error: TimeoutCancellationException) {
            val message = "Wallet did not respond within 90 seconds. No settlement success is assumed."
            if (fromJudgeRun) failJudgeRun(message) else fail(message, "WALLET_TIMEOUT")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val message = error.message ?: "AlphaBrief Devnet settlement was interrupted"
            if (fromJudgeRun) failJudgeRun(message) else fail(message, "WALLET_ERROR")
        }
    }

    fun disconnectWalletSession(sender: ActivityResultSender) {
        viewModelScope.launch {
            setLoading("Requesting MWA wallet disconnect…")
            try {
                when (val result = withTimeout(WALLET_OPERATION_TIMEOUT_MS) { walletAdapter.disconnect(sender) }) {
                is TransactionResult.Success -> {
                    clearConnection()
                    _state.value = AllowanceUiState(
                        allowanceState = AllowanceState.IDLE,
                        decisionReason = "MWA wallet session was deauthorized. Any onchain allowance remains unchanged until separately revoked.",
                        auditEvents = loadEvents(),
                        selectedServiceId = CommercialCatalog.byId(_state.value.selectedServiceId).id,
                        alphaBriefLiveProofSynced = preferences.getBoolean(KEY_ALPHABRIEF_SYNCED, false),
                        locallyPaused = preferences.getBoolean(KEY_LOCAL_PAUSED, false),
                    )
                    logEvent("MWA_DISCONNECTED", AllowanceState.IDLE, message = "MWA wallet session deauthorized; onchain allowance unchanged")
                }

                    is TransactionResult.NoWalletFound -> fail(result.message, "WALLET_ERROR")
                    is TransactionResult.Failure -> fail(result.message, "WALLET_ERROR")
                }
            } catch (error: TimeoutCancellationException) {
                fail("Wallet did not respond within 90 seconds. The app is unlocked; local session state was not cleared.", "WALLET_TIMEOUT")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error.message ?: "Wallet disconnect was interrupted", "WALLET_ERROR")
            }
        }
    }

    fun forgetLocalConnection() {
        clearConnection()
        _state.value = AllowanceUiState(
            allowanceState = AllowanceState.IDLE,
            decisionReason = "Local wallet session cleared. This does not revoke an onchain allowance.",
            auditEvents = loadEvents(),
            selectedServiceId = CommercialCatalog.byId(_state.value.selectedServiceId).id,
            alphaBriefLiveProofSynced = preferences.getBoolean(KEY_ALPHABRIEF_SYNCED, false),
            locallyPaused = preferences.getBoolean(KEY_LOCAL_PAUSED, false),
        )
        logEvent("LOCAL_SESSION_CLEARED", AllowanceState.IDLE, message = "Local wallet session forgotten")
        setActionFeedback("Local wallet session cleared. Onchain allowances and wallet funds were not changed.")
    }

    fun setActionFeedback(message: String) {
        _state.update { it.copy(actionFeedback = message) }
    }

    fun clearActionFeedback() {
        _state.update { it.copy(actionFeedback = "") }
    }

    private fun setLoading(reason: String? = null) {
        _state.update {
            it.copy(
                loading = true,
                error = "",
                loadingMessage = reason ?: "Waiting for wallet…",
                decisionReason = reason ?: it.decisionReason,
            )
        }
    }

    private fun blockIfLocallyPaused(amount: Double): Boolean {
        if (!_state.value.locallyPaused) return false
        val reason = "Local safety pause blocked this app-side request. No wallet or onchain action was invoked."
        _state.update {
            it.copy(
                allowanceState = AllowanceState.BLOCKED,
                decisionReason = reason,
                requestedAmount = amount,
                signature = "",
                error = "",
            )
        }
        logEvent("LOCAL_PAUSE_BLOCKED_REQUEST", AllowanceState.BLOCKED, amount, reason)
        return true
    }

    private fun fail(message: String, kind: String = "ERROR") {
        _state.update { it.copy(loading = false, loadingMessage = "", error = message) }
        logEvent(kind, AllowanceState.IDLE, message = message)
    }

    private fun failJudgeRun(message: String) {
        _state.update {
            it.copy(
                loading = false,
                loadingMessage = "",
                error = message,
                judgeRunStage = JudgeRunStage.FAILED,
                judgeRunMessage = "Stopped safely: $message",
                actionFeedback = "Judge Run stopped safely. No unverified success is claimed.",
            )
        }
        logEvent("JUDGE_RUN_FAILED", AllowanceState.IDLE, message = message)
    }

    private suspend fun awaitSuccessfulSignature(signature: String): DevnetSignatureStatus {
        var lastFailure: Throwable? = null
        repeat(15) { attempt ->
            try {
                val status = rpc.signatureStatus(signature)
                if (status.succeeded && status.confirmation in CONFIRMED_STATUSES) return status
                lastFailure = IllegalStateException("Transaction is ${status.confirmation}")
            } catch (error: Throwable) {
                lastFailure = error
            }
            if (attempt < 14) delay(1_000)
        }
        error("Settlement was broadcast but did not reach confirmed status: ${lastFailure?.message ?: "unknown status"}")
    }

    private fun persistConnection(publicKey: String, accountLabel: String, authToken: String) {
        secureSession.saveAuthToken(authToken)
        val publicIdentitySaved = preferences.edit()
            .putString(KEY_PUBLIC_KEY, publicKey)
            .putString(KEY_ACCOUNT_LABEL, accountLabel)
            .commit()
        if (!publicIdentitySaved) {
            secureSession.clearAuthToken()
            error("Unable to persist wallet public identity")
        }
        walletAdapter.authToken = authToken
    }

    private fun clearConnection() {
        clearStoredPublicIdentity()
        secureSession.clearAuthToken()
        walletAdapter.authToken = null
    }

    private fun clearStoredPublicIdentity() {
        preferences.edit()
            .remove(KEY_PUBLIC_KEY)
            .remove(KEY_ACCOUNT_LABEL)
            .remove(KEY_AUTH_TOKEN_LEGACY)
            .apply()
    }

    fun clearAuditEvents() {
        preferences.edit().remove(KEY_EVENTS).remove(KEY_ALPHABRIEF_SYNCED).apply()
        _state.update {
            it.copy(
                auditEvents = emptyList(),
                alphaBriefLiveProofSynced = false,
                actionFeedback = "Device-local audit history cleared. Public onchain evidence was not changed.",
            )
        }
    }

    fun auditExport(): String {
        val events = _state.value.auditEvents.joinToString(",\n") { event ->
            """  {"createdAt":${event.createdAt},"kind":${jsonString(event.kind)},"state":${jsonString(event.state.name)},"amount":${event.amount},"serviceId":${jsonString(event.serviceId)},"message":${jsonString(event.message)},"signature":${jsonString(event.signature)}}"""
        }
        return """{
  "schemaVersion": 2,
  "evidenceLevel": "DEVICE_LOCAL_AUDIT",
  "events": [
$events
  ]
}"""
    }

    fun auditFingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(auditExport().encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

    fun verifyDevnetProof() {
        val signature = _state.value.signature.ifBlank { RECORDED_LIVE_SIGNATURE }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    proofCheckLoading = true,
                    proofCheckPassed = null,
                    proofCheckMessage = "Querying Solana Devnet RPC…",
                    proofCheckedSignature = signature,
                )
            }
            runCatching { rpc.signatureStatus(signature) }
                .onSuccess { status ->
                    val passed = status.succeeded && status.confirmation in setOf("CONFIRMED", "FINALIZED")
                    val message = if (passed) {
                        "RPC independently confirmed the transaction without an execution error."
                    } else {
                        "The signature exists, but it is not yet confirmed successfully."
                    }
                    _state.update {
                        it.copy(
                            proofCheckLoading = false,
                            proofCheckPassed = passed,
                            proofCheckSlot = status.slot,
                            proofCheckConfirmation = status.confirmation,
                            proofCheckMessage = message,
                        )
                    }
                    logEvent("PROOF_RPC_VERIFIED", if (passed) AllowanceState.VERIFIED else AllowanceState.FROZEN, message = message, signature = signature)
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to verify the Devnet signature"
                    _state.update {
                        it.copy(
                            proofCheckLoading = false,
                            proofCheckPassed = false,
                            proofCheckSlot = null,
                            proofCheckConfirmation = "",
                            proofCheckMessage = message,
                        )
                    }
                    logEvent("PROOF_RPC_ERROR", AllowanceState.IDLE, message = message, signature = signature)
                }
        }
    }

    fun verifyProgramMatrix() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    programCheckLoading = true,
                    programCheckPassed = null,
                    programCheckMessage = "Reading Program transactions and allowance state…",
                )
            }
            runCatching { rpc.programMatrix() }
                .onSuccess { matrix ->
                    _state.update {
                        it.copy(
                            programCheckLoading = false,
                            programCheckPassed = matrix.passed,
                            programCheckMessage = matrix.message,
                            programSpentInPeriod = matrix.spentInPeriod,
                            programBlockedRejected = matrix.blockedRejected,
                            programFrozenPersisted = matrix.frozenPersisted,
                            programRevokedPersisted = matrix.revokedPersisted,
                            programSettlementVerified = matrix.settlementVerified,
                            programSourceTokenRaw = matrix.sourceTokenRaw,
                            programMerchantTokenRaw = matrix.merchantTokenRaw,
                        )
                    }
                    logEvent(
                        "PROGRAM_MATRIX_VERIFIED",
                        if (matrix.passed) AllowanceState.VERIFIED else AllowanceState.FROZEN,
                        message = matrix.message,
                    )
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            programCheckLoading = false,
                            programCheckPassed = false,
                            programCheckMessage = error.message ?: "Unable to verify Program evidence",
                        )
                    }
                }
        }
    }

    fun receiptSummary(): String {
        val current = _state.value
        val checks = listOf(
            "\"merchantMatches\": ${current.merchantTrusted}",
            "\"evidencePresent\": ${current.evidencePresent}",
            "\"perChargeWithinPolicy\": ${current.requestedAmount <= policy.perChargeCap}",
            "\"periodCapWithinPolicy\": ${current.periodSpent + current.requestedAmount <= policy.periodCap}",
        ).joinToString(",")
        return """
            {
              "receiptVersion": "2",
              "requestId": "${current.requestId}",
              "nonce": ${current.requestNonce},
              "allowanceId": "${policy.allowanceId}",
              "state": "${current.allowanceState.name}",
              "amount": ${current.requestedAmount},
              "periodSpentBefore": ${current.periodSpent},
              "token": "${policy.token}",
              "merchant": "${if (current.merchantTrusted) policy.merchant else "merchant:lookalike"}",
              "policyHash": "$policyHash",
              "evidenceHash": "${current.deliveryEvidenceHash}",
              "evidenceUri": "${if (current.deliveryEvidenceHash.isBlank()) "" else AlphaBriefReference.EVIDENCE_URI}",
              "requestExpiresAt": ${current.requestExpiresAt},
              "checks": {$checks},
              "signature": "${current.signature}",
              "evidenceLevel": "${if (current.commercialSettlementVerified == true && current.signature.isNotBlank()) "LIVE_DEVNET_SETTLEMENT" else if (current.signature.isBlank()) "SIMULATED" else "LIVE_DEVNET_PROOF"}",
              "settlement": {
                "asset": "DEVNET_SOL",
                "lamports": ${current.commercialSettlementLamports},
                "merchant": "${AlphaBriefLiveEvidence.MOBILE_SETTLEMENT_MERCHANT}",
                "merchantBalanceBefore": ${current.commercialMerchantBalanceBefore},
                "merchantBalanceAfter": ${current.commercialMerchantBalanceAfter},
                "merchantBalanceDelta": ${current.commercialMerchantBalanceAfter - current.commercialMerchantBalanceBefore},
                "rpcVerified": ${current.commercialSettlementVerified == true}
              }
            }
        """.trimIndent()
    }

    fun receiptFingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(receiptSummary().encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun logEvent(
        kind: String,
        state: AllowanceState,
        amount: Double = 0.0,
        message: String,
        signature: String = "",
        serviceId: String? = null,
    ) {
        val event = AuditEvent(
            createdAt = System.currentTimeMillis(),
            kind = kind,
            state = state,
            amount = amount,
            message = message.replace("|", "/"),
            signature = signature.replace("|", "/"),
            serviceId = serviceId ?: _state.value.selectedServiceId,
        )
        val events = (listOf(event) + loadEvents()).distinctBy { Triple(it.createdAt, it.kind, it.message) }.take(50)
        preferences.edit().putStringSet(KEY_EVENTS, events.map { encode(it) }.toSet()).apply()
        _state.update { it.copy(auditEvents = events) }
    }

    private fun loadEvents(): List<AuditEvent> = preferences.getStringSet(KEY_EVENTS, emptySet())
        .orEmpty().mapNotNull(::decode).sortedByDescending { it.createdAt }.take(50)

    private fun encode(event: AuditEvent): String = listOf(
        event.createdAt, event.kind, event.state.name, event.amount, event.signature, event.serviceId, event.message,
    ).joinToString("|")

    private fun decode(value: String): AuditEvent? = runCatching {
        val parts = value.split("|", limit = 7)
        if (parts.size >= 7) {
            AuditEvent(
                createdAt = parts[0].toLong(),
                kind = parts[1],
                state = AllowanceState.valueOf(parts[2]),
                amount = parts[3].toDouble(),
                message = parts[6],
                signature = parts[4],
                serviceId = parts[5],
            )
        } else {
            val legacyKind = parts[1]
            AuditEvent(
                createdAt = parts[0].toLong(),
                kind = legacyKind,
                state = AllowanceState.valueOf(parts[2]),
                amount = parts[3].toDouble(),
                message = parts[5],
                signature = parts[4],
                serviceId = if (legacyKind.startsWith("ALPHABRIEF_")) CommercialCatalog.DEFAULT_ID else "",
            )
        }
    }.getOrNull()

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
            }
        }
        append('"')
    }

    companion object {
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_ACCOUNT_LABEL = "account_label"
        private const val KEY_AUTH_TOKEN_LEGACY = "auth_token"
        private const val KEY_EVENTS = "audit_events"
        private const val KEY_LAST_SIGNATURE = "last_signature"
        private const val KEY_SELECTED_SERVICE = "selected_service"
        private const val KEY_LANGUAGE_CHINESE = "language_chinese"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete_v1"
        private const val KEY_ALPHABRIEF_SYNCED = "alphabrief_live_proof_synced_v1"
        private const val KEY_LOCAL_PAUSED = "local_safety_paused"
        private const val KEY_COMMERCIAL_SETTLEMENT_VERIFIED = "commercial_settlement_verified_v1"
        private const val KEY_COMMERCIAL_SETTLEMENT_LAMPORTS = "commercial_settlement_lamports_v1"
        private const val KEY_COMMERCIAL_MERCHANT_BALANCE_BEFORE = "commercial_merchant_balance_before_v1"
        private const val KEY_COMMERCIAL_MERCHANT_BALANCE_AFTER = "commercial_merchant_balance_after_v1"
        private const val WALLET_OPERATION_TIMEOUT_MS = 90_000L
        private val CONFIRMED_STATUSES = setOf("CONFIRMED", "FINALIZED")
        private val JUDGE_RUN_ACTIVE_STAGES = setOf(
            JudgeRunStage.RESETTING,
            JudgeRunStage.VERIFIED,
            JudgeRunStage.BLOCKED,
            JudgeRunStage.FROZEN,
            JudgeRunStage.LIVE_PROOF,
            JudgeRunStage.PROGRAM_MATRIX,
            JudgeRunStage.WALLET_APPROVAL,
        )
        const val RECORDED_LIVE_SIGNATURE = "4w1cjWABu9L9NGMe4NrRTkqFxZVnJBsdket94ifiuKrGaMsMDnYquFpirq4kte4hsCxRuT6Jo79U8zvKNgzQ3B9k"
    }
}
