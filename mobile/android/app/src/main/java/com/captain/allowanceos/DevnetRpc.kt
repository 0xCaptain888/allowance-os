package com.captain.allowanceos

import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoinj.base.Base58

data class DevnetSignatureStatus(
    val slot: Long,
    val confirmation: String,
    val succeeded: Boolean,
)

data class DevnetProgramMatrix(
    val passed: Boolean,
    val blockedRejected: Boolean,
    val frozenPersisted: Boolean,
    val revokedPersisted: Boolean,
    val spentInPeriod: ULong,
    val message: String,
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

    suspend fun programMatrix(): DevnetProgramMatrix = withContext(Dispatchers.IO) {
        val signatures = listOf(
            CREATED_SIGNATURE,
            VERIFIED_SIGNATURE,
            BLOCKED_SIGNATURE,
            FROZEN_SIGNATURE,
            REVOKED_SIGNATURE,
        )
        val statusesResponse = client.getSignatureStatuses(signatures, searchTransactionHistory = true)
        val statuses = statusesResponse.result
            ?: error(statusesResponse.error?.message ?: "Unable to fetch Program transaction statuses")
        if (statuses.size != signatures.size || statuses.any { it == null }) {
            error("One or more Program transactions were not found on Solana Devnet")
        }

        val createdSucceeded = statuses[0]?.err == null
        val verifiedSucceeded = statuses[1]?.err == null
        val blockedRejected = statuses[2]?.err != null
        val frozenSucceeded = statuses[3]?.err == null
        val revokedSucceeded = statuses[4]?.err == null

        val accountResponse = client.getAccountInfo(
            SolanaPublicKey(Base58.decode(ALLOWANCE_ACCOUNT)),
        )
        val account = accountResponse.result
            ?: error(accountResponse.error?.message ?: "Allowance account was not found")
        val data = account.data ?: error("Allowance account contains no data")
        if (data.size < STATE_SIZE) error("Allowance account data is shorter than expected")

        val spentInPeriod = data.readU64Le(SPENT_OFFSET)
        val revokedPersisted = data[REVOKED_OFFSET].toInt() == 1
        val frozenPersisted = data[FROZEN_OFFSET].toInt() == 1
        val ownerMatches = account.owner.base58() == PROGRAM_ID
        val passed = createdSucceeded && verifiedSucceeded && blockedRejected && frozenSucceeded &&
            revokedSucceeded && frozenPersisted && revokedPersisted && spentInPeriod == 1_000_000uL && ownerMatches

        DevnetProgramMatrix(
            passed = passed,
            blockedRejected = blockedRejected,
            frozenPersisted = frozenPersisted,
            revokedPersisted = revokedPersisted,
            spentInPeriod = spentInPeriod,
            message = if (passed) {
                "Five public transactions and the final allowance account match the expected Program state."
            } else {
                "The Program evidence exists, but one or more expected state checks did not match."
            },
        )
    }

    private fun ByteArray.readU64Le(offset: Int): ULong {
        var result = 0uL
        repeat(8) { index ->
            result = result or (this[offset + index].toUByte().toULong() shl (index * 8))
        }
        return result
    }

    companion object {
        const val PROGRAM_ID = "DJzPBS7FreCcWWGkApzznGcKq9T7Da38GpKFtpxWRcuE"
        const val ALLOWANCE_ACCOUNT = "4AzXfvZ6Ks2QFZFPTyAAzLL3vUWjzqUJguBvNUeoed9E"
        const val CREATED_SIGNATURE = "okfc2Z9S2ehRgLxtVrRKwZoB3KPJJWTJf7Zz6xDdTkwMvneCfJ3qzV42p4hWUeZ3NTQzwoZaS4MLeUtgy5K2bTQ"
        const val VERIFIED_SIGNATURE = "4fMAWn5T4gAjJz5ragh66eaNjk7hXfxWhjdSx2ojkDK4GAwNWhaZoNNdGKsvwngJXcz3LHN7HEWAnQWsP7f9JNfJ"
        const val BLOCKED_SIGNATURE = "5r8Cd9ajUmWoSmVsMar5S5wdxrNL6C8aFfQ58GBpCGqJEGJGQUdhDLbKBPUqsURbQF92UWM3DmRR5Lnb1KA77QV5"
        const val FROZEN_SIGNATURE = "3qHEpGrhwoVFkJfQj3ndr5bvKmebnTtJCE86bP5SHZo1Xx7wzMis8XNcb5dHYb7FWm8tajFaGrGG8ygmVWcJvcWF"
        const val REVOKED_SIGNATURE = "5gVQm2depgBZrMrMZc84ZGdvVVGfuwmmYeSyh5G9q1PbzTsNhfxoXyEdQ7ZsNGdcFAaWyPzcdesrQkYmSbT9CBr7"
        private const val STATE_SIZE = 259
        private const val SPENT_OFFSET = 145
        private const val REVOKED_OFFSET = 161
        private const val FROZEN_OFFSET = 162
    }
}
