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
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.toUnsignedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bitcoinj.base.Base58
import java.security.MessageDigest

data class AuditEvent(
    val createdAt: Long,
    val kind: String,
    val state: AllowanceState,
    val amount: Double = 0.0,
    val message: String,
    val signature: String = "",
)

enum class WalletSessionState {
    DISCONNECTED,
    CONNECTED,
    REAUTH_REQUIRED,
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
)

class AllowanceViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("allowance_os", 0)
    private val secureSession = SecureSessionStore(application)
    val preferredChinese: Boolean
        get() = preferences.getBoolean(KEY_LANGUAGE_CHINESE, true)
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
            identityUri = Uri.parse("https://0xcaptain888.github.io/allowance-os/"),
            iconUri = Uri.parse("favicon.svg"),
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
        ),
    )
    val state: StateFlow<AllowanceUiState> = _state

    fun connect(sender: ActivityResultSender) {
        viewModelScope.launch {
            setLoading()
            when (val result = walletAdapter.connect(sender)) {
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
                        )
                    }
                    logEvent("WALLET_CONNECTED", AllowanceState.IDLE, message = "MWA wallet authorization succeeded")
                }

                is TransactionResult.NoWalletFound -> fail(result.message, "WALLET_ERROR")
                is TransactionResult.Failure -> fail(result.message, "WALLET_ERROR")
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

    fun runVerified() = evaluate(
        amount = policy.perChargeCap,
        merchant = policy.merchant,
        evidence = "service-delivery-sha256",
    )

    fun runBlocked() = evaluate(
        amount = policy.perChargeCap + maxOf(1.0, policy.perChargeCap),
        merchant = policy.merchant,
        evidence = "service-delivery-sha256",
    )

    fun runFrozen() = evaluate(
        amount = policy.perChargeCap,
        merchant = "merchant:lookalike",
        evidence = "service-delivery-sha256",
    )

    fun runJudgeDemo() {
        val amount = policy.perChargeCap
        evaluate(amount, policy.merchant, "service-delivery-sha256", 0.0)
        evaluate(amount + maxOf(1.0, amount), policy.merchant, "service-delivery-sha256", 0.0)
        evaluate(amount, "merchant:lookalike", "service-delivery-sha256", 0.0)
    }

    fun runAlphaBriefDelivery() {
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

    fun evaluateCustom(amount: Double, merchantTrusted: Boolean, evidencePresent: Boolean, periodSpent: Double = 0.0) = evaluate(
        amount = amount,
        merchant = if (merchantTrusted) policy.merchant else "merchant:lookalike",
        evidence = if (evidencePresent) "service-delivery-sha256" else "",
        periodSpent = periodSpent,
    )

    private fun evaluate(amount: Double, merchant: String, evidence: String, periodSpent: Double = 0.0) {
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
                    _state.update { it.copy(loading = false, solBalance = balance, error = "") }
                }
                .onFailure { error -> fail(error.message ?: "Unable to refresh Devnet balance", "BALANCE_ERROR") }
        }
    }

    fun publishDevnetProof(sender: ActivityResultSender) {
        val current = _state.value
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

        viewModelScope.launch {
            setLoading("Policy passed. Approve the Devnet authorization proof in your wallet.")
            when (val result = walletAdapter.transact(sender) { authorization ->
                val publicKey = SolanaPublicKey(authorization.accounts.first().publicKey)
                val memo = listOf(
                    "ALLOWANCE_OS",
                    "CREATE",
                    policy.allowanceId,
                    "policy=$policyHash",
                    "merchant=${policy.merchant}",
                    "amount=${current.requestedAmount}",
                    "periodSpent=${current.periodSpent}",
                    "cap=${policy.perChargeCap}-${policy.token}",
                ).joinToString("|")

                val transaction = Message.Builder()
                    .setRecentBlockhash(rpc.latestBlockhash())
                    .addInstruction(MemoProgram.publishMemo(publicKey, memo))
                    .build()
                    .toUnsignedTransaction()

                val signatureBytes = signAndSendTransactions(arrayOf(transaction.serialize()))
                    .signatures
                    .first()
                Base58.encode(signatureBytes)
            }) {
                is TransactionResult.Success -> {
                    val account = result.authResult.accounts.first()
                    try {
                        persistConnection(
                            publicKey = SolanaPublicKey(account.publicKey).base58(),
                            accountLabel = account.accountLabel.orEmpty(),
                            authToken = result.authResult.authToken,
                        )
                    } catch (error: Exception) {
                        clearConnection()
                        fail(
                            "The proof was broadcast, but the encrypted reconnect session could not be saved. Verify the signature, then reconnect the wallet.",
                            "WALLET_SESSION_STORAGE_ERROR",
                        )
                        preferences.edit().putString(KEY_LAST_SIGNATURE, result.payload).apply()
                        _state.update { it.copy(signature = result.payload) }
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            walletAddress = SolanaPublicKey(account.publicKey).base58(),
                            walletLabel = account.accountLabel.orEmpty(),
                            walletSessionState = WalletSessionState.CONNECTED,
                            allowanceState = AllowanceState.VERIFIED,
                            decisionReason = "Policy passed and the wallet broadcast a real Devnet Memo authorization proof.",
                            signature = result.payload,
                            error = "",
                        )
                    }
                    preferences.edit().putString(KEY_LAST_SIGNATURE, result.payload).apply()
                    logEvent("DEVNET_MEMO_BROADCAST", AllowanceState.VERIFIED, current.requestedAmount, "Real Devnet Memo authorization proof", result.payload)
                }

                is TransactionResult.NoWalletFound -> fail(result.message, "WALLET_ERROR")
                is TransactionResult.Failure -> fail(result.message, "WALLET_ERROR")
            }
        }
    }

    fun disconnectWalletSession(sender: ActivityResultSender) {
        viewModelScope.launch {
            setLoading("Requesting MWA wallet disconnect…")
            when (val result = walletAdapter.disconnect(sender)) {
                is TransactionResult.Success -> {
                    clearConnection()
                    _state.value = AllowanceUiState(
                        allowanceState = AllowanceState.IDLE,
                        decisionReason = "MWA wallet session was deauthorized. Any onchain allowance remains unchanged until separately revoked.",
                        auditEvents = loadEvents(),
                        selectedServiceId = CommercialCatalog.byId(_state.value.selectedServiceId).id,
                    )
                    logEvent("MWA_DISCONNECTED", AllowanceState.IDLE, message = "MWA wallet session deauthorized; onchain allowance unchanged")
                }

                is TransactionResult.NoWalletFound -> fail(result.message, "WALLET_ERROR")
                is TransactionResult.Failure -> fail(result.message, "WALLET_ERROR")
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
        )
        logEvent("LOCAL_SESSION_CLEARED", AllowanceState.IDLE, message = "Local wallet session forgotten")
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

    private fun fail(message: String, kind: String = "ERROR") {
        _state.update { it.copy(loading = false, loadingMessage = "", error = message) }
        logEvent(kind, AllowanceState.IDLE, message = message)
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
        preferences.edit().remove(KEY_EVENTS).apply()
        _state.update { it.copy(auditEvents = emptyList()) }
    }

    fun auditExport(): String {
        val events = _state.value.auditEvents.joinToString(",\n") { event ->
            """  {"createdAt":${event.createdAt},"kind":${jsonString(event.kind)},"state":${jsonString(event.state.name)},"amount":${event.amount},"message":${jsonString(event.message)},"signature":${jsonString(event.signature)}}"""
        }
        return """{
  "schemaVersion": 1,
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
              "evidenceLevel": "${if (current.signature.isBlank()) "SIMULATED" else "LIVE_DEVNET_PROOF"}"
            }
        """.trimIndent()
    }

    fun receiptFingerprint(): String = MessageDigest.getInstance("SHA-256")
        .digest(receiptSummary().encodeToByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun logEvent(kind: String, state: AllowanceState, amount: Double = 0.0, message: String, signature: String = "") {
        val event = AuditEvent(System.currentTimeMillis(), kind, state, amount, message.replace("|", "/"), signature.replace("|", "/"))
        val events = (listOf(event) + loadEvents()).distinctBy { Triple(it.createdAt, it.kind, it.message) }.take(50)
        preferences.edit().putStringSet(KEY_EVENTS, events.map { encode(it) }.toSet()).apply()
        _state.update { it.copy(auditEvents = events) }
    }

    private fun loadEvents(): List<AuditEvent> = preferences.getStringSet(KEY_EVENTS, emptySet())
        .orEmpty().mapNotNull(::decode).sortedByDescending { it.createdAt }.take(50)

    private fun encode(event: AuditEvent): String = listOf(
        event.createdAt, event.kind, event.state.name, event.amount, event.signature, event.message,
    ).joinToString("|")

    private fun decode(value: String): AuditEvent? = runCatching {
        val parts = value.split("|", limit = 6)
        AuditEvent(parts[0].toLong(), parts[1], AllowanceState.valueOf(parts[2]), parts[3].toDouble(), parts[5], parts[4])
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
        const val RECORDED_LIVE_SIGNATURE = "4w1cjWABu9L9NGMe4NrRTkqFxZVnJBsdket94ifiuKrGaMsMDnYquFpirq4kte4hsCxRuT6Jo79U8zvKNgzQ3B9k"
    }
}
