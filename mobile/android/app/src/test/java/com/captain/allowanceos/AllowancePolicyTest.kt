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
}
