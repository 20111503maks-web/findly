package com.findly.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contract with the (future) Findly backend. See backend/openapi.yaml.
 * The backend owns all third-party API keys and affiliate credentials; the app only calls Findly.
 */
interface FindlyApi {

    @GET("v1/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("category") category: String? = null,
        @Query("min_price") minPriceMinor: Long? = null,
        @Query("max_price") maxPriceMinor: Long? = null,
        @Query("currency") currency: String? = null,
        @Query("limit") limit: Int = 40,
        @Query("locale") locale: String = "en",
    ): SearchResponseDto

    @GET("v1/products/{id}")
    suspend fun product(@Path("id") id: String): ProductDto

    @GET("v1/discover")
    suspend fun discover(@Query("interests") interests: String, @Query("limit") limit: Int): SearchResponseDto

    @GET("v1/resolve")
    suspend fun resolveUrl(@Query("url") url: String): ProductDto

    @Multipart
    @POST("v1/visual-search")
    suspend fun visualSearch(@Part image: MultipartBody.Part): VisualSearchResponseDto

    @POST("v1/ai/refine")
    suspend fun refine(@Body body: RefineRequestDto): RefineResponseDto
}

@Serializable
data class SearchResponseDto(
    val items: List<ProductDto> = emptyList(),
    val sources: List<SourceStatusDto> = emptyList(),
)

@Serializable
data class SourceStatusDto(val id: String, val name: String, val ok: Boolean, val message: String? = null)

@Serializable
data class PriceDto(@SerialName("amount_minor") val amountMinor: Long, val currency: String)

@Serializable
data class DeliveryDto(
    val cost: PriceDto? = null,
    @SerialName("min_days") val minDays: Int? = null,
    @SerialName("max_days") val maxDays: Int? = null,
    val note: String? = null,
)

@Serializable
data class StoreDto(val id: String, val name: String, val domain: String? = null, @SerialName("logo_url") val logoUrl: String? = null)

@Serializable
data class ProductDto(
    val id: String,
    val source: String,
    @SerialName("source_product_id") val sourceProductId: String,
    val title: String,
    val description: String? = null,
    val images: List<String> = emptyList(),
    val price: PriceDto,
    @SerialName("old_price") val oldPrice: PriceDto? = null,
    val store: StoreDto,
    val brand: String? = null,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int? = null,
    val delivery: DeliveryDto? = null,
    val availability: String? = null,
    @SerialName("product_url") val productUrl: String,
    @SerialName("affiliate_url") val affiliateUrl: String? = null,
    val category: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val gtin: String? = null,
    val mpn: String? = null,
    val model: String? = null,
    val sponsored: Boolean = false,
)

@Serializable
data class VisualSearchResponseDto(
    val category: String? = null,
    val colors: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val query: String,
    val confidence: Double? = null,
)

@Serializable
data class RefineRequestDto(val intents: List<String>, @SerialName("product_ids") val productIds: List<String>, val text: String? = null)

@Serializable
data class RefineResponseDto(
    @SerialName("ordered_ids") val orderedIds: List<String>,
    /** productId → intent → MATCHES / DOES_NOT_MATCH / UNKNOWN */
    val verdicts: Map<String, Map<String, String>> = emptyMap(),
    val unsupported: List<String> = emptyList(),
)
