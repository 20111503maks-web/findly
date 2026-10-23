package com.findly.core.demo

import com.findly.core.model.AttributeKeys as K
import com.findly.core.model.Availability
import com.findly.core.model.Category
import com.findly.core.model.Delivery
import com.findly.core.model.Money
import com.findly.core.model.Product
import com.findly.core.model.ProductIdentifiers
import com.findly.core.model.Store

/**
 * DEMO DATA ONLY. Fictional stores on `.example` domains with made-up prices, so the UI, filters
 * and comparison can be developed without partner API access. Every product is flagged
 * [Product.isDemo] and the app labels it. GTINs use the 200–299 "restricted circulation" range,
 * which is never assigned to real retail products.
 */
object DemoCatalog {

    val techNest = Store("technest", "TechNest", "technest.example")
    val urbanThread = Store("urbanthread", "UrbanThread", "urbanthread.example")
    val megaBazaar = Store("megabazaar", "MegaBazaar", "megabazaar.example")
    val shopLine = Store("shopline", "ShopLine", "shopline.example")

    val stores = listOf(techNest, urbanThread, megaBazaar, shopLine)

    private class Item(
        val key: String,
        val title: String,
        val brand: String?,
        val category: Category,
        val description: String,
        val gtin: String?,
        val model: String?,
        val attributes: Map<String, String>,
    )

    private class Offer(
        val store: Store,
        val price: Double,
        val old: Double? = null,
        val delivery: Double? = null,
        val deliveryDays: IntRange? = null,
        val rating: Double? = null,
        val reviews: Int? = null,
        val availability: Availability = Availability.IN_STOCK,
        val affiliate: Boolean = false,
        val sponsored: Boolean = false,
        val titleOverride: String? = null,
    )

    private val entries = mutableListOf<Pair<Item, List<Offer>>>()

    private fun item(
        key: String,
        title: String,
        brand: String?,
        category: Category,
        description: String,
        gtin: String? = null,
        model: String? = null,
        attributes: Map<String, String> = emptyMap(),
        offers: () -> List<Offer>,
    ) {
        entries += Item(key, title, brand, category, description, gtin, model, attributes) to offers()
    }

    init {
        // ---------- Audio ----------
        item(
            "sony-xm6", "Sony WH-1000XM6 Wireless Noise Cancelling Headphones", "Sony", Category.AUDIO,
            "Over-ear wireless headphones with adaptive noise cancelling, up to 30 h battery and multipoint Bluetooth.",
            gtin = "2000000000017", model = "WH-1000XM6",
            attributes = mapOf(K.COLOR to "black", K.CONNECTIVITY to "Bluetooth 5.3", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(
                Offer(techNest, 389.0, old = 449.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.7, reviews = 1240),
                Offer(megaBazaar, 359.0, delivery = 4.99, deliveryDays = 2..4, rating = 4.6, reviews = 860, affiliate = true),
                Offer(shopLine, 342.0, rating = 4.5, reviews = 310, deliveryDays = 3..6),
            )
        }
        item(
            "airpods-pro-3", "Apple AirPods Pro 3 with USB-C case", "Apple", Category.AUDIO,
            "In-ear earbuds with active noise cancellation and transparency mode.",
            gtin = "2000000000024", model = "AirPods Pro 3",
            attributes = mapOf(K.COLOR to "white", K.CONNECTIVITY to "Bluetooth", K.STYLE to "minimal", K.LOGO to "none"),
        ) {
            listOf(
                Offer(techNest, 279.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.8, reviews = 3200),
                Offer(megaBazaar, 259.0, old = 279.0, delivery = 0.0, deliveryDays = 2..3, rating = 4.7, reviews = 2100, affiliate = true),
            )
        }
        item(
            "soundcore-q30", "Soundcore Life Q30 Hybrid ANC Headphones", "Anker", Category.AUDIO,
            "Budget over-ear headphones with hybrid active noise cancelling.",
            gtin = "2000000000031", model = "Life Q30",
            attributes = mapOf(K.COLOR to "black,blue", K.CONNECTIVITY to "Bluetooth 5.0"),
        ) {
            listOf(
                Offer(megaBazaar, 59.99, old = 79.99, delivery = 2.99, deliveryDays = 2..5, rating = 4.4, reviews = 5400, affiliate = true),
                Offer(shopLine, 64.0, rating = 4.3, reviews = 900),
            )
        }

        // ---------- Smartphones ----------
        item(
            "iphone-17", "Apple iPhone 17 128 GB", "Apple", Category.SMARTPHONES,
            "6.3\" display, A19 chip, 48 MP dual camera. Demo listing.",
            gtin = "2000000000048", model = "iPhone 17 128GB",
            attributes = mapOf(K.COLOR to "black,white,blue", K.STORAGE to "128 GB", K.STYLE to "minimal"),
        ) {
            listOf(
                Offer(techNest, 949.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.7, reviews = 410),
                Offer(megaBazaar, 929.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.6, reviews = 290, affiliate = true),
                Offer(shopLine, 939.0, delivery = 5.0, deliveryDays = 2..5, availability = Availability.LOW_STOCK),
            )
        }
        item(
            "iphone-17-pro", "Apple iPhone 17 Pro 256 GB", "Apple", Category.SMARTPHONES,
            "Pro camera system, titanium-free aluminium body, 256 GB. Demo listing.",
            gtin = "2000000000055", model = "iPhone 17 Pro 256GB",
            attributes = mapOf(K.COLOR to "silver,blue", K.STORAGE to "256 GB"),
        ) {
            listOf(
                Offer(techNest, 1299.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.8, reviews = 150),
                Offer(megaBazaar, 1279.0, availability = Availability.PREORDER, affiliate = true),
            )
        }
        item(
            "pixel-10", "Google Pixel 10 128 GB", "Google", Category.SMARTPHONES,
            "Android smartphone with Tensor chip and 7 years of updates. Demo listing.",
            gtin = "2000000000062", model = "Pixel 10 128GB",
            attributes = mapOf(K.COLOR to "black,green", K.STORAGE to "128 GB", K.STYLE to "minimal"),
        ) {
            listOf(
                Offer(shopLine, 799.0, old = 899.0, delivery = 0.0, deliveryDays = 2..3, rating = 4.5, reviews = 220),
                Offer(techNest, 849.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.6, reviews = 180),
            )
        }

        // ---------- Mice ----------
        item(
            "gpx-superlight-2", "Logitech G PRO X SUPERLIGHT 2 Wireless Gaming Mouse", "Logitech", Category.COMPUTER_ACCESSORIES,
            "60 g wireless gaming mouse, HERO 2 sensor, up to 95 h battery.",
            gtin = "2000000000079", model = "PRO X SUPERLIGHT 2",
            attributes = mapOf(K.COLOR to "black,white", K.CONNECTIVITY to "Lightspeed wireless", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(
                Offer(techNest, 149.0, old = 169.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.8, reviews = 2600),
                Offer(megaBazaar, 139.0, delivery = 3.99, deliveryDays = 2..4, rating = 4.7, reviews = 1900, affiliate = true),
            )
        }
        item(
            "deathadder-v3", "Razer DeathAdder V3 Wired Gaming Mouse", "Razer", Category.COMPUTER_ACCESSORIES,
            "Ergonomic esports mouse, 59 g, Focus Pro 30K sensor.",
            gtin = "2000000000086", model = "DeathAdder V3",
            attributes = mapOf(K.COLOR to "black", K.CONNECTIVITY to "USB-C wired", K.LOGO to "large"),
        ) {
            listOf(
                Offer(megaBazaar, 79.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.6, reviews = 1300, affiliate = true, sponsored = true),
                Offer(shopLine, 74.0, delivery = 4.5, deliveryDays = 3..5, rating = 4.5, reviews = 400),
            )
        }
        item(
            "mx-master-3s", "Logitech MX Master 3S Wireless Mouse", "Logitech", Category.COMPUTER_ACCESSORIES,
            "Quiet-click productivity mouse with MagSpeed scroll wheel.",
            gtin = "2000000000093", model = "MX Master 3S",
            attributes = mapOf(K.COLOR to "grey,white", K.CONNECTIVITY to "Bluetooth, Logi Bolt", K.STYLE to "minimal"),
        ) {
            listOf(
                Offer(techNest, 99.0, old = 119.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.7, reviews = 4100),
                Offer(shopLine, 94.9, rating = 4.6, reviews = 800),
            )
        }

        // ---------- Keyboards ----------
        item(
            "keychron-v1", "Keychron V1 75% Mechanical Keyboard (Knob, Brown switches)", "Keychron", Category.COMPUTER_ACCESSORIES,
            "Hot-swappable 75% layout mechanical keyboard with QMK/VIA support.",
            gtin = "2000000000109", model = "V1",
            attributes = mapOf(K.COLOR to "black", K.LAYOUT to "75%", K.SWITCHES to "Brown", K.CONNECTIVITY to "USB-C wired", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(
                Offer(techNest, 89.0, delivery = 0.0, deliveryDays = 2..3, rating = 4.6, reviews = 760),
                Offer(megaBazaar, 84.0, delivery = 4.99, deliveryDays = 3..5, rating = 4.5, reviews = 510, affiliate = true),
            )
        }
        item(
            "rk84", "Royal Kludge RK84 75% Wireless Mechanical Keyboard", "Royal Kludge", Category.COMPUTER_ACCESSORIES,
            "Tri-mode (2.4 GHz / Bluetooth / USB-C) 75% keyboard, hot-swap, RGB.",
            gtin = "2000000000116", model = "RK84",
            attributes = mapOf(K.COLOR to "white,black", K.LAYOUT to "75%", K.SWITCHES to "Red", K.CONNECTIVITY to "Bluetooth, 2.4 GHz, USB-C"),
        ) {
            listOf(
                Offer(megaBazaar, 55.99, old = 69.99, delivery = 0.0, deliveryDays = 4..7, rating = 4.4, reviews = 3100, affiliate = true),
                Offer(shopLine, 58.0, delivery = 0.0, deliveryDays = 3..5, rating = 4.3, reviews = 700),
            )
        }
        item(
            "akko-5075b", "Akko 5075B Plus 75% Mechanical Keyboard", "Akko", Category.COMPUTER_ACCESSORIES,
            "Gasket-mounted 75% keyboard with knob, tri-mode connection.",
            gtin = "2000000000123", model = "5075B Plus",
            attributes = mapOf(K.COLOR to "black,white", K.LAYOUT to "75%", K.SWITCHES to "V3 Cream Yellow", K.CONNECTIVITY to "Bluetooth, 2.4 GHz, USB-C"),
        ) {
            listOf(Offer(shopLine, 59.0, delivery = 3.5, deliveryDays = 3..6, rating = 4.6, reviews = 420))
        }
        item(
            "mx-mechanical-mini", "Logitech MX Mechanical Mini Wireless Keyboard", "Logitech", Category.COMPUTER_ACCESSORIES,
            "Low-profile 75% mechanical keyboard with smart backlighting.",
            gtin = "2000000000130", model = "MX Mechanical Mini",
            attributes = mapOf(K.COLOR to "grey", K.LAYOUT to "75%", K.SWITCHES to "Tactile Quiet", K.CONNECTIVITY to "Bluetooth, Logi Bolt", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(Offer(techNest, 129.0, old = 149.0, delivery = 0.0, deliveryDays = 1..2, rating = 4.5, reviews = 980))
        }

        // ---------- Sneakers ----------
        item(
            "af1-white", "Nike Air Force 1 '07 Sneakers — White", "Nike", Category.SHOES,
            "Classic low-top leather sneakers.",
            gtin = "2000000000147", model = "Air Force 1 07",
            attributes = mapOf(K.COLOR to "white", K.MATERIAL to "leather", K.SIZES to "38,39,40,41,42,43,44,45", K.LOGO to "medium"),
        ) {
            listOf(
                Offer(urbanThread, 119.99, delivery = 0.0, deliveryDays = 2..4, rating = 4.8, reviews = 5200),
                Offer(megaBazaar, 109.0, old = 119.99, delivery = 4.99, deliveryDays = 3..5, rating = 4.6, reviews = 1800, affiliate = true),
            )
        }
        item(
            "stan-smith", "adidas Stan Smith Leather Sneakers — White/Green", "adidas", Category.SHOES,
            "Minimal leather tennis-inspired sneakers.",
            gtin = "2000000000154", model = "Stan Smith",
            attributes = mapOf(K.COLOR to "white,green", K.MATERIAL to "leather", K.SIZES to "37,38,39,40,41,42,43,44", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(
                Offer(urbanThread, 69.99, old = 99.99, delivery = 0.0, deliveryDays = 2..4, rating = 4.7, reviews = 3900),
                Offer(shopLine, 74.0, delivery = 0.0, deliveryDays = 3..6, rating = 4.5, reviews = 600),
            )
        }
        item(
            "club-c-85", "Reebok Club C 85 Sneakers — Chalk White", "Reebok", Category.SHOES,
            "Retro court sneakers in soft leather.",
            gtin = "2000000000161", model = "Club C 85",
            attributes = mapOf(K.COLOR to "white", K.MATERIAL to "leather", K.SIZES to "39,40,41,42,43,44,45", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(Offer(megaBazaar, 64.95, old = 89.95, delivery = 3.95, deliveryDays = 2..4, rating = 4.6, reviews = 2700, affiliate = true))
        }
        item(
            "nb-550", "New Balance 550 Sneakers — White/Grey", "New Balance", Category.SHOES,
            "Basketball-inspired low-top sneakers.",
            gtin = "2000000000178", model = "550",
            attributes = mapOf(K.COLOR to "white,grey", K.MATERIAL to "leather", K.SIZES to "40,41,42,43,44", K.LOGO to "large"),
        ) {
            listOf(Offer(urbanThread, 129.99, delivery = 0.0, deliveryDays = 2..4, rating = 4.7, reviews = 1500, availability = Availability.LOW_STOCK))
        }
        item(
            "nordline-court", "Nordline Court Low Sneakers — Off-White", "Nordline", Category.SHOES,
            "Clean minimal low-top with no visible branding.",
            gtin = "2000000000185", model = "Court Low",
            attributes = mapOf(K.COLOR to "white,beige", K.MATERIAL to "vegan leather", K.SIZES to "36,37,38,39,40,41,42,43,44", K.STYLE to "minimal", K.LOGO to "none"),
        ) {
            listOf(Offer(urbanThread, 59.0, delivery = 4.9, deliveryDays = 3..5, rating = 4.3, reviews = 180))
        }

        // ---------- Clothing ----------
        item(
            "nordline-hoodie", "Nordline Heavyweight Oversize Hoodie — Black", "Nordline", Category.CLOTHING,
            "420 gsm cotton fleece, dropped shoulders, no logos.",
            gtin = "2000000000192", model = "Heavyweight Oversize Hoodie",
            attributes = mapOf(K.COLOR to "black", K.FIT to "oversize", K.MATERIAL to "100% cotton", K.SIZES to "XS,S,M,L,XL", K.STYLE to "minimal", K.LOGO to "none"),
        ) {
            listOf(
                Offer(urbanThread, 65.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.6, reviews = 340),
                Offer(megaBazaar, 59.0, delivery = 4.99, deliveryDays = 4..7, rating = 4.4, reviews = 120, affiliate = true),
            )
        }
        item(
            "kroma-logo-hoodie", "Kroma Big Logo Oversize Hoodie — Black", "Kroma", Category.CLOTHING,
            "Oversize fit hoodie with a large front logo print.",
            gtin = "2000000000208", model = "Big Logo Hoodie",
            attributes = mapOf(K.COLOR to "black", K.FIT to "oversize", K.MATERIAL to "80% cotton, 20% polyester", K.SIZES to "S,M,L,XL", K.LOGO to "large"),
        ) {
            listOf(Offer(megaBazaar, 39.99, old = 54.99, delivery = 3.99, deliveryDays = 3..6, rating = 4.1, reviews = 860, affiliate = true))
        }
        item(
            "basics-hoodie", "Everyday Basics Oversize Hoodie — Charcoal", "Everyday Basics", Category.CLOTHING,
            "Relaxed fit pullover hoodie.",
            gtin = "2000000000215", model = "Oversize Hoodie",
            attributes = mapOf(K.COLOR to "grey,black", K.FIT to "oversize", K.SIZES to "S,M,L,XL,XXL"),
        ) {
            listOf(Offer(shopLine, 29.9, rating = 4.0, reviews = 95, availability = Availability.UNKNOWN))
        }
        item(
            "nordline-tee", "Nordline Organic Cotton T-Shirt — White", "Nordline", Category.CLOTHING,
            "Midweight organic cotton tee, regular fit.",
            gtin = "2000000000222", model = "Organic Tee",
            attributes = mapOf(K.COLOR to "white", K.FIT to "regular", K.SIZES to "XS,S,M,L,XL", K.STYLE to "minimal", K.LOGO to "none"),
        ) {
            listOf(Offer(urbanThread, 25.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.5, reviews = 610))
        }

        // ---------- Bags ----------
        item(
            "fjord-city", "Fjordpack City Backpack 20 L — Black", "Fjordpack", Category.BAGS,
            "Water-repellent everyday backpack with 15\" laptop sleeve.",
            gtin = "2000000000239", model = "City 20L",
            attributes = mapOf(K.COLOR to "black,beige", K.MATERIAL to "recycled polyester", K.STYLE to "minimal", K.LOGO to "small"),
        ) {
            listOf(
                Offer(urbanThread, 79.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.6, reviews = 420),
                Offer(megaBazaar, 72.0, delivery = 0.0, deliveryDays = 3..5, rating = 4.5, reviews = 200, affiliate = true),
            )
        }
        item(
            "school-pack", "Campus 26 L Backpack with Pencil Organizer", "Fjordpack", Category.BAGS,
            "Lightweight backpack with padded laptop sleeve and organizer pockets.",
            gtin = "2000000000246", model = "Campus 26L",
            attributes = mapOf(K.COLOR to "blue,black,pink", K.MATERIAL to "polyester"),
        ) {
            listOf(Offer(shopLine, 39.0, delivery = 0.0, deliveryDays = 2..4, rating = 4.4, reviews = 330))
        }

        // ---------- Home ----------
        item(
            "lumo-lamp", "Lumo Arc LED Desk Lamp with Dimmer", "Lumo", Category.HOME,
            "Aluminium desk lamp, 3 colour temperatures, USB-C charging port.",
            gtin = "2000000000253", model = "Arc LED",
            attributes = mapOf(K.COLOR to "white,black", K.MATERIAL to "aluminium", K.STYLE to "minimal", K.LOGO to "none"),
        ) {
            listOf(
                Offer(techNest, 49.0, delivery = 0.0, deliveryDays = 1..3, rating = 4.5, reviews = 260),
                Offer(megaBazaar, 44.99, old = 59.99, delivery = 5.99, deliveryDays = 3..5, rating = 4.3, reviews = 140, affiliate = true),
            )
        }
    }

    /** All demo listings, one [Product] per (item, store) offer. */
    val products: List<Product> by lazy {
        entries.flatMap { (item, offers) -> offers.map { toProduct(item, it) } }
    }

    fun forStore(storeId: String): List<Product> = products.filter { it.store.id == storeId }

    private fun toProduct(item: Item, offer: Offer): Product {
        val sku = item.key
        val url = "https://${offer.store.domain}/p/$sku"
        return Product(
            id = "demo-${offer.store.id}:$sku",
            sourceId = "demo-${offer.store.id}",
            sourceProductId = sku,
            title = offer.titleOverride ?: item.title,
            description = item.description,
            images = emptyList(),
            currentPrice = Money.of(offer.price),
            oldPrice = offer.old?.let { Money.of(it) },
            store = offer.store,
            brand = item.brand,
            rating = offer.rating,
            reviewCount = offer.reviews,
            delivery = if (offer.delivery == null && offer.deliveryDays == null) {
                null
            } else {
                Delivery(
                    cost = offer.delivery?.let { Money.of(it) },
                    minDays = offer.deliveryDays?.first,
                    maxDays = offer.deliveryDays?.last,
                )
            },
            availability = offer.availability,
            productUrl = url,
            affiliateUrl = if (offer.affiliate) "$url?ref=findly-demo" else null,
            category = item.category,
            attributes = item.attributes,
            identifiers = ProductIdentifiers(gtin = item.gtin, model = item.model),
            sponsored = offer.sponsored,
            isDemo = true,
        )
    }
}
