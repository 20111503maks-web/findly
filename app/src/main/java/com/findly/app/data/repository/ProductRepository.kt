package com.findly.app.data.repository

import com.findly.app.data.local.RecentSearchDao
import com.findly.app.data.local.RecentSearchEntity
import com.findly.core.ai.SimilarProducts
import com.findly.core.compare.FindCheaperResult
import com.findly.core.compare.FindCheaperUseCase
import com.findly.core.model.Product
import com.findly.core.provider.AggregatedResult
import com.findly.core.provider.ProductProvider
import com.findly.core.provider.ProviderCapability
import com.findly.core.provider.ProviderRegistry
import com.findly.core.provider.SearchAggregator
import com.findly.core.search.ParsedQuery
import com.findly.core.search.ProviderSearchRequest
import com.findly.core.search.SearchEngine
import com.findly.core.search.SearchOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single entry point for product data. Keeps a small in-memory cache so screens can open a product
 * by id without re-querying, and falls back to the provider (or the saved snapshot) when needed.
 */
class ProductRepository(
    private val registry: ProviderRegistry,
    private val aggregator: SearchAggregator,
    val engine: SearchEngine,
    private val findCheaper: FindCheaperUseCase,
    private val recentSearchDao: RecentSearchDao,
    private val favorites: FavoritesRepository,
    private val backend: ProductProvider?,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cache = object : LinkedHashMap<String, Product>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Product>?) = size > 500
    }

    val recentSearches: Flow<List<String>> = recentSearchDao.observe().map { list -> list.map { it.query } }

    fun parse(query: String): ParsedQuery = engine.parse(query)

    suspend fun search(parsed: ParsedQuery): SearchOutcome {
        if (parsed.original.isNotBlank()) {
            recentSearchDao.upsert(RecentSearchEntity(parsed.original.trim(), clock()))
        }
        return engine.search(parsed).also { remember(it.products) }
    }

    suspend fun clearRecentSearches() = recentSearchDao.clear()

    /** Home feed: based on recent searches and categories of saved items, if any. */
    suspend fun feed(interests: List<String>): AggregatedResult =
        aggregator.discover(interests, limit = 12).also { remember(it.products) }

    fun cached(id: String): Product? = synchronized(cache) { cache[id] }

    suspend fun product(id: String): Product? {
        cached(id)?.let { return it }
        favorites.get(id)?.let { return it }
        // Ids are "<sourceId>:<sourceProductId>"; ask the source directly.
        val sourceId = id.substringBefore(':', missingDelimiterValue = "")
        val provider = registry.get(sourceId) ?: return null
        return runCatching { provider.getProduct(id.substringAfter(':')) }.getOrNull()?.also { remember(listOf(it)) }
    }

    /** Fetches fresh data from the product's source. Returns null if the source is unavailable. */
    suspend fun refresh(product: Product): Product? = runCatching {
        val provider = registry.get(product.sourceId)
        val fresh = provider?.getProduct(product.sourceProductId) ?: backend?.getProduct(product.id)
        fresh?.also { remember(listOf(it)) }
    }.getOrNull()

    suspend fun findCheaper(product: Product): FindCheaperResult =
        findCheaper.byProduct(product).also { if (it is FindCheaperResult.Found) remember(it.comparison.offers.map { o -> o.product }) }

    suspend fun findCheaper(url: String): FindCheaperResult =
        findCheaper.byUrl(url).also { if (it is FindCheaperResult.Found) remember(it.comparison.offers.map { o -> o.product }) }

    suspend fun similar(product: Product): List<Product> {
        val pool = aggregator.search(ProviderSearchRequest(keywords = "", category = product.category, limit = 40)).products
        remember(pool)
        return SimilarProducts.find(product, pool, limit = 10)
    }

    fun urlLookupProviders(): List<ProductProvider> = registry.withCapability(ProviderCapability.URL_LOOKUP)

    fun providers(): List<ProductProvider> = registry.all()

    fun setProviderEnabled(id: String, enabled: Boolean) = registry.setEnabled(id, enabled)

    private fun remember(products: List<Product>) = synchronized(cache) {
        products.forEach { cache[it.id] = it }
    }
}
