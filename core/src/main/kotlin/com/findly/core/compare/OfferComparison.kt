package com.findly.core.compare

import com.findly.core.model.Availability
import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.search.TextNormalizer

/** How sure we are that two listings are the same physical product. */
enum class MatchConfidence {
    /** Same GTIN/EAN or same brand + MPN. */
    EXACT,
    /** Same brand + normalized model name. */
    LIKELY,
    /** Only titles are similar. Shown as "similar", never used for "best price" or savings. */
    SIMILAR,
}

data class ComparedOffer(
    val product: Product,
    /** Item price + delivery when delivery cost is known; otherwise just the item price. */
    val effectivePrice: Money,
    val deliveryKnown: Boolean,
    val confidence: MatchConfidence,
    val isBest: Boolean = false,
)

data class OfferComparison(
    val offers: List<ComparedOffer>,
    val best: ComparedOffer?,
    /** Difference between the most expensive and the best comparable offer. */
    val maxSavings: Money?,
    /** True if at least one offer has unknown delivery cost, so the ranking may change at checkout. */
    val hasUnknownDelivery: Boolean,
)

/** A product found in several stores. */
data class ProductGroup(
    val key: String,
    val offers: List<Product>,
) {
    val representative: Product get() = offers.first()
    val storeCount: Int get() = offers.map { it.store.id }.distinct().size
    val lowestPrice: Money get() = offers.minBy { it.currentPrice.amountMinor }.currentPrice
}

object ProductMatcher {

    /** Grouping key from strong identifiers only. Null when the product cannot be matched reliably. */
    fun matchKey(p: Product): String? {
        p.identifiers.gtin?.filter { it.isDigit() }?.takeIf { it.length >= 8 }?.let { return "gtin:${it.trimStart('0')}" }
        val brand = p.brand?.let { TextNormalizer.normalize(it).trim() }?.takeIf { it.isNotEmpty() } ?: return null
        p.identifiers.mpn?.let { return "mpn:$brand:${normalizeModel(it)}" }
        p.identifiers.model?.let { return "model:$brand:${normalizeModel(it)}" }
        return null
    }

    fun confidence(a: Product, b: Product): MatchConfidence? {
        val ga = a.identifiers.gtin?.filter(Char::isDigit)?.trimStart('0')
        val gb = b.identifiers.gtin?.filter(Char::isDigit)?.trimStart('0')
        if (!ga.isNullOrEmpty() && ga == gb) return MatchConfidence.EXACT
        val sameBrand = a.brand != null && a.brand.equals(b.brand, ignoreCase = true)
        if (sameBrand && a.identifiers.mpn != null && a.identifiers.mpn.let(::normalizeModel) == b.identifiers.mpn?.let(::normalizeModel)) {
            return MatchConfidence.EXACT
        }
        if (sameBrand && a.identifiers.model != null && normalizeModel(a.identifiers.model) == b.identifiers.model?.let(::normalizeModel)) {
            return MatchConfidence.LIKELY
        }
        if (sameBrand && titleSimilarity(a.title, b.title) >= 0.6) return MatchConfidence.SIMILAR
        return null
    }

    fun normalizeModel(s: String): String = TextNormalizer.normalize(s).replace(Regex("[^\\p{L}\\p{N}]"), "")

    /** Jaccard similarity of title tokens. */
    fun titleSimilarity(a: String, b: String): Double {
        val ta = TextNormalizer.tokenize(a).toSet()
        val tb = TextNormalizer.tokenize(b).toSet()
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        return (ta intersect tb).size.toDouble() / (ta union tb).size
    }

    /** Groups search results so the same product from several stores becomes one entry. */
    fun group(products: List<Product>): List<ProductGroup> {
        val groups = LinkedHashMap<String, MutableList<Product>>()
        for (p in products) {
            val key = matchKey(p) ?: "single:${p.id}"
            groups.getOrPut(key) { mutableListOf() } += p
        }
        return groups.map { (key, list) ->
            ProductGroup(key, list.sortedBy { it.currentPrice.amountMinor })
        }
    }
}

object OfferComparator {

    /**
     * Compares offers of the same product. The best offer is the cheapest effective price among
     * in-stock (or unknown-stock) offers with EXACT/LIKELY confidence. Delivery is included when known.
     */
    fun compare(reference: Product, candidates: List<Product>): OfferComparison {
        val all = (listOf(reference) + candidates).distinctBy { it.id }
        val compared = all.mapNotNull { p ->
            val confidence = if (p.id == reference.id) MatchConfidence.EXACT else ProductMatcher.confidence(reference, p) ?: return@mapNotNull null
            if (p.currentPrice.currency != reference.currentPrice.currency) return@mapNotNull null
            val total = p.totalPriceWithKnownDelivery
            ComparedOffer(
                product = p,
                effectivePrice = total ?: p.currentPrice,
                deliveryKnown = total != null,
                confidence = confidence,
            )
        }
        val eligible = compared.filter {
            it.confidence != MatchConfidence.SIMILAR && it.product.availability != Availability.OUT_OF_STOCK
        }
        val best = eligible.minWithOrNull(compareBy<ComparedOffer> { it.effectivePrice.amountMinor }.thenBy { !it.deliveryKnown })
        val sorted = compared
            .map { it.copy(isBest = it.product.id == best?.product?.id) }
            .sortedWith(
                compareBy<ComparedOffer> { it.confidence == MatchConfidence.SIMILAR }
                    .thenBy { it.product.availability == Availability.OUT_OF_STOCK }
                    .thenBy { it.effectivePrice.amountMinor },
            )
        val maxSavings = if (best != null && eligible.size > 1) {
            val worst = eligible.maxBy { it.effectivePrice.amountMinor }
            (worst.effectivePrice - best.effectivePrice).takeIf { it.amountMinor > 0 }
        } else {
            null
        }
        return OfferComparison(
            offers = sorted,
            best = sorted.firstOrNull { it.isBest },
            maxSavings = maxSavings,
            hasUnknownDelivery = eligible.any { !it.deliveryKnown },
        )
    }

    /** Savings of the best offer relative to the given product, if positive. */
    fun savingsFor(product: Product, comparison: OfferComparison): Money? {
        val best = comparison.best ?: return null
        if (best.product.id == product.id) return null
        val own = product.totalPriceWithKnownDelivery ?: product.currentPrice
        if (own.currency != best.effectivePrice.currency) return null
        return (own - best.effectivePrice).takeIf { it.amountMinor > 0 }
    }
}
