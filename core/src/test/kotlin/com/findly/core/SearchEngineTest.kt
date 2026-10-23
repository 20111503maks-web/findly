package com.findly.core

import com.findly.core.ai.AttributeRefinementEngine
import com.findly.core.demo.DemoCatalog
import com.findly.core.demo.DemoStoreProvider
import com.findly.core.filter.SortOption
import com.findly.core.provider.ProviderRegistry
import com.findly.core.provider.SearchAggregator
import com.findly.core.provider.SourceStatus
import com.findly.core.search.SearchEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    private fun engine(failing: Set<String> = emptySet()): SearchEngine {
        val providers = DemoCatalog.stores.map { s -> DemoStoreProvider(s, latencyMs = 0, shouldFail = { s.id in failing }) }
        return SearchEngine(SearchAggregator(ProviderRegistry(providers)), refinementEngine = AttributeRefinementEngine())
    }

    @Test
    fun `white sneakers under 70 euros`() = runTest {
        val e = engine()
        val parsed = e.parse("Найди белые кроссовки до 70 евро")
        val outcome = e.search(parsed)
        val presented = e.present(outcome, e.initialFilters(parsed), SortOption.PRICE_ASC)
        val all = presented.groups.flatMap { it.offers }
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.currentPrice.amountMinor <= 7000 && "white" in it.colors })
        assertTrue(all.any { it.brand == "adidas" })
    }

    @Test
    fun `same product from several stores is grouped`() = runTest {
        val e = engine()
        val parsed = e.parse("Sony WH-1000XM6")
        val presented = e.present(e.search(parsed), e.initialFilters(parsed), SortOption.RELEVANCE)
        val sony = presented.groups.first { it.representative.brand == "Sony" }
        assertEquals(3, sony.storeCount)
    }

    @Test
    fun `failing source is reported but others still return`() = runTest {
        val e = engine(failing = setOf("techNest".lowercase()))
        val outcome = e.search(e.parse("iphone"))
        assertTrue(outcome.sources.any { it is SourceStatus.Failed && it.provider.id == "demo-technest" })
        assertTrue(outcome.products.isNotEmpty())
    }

    @Test
    fun `no big logos intent ranks logo-free hoodie first without hiding others`() = runTest {
        val e = engine()
        val parsed = e.parse("чёрная oversize худи без больших логотипов")
        val presented = e.present(e.search(parsed), e.initialFilters(parsed), SortOption.RELEVANCE)
        val first = presented.groups.first().representative
        assertEquals("Nordline", first.brand)
        assertTrue(presented.groups.any { it.representative.brand == "Kroma" })
    }

    @Test
    fun `sponsored results are separated`() = runTest {
        val e = engine()
        val parsed = e.parse("gaming mouse")
        val presented = e.present(e.search(parsed), e.initialFilters(parsed), SortOption.RELEVANCE)
        assertTrue(presented.sponsored.all { it.sponsored })
        assertTrue(presented.groups.flatMap { it.offers }.none { it.sponsored })
    }
}
