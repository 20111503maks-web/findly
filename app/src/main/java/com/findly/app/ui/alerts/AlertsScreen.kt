package com.findly.app.ui.alerts

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.findly.app.R
import com.findly.app.data.repository.AlertWithProduct
import com.findly.app.data.repository.PriceTrackingRepository
import com.findly.app.notifications.PriceCheckWorker
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.components.ProductImage
import com.findly.app.ui.theme.FindlyTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.text.format.DateUtils

class AlertsViewModel(private val tracking: PriceTrackingRepository) : ViewModel() {
    val alerts: StateFlow<List<AlertWithProduct>?> = tracking.alerts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setActive(id: Long, active: Boolean) {
        viewModelScope.launch { tracking.setActive(id, active) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { tracking.delete(id) }
    }

    fun checkNow(context: Context) = PriceCheckWorker.runOnce(context)
}

@Composable
fun AlertsScreen(onOpenProduct: (String) -> Unit, onExplore: () -> Unit) {
    val vm = containerViewModel { AlertsViewModel(it.priceTrackingRepository) }
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 20.dp), verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.tab_alerts), style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                if (!alerts.isNullOrEmpty()) {
                    TextButton(onClick = { vm.checkNow(context) }) { Text(stringResource(R.string.action_check_now)) }
                }
            }
        }
        val list = alerts
        when {
            list == null -> Unit
            list.isEmpty() -> item {
                EmptyState(
                    icon = rememberVectorPainter(Icons.Filled.Notifications),
                    title = stringResource(R.string.state_alerts_empty_title),
                    message = stringResource(R.string.state_alerts_empty_message),
                    actionLabel = stringResource(R.string.action_explore),
                    onAction = onExplore,
                )
            }
            else -> {
                items(list, key = { it.alert.id }) { entry ->
                    AlertRow(
                        entry = entry,
                        onClick = { onOpenProduct(entry.alert.productId) },
                        onToggle = { vm.setActive(entry.alert.id, it) },
                        onDelete = { vm.delete(entry.alert.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
                item { InfoBanner(stringResource(R.string.alerts_how_it_works), Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun AlertRow(
    entry: AlertWithProduct,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alert = entry.alert
    val reached = alert.lastSeenPrice?.let { it.currency == alert.targetPrice.currency && it <= alert.targetPrice } == true
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(MaterialTheme.shapes.medium)) {
            entry.product?.let { ProductImage(it, Modifier.fillMaxSize()) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(alert.productTitle, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(alert.storeName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.alert_target, formatMoney(alert.targetPrice)),
                style = MaterialTheme.typography.labelLarge,
                color = if (reached) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.onSurface,
            )
            alert.lastSeenPrice?.let {
                Text(
                    stringResource(
                        if (reached) R.string.alert_reached else R.string.alert_last_seen,
                        formatMoney(it),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (reached) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.lastCheckedAt?.let {
                Text(
                    stringResource(R.string.alert_checked, DateUtils.getRelativeTimeSpanString(it).toString()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(checked = alert.isActive, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
