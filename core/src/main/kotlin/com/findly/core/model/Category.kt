package com.findly.core.model

import kotlinx.serialization.Serializable

/** Small top-level taxonomy that every source maps into. Sources keep their own path in [Product.attributes]. */
@Serializable
enum class Category(val keywords: List<String>) {
    SMARTPHONES(listOf("iphone", "smartphone", "phone", "galaxy", "pixel", "телефон", "смартфон")),
    AUDIO(listOf("headphones", "earbuds", "наушники", "speaker", "колонка", "airpods", "wh-1000")),
    COMPUTER_ACCESSORIES(listOf("mouse", "мышь", "мышка", "keyboard", "клавиатура", "monitor", "монитор", "webcam")),
    SHOES(listOf("sneakers", "кроссовки", "shoes", "обувь", "кеды", "boots", "ботинки")),
    CLOTHING(listOf("hoodie", "худи", "t-shirt", "футболка", "jacket", "куртка", "свитшот", "sweatshirt", "jeans", "джинсы")),
    BAGS(listOf("backpack", "рюкзак", "bag", "сумка")),
    HOME(listOf("lamp", "лампа", "chair", "кресло", "стул", "desk", "стол")),
    OTHER(emptyList());
}
