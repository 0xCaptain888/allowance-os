package com.captain.allowanceos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AllowancePolicyTest {
    private val policy = AllowancePolicy()

    @Test
    fun validChargeIsVerified() {
        val decision = PolicyEngine.evaluate(policy, 1.0, policy.merchant, "evidence")
        assertEquals(AllowanceState.VERIFIED, decision.state)
    }

    @Test
    fun overCapChargeIsBlocked() {
        val decision = PolicyEngine.evaluate(policy, 10.0, policy.merchant, "evidence")
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }

    @Test
    fun zeroNegativeAndNonFiniteChargesAreBlocked() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { amount ->
            assertEquals(
                "Amount $amount must be rejected",
                AllowanceState.BLOCKED,
                PolicyEngine.evaluate(policy, amount, policy.merchant, "evidence").state,
            )
        }
    }

    @Test
    fun invalidPolicyBudgetIsFrozen() {
        assertEquals(
            AllowanceState.FROZEN,
            PolicyEngine.evaluate(policy.copy(periodCap = 1.0), 1.0, policy.merchant, "evidence").state,
        )
    }

    @Test
    fun periodCapIsEnforcedBeforeWalletInvocation() {
        val decision = PolicyEngine.evaluate(policy, 1.5, policy.merchant, "evidence", periodSpent = 7.0)
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }

    @Test
    fun exactRemainingPeriodBudgetIsVerified() {
        val decision = PolicyEngine.evaluate(policy, 1.0, policy.merchant, "evidence", periodSpent = 7.0)
        assertEquals(AllowanceState.VERIFIED, decision.state)
    }

    @Test
    fun merchantMismatchIsFrozen() {
        val decision = PolicyEngine.evaluate(policy, 1.0, "merchant:lookalike", "evidence")
        assertEquals(AllowanceState.FROZEN, decision.state)
    }

    @Test
    fun policyHashChangesWhenCapChanges() {
        assertNotEquals(
            PolicyEngine.policyHash(policy),
            PolicyEngine.policyHash(policy.copy(perChargeCap = 3.0)),
        )
    }

    @Test
    fun commercialCatalogCoversFiveDistinctUseCases() {
        assertEquals(5, CommercialCatalog.templates.size)
        assertEquals(5, CommercialCatalog.templates.map { it.category }.distinct().size)
        assertEquals(5, CommercialCatalog.templates.map { it.merchant }.distinct().size)
    }

    @Test
    fun everyCommercialTemplateProducesAnEnforceablePolicy() {
        CommercialCatalog.templates.forEach { template ->
            val commercialPolicy = template.toPolicy()
            val decision = PolicyEngine.evaluate(
                commercialPolicy,
                template.perCharge,
                template.merchant,
                "service-delivery-evidence",
            )
            assertEquals("${template.name} should pass its own template", AllowanceState.VERIFIED, decision.state)
        }
    }

    @Test
    fun expiredChargeRequestIsBlocked() {
        val envelope = ChargeEnvelope("req_expired_001", 1, 100, 200, AlphaBriefReference.evidenceHash, AlphaBriefReference.EVIDENCE_URI)
        assertEquals(AllowanceState.BLOCKED, PolicyEngine.evaluateRequest(policy, 1.0, policy.merchant, envelope, 201).state)
    }

    @Test
    fun duplicateEvidenceIsBlockedWithoutASecondPayment() {
        val envelope = ChargeEnvelope("req_replay_001", 2, 100, 500, AlphaBriefReference.evidenceHash, AlphaBriefReference.EVIDENCE_URI)
        val decision = PolicyEngine.evaluateRequest(policy, 1.0, policy.merchant, envelope, 200, setOf(AlphaBriefReference.evidenceHash))
        assertEquals(AllowanceState.BLOCKED, decision.state)
    }
}
