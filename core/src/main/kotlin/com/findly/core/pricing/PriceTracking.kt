package com.findly.core.pricing

import com.findly.core.model.Money
import java.util.concurrent.TimeUnit

/** One real observation of a price, recorded when Findly saw it from a source. */
data class PricePoint(
    val productId: String,
    val price: Money,
    val observedAt: Long,
)

enum class HistoryRange(val days: Int) { WEEK(7), MONTH(30), QUARTER(90) }

data class PriceHistorySummary(
    val range: HistoryRange,
    val points: List<PricePoint>,
    val min: Money?,
    val max: Money?,
    val latest: Money?,
    /** False when there are too few observations to draw an honest chart. */
    val hasEnoughData: Boolean,
)

object PriceHistoryAnalyzer {

    /** At least two observations at least [minSpanHours] apart are needed for a meaningful chart. */
    fun summarize(
        points: List<PricePoint>,
        range: HistoryRange,
        now: Long,
        minSpanHours: Long = 12,
    ): PriceHistorySummary {
        val from = now - TimeUnit.DAYS.toMillis(range.days.toLong())
        val currency = points.maxByOrNull { it.observedAt }?.price?.currency
        val inRange = points
            .filter { it.observedAt in from..now && it.price.currency == currency }
            .sortedBy { it.observedAt }
        val span = if (inRange.size >= 2) inRange.last().observedAt - inRange.first().observedAt else 0
        return PriceHistorySummary(
            range = range,
            points = inRange,
            min = inRange.minByOrNull { it.price.amountMinor }?.price,
            max = inRange.maxByOrNull { it.price.amountMinor }?.price,
            latest = inRange.lastOrNull()?.price,
            hasEnoughData = inRange.size >= 2 && span >= TimeUnit.HOURS.toMillis(minSpanHours),
        )
    }

    /**
     * Whether a new observation should be stored: always the first one, then only when the price
     * changed or the last stored point is older than [minIntervalMs] (keeps the DB small).
     */
    fun shouldRecord(last: PricePoint?, new: PricePoint, minIntervalMs: Long = TimeUnit.HOURS.toMillis(6)): Boolean {
        if (last == null) return true
        if (last.price != new.price) return true
        return new.observedAt - last.observedAt >= minIntervalMs
    }
}

data class PriceAlert(
    val id: Long,
    val productId: String,
    val productTitle: String,
    val storeName: String,
    val targetPrice: Money,
    val createdAt: Long,
    val isActive: Boolean = true,
    val lastSeenPrice: Money? = null,
    val lastNotifiedPrice: Money? = null,
)

object AlertEvaluator {
    /**
     * Notify when the price is at or below the target, and only once per price level:
     * a new notification requires a further price drop below the previously notified price.
     */
    fun shouldNotify(alert: PriceAlert, current: Money): Boolean {
        if (!alert.isActive) return false
        if (current.currency != alert.targetPrice.currency) return false
        if (current > alert.targetPrice) return false
        val notified = alert.lastNotifiedPrice ?: return true
        return notified.currency == current.currency && current < notified
    }
}
