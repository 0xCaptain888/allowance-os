package com.captain.allowanceos

import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.TransactionInstruction
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DelegatedAllowanceSnapshot(
    val version: Int,
    val authority: String,
    val merchant: String,
    val executor: String,
    val verifier: String,
    val tokenMint: String,
    val sourceToken: String,
    val perChargeRaw: ULong,
    val periodCapRaw: ULong,
    val lifetimeCapRaw: ULong,
    val spentInPeriodRaw: ULong,
    val spentLifetimeRaw: ULong,
    val periodStartedAt: Long,
    val periodSeconds: Long,
    val expiresAt: Long,
    val nextNonce: ULong,
    val paused: Boolean,
    val revoked: Boolean,
    val frozen: Boolean,
    val policyHash: String,
    val lastEvidenceHash: String,
)

object DelegatedAllowanceV2 {
    const val STATE_SIZE = 332
    const val PROGRAM_ID = "7zARKWKDLawLgR7qokvQdkAv6ye2cXGNEvNQswBd6xvL"

    fun parseState(data: ByteArray): DelegatedAllowanceSnapshot {
        require(data.size >= STATE_SIZE) { "Delegated allowance state is shorter than $STATE_SIZE bytes" }
        val reader = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        fun publicKey(): String = SolanaPublicKey(ByteArray(32).also { reader.get(it) }).base58()
        fun digest(): String = ByteArray(32).also { reader.get(it) }.joinToString("") { "%02x".format(it) }
        fun bool(label: String): Boolean = when (val value = reader.get().toInt()) {
            0 -> false
            1 -> true
            else -> error("Invalid $label flag: $value")
        }
        val snapshot = DelegatedAllowanceSnapshot(
            version = reader.get().toInt() and 0xff,
            authority = publicKey(),
            merchant = publicKey(),
            executor = publicKey(),
            verifier = publicKey(),
            tokenMint = publicKey(),
            sourceToken = publicKey(),
            perChargeRaw = reader.long.toULong(),
            periodCapRaw = reader.long.toULong(),
            lifetimeCapRaw = reader.long.toULong(),
            spentInPeriodRaw = reader.long.toULong(),
            spentLifetimeRaw = reader.long.toULong(),
            periodStartedAt = reader.long,
            periodSeconds = reader.long,
            expiresAt = reader.long,
            nextNonce = reader.long.toULong(),
            paused = bool("paused"),
            revoked = bool("revoked"),
            frozen = bool("frozen"),
            policyHash = digest(),
            lastEvidenceHash = digest(),
        )
        require(snapshot.version == 2) { "Unsupported delegated allowance version: ${snapshot.version}" }
        require(snapshot.perChargeRaw > 0uL && snapshot.perChargeRaw <= snapshot.periodCapRaw) {
            "Invalid delegated per-charge/period limits"
        }
        require(snapshot.periodCapRaw <= snapshot.lifetimeCapRaw) { "Invalid delegated lifetime limit" }
        require(snapshot.spentInPeriodRaw <= snapshot.periodCapRaw) { "Period spend exceeds the committed cap" }
        require(snapshot.spentLifetimeRaw <= snapshot.lifetimeCapRaw) { "Lifetime spend exceeds the committed cap" }
        return snapshot
    }

    fun pause(programId: String, authority: String, allowance: String) = authorityControl(5, programId, authority, allowance)
    fun unpause(programId: String, authority: String, allowance: String) = authorityControl(6, programId, authority, allowance)

    fun revoke(programId: String, authority: String, allowance: String, sourceToken: String): TransactionInstruction =
        TransactionInstruction(
            SolanaPublicKey.from(programId),
            listOf(
                meta(authority, signer = true),
                meta(allowance, writable = true),
                meta(sourceToken, writable = true),
                meta(DevnetRpc.TOKEN_PROGRAM_ID),
            ),
            byteArrayOf(9),
        )

    fun rotateExecutor(programId: String, authority: String, allowance: String, newExecutor: String): TransactionInstruction =
        TransactionInstruction(
            SolanaPublicKey.from(programId),
            listOf(meta(authority, signer = true), meta(allowance, writable = true)),
            byteArrayOf(10) + SolanaPublicKey.from(newExecutor).bytes,
        )

    fun rotateVerifier(
        programId: String,
        authority: String,
        currentVerifier: String,
        allowance: String,
        newVerifier: String,
    ): TransactionInstruction = TransactionInstruction(
        SolanaPublicKey.from(programId),
        listOf(
            meta(authority, signer = true),
            meta(currentVerifier, signer = true),
            meta(allowance, writable = true),
        ),
        byteArrayOf(11) + SolanaPublicKey.from(newVerifier).bytes,
    )

    private fun authorityControl(discriminant: Int, programId: String, authority: String, allowance: String) =
        TransactionInstruction(
            SolanaPublicKey.from(programId),
            listOf(meta(authority, signer = true), meta(allowance, writable = true)),
            byteArrayOf(discriminant.toByte()),
        )

    private fun meta(address: String, signer: Boolean = false, writable: Boolean = false) =
        AccountMeta(SolanaPublicKey.from(address), signer, writable)
}
