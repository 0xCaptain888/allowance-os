package com.captain.allowanceos

import java.util.Calendar

data class UpcomingCharge(
    val serviceName: String,
    val merchant: String,
    val amount: Double,
    val token: String,
    val dueAt: Long,
    val evidenceRequired: Boolean,
)

data class DailyHabitsSnapshot(
    val todaySpend: Double,
    val weekSpend: Double,
    val blockedSpend: Double,
    val budgetUsedRatio: Double,
    val budgetPressure: Boolean,
    val merchantAnomaly: Boolean,
    val verifiedDeliveries: Int,
    val frozenEvents: Int,
    val upcomingCharges: List<UpcomingCharge>,
)

object DailyHabitsEngine {
    private val settlementKinds = setOf("ALPHABRIEF_LIVE_SETTLED", "LIVE_SETTLEMENT_RECORDED")

    fun snapshot(
        events: List<AuditEvent>,
        service: CommercialServiceTemplate,
        now: Long = System.currentTimeMillis(),
    ): DailyHabitsSnapshot {
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val weekStart = now - 7L * 24L * 60L * 60L * 1_000L
        val serviceEvents = events.filter { eventBelongsToService(it, service.id) }
        val paid = serviceEvents.filter { it.kind in settlementKinds && it.state == AllowanceState.VERIFIED }
        val weekEvents = serviceEvents.filter { it.createdAt >= weekStart }
        val weekSpend = paid.filter { it.createdAt >= weekStart }.sumOf { it.amount }
        val ratio = if (service.periodCap <= 0.0) 0.0 else weekSpend / service.periodCap
        val nextWindow = dayStart + 24L * 60L * 60L * 1_000L + 9L * 60L * 60L * 1_000L
        return DailyHabitsSnapshot(
            todaySpend = paid.filter { it.createdAt >= dayStart }.sumOf { it.amount },
            weekSpend = weekSpend,
            blockedSpend = weekEvents.filter { it.state == AllowanceState.BLOCKED }.sumOf { it.amount },
            budgetUsedRatio = ratio.coerceIn(0.0, 1.0),
            budgetPressure = ratio >= 0.75 || weekSpend + service.perCharge >= service.periodCap * 0.9,
            merchantAnomaly = weekEvents.any {
                it.state == AllowanceState.FROZEN || it.message.contains("lookalike", ignoreCase = true) ||
                    it.message.contains("merchant", ignoreCase = true) && it.message.contains("mismatch", ignoreCase = true)
            },
            verifiedDeliveries = weekEvents.count { it.kind == "ALPHABRIEF_LIVE_SETTLED" && it.state == AllowanceState.VERIFIED },
            frozenEvents = weekEvents.count { it.state == AllowanceState.FROZEN },
            upcomingCharges = listOf(
                UpcomingCharge(
                    serviceName = service.name,
                    merchant = service.merchant,
                    amount = service.perCharge,
                    token = service.token,
                    dueAt = nextWindow,
                    evidenceRequired = true,
                ),
            ),
        )
    }

    fun weeklyReport(snapshot: DailyHabitsSnapshot, locallyPaused: Boolean): String = """
        Allowance OS Weekly Safety Report
        Evidence level: DEVICE_LOCAL_AUDIT + LINKED_PUBLIC_DEVNET_PROOF
        Verified spend: ${"%.2f".format(snapshot.weekSpend)} test-token units
        Blocked requests: ${"%.2f".format(snapshot.blockedSpend)} test-token units
        Verified deliveries: ${snapshot.verifiedDeliveries}
        Frozen/anomaly events: ${snapshot.frozenEvents}
        Budget pressure: ${if (snapshot.budgetPressure) "ATTENTION" else "HEALTHY"}
        Merchant anomaly: ${if (snapshot.merchantAnomaly) "DETECTED" else "NONE DETECTED"}
        Local safety pause: ${if (locallyPaused) "ON" else "OFF"}
        Truth boundary: local dashboard calculations are not an onchain accounting oracle.
    """.trimIndent()

    private fun eventBelongsToService(event: AuditEvent, serviceId: String): Boolean = when {
        event.serviceId.isNotBlank() -> event.serviceId == serviceId
        event.kind.startsWith("ALPHABRIEF_") -> serviceId == CommercialCatalog.DEFAULT_ID
        else -> false
    }
}
