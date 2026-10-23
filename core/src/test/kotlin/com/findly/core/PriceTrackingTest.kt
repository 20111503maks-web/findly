package com.findly.core

import com.findly.core.model.Money
import com.findly.core.pricing.AlertEvaluator
import com.findly.core.pricing.HistoryRange
import com.findly.core.pricing.PriceAlert
import com.findly.core.pricing.PriceHistoryAnalyzer
import com.findly.core.pricing.PricePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceTrackingTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day

    @Test
    fun `single observation is not enough history`() {
        val s = PriceHistoryAnalyzer.summarize(listOf(PricePoint("p", Money.of(10.0), now)), HistoryRange.WEEK, now)
        assertFalse(s.hasEnoughData)
    }

    @Test
    fun `range limits points`() {
        val pts = listOf(40, 20, 5, 1).map { PricePoint("p", Money.of(it.toDouble()), now - it * day) }
        val week = PriceHistoryAnalyzer.summarize(pts, HistoryRange.WEEK, now)
        assertEquals(2, week.points.size)
        assertTrue(week.hasEnoughData)
        assertEquals(4, PriceHistoryAnalyzer.summarize(pts, HistoryRange.QUARTER, now).points.size)
    }

    @Test
    fun `alert fires once per price level`() {
        val alert = PriceAlert(1, "p", "t", "s", Money.of(60.0), 0)
        assertFalse(AlertEvaluator.shouldNotify(alert, Money.of(61.0)))
        assertTrue(AlertEvaluator.shouldNotify(alert, Money.of(59.0)))
        val notified = alert.copy(lastNotifiedPrice = Money.of(59.0))
        assertFalse(AlertEvaluator.shouldNotify(notified, Money.of(59.0)))
        assertTrue(AlertEvaluator.shouldNotify(notified, Money.of(55.0)))
    }
}
