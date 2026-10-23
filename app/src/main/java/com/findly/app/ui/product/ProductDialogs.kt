package com.findly.app.ui.product

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.components.DemoBadge
import com.findly.core.link.OutboundLink
import com.findly.core.model.Money
import com.findly.core.model.Product
import java.math.BigDecimal

/** Makes it explicit where the user is going before leaving Findly. */
@Composable
fun StoreRedirectDialog(product: Product, link: OutboundLink, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.redirect_title, link.storeName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.redirect_message, link.displayHost))
                if (link.isAffiliate) {
                    Text(
                        stringResource(R.string.redirect_affiliate_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (product.isDemo) {
                    Row {
                        DemoBadge()
                    }
                    Text(
                        stringResource(R.string.redirect_demo_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.action_open_store)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.error_no_browser, Toast.LENGTH_SHORT).show()
    }
}

/** "Notify me when the price drops below …" */
@Composable
fun PriceAlertDialog(product: Product, onCreate: (Money) -> Unit, onDismiss: () -> Unit) {
    val current = product.currentPrice
    val suggested = Money((current.amountMinor * 0.9).toLong() / 100 * 100, current.currency)
    var text by remember { mutableStateOf((suggested.amountMinor / 100).toString()) }
    val parsed = text.replace(',', '.').toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }?.let { Money.of(it, current.currency) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.alert_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.alert_dialog_current, formatMoney(current), product.store.name))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(9) },
                    label = { Text(stringResource(R.string.alert_dialog_target, current.currency)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 20).forEach { pct ->
                        AssistChip(
                            onClick = { text = ((current.amountMinor * (100 - pct) / 100) / 100).toString() },
                            label = { Text("−$pct%") },
                        )
                    }
                }
                if (parsed != null && parsed >= current) {
                    Text(
                        stringResource(R.string.alert_dialog_already_below),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.alert_dialog_how),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onCreate) }, enabled = parsed != null) { Text(stringResource(R.string.action_track_price)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
