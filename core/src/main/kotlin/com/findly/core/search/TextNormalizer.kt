package com.findly.core.search

import com.findly.core.model.Category
import com.findly.core.model.Product

/**
 * Lightweight multilingual (RU/EN) text matching used by demo providers and local ranking.
 * Real sources do their own full-text search; this only needs to be predictable.
 */
object TextNormalizer {

    private val splitRegex = Regex("[^\\p{L}\\p{N}%+\\-]+")

    /** Russian → English synonyms so "белые кроссовки" matches "White Sneakers". */
    private val synonyms: Map<String, List<String>> = mapOf(
        "кроссовк" to listOf("sneaker", "trainer"),
        "кеды" to listOf("sneaker", "canvas"),
        "худи" to listOf("hoodie"),
        "толстовк" to listOf("hoodie", "sweatshirt"),
        "свитшот" to listOf("sweatshirt"),
        "футболк" to listOf("t-shirt", "tee"),
        "куртк" to listOf("jacket"),
        "мыш" to listOf("mouse"),
        "клавиатур" to listOf("keyboard"),
        "наушник" to listOf("headphone", "earbud"),
        "колонк" to listOf("speaker"),
        "рюкзак" to listOf("backpack"),
        "сумк" to listOf("bag"),
        "игров" to listOf("gaming"),
        "механическ" to listOf("mechanical"),
        "беспроводн" to listOf("wireless"),
        "телефон" to listOf("phone", "smartphone"),
        "смартфон" to listOf("smartphone", "phone"),
        "лампа" to listOf("lamp"),
        "монитор" to listOf("monitor"),
    )

    fun normalize(text: String): String = text.lowercase().replace('ё', 'е')

    fun tokenize(text: String): List<String> =
        normalize(text).split(splitRegex).filter { it.isNotBlank() }

    /** Expands a token with its synonyms (and itself). */
    fun expand(token: String): List<String> {
        val t = normalize(token)
        val extra = synonyms.entries.firstOrNull { t.startsWith(it.key) }?.value.orEmpty()
        return listOf(t) + extra
    }

    /** Fuzzy single-token match tolerant to plural / case endings ("кроссовки" ~ "кроссовок", "sneakers" ~ "sneaker"). */
    fun tokenMatches(queryToken: String, fieldToken: String): Boolean {
        if (queryToken == fieldToken) return true
        if (queryToken.length <= 3 || fieldToken.length <= 3) return false
        val stem = queryToken.take(maxOf(4, queryToken.length - 2))
        return fieldToken.startsWith(stem) || queryToken.startsWith(fieldToken.take(maxOf(4, fieldToken.length - 1)))
    }

    fun searchableTokens(product: Product): List<String> = buildList {
        addAll(tokenize(product.title))
        product.brand?.let { addAll(tokenize(it)) }
        product.attributes.values.forEach { addAll(tokenize(it)) }
        product.category?.let { cat -> addAll(cat.keywords.flatMap { tokenize(it) }) }
    }

    /** Fraction (0..1) of query tokens that match the product. */
    fun matchScore(queryTokens: List<String>, product: Product): Double {
        if (queryTokens.isEmpty()) return 1.0
        val fieldTokens = searchableTokens(product)
        val matched = queryTokens.count { q ->
            expand(q).any { variant -> fieldTokens.any { f -> tokenMatches(variant, f) } }
        }
        return matched.toDouble() / queryTokens.size
    }

    fun detectCategory(tokens: List<String>): Category? =
        Category.entries.firstOrNull { cat ->
            cat.keywords.any { kw -> tokens.any { t -> tokenMatches(t, normalize(kw)) } }
        }
}
