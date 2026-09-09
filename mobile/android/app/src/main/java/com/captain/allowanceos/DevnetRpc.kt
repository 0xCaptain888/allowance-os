package com.captain.allowanceos

import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DevnetSignatureStatus(
    val slot: Long,
    val confirmation: String,
    val succeeded: Boolean,
)

class DevnetRpc {
    private val client = SolanaRpcClient(
        "https://api.devnet.solana.com",
        KtorNetworkDriver(),
    )

    suspend fun latestBlockhash(): String = withContext(Dispatchers.IO) {
        client.getLatestBlockhash().run {
            result?.blockhash ?: error(error?.message ?: "Unable to fetch a Devnet blockhash")
        }
    }

    suspend fun balance(publicKey: SolanaPublicKey): Double = withContext(Dispatchers.IO) {
        val lamports = client.getBalance(publicKey).run {
            result ?: error(error?.message ?: "Unable to fetch wallet balance")
        }
        lamports.toDouble() / 1_000_000_000.0
    }

    suspend fun signatureStatus(signature: String): DevnetSignatureStatus = withContext(Dispatchers.IO) {
        val response = client.getSignatureStatuses(
            signatures = listOf(signature),
            searchTransactionHistory = true,
        )
        val status = response.result?.firstOrNull()
            ?: error(response.error?.message ?: "Signature was not found on Solana Devnet")
        DevnetSignatureStatus(
            slot = status.slot,
            confirmation = status.confirmationStatus?.toString()?.uppercase() ?: "PROCESSED",
            succeeded = status.err == null,
        )
    }
}
