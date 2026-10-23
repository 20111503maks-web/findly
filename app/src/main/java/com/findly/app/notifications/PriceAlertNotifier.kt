package com.findly.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.findly.app.MainActivity
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.core.model.Money
import com.findly.core.model.Product

/** Local notifications for price alerts. Server push would deliver the same payload via [PushTokenRegistrar]. */
class PriceAlertNotifier(private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_alerts_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notifyPriceDrop(alertId: Long, product: Product, target: Money) {
        if (!canNotify()) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_PRODUCT_ID, product.id)
        }
        val pending = PendingIntent.getActivity(
            context,
            alertId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trending_down)
            .setContentTitle(context.getString(R.string.notification_price_drop_title, formatMoney(product.currentPrice)))
            .setContentText(context.getString(R.string.notification_price_drop_text, product.title, product.store.name, formatMoney(target)))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.notification_price_drop_text, product.title, product.store.name, formatMoney(target)),
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, alertId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    companion object {
        const val CHANNEL_ID = "price_alerts"
        private const val NOTIFICATION_TAG = "price_alert"
    }
}

/**
 * Seam for future server-side push (e.g. FCM). The backend would evaluate alerts against fresh
 * partner data and push; the app would only register its token here.
 */
interface PushTokenRegistrar {
    val isAvailable: Boolean
    suspend fun register(token: String)
}

object NoPushTokenRegistrar : PushTokenRegistrar {
    override val isAvailable = false
    override suspend fun register(token: String) = Unit
}
