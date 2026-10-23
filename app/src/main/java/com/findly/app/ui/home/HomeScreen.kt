package com.findly.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.findly.app.R
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.components.DemoModeBanner
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.ProductCard
import com.findly.app.ui.components.ProductCardSkeleton
import com.findly.app.ui.components.SourceStatusBanner
import com.findly.app.ui.theme.FindlyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: (query: String?) -> Unit,
    onOpenPhotoSearch: () -> Unit,
    onOpenFindCheaper: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    val vm = containerViewModel { HomeViewModel(it.productRepository, it.favoritesRepository, it.isDemoMode) }
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth {
                Column(Modifier.statusBarsPadding().padding(top = 20.dp)) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        stringResource(R.string.home_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    HeroSearchField(onClick = { onOpenSearch(null) }, onCamera = onOpenPhotoSearch)
                    Spacer(Modifier.height(14.dp))
                    SuggestionChips(onClick = onOpenSearch)
                }
            }
            fullWidth { FindCheaperCard(onOpenFindCheaper) }
            if (state.isDemoMode) fullWidth { DemoModeBanner() }
            fullWidth {
                Column {
                    Text(
                        stringResource(if (state.personalized) R.string.home_feed_for_you else R.string.home_feed_popular),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        stringResource(if (state.personalized) R.string.home_feed_for_you_hint else R.string.home_feed_popular_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.sources.isNotEmpty()) {
                        SourceStatusBanner(state.sources, Modifier.padding(top = 12.dp), onRetry = vm::refresh)
                    }
                }
            }

            when {
                state.loading -> items(6) { ProductCardSkeleton() }
                state.feed.isEmpty() -> fullWidth {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_cloud_off),
                        title = stringResource(R.string.state_feed_empty_title),
                        message = stringResource(R.string.state_feed_empty_message),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = vm::refresh,
                    )
                }
                else -> items(state.feed, key = { it.key }) { group ->
                    val product = group.representative
                    ProductCard(
                        product = product,
                        isFavorite = product.id in favorites,
                        onClick = { onOpenProduct(product.id) },
                        onToggleFavorite = { vm.toggleFavorite(product) },
                        storeCount = group.storeCount,
                        lowestPrice = group.lowestPrice,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun HeroSearchField(onClick: () -> Unit, onCamera: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.search_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onCamera),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_camera),
                contentDescription = stringResource(R.string.photo_search_title),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SuggestionChips(onClick: (String) -> Unit) {
    val suggestions = listOf(
        stringResource(R.string.suggestion_1),
        stringResource(R.string.suggestion_2),
        stringResource(R.string.suggestion_3),
        stringResource(R.string.suggestion_4),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(suggestions) { s ->
            Text(
                s,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onClick(s) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun FindCheaperCard(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.secondary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), FindlyTheme.extra.bestPrice.copy(alpha = 0.10f))))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.find_cheaper_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.find_cheaper_card_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_trending_down), contentDescription = null, tint = FindlyTheme.extra.bestPrice)
        }
    }
}
