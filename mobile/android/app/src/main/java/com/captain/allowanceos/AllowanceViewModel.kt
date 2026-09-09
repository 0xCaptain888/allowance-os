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

data class AllowanceUiState(
    val loading: Boolean = false,
    val walletAddress: String = "",
    val walletLabel: String = "",
    val solBalance: Double? = null,
    val allowanceState: AllowanceState = AllowanceState.IDLE,
    val decisionReason: String = "Connect a Devnet wallet or replay a policy outcome.",
    val signature: String = "",
    val error: String = "",
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
                }

                is TransactionResult.NoWalletFound -> fail(result.message)
                is TransactionResult.Failure -> fail(result.message)
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

    private fun evaluate(amount: Double, merchant: String, evidence: String) {
        val decision = PolicyEngine.evaluate(policy, amount, merchant, evidence)
        _state.update {
            it.copy(
                allowanceState = decision.state,
                decisionReason = decision.reason,
                signature = "",
                error = "",
            )
        }
    }

    fun publishDevnetProof(sender: ActivityResultSender) {
        val preflight = PolicyEngine.evaluate(
            policy = policy,
            amount = 1.0,
            merchant = policy.merchant,
            evidence = "research-report-sha256",
        )
        if (preflight.state != AllowanceState.VERIFIED) {
            _state.update { it.copy(allowanceState = preflight.state, decisionReason = preflight.reason) }
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
                }

                is TransactionResult.NoWalletFound -> fail(result.message)
                is TransactionResult.Failure -> fail(result.message)
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
                    )
                }

                is TransactionResult.NoWalletFound -> fail(result.message)
                is TransactionResult.Failure -> fail(result.message)
            }
        }
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

    private fun fail(message: String) {
        _state.update { it.copy(loading = false, error = message) }
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
        preferences.edit().clear().apply()
        walletAdapter.authToken = null
    }

    companion object {
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_ACCOUNT_LABEL = "account_label"
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
