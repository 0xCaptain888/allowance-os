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

data class AllowanceUiState(
    val loading: Boolean = false,
    val walletAddress: String = "",
    val walletLabel: String = "",
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
)

class AllowanceViewModel(application: Application) : AndroidViewModel(application) {
    val policy = AllowancePolicy()
    val policyHash = PolicyEngine.policyHash(policy)

    private val preferences = application.getSharedPreferences("allowance_os", 0)
    private val rpc = DevnetRpc()
    private val walletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://0xcaptain888.github.io/allowance-os/"),
            iconUri = Uri.parse("favicon.ico"),
            identityName = "Allowance OS",
        ),
    ).apply {
        blockchain = Solana.Devnet
        authToken = preferences.getString(KEY_AUTH_TOKEN, null)
    }

    private val _state = MutableStateFlow(
        AllowanceUiState(
            walletAddress = preferences.getString(KEY_PUBLIC_KEY, "").orEmpty(),
            walletLabel = preferences.getString(KEY_ACCOUNT_LABEL, "").orEmpty(),
            signature = preferences.getString(KEY_LAST_SIGNATURE, "").orEmpty(),
            auditEvents = loadEvents(),
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
                    persistConnection(
                        publicKey = publicKey.base58(),
                        accountLabel = account.accountLabel.orEmpty(),
                        authToken = result.authResult.authToken,
                    )
                    _state.update {
                        it.copy(
                            loading = false,
                            walletAddress = publicKey.base58(),
                            walletLabel = account.accountLabel.orEmpty(),
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

    fun runVerified() = evaluate(
        amount = 1.0,
        merchant = policy.merchant,
        evidence = "research-report-sha256",
    )

    fun runBlocked() = evaluate(
        amount = 10.0,
        merchant = policy.merchant,
        evidence = "research-report-sha256",
    )

    fun runFrozen() = evaluate(
        amount = 1.0,
        merchant = "merchant:lookalike",
        evidence = "research-report-sha256",
    )

    fun runJudgeDemo() {
        evaluate(1.0, policy.merchant, "research-report-sha256", 1.5)
        evaluate(10.0, policy.merchant, "research-report-sha256", 1.5)
        evaluate(1.0, "merchant:lookalike", "research-report-sha256", 1.5)
    }

    fun evaluateCustom(amount: Double, merchantTrusted: Boolean, evidencePresent: Boolean, periodSpent: Double = 0.0) = evaluate(
        amount = amount,
        merchant = if (merchantTrusted) policy.merchant else "merchant:lookalike",
        evidence = if (evidencePresent) "research-report-sha256" else "",
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
            evidence = if (current.evidencePresent) "research-report-sha256" else "",
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
                    persistConnection(
                        publicKey = SolanaPublicKey(account.publicKey).base58(),
                        accountLabel = account.accountLabel.orEmpty(),
                        authToken = result.authResult.authToken,
                    )
                    _state.update {
                        it.copy(
                            loading = false,
                            walletAddress = SolanaPublicKey(account.publicKey).base58(),
                            walletLabel = account.accountLabel.orEmpty(),
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

    fun revoke(sender: ActivityResultSender) {
        viewModelScope.launch {
            setLoading("Requesting MWA deauthorization…")
            when (val result = walletAdapter.disconnect(sender)) {
                is TransactionResult.Success -> {
                    clearConnection()
                    _state.value = AllowanceUiState(
                        allowanceState = AllowanceState.REVOKED,
                        decisionReason = "Wallet authorization was deauthorized through MWA.",
                        auditEvents = loadEvents(),
                    )
                    logEvent("MWA_REVOKED", AllowanceState.REVOKED, message = "MWA authorization deauthorized")
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
            decisionReason = "Local wallet session cleared. Connect your MWA wallet again.",
            auditEvents = loadEvents(),
        )
        logEvent("LOCAL_SESSION_CLEARED", AllowanceState.IDLE, message = "Local wallet session forgotten")
    }

    private fun setLoading(reason: String? = null) {
        _state.update {
            it.copy(
                loading = true,
                error = "",
                decisionReason = reason ?: it.decisionReason,
            )
        }
    }

    private fun fail(message: String, kind: String = "ERROR") {
        _state.update { it.copy(loading = false, error = message) }
        logEvent(kind, AllowanceState.IDLE, message = message)
    }

    private fun persistConnection(publicKey: String, accountLabel: String, authToken: String) {
        preferences.edit()
            .putString(KEY_PUBLIC_KEY, publicKey)
            .putString(KEY_ACCOUNT_LABEL, accountLabel)
            .putString(KEY_AUTH_TOKEN, authToken)
            .apply()
        walletAdapter.authToken = authToken
    }

    private fun clearConnection() {
        preferences.edit()
            .remove(KEY_PUBLIC_KEY)
            .remove(KEY_ACCOUNT_LABEL)
            .remove(KEY_AUTH_TOKEN)
            .apply()
        walletAdapter.authToken = null
    }

    fun clearAuditEvents() {
        preferences.edit().remove(KEY_EVENTS).apply()
        _state.update { it.copy(auditEvents = emptyList()) }
    }

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
              "receiptVersion": "1",
              "allowanceId": "${policy.allowanceId}",
              "state": "${current.allowanceState.name}",
              "amount": ${current.requestedAmount},
              "periodSpentBefore": ${current.periodSpent},
              "token": "${policy.token}",
              "merchant": "${if (current.merchantTrusted) policy.merchant else "merchant:lookalike"}",
              "policyHash": "$policyHash",
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

    companion object {
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_ACCOUNT_LABEL = "account_label"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_EVENTS = "audit_events"
        private const val KEY_LAST_SIGNATURE = "last_signature"
        const val RECORDED_LIVE_SIGNATURE = "4w1cjWABu9L9NGMe4NrRTkqFxZVnJBsdket94ifiuKrGaMsMDnYquFpirq4kte4hsCxRuT6Jo79U8zvKNgzQ3B9k"
    }
}
