package com.findly.core.search

/** Canonical colour names (English, lowercase) and the word stems that map to them. */
object ColorDictionary {

    val canonical: Map<String, List<String>> = linkedMapOf(
        "white" to listOf("white", "бел"),
        "black" to listOf("black", "черн", "чёрн"),
        "grey" to listOf("grey", "gray", "сер"),
        "blue" to listOf("blue", "navy", "син", "голуб"),
        "red" to listOf("red", "красн"),
        "green" to listOf("green", "зелен", "зелён"),
        "beige" to listOf("beige", "бежев", "cream"),
        "brown" to listOf("brown", "коричнев"),
        "pink" to listOf("pink", "розов"),
        "yellow" to listOf("yellow", "желт", "жёлт"),
        "purple" to listOf("purple", "violet", "фиолет"),
        "silver" to listOf("silver", "серебр"),
    )

    /** Returns the canonical colour for a token, or null. Avoids short false positives like "серия". */
    fun match(token: String): String? {
        val t = TextNormalizer.normalize(token)
        for ((color, stems) in canonical) {
            for (stem in stems) {
                val s = TextNormalizer.normalize(stem)
                val ok = if (s.all { it in 'a'..'z' }) t == s || t == s + "s" else t.startsWith(s) && t.length <= s.length + 4
                if (ok && !(s == "сер" && (t.startsWith("сери") || t.startsWith("серв")))) return color
            }
        }
        return null
    }
}
