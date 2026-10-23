package com.findly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.provider.SourceStatus

/** Friendly full-width state: nothing found, offline, empty favourites, no history… */
@Composable
fun EmptyState(
    icon: Painter,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    accent: Color = MaterialTheme.colorScheme.secondary,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.02f))), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(60.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}

/** Inline, non-blocking notice: e.g. "1 store didn't respond". */
@Composable
fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    warning: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tint = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
    Row(
        modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            rememberVectorPainter(if (warning) Icons.Filled.Warning else Icons.Filled.Info),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                modifier = Modifier.clickableNoRipple(onAction).padding(4.dp),
            )
        }
    }
}

@Composable
fun SourceStatusBanner(sources: List<SourceStatus>, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val failed = sources.filter { it !is SourceStatus.Ok }
    if (failed.isEmpty()) return
    val names = failed.joinToString(", ") { it.provider.displayName }
    InfoBanner(
        text = pluralStringResource(R.plurals.sources_failed, failed.size, failed.size, names),
        warning = true,
        modifier = modifier,
        action = onRetry?.let { stringResource(R.string.action_retry) },
        onAction = onRetry,
    )
}

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(FindlyTheme.extra.demo.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DemoBadge()
        Text(
            stringResource(R.string.demo_mode_banner),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
