package com.findly.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.findly.app.R
import com.findly.app.ui.common.colorLabelRes
import com.findly.app.ui.common.colorSwatch
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.common.labelRes
import com.findly.core.filter.Facets
import com.findly.core.filter.SearchFilters
import com.findly.core.model.Money
import kotlin.math.roundToLong

/**
 * Full filter panel. Sections only appear when the current results actually contain that data
 * (e.g. no rating filter if no source returned ratings).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FiltersSheet(
    filters: SearchFilters,
    facets: Facets,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(filters) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            val range = facets.priceRange
            val currency = facets.currency
            if (range != null && currency != null && range.endInclusive > range.start) {
                Section(stringResource(R.string.filter_price)) {
                    val lo = (draft.minPrice?.amountMinor ?: range.start).coerceIn(range.start, range.endInclusive)
                    val hi = (draft.maxPrice?.amountMinor ?: range.endInclusive).coerceIn(range.start, range.endInclusive)
                    Text(
                        "${formatMoney(Money(lo, currency))} — ${formatMoney(Money(hi, currency))}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    RangeSlider(
                        value = lo.toFloat()..hi.toFloat(),
                        onValueChange = { v ->
                            val newLo = (v.start / 100).roundToLong() * 100
                            val newHi = (v.endInclusive / 100).roundToLong() * 100
                            draft = draft.copy(
                                minPrice = if (newLo <= range.start) null else Money(newLo, currency),
                                maxPrice = if (newHi >= range.endInclusive) null else Money(newHi, currency),
                            )
                        },
                        valueRange = range.start.toFloat()..range.endInclusive.toFloat(),
                    )
                }
            }

            if (facets.stores.size > 1) {
                Section(stringResource(R.string.filter_store)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        facets.stores.forEach { s ->
                            FilterChip(
                                selected = s.value in draft.storeIds,
                                onClick = { draft = draft.copy(storeIds = draft.storeIds.toggle(s.value)) },
                                label = { Text("${s.label} · ${s.count}") },
                            )
                        }
                    }
                }
            }

            if (facets.brands.size > 1) {
                Section(stringResource(R.string.filter_brand)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        facets.brands.forEach { b ->
                            FilterChip(
                                selected = b.value in draft.brands,
                                onClick = { draft = draft.copy(brands = draft.brands.toggle(b.value)) },
                                label = { Text("${b.label} · ${b.count}") },
                            )
                        }
                    }
                }
            }

            if (facets.categories.size > 1) {
                Section(stringResource(R.string.filter_category)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        facets.categories.forEach { c ->
                            FilterChip(
                                selected = c.value in draft.categories,
                                onClick = { draft = draft.copy(categories = draft.categories.toggle(c.value)) },
                                label = { Text(stringResource(c.value.labelRes())) },
                            )
                        }
                    }
                }
            }

            if (facets.hasRatings) {
                Section(stringResource(R.string.filter_rating)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4.0, 4.5).forEach { r ->
                            FilterChip(
                                selected = draft.minRating == r,
                                onClick = { draft = draft.copy(minRating = if (draft.minRating == r) null else r) },
                                label = { Text(stringResource(R.string.filter_rating_from, r.toString())) },
                            )
                        }
                    }
                }
            }

            if (facets.hasDiscounts) {
                Section(stringResource(R.string.filter_discount)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = draft.onlyDiscounted && draft.minDiscountPercent == null,
                            onClick = { draft = draft.copy(onlyDiscounted = !draft.onlyDiscounted, minDiscountPercent = null) },
                            label = { Text(stringResource(R.string.filter_any_discount)) },
                        )
                        listOf(10, 20, 30).forEach { d ->
                            FilterChip(
                                selected = draft.minDiscountPercent == d,
                                onClick = {
                                    draft = draft.copy(minDiscountPercent = if (draft.minDiscountPercent == d) null else d, onlyDiscounted = false)
                                },
                                label = { Text("−$d%+") },
                            )
                        }
                    }
                }
            }

            if (facets.colors.isNotEmpty()) {
                Section(stringResource(R.string.filter_color)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        facets.colors.forEach { c ->
                            ColorDot(
                                color = Color(colorSwatch(c.value)),
                                label = colorLabelRes(c.value)?.let { stringResource(it) } ?: c.value,
                                selected = c.value in draft.colors,
                                onClick = { draft = draft.copy(colors = draft.colors.toggle(c.value)) },
                            )
                        }
                    }
                }
            }

            if (facets.sizes.isNotEmpty()) {
                Section(stringResource(R.string.filter_size)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        facets.sizes.sortedWith(compareBy({ it.value.toDoubleOrNull() ?: Double.MAX_VALUE }, { it.value })).forEach { s ->
                            FilterChip(
                                selected = s.value in draft.sizes,
                                onClick = { draft = draft.copy(sizes = draft.sizes.toggle(s.value)) },
                                label = { Text(s.value) },
                            )
                        }
                    }
                }
            }

            if (facets.hasDeliveryInfo) {
                Section(stringResource(R.string.filter_delivery)) {
                    SwitchRow(stringResource(R.string.filter_free_delivery), draft.freeDeliveryOnly) {
                        draft = draft.copy(freeDeliveryOnly = it)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 7).forEach { d ->
                            FilterChip(
                                selected = draft.maxDeliveryDays == d,
                                onClick = { draft = draft.copy(maxDeliveryDays = if (draft.maxDeliveryDays == d) null else d) },
                                label = { Text(stringResource(R.string.filter_delivery_days, d)) },
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.filter_delivery_unknown_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (facets.hasAvailabilityInfo) {
                Section(stringResource(R.string.filter_availability)) {
                    SwitchRow(stringResource(R.string.filter_in_stock), draft.inStockOnly) { draft = draft.copy(inStockOnly = it) }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { draft = SearchFilters() }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_reset))
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_apply))
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorDot(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(36.dp)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .padding(4.dp)
                .background(color, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
