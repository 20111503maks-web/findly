package com.findly.core.search

import com.findly.core.ai.AiIntent
import com.findly.core.model.Category
import com.findly.core.model.Money

/**
 * Result of deterministic query parsing. Hard constraints (price, colour, category, size)
 * become regular filters. Vague wishes ("minimalist", "no big logos") are kept as [intents]
 * and handled separately by the AI/refinement layer — they are never silently turned into filters.
 */
data class ParsedQuery(
    val original: String,
    /** Meaningful search tokens after removing filler words and parsed constraints. */
    val keywords: List<String>,
    val minPrice: Money? = null,
    val maxPrice: Money? = null,
    val colors: Set<String> = emptySet(),
    val category: Category? = null,
    val size: String? = null,
    val intents: Set<AiIntent> = emptySet(),
    /** Human-readable list of what the parser understood, for "Понял: …" chips in the UI. */
    val understood: List<UnderstoodPart> = emptyList(),
) {
    val keywordText: String get() = keywords.joinToString(" ")
    val isEmpty: Boolean get() = keywords.isEmpty() && category == null && colors.isEmpty()
}

sealed interface UnderstoodPart {
    data class MaxPrice(val price: Money) : UnderstoodPart
    data class MinPrice(val price: Money) : UnderstoodPart
    data class Color(val color: String) : UnderstoodPart
    data class CategoryPart(val category: Category) : UnderstoodPart
    data class Size(val size: String) : UnderstoodPart
    data class Intent(val intent: AiIntent) : UnderstoodPart
}

/** What is sent to each provider. Providers may ignore fields they cannot express. */
data class ProviderSearchRequest(
    val keywords: String,
    val category: Category? = null,
    val minPrice: Money? = null,
    val maxPrice: Money? = null,
    val limit: Int = 40,
    val locale: String = "en",
)
