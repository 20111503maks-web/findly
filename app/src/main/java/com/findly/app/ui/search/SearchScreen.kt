package com.findly.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.findly.app.R
import com.findly.app.ui.common.colorLabelRes
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.common.labelRes
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.components.ProductCard
import com.findly.app.ui.components.ProductCardSkeleton
import com.findly.app.ui.components.SourceStatusBanner
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.ai.AiIntent
import com.findly.core.filter.SortOption
import com.findly.core.search.UnderstoodPart

@Composable
fun SearchScreen(
    initialQuery: String?,
    autoFocus: Boolean,
    onOpenProduct: (String) -> Unit,
    onOpenPhotoSearch: () -> Unit,
) {
    val vm = containerViewModel(key = "search:${initialQuery.orEmpty()}") {
        SearchViewModel(it.productRepository, it.favoritesRepository, it.connectivity, initialQuery)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchField(
            value = state.query,
            autoFocus = autoFocus && initialQuery.isNullOrBlank(),
            onValueChange = vm::onQueryChange,
            onSubmit = {
                focusManager.clearFocus()
                vm.submit()
            },
            onCamera = onOpenPhotoSearch,
        )

        when (state.phase) {
            SearchPhase.IDLE -> IdleContent(
                recent = recent,
                onPick = {
                    focusManager.clearFocus()
                    vm.submit(it)
                },
                onClearRecent = vm::clearRecent,
            )
            SearchPhase.OFFLINE -> EmptyState(
                icon = painterResource(R.drawable.ic_cloud_off),
                title = stringResource(R.string.state_offline_title),
                message = stringResource(R.string.state_offline_message),
                actionLabel = stringResource(R.string.action_retry),
                onAction = vm::retry,
            )
            SearchPhase.ALL_SOURCES_FAILED -> EmptyState(
                icon = painterResource(R.drawable.ic_cloud_off),
                title = stringResource(R.string.state_sources_failed_title),
                message = stringResource(R.string.state_sources_failed_message),
                actionLabel = stringResource(R.string.action_retry),
                onAction = vm::retry,
                accent = MaterialTheme.colorScheme.error,
            )
            SearchPhase.LOADING, SearchPhase.RESULTS -> ResultsContent(
                state = state,
                favorites = favorites,
                onOpenProduct = onOpenProduct,
                onToggleFavorite = vm::toggleFavorite,
                onOpenFilters = { showFilters = true },
                onSort = vm::setSort,
                onFilters = vm::setFilters,
                onClearFilters = vm::clearFilters,
                onToggleIntent = vm::toggleIntent,
                onRetry = vm::retry,
            )
        }
    }

    val results = state.results
    if (showFilters && results != null) {
        FiltersSheet(
            filters = state.filters,
            facets = results.facets,
            onApply = {
                vm.setFilters(it)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    autoFocus: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCamera: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) focusRequester.requestFocus() }
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                    }
                }
                IconButton(onClick = onCamera) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.photo_search_title))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .focusRequester(focusRequester),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(recent: List<String>, onPick: (String) -> Unit, onClearRecent: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.action_clear),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onClearRecent).padding(8.dp),
                )
            }
            recent.forEach { q ->
                Text(
                    q,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onPick(q) }.padding(vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Text(stringResource(R.string.search_try), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.suggestion_1, R.string.suggestion_2, R.string.suggestion_3, R.string.suggestion_4, R.string.suggestion_5).forEach { res ->
                val s = stringResource(res)
                Text(
                    s,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onPick(s) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        InfoBanner(stringResource(R.string.search_nl_explainer))
    }
}

@Composable
private fun ResultsContent(
    state: SearchUiState,
    favorites: Set<String>,
    onOpenProduct: (String) -> Unit,
    onToggleFavorite: (com.findly.core.model.Product) -> Unit,
    onOpenFilters: () -> Unit,
    onSort: (SortOption) -> Unit,
    onFilters: (com.findly.core.filter.SearchFilters) -> Unit,
    onClearFilters: () -> Unit,
    onToggleIntent: (AiIntent) -> Unit,
    onRetry: () -> Unit,
) {
    val results = state.results
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        state.parsed?.let { parsed ->
            if (parsed.understood.isNotEmpty()) fullWidth { UnderstoodRow(parsed.understood) }
        }
        fullWidth {
            ControlsRow(
                state = state,
                onOpenFilters = onOpenFilters,
                onSort = onSort,
                onFilters = onFilters,
            )
        }
        fullWidth { AiRefineRow(state, onToggleIntent) }

        if (results == null) {
            items(6) { ProductCardSkeleton() }
            return@LazyVerticalGrid
        }

        state.outcome?.let { outcome ->
            if (outcome.failedSources.isNotEmpty()) fullWidth { SourceStatusBanner(outcome.sources, onRetry = onRetry) }
            if (outcome.partialMatches) fullWidth { InfoBanner(stringResource(R.string.search_partial_matches)) }
        }

        fullWidth {
            Text(
                stringResource(R.string.search_results_count, results.groups.size, results.groups.sumOf { it.offers.size }),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (results.groups.isEmpty()) {
            fullWidth {
                if (results.totalBeforeFilters > 0) {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_tune),
                        title = stringResource(R.string.state_filtered_out_title),
                        message = stringResource(R.string.state_filtered_out_message, results.totalBeforeFilters),
                        actionLabel = stringResource(R.string.action_reset_filters),
                        onAction = onClearFilters,
                    )
                } else {
                    EmptyState(
                        icon = rememberSearchPainter(),
                        title = stringResource(R.string.state_nothing_found_title),
                        message = stringResource(R.string.state_nothing_found_message),
                    )
                }
            }
        }

        if (results.sponsored.isNotEmpty() && results.groups.isNotEmpty()) {
            fullWidth {
                Text(
                    stringResource(R.string.sponsored_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = FindlyTheme.extra.sponsored,
                )
            }
            items(results.sponsored, key = { "ad:" + it.id }) { p ->
                ProductCard(
                    product = p,
                    isFavorite = p.id in favorites,
                    onClick = { onOpenProduct(p.id) },
                    onToggleFavorite = { onToggleFavorite(p) },
                )
            }
            fullWidth { Spacer(Modifier.height(1.dp)) }
        }

        items(results.groups, key = { it.key }) { group ->
            val p = group.representative
            ProductCard(
                product = p,
                isFavorite = p.id in favorites,
                onClick = { onOpenProduct(p.id) },
                onToggleFavorite = { onToggleFavorite(p) },
                storeCount = group.storeCount,
                lowestPrice = group.lowestPrice,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun rememberSearchPainter() = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Filled.Search)

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun UnderstoodRow(parts: List<UnderstoodPart>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.search_understood),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(parts) { part ->
                val label = when (part) {
                    is UnderstoodPart.MaxPrice -> stringResource(R.string.understood_max_price, formatMoney(part.price))
                    is UnderstoodPart.MinPrice -> stringResource(R.string.understood_min_price, formatMoney(part.price))
                    is UnderstoodPart.Color -> colorLabelRes(part.color)?.let { stringResource(it) } ?: part.color
                    is UnderstoodPart.CategoryPart -> stringResource(part.category.labelRes())
                    is UnderstoodPart.Size -> stringResource(R.string.understood_size, part.size)
                    is UnderstoodPart.Intent -> "✦ " + stringResource(part.intent.labelRes())
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ControlsRow(
    state: SearchUiState,
    onOpenFilters: () -> Unit,
    onSort: (SortOption) -> Unit,
    onFilters: (com.findly.core.filter.SearchFilters) -> Unit,
) {
    val f = state.filters
    var sortMenu by remember { mutableStateOf(false) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = f.activeCount > 0,
                onClick = onOpenFilters,
                label = { Text(if (f.activeCount > 0) stringResource(R.string.filters_with_count, f.activeCount) else stringResource(R.string.filters)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_tune), contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
        item {
            Box {
                FilterChip(
                    selected = state.sort != SortOption.RELEVANCE,
                    onClick = { sortMenu = true },
                    label = { Text(stringResource(state.sort.labelRes())) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_sort), contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes())) },
                            onClick = {
                                sortMenu = false
                                onSort(option)
                            },
                        )
                    }
                }
            }
        }
        item {
            FilterChip(
                selected = f.onlyDiscounted,
                onClick = { onFilters(f.copy(onlyDiscounted = !f.onlyDiscounted)) },
                label = { Text(stringResource(R.string.filter_discounted)) },
            )
        }
        item {
            FilterChip(
                selected = f.freeDeliveryOnly,
                onClick = { onFilters(f.copy(freeDeliveryOnly = !f.freeDeliveryOnly)) },
                label = { Text(stringResource(R.string.filter_free_delivery)) },
            )
        }
        item {
            FilterChip(
                selected = f.inStockOnly,
                onClick = { onFilters(f.copy(inStockOnly = !f.inStockOnly)) },
                label = { Text(stringResource(R.string.filter_in_stock)) },
            )
        }
    }
}

/** AI refinements are visually and logically separate from regular filters. */
@Composable
private fun AiRefineRow(state: SearchUiState, onToggle: (AiIntent) -> Unit) {
    val active = state.activeIntents
    val refinement = state.results?.refinement
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sparkle), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.ai_refine_title), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.ai_refine_beta),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf(AiIntent.MINIMALIST, AiIntent.NO_BIG_LOGOS, AiIntent.FOR_SCHOOL, AiIntent.PREMIUM_DESIGN)) { intent ->
                FilterChip(
                    selected = intent in active,
                    onClick = { onToggle(intent) },
                    label = { Text(stringResource(intent.labelRes())) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }
        AnimatedVisibility(
            visible = active.isNotEmpty() && refinement != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            if (refinement != null) {
                val unsupported = refinement.unsupported.intersect(active).map { stringResource(it.labelRes()) }
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        stringResource(if (refinement.isModelBased) R.string.ai_refine_model_note else R.string.ai_refine_attribute_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (unsupported.isNotEmpty()) {
                        Text(
                            stringResource(
                                R.string.ai_refine_unsupported,
                                unsupported.joinToString(", ") { "«$it»" },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun SortOption.labelRes(): Int = when (this) {
    SortOption.RELEVANCE -> R.string.sort_relevance
    SortOption.PRICE_ASC -> R.string.sort_price_asc
    SortOption.PRICE_DESC -> R.string.sort_price_desc
    SortOption.TOTAL_PRICE_ASC -> R.string.sort_total_price
    SortOption.DISCOUNT -> R.string.sort_discount
    SortOption.RATING -> R.string.sort_rating
}
