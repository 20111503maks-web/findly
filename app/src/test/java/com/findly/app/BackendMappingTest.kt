package com.findly.app

import com.findly.app.data.local.ProductJson
import com.findly.app.data.remote.PriceDto
import com.findly.app.data.remote.ProductDto
import com.findly.app.data.remote.StoreDto
import com.findly.app.data.remote.toDomainOrNull
import com.findly.core.model.Availability
import com.findly.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackendMappingTest {

    private val dto = ProductDto(
        id = "ebay:123",
        source = "ebay",
        sourceProductId = "123",
        title = "Sony WH-1000XM6",
        price = PriceDto(34200, "EUR"),
        store = StoreDto("ebay", "eBay"),
        productUrl = "https://www.ebay.de/itm/123",
        availability = "in_stock",
        category = "audio",
        rating = 7.5,
    )

    @Test
    fun `maps fields and keeps unknowns unknown`() {
        val p = dto.toDomainOrNull()!!
        assertEquals(Availability.IN_STOCK, p.availability)
        assertEquals(Category.AUDIO, p.category)
        assertNull("out-of-range rating is dropped, not clamped", p.rating)
        assertNull(p.delivery)
        assertNull(p.discountPercent)
        assertFalse(p.isDemo)
    }

    @Test
    fun `invalid items are dropped`() {
        assertNull(dto.copy(productUrl = "").toDomainOrNull())
    }

    @Test
    fun `snapshot json round trip`() {
        val p = dto.toDomainOrNull()!!
        assertEquals(p, ProductJson.decode(ProductJson.encode(p)))
    }
}
