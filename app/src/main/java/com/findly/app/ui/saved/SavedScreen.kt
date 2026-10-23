package com.findly.app.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.findly.app.R
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.ProductCard
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.model.Product
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavedViewModel(private val favorites: FavoritesRepository) : ViewModel() {
    /** null while loading from the database. */
    val items: StateFlow<List<Product>?> = favorites.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(product: Product) {
        viewModelScope.launch { favorites.remove(product.id) }
    }
}

@Composable
fun SavedScreen(onOpenProduct: (String) -> Unit, onExplore: () -> Unit) {
    val vm = containerViewModel { SavedViewModel(it.favoritesRepository) }
    val items by vm.items.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.statusBarsPadding().padding(top = 20.dp)) {
                Text(stringResource(R.string.tab_saved), style = MaterialTheme.typography.displaySmall)
                val count = items?.size ?: 0
                if (count > 0) {
                    Text(
                        pluralStringResource(R.plurals.saved_count, count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val list = items
        if (list != null && list.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = rememberVectorPainter(Icons.Filled.FavoriteBorder),
                    title = stringResource(R.string.state_saved_empty_title),
                    message = stringResource(R.string.state_saved_empty_message),
                    actionLabel = stringResource(R.string.action_explore),
                    onAction = onExplore,
                    accent = FindlyTheme.extra.discount,
                )
            }
        } else if (list != null) {
            items(list, key = { it.id }) { p ->
                ProductCard(
                    product = p,
                    isFavorite = true,
                    onClick = { onOpenProduct(p.id) },
                    onToggleFavorite = { vm.remove(p) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
