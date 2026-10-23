package com.findly.core.search

import com.findly.core.ai.AiIntent
import com.findly.core.model.Money

/**
 * Deterministic, rule-based parser for natural-language queries (RU/EN):
 *
 *  "Найди белые кроссовки до 70 евро" → keywords [кроссовки], color white, maxPrice €70, category SHOES
 *  "Хочу чёрную oversize худи без больших логотипов" → keywords [oversize, худи], color black, intent NO_BIG_LOGOS
 *
 * It does NOT pretend to understand meaning. A future LLM-based parser on the backend can
 * replace it behind the same [ParsedQuery] contract.
 */
class QueryParser(private val defaultCurrency: String = "EUR") {

    private val fillerWords = setOf(
        "найди", "найти", "найдите", "хочу", "покажи", "покажите", "мне", "нужен", "нужна", "нужно", "нужны",
        "пожалуйста", "какой", "какие", "хороший", "хорошую", "хорошая", "хорошие", "лучший", "лучшие",
        "купить", "ищу", "подбери", "для", "и", "с", "в", "на", "a", "an", "the", "find", "me", "show", "i", "want",
        "need", "please", "good", "best", "buy", "looking", "for", "some", "with", "and", "nice",
    )

    private val currencyWords = mapOf(
        "€" to "EUR", "eur" to "EUR", "euro" to "EUR", "euros" to "EUR", "евро" to "EUR",
        "$" to "USD", "usd" to "USD", "dollars" to "USD", "долларов" to "USD",
        "£" to "GBP", "gbp" to "GBP",
    )

    // Order matters: longer phrases first.
    private val maxPricePatterns = listOf(
        Regex("""(?<![\p{L}\p{N}])(?:не\s+дороже|дешевле|до|under|below|less\s+than|max|maximum|up\s+to|<=?)\s*([€$£])?\s*(\d+(?:[.,]\d{1,2})?)\s*(€|\$|£|eur|euros?|евро|usd|dollars|долларов|gbp)?"""),
    )
    private val minPricePatterns = listOf(
        Regex("""(?<![\p{L}\p{N}])(?:от|дороже|from|over|above|more\s+than|min|>=?)\s*([€$£])?\s*(\d+(?:[.,]\d{1,2})?)\s*(€|\$|£|eur|euros?|евро|usd|dollars|долларов|gbp)?"""),
    )
    private val sizePattern = Regex("""(?:размер|размера|size)\s*([0-9]{2}(?:[.,]5)?|xxs|xs|s|m|l|xl|xxl)\b|\b([0-9]{2}(?:[.,]5)?)\s*(?:размер|размера|size)""")

    /** Phrases that express vague wishes; they go to the AI layer, not to hard filters. */
    private val intentPhrases: List<Pair<Regex, AiIntent>> = listOf(
        Regex("""без\s+(?:больших\s+|крупных\s+|ярких\s+)?логотип\p{L}*|no\s+(?:big\s+|large\s+)?logos?|logo[- ]?free""") to AiIntent.NO_BIG_LOGOS,
        Regex("""минималист\p{L}*|minimal\p{L}*""") to AiIntent.MINIMALIST,
        Regex("""для\s+школы|for\s+school|школьн\p{L}*""") to AiIntent.FOR_SCHOOL,
        Regex("""премиальн\p{L}*|premium(?:\s+design)?|премиум\p{L}*""") to AiIntent.PREMIUM_DESIGN,
    )

    fun parse(input: String): ParsedQuery {
        var text = TextNormalizer.normalize(input).trim()
        val understood = mutableListOf<UnderstoodPart>()

        var maxPrice: Money? = null
        var minPrice: Money? = null

        for (p in maxPricePatterns) {
            p.find(text)?.let { m ->
                maxPrice = toMoney(m.groupValues[2], m.groupValues[1].ifEmpty { m.groupValues[3] })
                text = text.removeRange(m.range).replaceRange()
            }
        }
        for (p in minPricePatterns) {
            p.find(text)?.let { m ->
                minPrice = toMoney(m.groupValues[2], m.groupValues[1].ifEmpty { m.groupValues[3] })
                text = text.removeRange(m.range).replaceRange()
            }
        }
        maxPrice?.let { understood += UnderstoodPart.MaxPrice(it) }
        minPrice?.let { understood += UnderstoodPart.MinPrice(it) }

        var size: String? = null
        sizePattern.find(text)?.let { m ->
            size = (m.groupValues[1].ifEmpty { m.groupValues[2] }).uppercase().replace(',', '.')
            text = text.removeRange(m.range).replaceRange()
            understood += UnderstoodPart.Size(size!!)
        }

        val intents = linkedSetOf<AiIntent>()
        for ((regex, intent) in intentPhrases) {
            regex.find(text)?.let { m ->
                intents += intent
                text = text.removeRange(m.range).replaceRange()
                understood += UnderstoodPart.Intent(intent)
            }
        }

        val tokens = TextNormalizer.tokenize(text)
        val colors = linkedSetOf<String>()
        val keywords = mutableListOf<String>()
        for (token in tokens) {
            if (token in fillerWords) continue
            if (currencyWords.containsKey(token)) continue
            val color = ColorDictionary.match(token)
            if (color != null) {
                colors += color
                continue
            }
            keywords += token
        }
        colors.forEach { understood += UnderstoodPart.Color(it) }

        val category = TextNormalizer.detectCategory(keywords)
        category?.let { understood += UnderstoodPart.CategoryPart(it) }

        return ParsedQuery(
            original = input,
            keywords = keywords,
            minPrice = minPrice,
            maxPrice = maxPrice,
            colors = colors,
            category = category,
            size = size,
            intents = intents,
            understood = understood,
        )
    }

    private fun String.replaceRange(): String = replace(Regex("\\s+"), " ").trim()

    private fun toMoney(number: String, currencyToken: String): Money {
        val currency = currencyWords[currencyToken.trim()] ?: defaultCurrency
        return Money.of(number.replace(',', '.').toBigDecimal(), currency)
    }
}
