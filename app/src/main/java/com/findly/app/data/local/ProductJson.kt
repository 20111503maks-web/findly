package com.findly.app.data.local

import com.findly.core.model.Product
import kotlinx.serialization.json.Json

/** JSON codec for product snapshots stored in Room. */
object ProductJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(product: Product): String = json.encodeToString(Product.serializer(), product)

    fun decode(raw: String): Product? = runCatching { json.decodeFromString(Product.serializer(), raw) }.getOrNull()
}
