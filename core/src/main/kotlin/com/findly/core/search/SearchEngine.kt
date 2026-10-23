package com.findly.core.search

import com.findly.core.ai.AiIntent
import com.findly.core.ai.AiRefinementEngine
import com.findly.core.ai.RefinementRequest
import com.findly.core.ai.RefinementResult
import com.findly.core.compare.ProductGroup
import com.findly.core.compare.ProductMatcher
import com.findly.core.filter.Facets
import com.findly.core.filter.FilterEngine
import com.findly.core.filter.SearchFilters
import com.findly.core.filter.SortOption
import com.findly.core.model.Product
import com.findly.core.provider.AggregatedResult
import com.findly.core.provider.SearchAggregator
import com.findly.core.provider.SourceStatus

/** Raw outcome of a query, before user filters/sorting are applied (those are cheap and local). */
data class SearchOutcome(
    val parsed: ParsedQuery,
    val products: List<Product>,
    val relevance: Map<String, Double>,
    val sources: List<SourceStatus>,
    /** True when no product matched all keywords and we fell back to partial matches. */
    val partialMatches: Boolean,
) {
    val failedSources get() = sources.filter { it !is SourceStatus.Ok }
    val allSourcesFailed get() = sources.isNotEmpty() && failedSources.size == sources.size
}

data class PresentedResults(
    /** Organic results grouped by product; ranking ignores affiliate/commission data. */
    val groups: List<ProductGroup>,
    /** Paid placements, always shown separately with a "Sponsored" label. */
    val sponsored: List<Product>,
    val facets: Facets,
    val totalBeforeFilters: Int,
    val refinement: RefinementResult?,
)

/**
 * Orchestrates: parse → query all sources → score relevance → (user) filters → sort → group → optional refinement.
 */
class SearchEngine(
    private val aggregator: SearchAggregator,
    private val parser: QueryParser = QueryParser(),
    private val filterEngine: FilterEngine = FilterEngine(),
    private val refinementEngine: AiRefinementEngine? = null,
) {

    fun parse(query: String): ParsedQuery = parser.parse(query)

    /** Filters that come straight from what the user typed ("до 70 евро", "белые", "размер 42"). */
    fun initialFilters(parsed: ParsedQuery): SearchFilters = SearchFilters(
        minPrice = parsed.minPrice,
        maxPrice = parsed.maxPrice,
        colors = parsed.colors,
        sizes = setOfNotNull(parsed.size),
    )

    suspend fun search(parsed: ParsedQuery): SearchOutcome {
        val request = ProviderSearchRequest(
            keywords = (parsed.keywords + parsed.colors).joinToString(" "),
            category = parsed.category,
            minPrice = parsed.minPrice,
            maxPrice = parsed.maxPrice,
        )
        val result: AggregatedResult = aggregator.search(request)
        return score(parsed, result)
    }

    private fun score(parsed: ParsedQuery, result: AggregatedResult): SearchOutcome {
        val tokens = parsed.keywords
        val scored = result.products.associate { p ->
            var s = TextNormalizer.matchScore(tokens, p)
            if (parsed.category != null && p.category == parsed.category) s += 0.25
            p.id to s
        }
        val full = result.products.filter { (scored[it.id] ?: 0.0) >= 1.0 }
        val partial = full.isEmpty() && tokens.size > 1
        val kept = when {
            full.isNotEmpty() -> full
            partial -> result.products.filter { (scored[it.id] ?: 0.0) >= 0.5 }
            else -> emptyList()
        }
        return SearchOutcome(parsed, kept, scored, result.sources, partialMatches = partial && kept.isNotEmpty())
    }

    suspend fun present(
        outcome: SearchOutcome,
        filters: SearchFilters,
        sort: SortOption,
        extraIntents: Set<AiIntent> = emptySet(),
        reference: Product? = null,
    ): PresentedResults {
        val (sponsored, organic) = outcome.products.partition { it.sponsored }
        val filtered = filterEngine.apply(organic, filters)
        var sorted = filterEngine.sort(filtered, sort, outcome.relevance)

        val intents = outcome.parsed.intents + extraIntents
        var refinement: RefinementResult? = null
        if (intents.isNotEmpty() && refinementEngine != null) {
            refinement = refinementEngine.refine(sorted, RefinementRequest(intents, reference, outcome.parsed.original))
            if (sort == SortOption.RELEVANCE) sorted = refinement.products
        }

        return PresentedResults(
            groups = ProductMatcher.group(sorted),
            sponsored = filterEngine.apply(sponsored, filters).take(MAX_SPONSORED),
            facets = filterEngine.facets(organic),
            totalBeforeFilters = organic.size,
            refinement = refinement,
        )
    }

    private companion object {
        const val MAX_SPONSORED = 2
    }
}
