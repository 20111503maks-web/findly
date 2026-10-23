package com.findly.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.findly.app.ui.theme.FindlyTheme

/** Soft moving highlight used for skeleton placeholders. */
fun Modifier.shimmer(): Modifier = composed {
    val base = FindlyTheme.extra.skeleton
    val highlight = FindlyTheme.extra.skeletonHighlight
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX",
    )
    background(
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x, 0f),
            end = Offset(x + 600f, 300f),
        ),
    )
}

@Composable
fun SkeletonBlock(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).shimmer())
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .clip(MaterialTheme.shapes.large)
                .shimmer(),
        )
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(10.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(14.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(14.dp))
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.fillMaxWidth(0.35f).height(18.dp).padding(end = 4.dp))
    }
}
