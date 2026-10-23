package com.findly.core.model

import kotlinx.serialization.Serializable

/**
 * The single normalized product model. Every source (official API, partner feed,
 * Findly backend, demo provider) is mapped into this shape by its adapter.
 *
 * Optional fields are nullable on purpose: if a source does not provide a value
 * (rating, delivery cost, colour…), we keep it unknown instead of inventing one.
 */
@Serializable
data class Product(
    /** Globally unique id: "<sourceId>:<sourceProductId>". */
    val id: String,
    val sourceId: String,
    val sourceProductId: String,
    val title: String,
    val description: String? = null,
    val images: List<String> = emptyList(),
    val currentPrice: Money,
    /** Only set when the source reports a previous/list price. Never computed by Findly. */
    val oldPrice: Money? = null,
    val store: Store,
    val brand: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val delivery: Delivery? = null,
    val availability: Availability = Availability.UNKNOWN,
    /** Canonical product page at the seller. */
    val productUrl: String,
    /** Partner link supplied by the source/affiliate network, if any. */
    val affiliateUrl: String? = null,
    val category: Category? = null,
    /** Free-form attributes exactly as the source supplies them (normalized keys, see [AttributeKeys]). */
    val attributes: Map<String, String> = emptyMap(),
    /** Global identifiers used to match the same item across stores. */
    val identifiers: ProductIdentifiers = ProductIdentifiers(),
    /** Paid placement. Must always be rendered with a visible "Sponsored" label. */
    val sponsored: Boolean = false,
    /** True for data produced by a demo/mock provider. UI marks it as such. */
    val isDemo: Boolean = false,
) {
    /** Discount percentage, only when the source gave a real old price higher than the current one. */
    val discountPercent: Int?
        get() {
            val old = oldPrice ?: return null
            if (old.currency != currentPrice.currency || old.amountMinor <= currentPrice.amountMinor) return null
            val pct = (old.amountMinor - currentPrice.amountMinor) * 100.0 / old.amountMinor
            return pct.toInt().takeIf { it >= 1 }
        }

    val colors: List<String>
        get() = attributes[AttributeKeys.COLOR]?.split(',')?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }.orEmpty()

    val sizes: List<String>
        get() = attributes[AttributeKeys.SIZES]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()

    /** Price including delivery when the delivery cost is known; null otherwise. */
    val totalPriceWithKnownDelivery: Money?
        get() {
            val d = delivery ?: return null
            val cost = d.cost ?: return null
            if (cost.currency != currentPrice.currency) return null
            return currentPrice + cost
        }
}

@Serializable
data class ProductIdentifiers(
    val gtin: String? = null,
    val mpn: String? = null,
    /** Normalized model name, e.g. "wh-1000xm6". */
    val model: String? = null,
)

@Serializable
data class Store(
    val id: String,
    val name: String,
    val domain: String? = null,
    val logoUrl: String? = null,
)

@Serializable
data class Delivery(
    /** Known delivery cost. 0 means free. null means the source did not state it. */
    val cost: Money? = null,
    val minDays: Int? = null,
    val maxDays: Int? = null,
    val note: String? = null,
) {
    val isFree: Boolean get() = cost?.amountMinor == 0L
}

@Serializable
enum class Availability { IN_STOCK, LOW_STOCK, OUT_OF_STOCK, PREORDER, UNKNOWN }

object AttributeKeys {
    const val COLOR = "color"
    const val SIZES = "sizes"
    const val MATERIAL = "material"
    const val STYLE = "style"
    const val LOGO = "logo"
    const val LAYOUT = "layout"
    const val SWITCHES = "switches"
    const val CONNECTIVITY = "connectivity"
    const val STORAGE = "storage"
    const val FIT = "fit"
}
