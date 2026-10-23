package com.findly.app.data.repository

import com.findly.app.data.local.PriceAlertEntity
import com.findly.app.data.local.PriceAlertsDao
import com.findly.app.data.local.PriceHistoryDao
import com.findly.app.data.local.PricePointEntity
import com.findly.app.data.local.ProductJson
import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.pricing.PriceAlert
import com.findly.core.pricing.PricePoint
import com.findly.core.pricing.PriceHistoryAnalyzer
import com.findly.core.pro.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

data class AlertWithProduct(val alert: PriceAlert, val product: Product?, val lastCheckedAt: Long?)

sealed interface CreateAlertResult {
    data object Created : CreateAlertResult
    data class LimitReached(val limit: Int) : CreateAlertResult
}

/**
 * Stores real price observations for tracked products (saved or with an alert) and manages alerts.
 * Nothing here generates prices: history only contains what a source actually returned.
 */
class PriceTrackingRepository(
    private val historyDao: PriceHistoryDao,
    private val alertsDao: PriceAlertsDao,
    private val entitlements: EntitlementRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    fun history(productId: String): Flow<List<PricePoint>> =
        historyDao.observe(productId).map { list -> list.map { PricePoint(it.productId, Money(it.amountMinor, it.currency), it.observedAt) } }

    suspend fun record(product: Product) {
        val last = historyDao.latest(product.id)?.let { PricePoint(it.productId, Money(it.amountMinor, it.currency), it.observedAt) }
        val point = PricePoint(product.id, product.currentPrice, clock())
        if (PriceHistoryAnalyzer.shouldRecord(last, point)) {
            historyDao.insert(PricePointEntity(productId = product.id, amountMinor = point.price.amountMinor, currency = point.price.currency, observedAt = point.observedAt))
        }
    }

    suspend fun pruneHistory() {
        val days = entitlements.currentPlan().limits.priceHistoryDays
        historyDao.deleteOlderThan(clock() - TimeUnit.DAYS.toMillis(days.toLong()))
    }

    val alerts: Flow<List<AlertWithProduct>> = alertsDao.observeAll().map { list -> list.map { it.toAlertWithProduct() } }

    fun alertFor(productId: String): Flow<PriceAlert?> = alertsDao.observeForProduct(productId).map { it?.toAlertWithProduct()?.alert }

    suspend fun createAlert(product: Product, target: Money): CreateAlertResult {
        val limit = entitlements.currentPlan().limits.maxActivePriceAlerts
        if (alertsDao.countActive() >= limit && alertsDao.getActive().none { it.productId == product.id }) {
            return CreateAlertResult.LimitReached(limit)
        }
        alertsDao.insert(
            PriceAlertEntity(
                productId = product.id,
                productJson = ProductJson.encode(product),
                targetMinor = target.amountMinor,
                currency = target.currency,
                createdAt = clock(),
                lastSeenMinor = product.currentPrice.amountMinor,
                lastCheckedAt = clock(),
            ),
        )
        record(product)
        return CreateAlertResult.Created
    }

    suspend fun setActive(id: Long, active: Boolean) {
        val e = alertsDao.get(id) ?: return
        alertsDao.upsert(e.copy(isActive = active))
    }

    suspend fun delete(id: Long) = alertsDao.delete(id)

    suspend fun activeAlerts(): List<AlertWithProduct> = alertsDao.getActive().map { it.toAlertWithProduct() }

    suspend fun markChecked(alertId: Long, seen: Product, notified: Boolean) {
        val e = alertsDao.get(alertId) ?: return
        alertsDao.upsert(
            e.copy(
                productJson = ProductJson.encode(seen),
                lastSeenMinor = seen.currentPrice.amountMinor,
                lastCheckedAt = clock(),
                lastNotifiedMinor = if (notified) seen.currentPrice.amountMinor else e.lastNotifiedMinor,
            ),
        )
    }

    private fun PriceAlertEntity.toAlertWithProduct(): AlertWithProduct {
        val product = ProductJson.decode(productJson)
        return AlertWithProduct(
            alert = PriceAlert(
                id = id,
                productId = productId,
                productTitle = product?.title.orEmpty(),
                storeName = product?.store?.name.orEmpty(),
                targetPrice = Money(targetMinor, currency),
                createdAt = createdAt,
                isActive = isActive,
                lastSeenPrice = lastSeenMinor?.let { Money(it, currency) },
                lastNotifiedPrice = lastNotifiedMinor?.let { Money(it, currency) },
            ),
            product = product,
            lastCheckedAt = lastCheckedAt,
        )
    }
}
