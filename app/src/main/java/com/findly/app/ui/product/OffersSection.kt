package com.findly.app.ui.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.common.labelRes
import com.findly.app.ui.components.BestPriceBadge
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.components.SkeletonBlock
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.compare.ComparedOffer
import com.findly.core.compare.MatchConfidence
import com.findly.core.compare.OfferComparison
import com.findly.core.model.Product

@Composable
fun OffersSection(
    current: Product,
    loading: Boolean,
    comparison: OfferComparison?,
    onOpenOffer: (Product) -> Unit,
    onCompare: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.offers_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        when {
            loading -> repeat(2) {
                SkeletonBlock(Modifier.fillMaxWidth().height(64.dp))
                Spacer(Modifier.height(8.dp))
            }
            comparison == null || comparison.offers.size <= 1 -> Text(
                stringResource(R.string.offers_only_one),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                comparison.offers.filter { it.confidence != MatchConfidence.SIMILAR }.take(3).forEach { offer ->
                    OfferRow(offer, isCurrent = offer.product.id == current.id, onClick = { onOpenOffer(offer.product) })
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedButton(onClick = onCompare, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_compare_offers, comparison.offers.size))
                }
            }
        }
    }
}

@Composable
fun OfferRow(offer: ComparedOffer, isCurrent: Boolean, onClick: () -> Unit) {
    val p = offer.product
    val borderColor = if (offer.isBest) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(if (offer.isBest) 1.5.dp else 1.dp, borderColor, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.store.name, style = MaterialTheme.typography.titleSmall)
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.offer_current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(2.dp))
            val deliveryCost = p.delivery?.cost
            val deliveryText = when {
                deliveryCost == null -> stringResource(R.string.delivery_unknown)
                deliveryCost.amountMinor == 0L -> stringResource(R.string.delivery_free)
                else -> stringResource(R.string.delivery_cost, formatMoney(deliveryCost))
            }
            Text(deliveryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.availability.labelRes()?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (offer.confidence == MatchConfidence.SIMILAR) {
                Text(stringResource(R.string.offer_similar), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (offer.isBest) {
                BestPriceBadge()
                Spacer(Modifier.height(4.dp))
            }
            Text(formatMoney(p.currentPrice), style = MaterialTheme.typography.titleMedium)
            if (offer.deliveryKnown && p.delivery?.isFree != true) {
                Text(
                    stringResource(R.string.offer_total, formatMoney(offer.effectivePrice)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareSheet(current: Product, comparison: OfferComparison, onOpenOffer: (Product) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.compare_title), style = MaterialTheme.typography.headlineSmall)
            Text(current.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            comparison.best?.let { best ->
                Text(
                    stringResource(R.string.compare_best, formatMoney(best.effectivePrice), best.product.store.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = FindlyTheme.extra.bestPrice,
                )
            }
            comparison.maxSavings?.let {
                Text(stringResource(R.string.compare_savings, formatMoney(it)), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(4.dp))
            comparison.offers.forEach { offer ->
                OfferRow(offer, isCurrent = offer.product.id == current.id, onClick = { onOpenOffer(offer.product) })
            }
            if (comparison.hasUnknownDelivery) InfoBanner(stringResource(R.string.compare_unknown_delivery))
            InfoBanner(stringResource(R.string.compare_ranking_note))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun BestPriceCallout(current: Product, comparison: OfferComparison?, onOpen: (Product) -> Unit) {
    if (comparison == null) return
    val best = comparison.best ?: return
    if (best.product.id == current.id) {
        if (comparison.offers.size > 1) {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(FindlyTheme.extra.bestPrice.copy(alpha = 0.10f)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BestPriceBadge()
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.best_price_here, comparison.offers.size), style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }
    val own = current.totalPriceWithKnownDelivery ?: current.currentPrice
    val savings = (own - best.effectivePrice).takeIf { it.amountMinor > 0 && own.currency == best.effectivePrice.currency } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(FindlyTheme.extra.bestPrice.copy(alpha = 0.10f))
            .clickable { onOpen(best.product) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_trending_down),
            contentDescription = null,
            tint = FindlyTheme.extra.bestPrice,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.cheaper_at, best.product.store.name, formatMoney(best.effectivePrice)),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.savings_value, formatMoney(savings)),
                style = MaterialTheme.typography.bodySmall,
                color = FindlyTheme.extra.bestPrice,
            )
        }
        Text(stringResource(R.string.action_view), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
    }
}
