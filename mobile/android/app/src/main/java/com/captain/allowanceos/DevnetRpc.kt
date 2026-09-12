package com.captain.allowanceos

import com.solana.networking.KtorNetworkDriver
import com.solana.publickey.SolanaPublicKey
import com.solana.rpc.SolanaRpcClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    val settlementVerified: Boolean,
    val spentInPeriod: ULong,
    val sourceTokenRaw: ULong,
    val merchantTokenRaw: ULong,
    val message: String,
)

fun parseRpcEndpoints(raw: String): List<String> = raw
    .split(',')
    .map(String::trim)
    .filter(String::isNotBlank)
    .distinct()

class DevnetRpc(
    endpoints: List<String> = parseRpcEndpoints(BuildConfig.SOLANA_RPC_URLS),
    private val attemptsPerEndpoint: Int = 3,
) {
    private val clients = endpoints.map(String::trim).filter(String::isNotBlank).distinct().map {
        SolanaRpcClient(it, KtorNetworkDriver())
    }

    init {
        require(clients.isNotEmpty()) { "At least one Solana Devnet RPC endpoint is required" }
        require(attemptsPerEndpoint in 1..5) { "RPC attempts must be between 1 and 5" }
    }

    suspend fun latestBlockhash(): String = withContext(Dispatchers.IO) {
        withRpcFailover { client ->
            client.getLatestBlockhash().run {
                result?.blockhash ?: error(error?.message ?: "Unable to fetch a Devnet blockhash")
            }
        }
    }

    suspend fun balanceLamports(publicKey: SolanaPublicKey): Long = withContext(Dispatchers.IO) {
        withRpcFailover { client ->
            client.getBalance(publicKey).run {
                result ?: error(error?.message ?: "Unable to fetch wallet balance")
            }
        }
    }

    suspend fun balance(publicKey: SolanaPublicKey): Double =
        balanceLamports(publicKey).toDouble() / 1_000_000_000.0

    suspend fun signatureStatus(signature: String): DevnetSignatureStatus = withContext(Dispatchers.IO) {
        withRpcFailover { client ->
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

    suspend fun programMatrix(): DevnetProgramMatrix = withContext(Dispatchers.IO) {
        withRpcFailover { client -> programMatrix(client) }
    }

    /** Read the deployed Delegated Settlement v2 allowance used by the public control matrix. */
    suspend fun delegatedV2Allowance(): DelegatedAllowanceSnapshot = withContext(Dispatchers.IO) {
        withRpcFailover { client ->
            val response = client.getAccountInfo(
                SolanaPublicKey(Base58.decode(DelegatedAllowanceV2.ALLOWANCE_ACCOUNT)),
            )
            val account = response.result
                ?: error(response.error?.message ?: "Delegated v2 allowance was not found")
            check(account.owner.base58() == DelegatedAllowanceV2.PROGRAM_ID) {
                "Delegated v2 allowance is owned by an unexpected Program"
            }
            val data = account.data ?: error("Delegated v2 allowance contains no data")
            DelegatedAllowanceV2.parseState(data)
        }
    }

    private suspend fun programMatrix(client: SolanaRpcClient): DevnetProgramMatrix {
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

        val sourceToken = tokenAccount(client, SOURCE_TOKEN_ACCOUNT, SOURCE_OWNER)
        val merchantToken = tokenAccount(client, MERCHANT_TOKEN_ACCOUNT, MERCHANT_OWNER)
        val settlementVerified = sourceToken == 19_000_000uL && merchantToken == 1_000_000uL
        val passed = createdSucceeded && verifiedSucceeded && blockedRejected && frozenSucceeded &&
            revokedSucceeded && frozenPersisted && revokedPersisted && spentInPeriod == 1_000_000uL &&
            ownerMatches && settlementVerified

        return DevnetProgramMatrix(
            passed = passed,
            blockedRejected = blockedRejected,
            frozenPersisted = frozenPersisted,
            revokedPersisted = revokedPersisted,
            settlementVerified = settlementVerified,
            spentInPeriod = spentInPeriod,
            sourceTokenRaw = sourceToken,
            merchantTokenRaw = merchantToken,
            message = if (passed) {
                "Five public transactions, the final allowance state, and the SPL-token settlement balances match."
            } else {
                "The evidence exists, but one or more Program state or token-settlement checks did not match."
            },
        )
    }

    private suspend fun tokenAccount(client: SolanaRpcClient, address: String, expectedOwner: String): ULong {
        val response = client.getAccountInfo(SolanaPublicKey(Base58.decode(address)))
        val account = response.result ?: error(response.error?.message ?: "Token account was not found")
        val data = account.data ?: error("Token account contains no data")
        if (data.size < TOKEN_ACCOUNT_SIZE) error("Token account data is shorter than expected")
        if (account.owner.base58() != TOKEN_PROGRAM_ID) error("Account is not owned by the SPL Token Program")
        if (Base58.encode(data.copyOfRange(0, 32)) != TOKEN_MINT) error("Token mint does not match the policy")
        if (Base58.encode(data.copyOfRange(32, 64)) != expectedOwner) error("Token owner does not match the policy")
        return data.readU64Le(TOKEN_AMOUNT_OFFSET)
    }

    private suspend fun <T> withRpcFailover(operation: suspend (SolanaRpcClient) -> T): T {
        var lastFailure: Throwable? = null
        clients.forEach { client ->
            repeat(attemptsPerEndpoint) { attempt ->
                try {
                    return operation(client)
                } catch (error: Throwable) {
                    lastFailure = error
                    if (attempt + 1 < attemptsPerEndpoint) delay(250L shl attempt)
                }
            }
        }
        throw IllegalStateException(
            "All configured Solana Devnet RPC attempts failed: ${lastFailure?.message ?: "unknown RPC failure"}",
            lastFailure,
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
        const val ALLOWANCE_ACCOUNT = "BRqgbZzdZPrueoWEcjusZ49etotWfNGtu2Bs1HiQ7E5x"
        const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        const val TOKEN_MINT = "3KVq4nkUnb7GS7DjYCaGR7JJGAhDG1YPsz84xxThn5de"
        const val SOURCE_OWNER = "ETJL7fK6CkaYsjrfXm6NhyNcztes8PcZKJ6jK3xGgMXF"
        const val MERCHANT_OWNER = "D3XJqkeFiPNtuwKkyeJfVG1Gjvi88AV6fiNs29ukjKm6"
        const val SOURCE_TOKEN_ACCOUNT = "9JxQGW1dMBtpq2qoWubS24QKkfYxtySvavgtnzSGf4i7"
        const val MERCHANT_TOKEN_ACCOUNT = "56UFXEt2H4wLNCDuWjwaeRJYuJE5r52ohmjjG35BSbbb"
        const val CREATED_SIGNATURE = "kmeiVR39tEkX1Rgo4pkNHz784anFvi3eX1J3YYeyry6pAPHKwvJNsEMaxqSLo1grJkZ5fSrNmKoQrMiVdTnrHYH"
        const val VERIFIED_SIGNATURE = "22xKvkfk2YwEV6mVQXmhGeGFWSSTfvE9FGPLGJ7McSf93DpxuntZYLE8mjdv7SugEmMoo3rvswKa7eMfzf9nSPUU"
        const val BLOCKED_SIGNATURE = "EU6bUcBTdpxCMu9rRBhDqQjPwnvPLtGmDtKepZnRZK5rKnRdeiWGqVvpKerPRAheddBgkhmpQgNdcTVZCCgvted"
        const val FROZEN_SIGNATURE = "5REqbiziiN5bAWPDYDMSq7TgS3rvMVUeW9UmqfMdzpfCPxYctWxeQa83beR14tHbSSdTRWg1P7jbWasE5U37MLpu"
        const val REVOKED_SIGNATURE = "5MpttoR2mfpDXhWq1B3nr6AWHC9AFCTsQ7626EXDEka6nWzgF3GdJGZnTxk9BmU4sBxiLjAAbbES9GXheGCqqysu"
        private const val STATE_SIZE = 259
        private const val TOKEN_ACCOUNT_SIZE = 165
        private const val TOKEN_AMOUNT_OFFSET = 64
        private const val SPENT_OFFSET = 145
        private const val REVOKED_OFFSET = 161
        private const val FROZEN_OFFSET = 162
    }
}
