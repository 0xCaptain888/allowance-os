package com.captain.allowanceos

import java.security.MessageDigest

enum class AllowanceState {
    IDLE,
    VERIFIED,
    BLOCKED,
    FROZEN,
    REVOKED,
}

data class AllowancePolicy(
    val allowanceId: String = "allowance-alphabrief-001",
    val merchant: String = "merchant:alphabrief",
    val token: String = "USDC",
    val perChargeCap: Double = 2.0,
    val periodCap: Double = 8.0,
    val program: String = "Memo111111111111111111111111111111111111111",
)

data class PolicyDecision(
    val state: AllowanceState,
    val reason: String,
)

data class ChargeEnvelope(
    val requestId: String,
    val nonce: Long,
    val requestedAtMillis: Long,
    val expiresAtMillis: Long,
    val evidenceHash: String,
    val evidenceUri: String,
)

object PolicyEngine {
    fun evaluate(
        policy: AllowancePolicy,
        amount: Double,
        merchant: String,
        evidence: String,
        periodSpent: Double = 0.0,
    ): PolicyDecision = when {
        !policy.perChargeCap.isFinite() || policy.perChargeCap <= 0.0 ||
            !policy.periodCap.isFinite() || policy.periodCap < policy.perChargeCap -> PolicyDecision(
            AllowanceState.FROZEN,
            "Allowance policy has invalid budget boundaries; freeze until it is corrected.",
        )

        !amount.isFinite() || amount <= 0.0 -> PolicyDecision(
            AllowanceState.BLOCKED,
            "Charge amount must be a positive finite number.",
        )

        !periodSpent.isFinite() || periodSpent < 0.0 -> PolicyDecision(
            AllowanceState.BLOCKED,
            "Current-period spend must be a non-negative finite number.",
        )

        merchant != policy.merchant -> PolicyDecision(
            AllowanceState.FROZEN,
            "Merchant identity changed; freeze and require explicit recovery.",
        )

        evidence.isBlank() -> PolicyDecision(
            AllowanceState.FROZEN,
            "Evidence is missing; a successful tool call is not enough for payment.",
        )

        amount > policy.perChargeCap -> PolicyDecision(
            AllowanceState.BLOCKED,
            "Charge exceeds the ${policy.perChargeCap} ${policy.token} per-charge limit.",
        )

        periodSpent + amount > policy.periodCap -> PolicyDecision(
            AllowanceState.BLOCKED,
            "Charge would exceed the ${policy.periodCap} ${policy.token} period cap (${periodSpent} already spent).",
        )

        else -> PolicyDecision(
            AllowanceState.VERIFIED,
            "Merchant, evidence, token, program, and budget checks passed.",
        )
    }

    fun policyHash(policy: AllowancePolicy): String {
        val canonical = listOf(
            policy.allowanceId,
            policy.merchant,
            policy.token,
            policy.perChargeCap.toString(),
            policy.periodCap.toString(),
            policy.program,
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun evaluateRequest(
        policy: AllowancePolicy,
        amount: Double,
        merchant: String,
        envelope: ChargeEnvelope,
        nowMillis: Long,
        usedEvidenceHashes: Set<String> = emptySet(),
    ): PolicyDecision = when {
        envelope.requestId.length < 8 -> PolicyDecision(AllowanceState.BLOCKED, "Request ID is invalid.")
        envelope.nonce < 0 -> PolicyDecision(AllowanceState.BLOCKED, "Nonce is invalid.")
        envelope.expiresAtMillis <= nowMillis || envelope.expiresAtMillis <= envelope.requestedAtMillis ->
            PolicyDecision(AllowanceState.BLOCKED, "Charge request expired before settlement.")
        envelope.evidenceHash in usedEvidenceHashes ->
            PolicyDecision(AllowanceState.BLOCKED, "Evidence replay rejected; no second payment is permitted.")
        else -> evaluate(policy, amount, merchant, envelope.evidenceHash)
    }

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.encodeToByteArray())
        .joinToString("") { "%02x".format(it) }
}

object AlphaBriefReference {
    const val REPORT = "Allowance-based payments let users approve bounded research purchases without granting unlimited merchant authority."
    const val EVIDENCE_URI = "app://embedded/alphabrief-report-v1"
    val evidenceHash: String get() = PolicyEngine.sha256(REPORT)
}
