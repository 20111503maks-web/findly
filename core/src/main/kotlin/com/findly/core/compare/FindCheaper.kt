package com.findly.core.compare

import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.provider.SearchAggregator
import com.findly.core.provider.SourceStatus
import com.findly.core.search.ProviderSearchRequest

sealed interface FindCheaperResult {
    data class Found(
        val reference: Product,
        val comparison: OfferComparison,
        /** Savings of the best offer vs. the reference product. Null when the reference is already the best. */
        val savings: Money?,
        val sources: List<SourceStatus>,
    ) : FindCheaperResult

    /** The link is not from a connected store, or the store has no lookup API. */
    data object UnsupportedLink : FindCheaperResult

    data object ProductNotFound : FindCheaperResult
}

/**
 * "Find cheaper": take a product (or a link from a supported store) and look for the SAME product
 * in other connected sources. Only exact/likely matches count toward the best price.
 */
class FindCheaperUseCase(private val aggregator: SearchAggregator) {

    suspend fun byUrl(url: String): FindCheaperResult {
        if (!aggregator.canResolveUrl(url)) return FindCheaperResult.UnsupportedLink
        val product = try {
            aggregator.resolveUrl(url)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return FindCheaperResult.ProductNotFound
        return byProduct(product)
    }

    suspend fun byProduct(product: Product): FindCheaperResult {
        val query = listOfNotNull(product.brand, product.identifiers.model ?: product.title)
            .joinToString(" ")
            .ifBlank { product.title }
        val result = aggregator.search(ProviderSearchRequest(keywords = query, category = product.category, limit = 60))
        val candidates = result.products.filter { it.id != product.id }
        val comparison = OfferComparator.compare(product, candidates)
        return FindCheaperResult.Found(
            reference = product,
            comparison = comparison,
            savings = OfferComparator.savingsFor(product, comparison),
            sources = result.sources,
        )
    }
}
