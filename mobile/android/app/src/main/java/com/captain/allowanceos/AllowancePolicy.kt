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
    val allowanceId: String = "allowance-researchpulse-001",
    val merchant: String = "merchant:researchpulse",
    val token: String = "USDC",
    val perChargeCap: Double = 2.0,
    val periodCap: Double = 8.0,
    val program: String = "Memo111111111111111111111111111111111111111",
)

data class PolicyDecision(
    val state: AllowanceState,
    val reason: String,
)

object PolicyEngine {
    fun evaluate(
        policy: AllowancePolicy,
        amount: Double,
        merchant: String,
        evidence: String,
    ): PolicyDecision = when {
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
}
