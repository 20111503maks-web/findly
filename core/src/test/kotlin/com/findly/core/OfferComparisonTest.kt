package com.findly.core

import com.findly.core.compare.FindCheaperResult
import com.findly.core.compare.FindCheaperUseCase
import com.findly.core.compare.OfferComparator
import com.findly.core.demo.DemoCatalog
import com.findly.core.demo.DemoStoreProvider
import com.findly.core.link.OutboundLinkResolver
import com.findly.core.model.Money
import com.findly.core.provider.ProviderRegistry
import com.findly.core.provider.SearchAggregator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferComparisonTest {

    private val sony = DemoCatalog.products.filter { it.brand == "Sony" }

    @Test
    fun `best offer considers known delivery`() {
        val ref = sony.first { it.store.id == "technest" }
        val cmp = OfferComparator.compare(ref, sony)
        // ShopLine 342 (delivery unknown) < MegaBazaar 359 + 4.99 < TechNest 389 free
        assertEquals("shopline", cmp.best?.product?.store?.id)
        assertTrue(cmp.hasUnknownDelivery)
        assertEquals(Money.of(47.0), OfferComparator.savingsFor(ref, cmp))
    }

    @Test
    fun `find cheaper by supported url`() = runTest {
        val agg = SearchAggregator(ProviderRegistry(DemoCatalog.stores.map { DemoStoreProvider(it, latencyMs = 0) }))
        val result = FindCheaperUseCase(agg).byUrl("https://technest.example/p/sony-xm6")
        assertTrue(result is FindCheaperResult.Found)
        result as FindCheaperResult.Found
        assertEquals(3, result.comparison.offers.size)
        assertEquals(Money.of(47.0), result.savings)
    }

    @Test
    fun `unsupported url is reported honestly`() = runTest {
        val agg = SearchAggregator(ProviderRegistry(DemoCatalog.stores.map { DemoStoreProvider(it, latencyMs = 0) }))
        assertEquals(FindCheaperResult.UnsupportedLink, FindCheaperUseCase(agg).byUrl("https://unknown-shop.com/item/1"))
    }

    @Test
    fun `affiliate link is used only when provided and disclosed`() {
        val resolver = OutboundLinkResolver()
        val mega = sony.first { it.store.id == "megabazaar" }
        val link = resolver.resolve(mega)!!
        assertTrue(link.isAffiliate)
        assertEquals("megabazaar.example", link.displayHost)
        val tech = sony.first { it.store.id == "technest" }
        assertFalse(resolver.resolve(tech)!!.isAffiliate)
        assertFalse(resolver.isSafeWebUrl("javascript:alert(1)"))
    }

    @Test
    fun `discount is only computed from real old price`() {
        val p = sony.first { it.store.id == "technest" }
        assertEquals(13, p.discountPercent)
        assertEquals(null, p.copy(oldPrice = null).discountPercent)
        assertEquals(null, p.copy(oldPrice = Money.of(100.0)).discountPercent)
    }
}
