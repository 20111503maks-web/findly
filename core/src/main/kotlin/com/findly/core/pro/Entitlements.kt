package com.findly.core.pro

/**
 * Findly Pro foundation. Core search, comparison, favourites and basic alerts are always free;
 * Pro only extends limits and adds optional extras. Nothing here gates searching.
 */
enum class ProFeature {
    EXTENDED_PRICE_ALERTS,
    EXTENDED_PRICE_HISTORY,
    ADVANCED_AI_SEARCH,
}

data class PlanLimits(
    val maxActivePriceAlerts: Int,
    val priceHistoryDays: Int,
)

sealed interface Plan {
    val limits: PlanLimits
    fun has(feature: ProFeature): Boolean

    data object Free : Plan {
        override val limits = PlanLimits(maxActivePriceAlerts = 25, priceHistoryDays = 90)
        override fun has(feature: ProFeature) = false
    }

    data object Pro : Plan {
        override val limits = PlanLimits(maxActivePriceAlerts = 500, priceHistoryDays = 365)
        override fun has(feature: ProFeature) = true
    }
}

/** Backed by Google Play Billing (and verified server-side) in the future. */
interface EntitlementRepository {
    suspend fun currentPlan(): Plan
}

class FreeEntitlementRepository : EntitlementRepository {
    override suspend fun currentPlan(): Plan = Plan.Free
}
