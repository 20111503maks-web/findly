package com.findly.core.provider

import com.findly.core.model.Product
import com.findly.core.search.ProviderSearchRequest

/**
 * Adapter contract for a product source. Implementations wrap ONE source
 * (official API, affiliate feed, the Findly backend, or a demo dataset) and map it to [Product].
 *
 * Rules for implementations:
 *  - only official APIs, product feeds or authorized partner programs; no scraping or bypassing protection;
 *  - never invent fields the source does not provide (leave them null);
 *  - never put third-party secrets in the Android client — such sources belong on the backend.
 */
interface ProductProvider {
    val info: ProviderInfo

    suspend fun search(request: ProviderSearchRequest): List<Product>

    suspend fun getProduct(sourceProductId: String): Product?

    /** Products for the home feed based on user interests (recent searches, saved categories). */
    suspend fun discover(interests: List<String>, limit: Int): List<Product> = emptyList()

    /** True if [url] points to a product page this source can resolve (used by "Find cheaper"). */
    fun canResolveUrl(url: String): Boolean = false

    suspend fun resolveUrl(url: String): Product? = null
}

data class ProviderInfo(
    val id: String,
    val displayName: String,
    val kind: ProviderKind,
    val capabilities: Set<ProviderCapability>,
)

enum class ProviderKind {
    /** Clearly labelled demo data for development. Not real offers. */
    DEMO,
    /** Findly's own backend that aggregates official/partner sources server-side. */
    FINDLY_BACKEND,
    /** Direct official public API that needs no secret (rare). */
    OFFICIAL_API,
    /** Affiliate network product feed. */
    AFFILIATE_FEED,
}

enum class ProviderCapability { SEARCH, PRODUCT_DETAILS, DISCOVER, URL_LOOKUP, RATINGS, DELIVERY_INFO, AFFILIATE_LINKS }

class ProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)
