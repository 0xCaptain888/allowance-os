package com.captain.allowanceos

import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}
