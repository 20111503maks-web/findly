package com.findly.core.filter

import com.findly.core.model.Category
import com.findly.core.model.Money

/** Regular, data-backed filters. Every filter here only uses fields the source actually provided. */
data class SearchFilters(
    val minPrice: Money? = null,
    val maxPrice: Money? = null,
    val storeIds: Set<String> = emptySet(),
    val brands: Set<String> = emptySet(),
    val categories: Set<Category> = emptySet(),
    val minRating: Double? = null,
    val onlyDiscounted: Boolean = false,
    val minDiscountPercent: Int? = null,
    val colors: Set<String> = emptySet(),
    val sizes: Set<String> = emptySet(),
    val freeDeliveryOnly: Boolean = false,
    val maxDeliveryDays: Int? = null,
    val inStockOnly: Boolean = false,
) {
    val activeCount: Int
        get() = listOf(
            minPrice != null || maxPrice != null,
            storeIds.isNotEmpty(),
            brands.isNotEmpty(),
            categories.isNotEmpty(),
            minRating != null,
            onlyDiscounted || minDiscountPercent != null,
            colors.isNotEmpty(),
            sizes.isNotEmpty(),
            freeDeliveryOnly || maxDeliveryDays != null,
            inStockOnly,
        ).count { it }

    val isEmpty: Boolean get() = activeCount == 0
}

enum class SortOption {
    RELEVANCE,
    PRICE_ASC,
    PRICE_DESC,
    /** Item price + known delivery. Offers with unknown delivery are ranked by item price after known totals tie. */
    TOTAL_PRICE_ASC,
    DISCOUNT,
    RATING,
}

/** Values available for filtering, computed from the current result set. */
data class Facets(
    val stores: List<FacetValue<String>> = emptyList(),
    val brands: List<FacetValue<String>> = emptyList(),
    val categories: List<FacetValue<Category>> = emptyList(),
    val colors: List<FacetValue<String>> = emptyList(),
    val sizes: List<FacetValue<String>> = emptyList(),
    val priceRange: ClosedRange<Long>? = null,
    val currency: String? = null,
    val hasRatings: Boolean = false,
    val hasDiscounts: Boolean = false,
    val hasDeliveryInfo: Boolean = false,
    val hasAvailabilityInfo: Boolean = false,
)

data class FacetValue<T>(val value: T, val label: String, val count: Int)
