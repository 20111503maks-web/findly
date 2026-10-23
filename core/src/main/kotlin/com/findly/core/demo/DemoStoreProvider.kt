package com.findly.core.demo

import com.findly.core.model.Product
import com.findly.core.model.Store
import com.findly.core.provider.ProductProvider
import com.findly.core.provider.ProviderCapability
import com.findly.core.provider.ProviderException
import com.findly.core.provider.ProviderInfo
import com.findly.core.provider.ProviderKind
import com.findly.core.search.ProviderSearchRequest
import com.findly.core.search.TextNormalizer
import kotlinx.coroutines.delay
import java.net.URI

/**
 * Demo adapter for one fictional store from [DemoCatalog]. Behaves like a real adapter
 * (latency, keyword search, price filters, URL lookup) so the rest of the app is exercised end-to-end.
 *
 * @param latencyMs simulated network latency, to show skeleton loading.
 * @param shouldFail lets developer settings simulate a failing source.
 */
class DemoStoreProvider(
    private val store: Store,
    private val latencyMs: Long = 600,
    private val shouldFail: () -> Boolean = { false },
) : ProductProvider {

    override val info = ProviderInfo(
        id = "demo-${store.id}",
        displayName = "${store.name} (demo)",
        kind = ProviderKind.DEMO,
        capabilities = setOf(
            ProviderCapability.SEARCH,
            ProviderCapability.PRODUCT_DETAILS,
            ProviderCapability.DISCOVER,
            ProviderCapability.URL_LOOKUP,
            ProviderCapability.RATINGS,
            ProviderCapability.DELIVERY_INFO,
            ProviderCapability.AFFILIATE_LINKS,
        ),
    )

    private val products: List<Product> get() = DemoCatalog.forStore(store.id)

    override suspend fun search(request: ProviderSearchRequest): List<Product> {
        simulateNetwork()
        val tokens = TextNormalizer.tokenize(request.keywords)
        return products
            .asSequence()
            .filter { p -> request.maxPrice?.let { it.currency != p.currentPrice.currency || p.currentPrice <= it } ?: true }
            .filter { p -> request.minPrice?.let { it.currency != p.currentPrice.currency || p.currentPrice >= it } ?: true }
            .map { it to TextNormalizer.matchScore(tokens, it) + if (request.category != null && it.category == request.category) 0.3 else 0.0 }
            .filter { (_, score) -> tokens.isEmpty() || score >= 0.5 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(request.limit)
            .toList()
    }

    override suspend fun getProduct(sourceProductId: String): Product? {
        simulateNetwork()
        return products.firstOrNull { it.sourceProductId == sourceProductId }
    }

    override suspend fun discover(interests: List<String>, limit: Int): List<Product> {
        simulateNetwork()
        val tokens = interests.flatMap { TextNormalizer.tokenize(it) }
        val ranked = products.sortedWith(
            compareByDescending<Product> { if (tokens.isEmpty()) 0.0 else TextNormalizer.matchScore(tokens, it) }
                .thenByDescending { it.discountPercent ?: 0 }
                .thenByDescending { it.reviewCount ?: 0 },
        )
        return ranked.take(limit)
    }

    override fun canResolveUrl(url: String): Boolean =
        runCatching { URI(url.trim()).host?.removePrefix("www.") == store.domain }.getOrDefault(false)

    override suspend fun resolveUrl(url: String): Product? {
        if (!canResolveUrl(url)) return null
        val sku = runCatching { URI(url.trim()).path.trimEnd('/').substringAfterLast('/') }.getOrNull() ?: return null
        return getProduct(sku)
    }

    private suspend fun simulateNetwork() {
        if (latencyMs > 0) delay(latencyMs)
        if (shouldFail()) throw ProviderException("${store.name} is not responding (simulated)")
    }
}
