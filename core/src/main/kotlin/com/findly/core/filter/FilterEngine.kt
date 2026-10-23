package com.findly.core.filter

import com.findly.core.model.Availability
import com.findly.core.model.Product

/**
 * Applies hard filters and sorting. Unknown data is handled explicitly: e.g. "free delivery only"
 * excludes products whose delivery cost is unknown, because we cannot claim it is free.
 */
class FilterEngine {

    fun apply(products: List<Product>, filters: SearchFilters): List<Product> =
        products.filter { matches(it, filters) }

    fun matches(p: Product, f: SearchFilters): Boolean {
        f.minPrice?.let { if (it.currency == p.currentPrice.currency && p.currentPrice < it) return false }
        f.maxPrice?.let { if (it.currency == p.currentPrice.currency && p.currentPrice > it) return false }
        if (f.storeIds.isNotEmpty() && p.store.id !in f.storeIds) return false
        if (f.brands.isNotEmpty() && (p.brand == null || f.brands.none { it.equals(p.brand, ignoreCase = true) })) return false
        if (f.categories.isNotEmpty() && p.category !in f.categories) return false
        f.minRating?.let { min -> if ((p.rating ?: return false) < min) return false }
        if (f.onlyDiscounted && p.discountPercent == null) return false
        f.minDiscountPercent?.let { min -> if ((p.discountPercent ?: return false) < min) return false }
        if (f.colors.isNotEmpty() && p.colors.none { it in f.colors }) return false
        if (f.sizes.isNotEmpty() && p.sizes.none { s -> f.sizes.any { it.equals(s, ignoreCase = true) } }) return false
        if (f.freeDeliveryOnly && p.delivery?.isFree != true) return false
        f.maxDeliveryDays?.let { max -> if ((p.delivery?.maxDays ?: return false) > max) return false }
        if (f.inStockOnly && p.availability !in setOf(Availability.IN_STOCK, Availability.LOW_STOCK)) return false
        return true
    }

    /**
     * Sorts results. [relevance] provides the relevance order (by product id) for [SortOption.RELEVANCE].
     * Commission / affiliate status is intentionally NOT an input here.
     */
    fun sort(products: List<Product>, option: SortOption, relevance: Map<String, Double> = emptyMap()): List<Product> =
        when (option) {
            SortOption.RELEVANCE -> products.sortedWith(
                compareByDescending<Product> { relevance[it.id] ?: 0.0 }
                    .thenBy { it.availability == Availability.OUT_OF_STOCK }
                    .thenBy { it.currentPrice.amountMinor },
            )
            SortOption.PRICE_ASC -> products.sortedBy { it.currentPrice.amountMinor }
            SortOption.PRICE_DESC -> products.sortedByDescending { it.currentPrice.amountMinor }
            SortOption.TOTAL_PRICE_ASC -> products.sortedWith(
                compareBy<Product> { (it.totalPriceWithKnownDelivery ?: it.currentPrice).amountMinor }
                    .thenBy { it.totalPriceWithKnownDelivery == null },
            )
            SortOption.DISCOUNT -> products.sortedByDescending { it.discountPercent ?: 0 }
            SortOption.RATING -> products.sortedWith(
                compareByDescending<Product> { it.rating ?: -1.0 }.thenByDescending { it.reviewCount ?: 0 },
            )
        }

    fun facets(products: List<Product>): Facets {
        fun <T> count(values: List<T>, label: (T) -> String) =
            values.groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }
                .map { FacetValue(it.key, label(it.key), it.value) }

        val currency = products.groupingBy { it.currentPrice.currency }.eachCount().maxByOrNull { it.value }?.key
        val prices = products.filter { it.currentPrice.currency == currency }.map { it.currentPrice.amountMinor }
        return Facets(
            stores = products.groupBy { it.store.id }.map { (id, list) -> FacetValue(id, list.first().store.name, list.size) }
                .sortedByDescending { it.count },
            brands = count(products.mapNotNull { it.brand }) { it },
            categories = count(products.mapNotNull { it.category }) { it.name },
            colors = count(products.flatMap { it.colors }) { it },
            sizes = count(products.flatMap { it.sizes }) { it },
            priceRange = if (prices.isEmpty()) null else prices.min()..prices.max(),
            currency = currency,
            hasRatings = products.any { it.rating != null },
            hasDiscounts = products.any { it.discountPercent != null },
            hasDeliveryInfo = products.any { it.delivery != null },
            hasAvailabilityInfo = products.any { it.availability != Availability.UNKNOWN },
        )
    }
}
