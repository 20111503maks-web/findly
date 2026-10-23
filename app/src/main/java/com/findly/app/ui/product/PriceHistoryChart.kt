package com.findly.app.ui.product

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.components.EmptyState
import com.findly.core.pricing.HistoryRange
import com.findly.core.pricing.PriceHistorySummary

/**
 * Price history from REAL observations only. With too little data we say so instead of drawing
 * a misleading line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistorySection(
    summary: PriceHistorySummary?,
    range: HistoryRange,
    isTracked: Boolean,
    onRangeChange: (HistoryRange) -> Unit,
    onStartTracking: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.price_history_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            HistoryRange.entries.forEachIndexed { i, r ->
                SegmentedButton(
                    selected = r == range,
                    onClick = { onRangeChange(r) },
                    shape = SegmentedButtonDefaults.itemShape(i, HistoryRange.entries.size),
                    label = {
                        Text(
                            stringResource(
                                when (r) {
                                    HistoryRange.WEEK -> R.string.range_7d
                                    HistoryRange.MONTH -> R.string.range_30d
                                    HistoryRange.QUARTER -> R.string.range_3m
                                },
                            ),
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (summary != null && summary.hasEnoughData) {
            Row(Modifier.fillMaxWidth()) {
                Stat(stringResource(R.string.price_history_min), summary.min?.let(::formatMoney).orEmpty(), Modifier.weight(1f))
                Stat(stringResource(R.string.price_history_max), summary.max?.let(::formatMoney).orEmpty(), Modifier.weight(1f))
                Stat(stringResource(R.string.price_history_now), summary.latest?.let(::formatMoney).orEmpty(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Chart(summary)
            Text(
                stringResource(R.string.price_history_points, summary.points.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            EmptyState(
                icon = painterResource(R.drawable.ic_trending_down),
                title = stringResource(R.string.state_no_history_title),
                message = stringResource(if (isTracked) R.string.state_no_history_tracked else R.string.state_no_history_untracked),
                actionLabel = if (isTracked) null else stringResource(R.string.action_track_price),
                onAction = if (isTracked) null else onStartTracking,
                modifier = Modifier.padding(vertical = 0.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Chart(summary: PriceHistorySummary) {
    val line = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val progress = remember(summary.range) { Animatable(0f) }
    LaunchedEffect(summary.range, summary.points.size) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(700))
    }
    Canvas(Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)) {
        val pts = summary.points
        val minT = pts.first().observedAt.toFloat()
        val maxT = pts.last().observedAt.toFloat()
        val minP = pts.minOf { it.price.amountMinor }.toFloat()
        val maxP = pts.maxOf { it.price.amountMinor }.toFloat()
        val pad = ((maxP - minP) * 0.15f).coerceAtLeast(100f)
        val lo = minP - pad
        val hi = maxP + pad
        fun x(t: Long) = if (maxT == minT) 0f else (t - minT) / (maxT - minT) * size.width
        fun y(p: Long) = size.height - (p - lo) / (hi - lo) * size.height

        for (i in 0..3) {
            val gy = size.height * i / 3f
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
        }
        // Step line: a price stays the same until the next observation.
        val path = Path()
        pts.forEachIndexed { i, p ->
            val px = x(p.observedAt)
            val py = y(p.price.amountMinor)
            if (i == 0) {
                path.moveTo(px, py)
            } else {
                path.lineTo(px, y(pts[i - 1].price.amountMinor))
                path.lineTo(px, py)
            }
        }
        val fill = Path().apply {
            addPath(path)
            lineTo(x(pts.last().observedAt), size.height)
            lineTo(0f, size.height)
            close()
        }
        clipRect(right = size.width * progress.value) {
            drawPath(fill, Brush.verticalGradient(listOf(line.copy(alpha = 0.22f), line.copy(alpha = 0f))))
            drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        pts.forEach { p ->
            if (x(p.observedAt) <= size.width * progress.value) {
                drawCircle(line, radius = 3.5.dp.toPx(), center = Offset(x(p.observedAt), y(p.price.amountMinor)))
            }
        }
    }
}
