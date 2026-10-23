package com.findly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.common.formatRating
import com.findly.app.ui.common.labelRes
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.model.Product

/**
 * Product photo. When a source provides no image (or it fails to load) we show a calm, typographic
 * placeholder instead of a stock photo that might not match the product.
 */
@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier, large: Boolean = false) {
    val url = product.images.firstOrNull()
    Box(modifier.background(Brush.linearGradient(FindlyTheme.extra.imageBackdrop))) {
        if (url != null) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = product.title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(if (large) 24.dp else 12.dp),
                loading = { Box(Modifier.fillMaxSize().shimmer()) },
                error = { ImagePlaceholder(product, large) },
            )
        } else {
            ImagePlaceholder(product, large)
        }
    }
}

@Composable
private fun ImagePlaceholder(product: Product, large: Boolean) {
    Box(Modifier.fillMaxSize().padding(if (large) 32.dp else 14.dp), contentAlignment = Alignment.Center) {
        Text(
            text = product.brand ?: product.title.substringBefore(' '),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = if (large) 56.sp else 26.sp,
                fontWeight = FontWeight.Black,
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Clip,
        )
        product.category?.let {
            Text(
                text = stringResource(it.labelRes()).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
) {
    Text(
        text = text,
        color = content,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize, fontWeight = FontWeight.SemiBold),
        modifier = modifier
            .background(container, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1,
    )
}

@Composable
fun DiscountBadge(percent: Int, modifier: Modifier = Modifier) {
    Pill("−$percent%", FindlyTheme.extra.discount, FindlyTheme.extra.onDiscount, modifier)
}

@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Pill(stringResource(R.string.badge_demo), FindlyTheme.extra.demo.copy(alpha = 0.14f), FindlyTheme.extra.demo, modifier)
}

@Composable
fun SponsoredBadge(modifier: Modifier = Modifier) {
    Pill(stringResource(R.string.badge_sponsored), FindlyTheme.extra.sponsored.copy(alpha = 0.14f), FindlyTheme.extra.sponsored, modifier)
}

@Composable
fun BestPriceBadge(modifier: Modifier = Modifier) {
    Pill(stringResource(R.string.badge_best_price), FindlyTheme.extra.bestPrice, FindlyTheme.extra.onBestPrice, modifier)
}

@Composable
fun RatingRow(rating: Double, reviews: Int?, modifier: Modifier = Modifier, compact: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = FindlyTheme.extra.rating, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
        Spacer(Modifier.width(3.dp))
        Text(
            formatRating(rating),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (reviews != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.reviews_count, reviews),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Current price, previous price (struck through) and discount — only when the source gives an old price. */
@Composable
fun PriceRow(product: Product, modifier: Modifier = Modifier, large: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            formatMoney(product.currentPrice),
            style = if (large) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        val discount = product.discountPercent
        val oldPrice = product.oldPrice
        if (discount != null && oldPrice != null) {
            Spacer(Modifier.width(if (large) 10.dp else 6.dp))
            Text(
                formatMoney(oldPrice),
                style = (if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium)
                    .copy(textDecoration = TextDecoration.LineThrough),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = if (large) 5.dp else 1.dp),
            )
        }
    }
}
