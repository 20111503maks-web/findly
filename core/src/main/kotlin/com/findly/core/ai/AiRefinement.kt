package com.findly.core.ai

import com.findly.core.model.AttributeKeys
import com.findly.core.model.Product

/**
 * Vague, subjective wishes. They are NOT regular filters: whether a product is "minimalist"
 * is a judgement that needs data (structured attributes) or a model (image/text understanding).
 */
enum class AiIntent {
    MINIMALIST,
    NO_BIG_LOGOS,
    FOR_SCHOOL,
    PREMIUM_DESIGN,
    SIMILAR_TO,
}

data class RefinementRequest(
    val intents: Set<AiIntent>,
    /** Reference product for [AiIntent.SIMILAR_TO]. */
    val reference: Product? = null,
    val freeText: String? = null,
)

/** Per-product verdict for one intent. UNKNOWN means "no data", never "doesn't match". */
enum class Verdict { MATCHES, DOES_NOT_MATCH, UNKNOWN }

data class RefinementResult(
    /** Same products, re-ordered. Refinement never adds products and never hides products with unknown data. */
    val products: List<Product>,
    val verdicts: Map<String, Map<AiIntent, Verdict>>,
    /** Intents this engine cannot evaluate (e.g. needs a backend model). */
    val unsupported: Set<AiIntent>,
    val engineName: String,
    val isModelBased: Boolean,
)

/** Re-ranking layer, separate from [com.findly.core.filter.FilterEngine]. */
interface AiRefinementEngine {
    val name: String
    fun supports(intent: AiIntent): Boolean
    suspend fun refine(products: List<Product>, request: RefinementRequest): RefinementResult
}

/**
 * Local, non-AI engine: evaluates intents ONLY from explicit structured attributes the source
 * provided (e.g. `logo=none`, `style=minimal`). Products without such attributes get [Verdict.UNKNOWN].
 * Intents that cannot be derived from attributes are reported as unsupported.
 */
class AttributeRefinementEngine : AiRefinementEngine {
    override val name = "Attribute matcher (local, no AI)"

    private val supported = setOf(AiIntent.MINIMALIST, AiIntent.NO_BIG_LOGOS, AiIntent.SIMILAR_TO)

    override fun supports(intent: AiIntent) = intent in supported

    override suspend fun refine(products: List<Product>, request: RefinementRequest): RefinementResult {
        val active = request.intents.filter(::supports).toSet()
        val verdicts = products.associate { p -> p.id to active.associateWith { verdict(p, it, request.reference) } }
        val score: (Product) -> Int = { p ->
            verdicts[p.id].orEmpty().values.sumOf { v: Verdict ->
                when (v) {
                    Verdict.MATCHES -> 2
                    Verdict.UNKNOWN -> 1
                    Verdict.DOES_NOT_MATCH -> 0
                }.toInt()
            }
        }
        // Stable sort keeps the original relevance order inside equal scores.
        val ordered = if (active.isEmpty()) products else products.sortedByDescending(score)
        return RefinementResult(
            products = ordered,
            verdicts = verdicts,
            unsupported = request.intents - supported,
            engineName = name,
            isModelBased = false,
        )
    }

    private fun verdict(p: Product, intent: AiIntent, reference: Product?): Verdict = when (intent) {
        AiIntent.NO_BIG_LOGOS -> when (p.attributes[AttributeKeys.LOGO]?.lowercase()) {
            null -> Verdict.UNKNOWN
            "none", "small", "tonal", "minimal" -> Verdict.MATCHES
            else -> Verdict.DOES_NOT_MATCH
        }
        AiIntent.MINIMALIST -> when (val style = p.attributes[AttributeKeys.STYLE]?.lowercase()) {
            null -> Verdict.UNKNOWN
            else -> if ("minimal" in style || "clean" in style) Verdict.MATCHES else Verdict.DOES_NOT_MATCH
        }
        AiIntent.SIMILAR_TO -> when {
            reference == null -> Verdict.UNKNOWN
            p.id == reference.id -> Verdict.DOES_NOT_MATCH
            p.category != null && p.category == reference.category && sharedAttributes(p, reference) >= 1 -> Verdict.MATCHES
            p.category != null && p.category == reference.category -> Verdict.UNKNOWN
            else -> Verdict.DOES_NOT_MATCH
        }
        else -> Verdict.UNKNOWN
    }

    private fun sharedAttributes(a: Product, b: Product): Int =
        a.attributes.count { (k, v) -> b.attributes[k]?.equals(v, ignoreCase = true) == true } +
            (if (a.brand != null && a.brand == b.brand) 1 else 0)
}

/** Similar products based on shared category/attributes — deterministic, used on the product page. */
object SimilarProducts {
    fun find(reference: Product, pool: List<Product>, limit: Int = 10): List<Product> =
        pool.asSequence()
            .filter { it.id != reference.id && it.category == reference.category }
            .filter { com.findly.core.compare.ProductMatcher.confidence(reference, it) == null }
            .sortedByDescending { candidate ->
                candidate.attributes.count { (k, v) -> reference.attributes[k]?.equals(v, true) == true } +
                    (if (candidate.brand == reference.brand) 1 else 0)
            }
            .distinctBy { com.findly.core.compare.ProductMatcher.matchKey(it) ?: it.id }
            .take(limit)
            .toList()
}
