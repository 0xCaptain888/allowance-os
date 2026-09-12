package com.captain.allowanceos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyHabitsTest {
    @Test
    fun `dashboard counts only real settlement event kinds as spend`() {
        val now = 1_800_000_000_000L
        val events = listOf(
            AuditEvent(now, "POLICY_DECISION", AllowanceState.VERIFIED, 99.0, "simulation"),
            AuditEvent(now, "ALPHABRIEF_LIVE_SETTLED", AllowanceState.VERIFIED, 2.0, "public proof"),
        )
        val result = DailyHabitsEngine.snapshot(events, CommercialCatalog.byId("alphabrief"), now)
        assertEquals(2.0, result.todaySpend, 0.001)
        assertEquals(2.0, result.weekSpend, 0.001)
        assertEquals(1, result.verifiedDeliveries)
    }

    @Test
    fun `freeze raises merchant anomaly without adding spend`() {
        val now = 1_800_000_000_000L
        val result = DailyHabitsEngine.snapshot(
            listOf(AuditEvent(now, "ALPHABRIEF_BAD_OUTPUT_FROZEN", AllowanceState.FROZEN, 0.0, "bad output")),
            CommercialCatalog.byId("alphabrief"),
            now,
        )
        assertTrue(result.merchantAnomaly)
        assertEquals(1, result.frozenEvents)
        assertEquals(0.0, result.weekSpend, 0.001)
    }

    @Test
    fun `weekly report discloses local accounting boundary`() {
        val snapshot = DailyHabitsEngine.snapshot(emptyList(), CommercialCatalog.byId("alphabrief"), 1_800_000_000_000L)
        val report = DailyHabitsEngine.weeklyReport(snapshot, locallyPaused = false)
        assertTrue(report.contains("DEVICE_LOCAL_AUDIT"))
        assertTrue(report.contains("not an onchain accounting oracle"))
        assertFalse(report.contains("Mainnet"))
    }

    @Test
    fun `settlement spend is isolated to its commercial service`() {
        val now = 1_800_000_000_000L
        val events = listOf(
            AuditEvent(
                createdAt = now,
                kind = "ALPHABRIEF_LIVE_SETTLED",
                state = AllowanceState.VERIFIED,
                amount = 2.0,
                message = "public proof",
                serviceId = "alphabrief",
            ),
        )

        val alphaBrief = DailyHabitsEngine.snapshot(events, CommercialCatalog.byId("alphabrief"), now)
        val agentCloud = DailyHabitsEngine.snapshot(events, CommercialCatalog.byId("agentcloud"), now)

        assertEquals(2.0, alphaBrief.weekSpend, 0.001)
        assertEquals(0.0, agentCloud.weekSpend, 0.001)
        assertEquals(0, agentCloud.verifiedDeliveries)
    }
}
