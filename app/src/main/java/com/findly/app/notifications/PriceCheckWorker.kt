package com.findly.app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.findly.app.FindlyApplication
import com.findly.core.pricing.AlertEvaluator
import java.util.concurrent.TimeUnit

/**
 * Periodically re-fetches tracked products (with alerts or saved), records real price
 * observations and fires local notifications when an alert target is reached.
 */
class PriceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FindlyApplication).container
        val products = container.productRepository
        val tracking = container.priceTrackingRepository
        val favorites = container.favoritesRepository
        val notifier = container.notifier

        var anyFailed = false

        for (entry in tracking.activeAlerts()) {
            val snapshot = entry.product ?: continue
            val fresh = products.refresh(snapshot)
            if (fresh == null) {
                anyFailed = true
                continue
            }
            tracking.record(fresh)
            val notify = AlertEvaluator.shouldNotify(entry.alert, fresh.currentPrice)
            if (notify) notifier.notifyPriceDrop(entry.alert.id, fresh, entry.alert.targetPrice)
            tracking.markChecked(entry.alert.id, fresh, notified = notify)
        }

        for (saved in favorites.all()) {
            val fresh = products.refresh(saved) ?: continue
            tracking.record(fresh)
            favorites.updateSnapshot(fresh)
        }

        tracking.pruneHistory()
        return if (anyFailed && runAttemptCount < 2) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC_NAME = "price-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Manual "check now" from the Alerts screen. */
        fun runOnce(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<PriceCheckWorker>().build())
        }
    }
}
