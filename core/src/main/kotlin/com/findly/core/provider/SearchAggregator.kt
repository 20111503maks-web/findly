package com.findly.core.provider

import com.findly.core.model.Product
import com.findly.core.search.ProviderSearchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

sealed interface SourceStatus {
    val provider: ProviderInfo

    data class Ok(override val provider: ProviderInfo, val count: Int, val tookMs: Long) : SourceStatus
    data class Failed(override val provider: ProviderInfo, val reason: String) : SourceStatus
    data class TimedOut(override val provider: ProviderInfo) : SourceStatus
}

data class AggregatedResult(
    val products: List<Product>,
    val sources: List<SourceStatus>,
) {
    val failedSources: List<SourceStatus> get() = sources.filter { it !is SourceStatus.Ok }
    val allFailed: Boolean get() = sources.isNotEmpty() && failedSources.size == sources.size
}

/**
 * Queries all registered providers in parallel. A failing or slow source never breaks the whole
 * search: its status is reported so the UI can show "source unavailable" honestly.
 */
class SearchAggregator(
    private val registry: ProviderRegistry,
    private val timeoutMs: Long = 8_000,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun search(request: ProviderSearchRequest): AggregatedResult =
        fanOut(registry.withCapability(ProviderCapability.SEARCH)) { it.search(request) }

    suspend fun discover(interests: List<String>, limit: Int): AggregatedResult =
        fanOut(registry.withCapability(ProviderCapability.DISCOVER)) { it.discover(interests, limit) }

    suspend fun resolveUrl(url: String): Product? {
        val provider = registry.enabled().firstOrNull { it.canResolveUrl(url) } ?: return null
        return provider.resolveUrl(url)
    }

    private suspend fun fanOut(
        providers: List<ProductProvider>,
        call: suspend (ProductProvider) -> List<Product>,
    ): AggregatedResult = coroutineScope {
        val results = providers.map { provider ->
            async {
                val start = clock()
                try {
                    val items = withTimeout(timeoutMs) { call(provider) }
                    items to SourceStatus.Ok(provider.info, items.size, clock() - start)
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    emptyList<Product>() to SourceStatus.TimedOut(provider.info)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    emptyList<Product>() to SourceStatus.Failed(provider.info, e.message ?: e::class.simpleName.orEmpty())
                }
            }
        }.awaitAll()
        AggregatedResult(
            products = results.flatMap { it.first }.distinctBy { it.id },
            sources = results.map { it.second },
        )
    }
}

/** Holds all provider adapters. Adding a store = implementing [ProductProvider] and registering it here. */
class ProviderRegistry(providers: List<ProductProvider>) {
    private val all = providers.associateBy { it.info.id }
    private val disabled: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    fun all(): List<ProductProvider> = all.values.toList()

    fun enabled(): List<ProductProvider> = all.values.filter { it.info.id !in disabled }

    fun withCapability(capability: ProviderCapability) = enabled().filter { capability in it.info.capabilities }

    fun get(id: String): ProductProvider? = all[id]

    fun setEnabled(id: String, enabled: Boolean) {
        if (enabled) disabled -= id else disabled += id
    }
}
