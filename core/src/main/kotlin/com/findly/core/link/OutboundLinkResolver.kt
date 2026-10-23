package com.findly.core.link

import com.findly.core.model.Product
import java.net.URI

data class OutboundLink(
    val url: String,
    val storeName: String,
    /** Host the user will land on (shown before leaving the app). */
    val displayHost: String,
    /** True when a partner link from the source is used. Disclosed in the UI. */
    val isAffiliate: Boolean,
)

/**
 * Chooses the link for "Go to store". Affiliate links are used only when the SOURCE supplied them;
 * Findly never rewrites a store link into its own tracking link, and always discloses partner links.
 */
class OutboundLinkResolver {

    fun resolve(product: Product): OutboundLink? {
        val direct = product.productUrl.takeIf(::isSafeWebUrl)
        val affiliate = product.affiliateUrl?.takeIf(::isSafeWebUrl)
        val url = affiliate ?: direct ?: return null
        val host = hostOf(product.productUrl) ?: product.store.domain ?: hostOf(url).orEmpty()
        return OutboundLink(
            url = url,
            storeName = product.store.name,
            displayHost = host,
            isAffiliate = affiliate != null,
        )
    }

    fun isSafeWebUrl(url: String): Boolean = runCatching {
        val uri = URI(url.trim())
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    fun hostOf(url: String): String? = runCatching { URI(url.trim()).host?.removePrefix("www.") }.getOrNull()
}
