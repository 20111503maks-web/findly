package com.findly.app.data.remote

import com.findly.core.model.Availability
import com.findly.core.model.Category
import com.findly.core.model.Delivery
import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.model.ProductIdentifiers
import com.findly.core.model.Store
import com.findly.core.provider.ProductProvider
import com.findly.core.provider.ProviderCapability
import com.findly.core.provider.ProviderInfo
import com.findly.core.provider.ProviderKind
import com.findly.core.search.ProviderSearchRequest

/**
 * Adapter for the Findly backend, which aggregates official store APIs and affiliate feeds
 * server-side. Only registered when FINDLY_API_BASE_URL is configured.
 */
class BackendProductProvider(private val api: FindlyApi) : ProductProvider {

    override val info = ProviderInfo(
        id = "findly-backend",
        displayName = "Findly",
        kind = ProviderKind.FINDLY_BACKEND,
        capabilities = ProviderCapability.entries.toSet(),
    )

    override suspend fun search(request: ProviderSearchRequest): List<Product> =
        api.search(
            query = request.keywords,
            category = request.category?.name,
            minPriceMinor = request.minPrice?.amountMinor,
            maxPriceMinor = request.maxPrice?.amountMinor,
            currency = (request.maxPrice ?: request.minPrice)?.currency,
            limit = request.limit,
            locale = request.locale,
        ).items.mapNotNull { it.toDomainOrNull() }

    override suspend fun getProduct(sourceProductId: String): Product? = api.product(sourceProductId).toDomainOrNull()

    override suspend fun discover(interests: List<String>, limit: Int): List<Product> =
        api.discover(interests.joinToString(","), limit).items.mapNotNull { it.toDomainOrNull() }

    // The backend knows which stores it can resolve; let it decide.
    override fun canResolveUrl(url: String): Boolean = url.startsWith("https://")

    override suspend fun resolveUrl(url: String): Product? = api.resolveUrl(url).toDomainOrNull()
}

/** Maps a backend DTO into the domain model. Invalid items (no price, no URL) are dropped, not patched. */
fun ProductDto.toDomainOrNull(): Product? {
    if (title.isBlank() || productUrl.isBlank()) return null
    return Product(
        id = id,
        sourceId = source,
        sourceProductId = sourceProductId,
        title = title,
        description = description,
        images = images,
        currentPrice = Money(price.amountMinor, price.currency),
        oldPrice = oldPrice?.let { Money(it.amountMinor, it.currency) },
        store = Store(store.id, store.name, store.domain, store.logoUrl),
        brand = brand,
        rating = rating?.takeIf { it in 0.0..5.0 },
        reviewCount = reviewCount,
        delivery = delivery?.let { d -> Delivery(d.cost?.let { Money(it.amountMinor, it.currency) }, d.minDays, d.maxDays, d.note) },
        availability = availability?.let { a -> Availability.entries.firstOrNull { it.name.equals(a, ignoreCase = true) } } ?: Availability.UNKNOWN,
        productUrl = productUrl,
        affiliateUrl = affiliateUrl,
        category = category?.let { c -> Category.entries.firstOrNull { it.name.equals(c, ignoreCase = true) } },
        attributes = attributes,
        identifiers = ProductIdentifiers(gtin = gtin, mpn = mpn, model = model),
        sponsored = sponsored,
        isDemo = false,
    )
}
