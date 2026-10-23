package com.findly.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.data.repository.ProductRepository
import com.findly.core.compare.ProductGroup
import com.findly.core.compare.ProductMatcher
import com.findly.core.model.Product
import com.findly.core.provider.SourceStatus
import com.findly.core.search.TextNormalizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val feed: List<ProductGroup> = emptyList(),
    val sources: List<SourceStatus> = emptyList(),
    /** True when the feed is based on the user's own searches/saved items. */
    val personalized: Boolean = false,
    val isDemoMode: Boolean = false,
)

class HomeViewModel(
    private val products: ProductRepository,
    private val favorites: FavoritesRepository,
    isDemoMode: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState(isDemoMode = isDemoMode))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = favorites.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var loadJob: Job? = null

    init {
        load(initial = true)
    }

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { if (initial) it.copy(loading = true) else it.copy(refreshing = true) }
            val recent = products.recentSearches.first().take(3)
            val savedCategories = favorites.all().mapNotNull { it.category?.keywords?.firstOrNull() }.distinct().take(2)
            val interests = recent + savedCategories
            val result = products.feed(interests)
            _state.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    feed = ProductMatcher.group(rank(result.products.filterNot(Product::sponsored), interests)).take(20),
                    sources = result.sources,
                    personalized = interests.isNotEmpty(),
                )
            }
        }
    }

    /** Interleaves stores: interest match first, then real discounts, then popularity. */
    private fun rank(items: List<Product>, interests: List<String>): List<Product> {
        val tokens = interests.flatMap { TextNormalizer.tokenize(it) }
        return items.sortedWith(
            compareByDescending<Product> { if (tokens.isEmpty()) 0.0 else TextNormalizer.matchScore(tokens, it) }
                .thenByDescending { it.discountPercent ?: 0 }
                .thenByDescending { it.reviewCount ?: 0 },
        )
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch { favorites.toggle(product) }
    }
}
