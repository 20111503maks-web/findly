package com.findly.app.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findly.app.data.repository.CreateAlertResult
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.data.repository.PriceTrackingRepository
import com.findly.app.data.repository.ProductRepository
import com.findly.core.compare.FindCheaperResult
import com.findly.core.compare.OfferComparison
import com.findly.core.link.OutboundLink
import com.findly.core.link.OutboundLinkResolver
import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.pricing.HistoryRange
import com.findly.core.pricing.PriceAlert
import com.findly.core.pricing.PriceHistoryAnalyzer
import com.findly.core.pricing.PriceHistorySummary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductUiState(
    val loading: Boolean = true,
    val product: Product? = null,
    val notFound: Boolean = false,
    val offersLoading: Boolean = true,
    val comparison: OfferComparison? = null,
    val similarLoading: Boolean = true,
    val similar: List<Product> = emptyList(),
)

sealed interface ProductEvent {
    data object AlertCreated : ProductEvent
    data class AlertLimit(val limit: Int) : ProductEvent
}

class ProductViewModel(
    private val productId: String,
    private val products: ProductRepository,
    private val favorites: FavoritesRepository,
    private val tracking: PriceTrackingRepository,
    private val linkResolver: OutboundLinkResolver,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(ProductUiState())
    val state: StateFlow<ProductUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ProductEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProductEvent> = _events

    val isFavorite: StateFlow<Boolean> = favorites.favoriteIds.map { productId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val alert: StateFlow<PriceAlert?> = tracking.alertFor(productId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val favoriteIds: StateFlow<Set<String>> = favorites.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val range = MutableStateFlow(HistoryRange.MONTH)
    val historyRange: StateFlow<HistoryRange> = range.asStateFlow()

    val history: StateFlow<PriceHistorySummary?> = tracking.history(productId).combine(range) { points, r ->
        PriceHistoryAnalyzer.summarize(points, r, clock())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val product = products.product(productId)
        if (product == null) {
            _state.update { it.copy(loading = false, notFound = true, offersLoading = false, similarLoading = false) }
            return@launch
        }
        _state.update { it.copy(loading = false, product = product) }

        launch {
            // Fresh price from the source; tracked products get a real observation recorded.
            val fresh = products.refresh(product) ?: product
            if (fresh != product) _state.update { it.copy(product = fresh) }
            val tracked = favorites.isFavorite(fresh.id) || tracking.alertFor(productId).first() != null
            if (tracked) {
                tracking.record(fresh)
                favorites.updateSnapshot(fresh)
            }
        }
        launch {
            val result = runCatching { products.findCheaper(product) }.getOrNull()
            _state.update { it.copy(offersLoading = false, comparison = (result as? FindCheaperResult.Found)?.comparison) }
        }
        launch {
            val similar = runCatching { products.similar(product) }.getOrDefault(emptyList())
            _state.update { it.copy(similarLoading = false, similar = similar) }
        }
    }

    fun setRange(r: HistoryRange) {
        range.value = r
    }

    fun toggleFavorite() {
        val product = _state.value.product ?: return
        toggleFavorite(product)
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch {
            val nowFavorite = favorites.toggle(product)
            // Saving starts tracking: record the first real observation.
            if (nowFavorite) tracking.record(product)
        }
    }

    fun createAlert(target: Money) {
        val product = _state.value.product ?: return
        viewModelScope.launch {
            when (val r = tracking.createAlert(product, target)) {
                CreateAlertResult.Created -> _events.emit(ProductEvent.AlertCreated)
                is CreateAlertResult.LimitReached -> _events.emit(ProductEvent.AlertLimit(r.limit))
            }
        }
    }

    fun deleteAlert() {
        val a = alert.value ?: return
        viewModelScope.launch { tracking.delete(a.id) }
    }

    fun link(product: Product): OutboundLink? = linkResolver.resolve(product)
}
