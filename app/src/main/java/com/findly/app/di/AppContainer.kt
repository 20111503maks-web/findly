package com.findly.app.di

import android.content.Context
import androidx.room.Room
import com.findly.app.config.AppConfig
import com.findly.app.data.local.FindlyDatabase
import com.findly.app.data.remote.BackendProductProvider
import com.findly.app.data.remote.FindlyApi
import com.findly.app.data.remote.RemoteRefinementEngine
import com.findly.app.data.repository.AppSettings
import com.findly.app.data.repository.ConnectivityObserver
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.data.repository.PhotoSearchRepository
import com.findly.app.data.repository.PriceTrackingRepository
import com.findly.app.data.repository.ProductRepository
import com.findly.app.data.repository.SettingsRepository
import com.findly.app.notifications.NoPushTokenRegistrar
import com.findly.app.notifications.PriceAlertNotifier
import com.findly.app.notifications.PushTokenRegistrar
import com.findly.core.ai.AiRefinementEngine
import com.findly.core.ai.AttributeRefinementEngine
import com.findly.core.compare.FindCheaperUseCase
import com.findly.core.demo.DemoCatalog
import com.findly.core.demo.DemoStoreProvider
import com.findly.core.link.OutboundLinkResolver
import com.findly.core.pro.EntitlementRepository
import com.findly.core.pro.FreeEntitlementRepository
import com.findly.core.provider.ProductProvider
import com.findly.core.provider.ProviderKind
import com.findly.core.provider.ProviderRegistry
import com.findly.core.provider.SearchAggregator
import com.findly.core.search.QueryParser
import com.findly.core.search.SearchEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection. Small enough not to need Hilt/Koin; swap in later if the graph grows.
 * To add a store: implement [ProductProvider] and add it to [providers].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val appScope = CoroutineScope(SupervisorJob())

    val settingsRepository = SettingsRepository(appContext)
    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(appScope, SharingStarted.Eagerly, AppSettings())

    private val database = Room.databaseBuilder(appContext, FindlyDatabase::class.java, "findly.db").build()

    private val api: FindlyApi? = if (AppConfig.isBackendConfigured) createApi(AppConfig.apiBaseUrl) else null

    private val backendProvider: ProductProvider? = api?.let { BackendProductProvider(it) }

    private val providers: List<ProductProvider> = buildList {
        backendProvider?.let(::add)
        if (AppConfig.demoProvidersEnabled) {
            DemoCatalog.stores.forEach { store ->
                add(DemoStoreProvider(store, latencyMs = 450L + store.id.length * 40L, shouldFail = { "demo-${store.id}" in settings.value.failingDemoSources }))
            }
        }
    }

    val registry = ProviderRegistry(providers)

    private val aggregator = SearchAggregator(registry)

    private val refinementEngine: AiRefinementEngine = AttributeRefinementEngine().let { local ->
        if (api != null) RemoteRefinementEngine(api, local) else local
    }

    val entitlements: EntitlementRepository = FreeEntitlementRepository()

    val favoritesRepository = FavoritesRepository(database.favorites())

    val productRepository = ProductRepository(
        registry = registry,
        aggregator = aggregator,
        engine = SearchEngine(aggregator, QueryParser(), refinementEngine = refinementEngine),
        findCheaper = FindCheaperUseCase(aggregator),
        recentSearchDao = database.recentSearches(),
        favorites = favoritesRepository,
        backend = backendProvider,
    )

    val priceTrackingRepository = PriceTrackingRepository(database.priceHistory(), database.alerts(), entitlements)

    val photoSearchRepository = PhotoSearchRepository(appContext, api)

    val connectivity = ConnectivityObserver(appContext)

    val linkResolver = OutboundLinkResolver()

    val notifier = PriceAlertNotifier(appContext)

    val pushRegistrar: PushTokenRegistrar = NoPushTokenRegistrar

    val isDemoMode: Boolean get() = providers.any { it.info.kind == ProviderKind.DEMO }

    init {
        // Apply persisted enable/disable choices whenever they change.
        appScope.launch {
            settings.collect { s -> providers.forEach { registry.setEnabled(it.info.id, it.info.id !in s.disabledSources) } }
        }
    }

    private fun createApi(baseUrl: String): FindlyApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FindlyApi::class.java)
    }
}
