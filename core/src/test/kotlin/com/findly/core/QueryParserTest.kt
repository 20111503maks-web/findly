package com.findly.core

import com.findly.core.ai.AiIntent
import com.findly.core.model.Category
import com.findly.core.model.Money
import com.findly.core.search.QueryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryParserTest {
    private val parser = QueryParser()

    @Test
    fun `plain query keeps keywords`() {
        val q = parser.parse("iPhone 17")
        assertEquals(listOf("iphone", "17"), q.keywords)
        assertEquals(Category.SMARTPHONES, q.category)
        assertNull(q.maxPrice)
    }

    @Test
    fun `russian natural language with max price and color`() {
        val q = parser.parse("Найди белые кроссовки до 70 евро")
        assertEquals(Money.of(70.0, "EUR"), q.maxPrice)
        assertEquals(setOf("white"), q.colors)
        assertEquals(listOf("кроссовки"), q.keywords)
        assertEquals(Category.SHOES, q.category)
    }

    @Test
    fun `soft wishes become intents not keywords`() {
        val q = parser.parse("Хочу чёрную oversize худи без больших логотипов")
        assertEquals(setOf("black"), q.colors)
        assertEquals(listOf("oversize", "худи"), q.keywords)
        assertTrue(AiIntent.NO_BIG_LOGOS in q.intents)
        assertEquals(Category.CLOTHING, q.category)
    }

    @Test
    fun `mixed language keyboard query`() {
        val q = parser.parse("Найди хороший механический keyboard 75% до 60 евро")
        assertEquals(Money.of(60.0), q.maxPrice)
        assertEquals(listOf("механический", "keyboard", "75%"), q.keywords)
        assertEquals(Category.COMPUTER_ACCESSORIES, q.category)
    }

    @Test
    fun `english price formats`() {
        assertEquals(Money.of(60.0), parser.parse("headphones under €60").maxPrice)
        assertEquals(Money.of(49.99), parser.parse("mouse below 49.99 eur").maxPrice)
        val range = parser.parse("sneakers from 30 to 80")
        assertEquals(Money.of(30.0), range.minPrice)
    }

    @Test
    fun `preposition inside word is not a price`() {
        val q = parser.parse("подберите рюкзак")
        assertNull(q.maxPrice)
        assertNull(q.minPrice)
    }

    @Test
    fun `size is extracted`() {
        val q = parser.parse("кроссовки размер 42")
        assertEquals("42", q.size)
        assertEquals(listOf("кроссовки"), q.keywords)
    }
}
