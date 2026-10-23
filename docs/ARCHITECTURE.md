# Findly — architecture

> One search → offers from many stores → comparison → best price → go to the seller.

## Modules

```
findly/
├── core/                 Pure Kotlin (JVM). No Android. Fully unit-tested.
│   └── com.findly.core
│       ├── model/        Product, Money, Store, Delivery, Category, Availability
│       ├── provider/     ProductProvider (adapter contract), ProviderRegistry, SearchAggregator
│       ├── search/       QueryParser (NL → constraints), TextNormalizer, SearchEngine
│       ├── filter/       SearchFilters, SortOption, FilterEngine, Facets
│       ├── compare/      ProductMatcher (same product across stores), OfferComparator, FindCheaperUseCase
│       ├── ai/           AiIntent, AiRefinementEngine (+ local AttributeRefinementEngine), SimilarProducts
│       ├── visual/       VisualSearchService contract, on-device colour analysis
│       ├── link/         OutboundLinkResolver (affiliate disclosure, https only)
│       ├── pricing/      PricePoint, PriceHistoryAnalyzer, PriceAlert, AlertEvaluator
│       ├── pro/          Plan / ProFeature / EntitlementRepository
│       └── demo/         DemoCatalog + DemoStoreProvider (clearly labelled fake stores)
├── app/                  Android (Kotlin, Jetpack Compose, Material 3)
│   └── com.findly.app
│       ├── di/           AppContainer — manual DI, provider registration
│       ├── config/       AppConfig (public BuildConfig values only)
│       ├── data/local/   Room: favorites, price_points, price_alerts, recent_searches
│       ├── data/remote/  Retrofit FindlyApi, BackendProductProvider, RemoteRefinementEngine
│       ├── data/repository/ Product/Favorites/PriceTracking/PhotoSearch/Settings repositories
│       ├── notifications/ PriceCheckWorker (WorkManager), PriceAlertNotifier, PushTokenRegistrar seam
│       └── ui/           theme, components, navigation, home, search, product, saved, alerts, profile, photo, cheaper
├── backend/openapi.yaml  Contract for the future backend
└── docs/
```

`core` knows nothing about Android, so business rules (parsing, filtering, matching, best-offer
selection, alert rules) are tested on the JVM in milliseconds.

## Data flow of a search

```
"Найди белые кроссовки до 70 евро"
        │ QueryParser (deterministic, RU/EN)
        ▼
ParsedQuery(keywords=[кроссовки], colors={white}, maxPrice=€70, category=SHOES, intents={})
        │                                   │
        │ SearchAggregator (parallel,       │ initialFilters() → visible, editable filters
        │ timeout per source, errors        │
        ▼ reported per source)              ▼
List<Product> from every provider  ──►  SearchEngine.present()
                                          1. split sponsored / organic
                                          2. FilterEngine (hard filters, data-backed only)
                                          3. sort (relevance | price | price+delivery | discount | rating)
                                          4. AiRefinementEngine (only re-orders; never hides "unknown")
                                          5. ProductMatcher.group → one card per product, "in N stores"
```

## Adding a store

1. Get official access (API, product feed or affiliate programme). No scraping.
2. Preferably implement the adapter **in the backend** (it holds the keys) and expose it through
   `/v1/search`. The app needs no change.
3. If a source is truly public and keyless, implement `ProductProvider` in the app, map the
   response to `Product` (leave unknown fields `null`), and register it in `AppContainer.providers`.

## Honesty rules baked into the code

| Rule | Where |
|---|---|
| Discount only from a real `oldPrice` given by the source | `Product.discountPercent` |
| Unknown delivery never counted as free; "free delivery" filter excludes unknown | `FilterEngine`, `OfferComparator` |
| Best offer includes delivery when known, flags when some delivery is unknown | `OfferComparator.compare` |
| Title-only matches are "similar", never used for best price / savings | `MatchConfidence.SIMILAR` |
| Ranking has no commission/affiliate input | `FilterEngine.sort`, `SearchEngine.present` |
| Sponsored items separated and labelled «Реклама» | `PresentedResults.sponsored`, `SponsoredBadge` |
| Affiliate link only if the source provided it; disclosed before redirect | `OutboundLinkResolver`, `StoreRedirectDialog` |
| AI intents re-rank only; products without data stay visible; unsupported intents are reported | `AttributeRefinementEngine`, `AiRefineRow` |
| Price history = only observed values; "not enough data" state otherwise | `PriceTrackingRepository`, `PriceHistorySection` |
| Photo search without a vision API = on-device colour only, user picks the type, labelled demo | `LocalColorVisualSearchService`, `PhotoSearchScreen` |
| Demo data flagged `isDemo`, `.example` domains, GTINs from the restricted 2xx range | `DemoCatalog` |

## Price tracking & alerts

* A product is *tracked* when it is saved or has an alert.
* Observations are recorded when the product page loads fresh data, when it is saved/alerted, and by
  `PriceCheckWorker` (every ~6 h, network required). Duplicate points are throttled.
* `AlertEvaluator`: notify when price ≤ target; then only again on a further drop.
* Future server push: the backend evaluates alerts against fresh partner data and sends FCM;
  the app registers its token through `PushTokenRegistrar`.

## Monetization & Pro

* Affiliate links via `Product.affiliateUrl` (from source/backend only).
* Sponsored placements via `Product.sponsored` (always labelled).
* `Plan.Free` / `Plan.Pro` with limits (alerts count, history depth). Search, comparison,
  favourites and basic alerts are never gated. Billing would back `EntitlementRepository`
  (Google Play Billing + server-side verification).

## Account sync (future)

Favourites and alerts carry a `SyncState` column. A `FavoritesSyncService` implementation
would upload `PENDING_UPLOAD` rows and apply `PENDING_DELETE`; the current one is a no-op.
