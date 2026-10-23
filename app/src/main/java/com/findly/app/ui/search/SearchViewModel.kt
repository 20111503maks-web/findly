package com.findly.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findly.app.data.repository.ConnectivityObserver
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.data.repository.ProductRepository
import com.findly.core.ai.AiIntent
import com.findly.core.filter.SearchFilters
import com.findly.core.filter.SortOption
import com.findly.core.model.Product
import com.findly.core.search.ParsedQuery
import com.findly.core.search.PresentedResults
import com.findly.core.search.SearchOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchPhase { IDLE, LOADING, RESULTS, ALL_SOURCES_FAILED, OFFLINE }

data class SearchUiState(
    val query: String = "",
    val phase: SearchPhase = SearchPhase.IDLE,
    val parsed: ParsedQuery? = null,
    val outcome: SearchOutcome? = null,
    val results: PresentedResults? = null,
    val filters: SearchFilters = SearchFilters(),
    val sort: SortOption = SortOption.RELEVANCE,
    /** AI refinements toggled by the user via chips (in addition to ones parsed from the query). */
    val extraIntents: Set<AiIntent> = emptySet(),
) {
    val activeIntents: Set<AiIntent> get() = parsed?.intents.orEmpty() + extraIntents
}

class SearchViewModel(
    private val products: ProductRepository,
    private val favorites: FavoritesRepository,
    private val connectivity: ConnectivityObserver,
    initialQuery: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches = products.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteIds = favorites.favoriteIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var searchJob: Job? = null
    private var presentJob: Job? = null

    init {
        if (!initialQuery.isNullOrBlank()) {
            _state.update { it.copy(query = initialQuery) }
            submit()
        }
    }

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        if (text.isBlank()) {
            searchJob?.cancel()
            _state.update { SearchUiState() }
        }
    }

    fun submit(query: String = _state.value.query) {
        val text = query.trim()
        if (text.isEmpty()) return
        val parsed = products.parse(text)
        _state.update {
            it.copy(
                query = text,
                parsed = parsed,
                phase = SearchPhase.LOADING,
                // What the user typed ("до 70 евро", "белые") becomes visible, editable filters.
                filters = products.engine.initialFilters(parsed),
                extraIntents = emptySet(),
                results = null,
            )
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val outcome = try {
                products.search(parsed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (outcome == null || outcome.allSourcesFailed) {
                _state.update {
                    it.copy(phase = if (!connectivity.isOnline()) SearchPhase.OFFLINE else SearchPhase.ALL_SOURCES_FAILED, outcome = outcome)
                }
                return@launch
            }
            _state.update { it.copy(outcome = outcome) }
            present()
        }
    }

    fun retry() = submit()

    fun setFilters(filters: SearchFilters) {
        _state.update { it.copy(filters = filters) }
        present()
    }

    fun clearFilters() = setFilters(SearchFilters())

    fun setSort(sort: SortOption) {
        _state.update { it.copy(sort = sort) }
        present()
    }

    fun toggleIntent(intent: AiIntent) {
        val s = _state.value
        val parsed = s.parsed
        _state.value = if (parsed != null && intent in parsed.intents) {
            // Removing an intent the parser found in the query text.
            val updated = parsed.copy(intents = parsed.intents - intent)
            s.copy(parsed = updated, outcome = s.outcome?.copy(parsed = updated))
        } else {
            s.copy(extraIntents = if (intent in s.extraIntents) s.extraIntents - intent else s.extraIntents + intent)
        }
        present()
    }

    fun toggleFavorite(product: Product) {
        viewModelScope.launch { favorites.toggle(product) }
    }

    fun clearRecent() {
        viewModelScope.launch { products.clearRecentSearches() }
    }

    private fun present() {
        val s = _state.value
        val outcome = s.outcome ?: return
        presentJob?.cancel()
        presentJob = viewModelScope.launch {
            val presented = products.engine.present(outcome, s.filters, s.sort, s.extraIntents)
            _state.update { it.copy(results = presented, phase = SearchPhase.RESULTS) }
        }
    }
}
