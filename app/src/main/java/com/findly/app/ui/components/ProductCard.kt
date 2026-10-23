package com.findly.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.model.Money
import com.findly.core.model.Product

/**
 * The main product tile. Shows only what the source provided: rating, discount and delivery
 * rows simply disappear when unknown.
 *
 * @param storeCount number of stores offering the same product (from grouping); >1 shows "in N stores".
 * @param lowestPrice lowest price across those stores.
 */
@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    storeCount: Int = 1,
    lowestPrice: Money? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "cardScale")

    Column(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.86f)
                .clip(MaterialTheme.shapes.large),
        ) {
            ProductImage(product, Modifier.fillMaxSize())
            Row(
                Modifier.align(Alignment.TopStart).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                product.discountPercent?.let { DiscountBadge(it) }
                if (product.sponsored) SponsoredBadge()
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.action_unsave else R.string.action_save),
                    tint = if (isFavorite) FindlyTheme.extra.discount else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (product.isDemo) {
                DemoBadge(Modifier.align(Alignment.BottomStart).padding(10.dp))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = if (storeCount > 1) stringResource(R.string.in_n_stores, storeCount) else product.store.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            product.rating?.let {
                Spacer(Modifier.width(8.dp))
                RatingRow(it, null)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            product.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(Modifier.height(6.dp))
        if (storeCount > 1 && lowestPrice != null) {
            Row(Modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.price_from),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp, end = 4.dp),
                )
                Text(formatMoney(lowestPrice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            PriceRow(product, Modifier.padding(horizontal = 2.dp))
        }
        DeliveryLine(product, Modifier.padding(horizontal = 2.dp, vertical = 2.dp))
    }
}

@Composable
fun DeliveryLine(product: Product, modifier: Modifier = Modifier) {
    val delivery = product.delivery ?: return
    val cost = delivery.cost
    val maxDays = delivery.maxDays
    val text = when {
        cost != null && cost.amountMinor == 0L -> stringResource(R.string.delivery_free)
        cost != null -> stringResource(R.string.delivery_cost, formatMoney(cost))
        maxDays != null -> stringResource(R.string.delivery_days, delivery.minDays ?: maxDays, maxDays)
        else -> return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_shipping),
            contentDescription = null,
            tint = if (delivery.isFree) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (delivery.isFree) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
    ) { content() }
}
