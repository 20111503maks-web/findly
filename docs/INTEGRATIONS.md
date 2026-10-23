# What is needed to connect real services

Nothing below is simulated in the code as a "real" integration. Each has an interface and,
where useful, a clearly labelled demo/local implementation.

## Product sources (backend adapters)

| Source | What to obtain | Notes |
|---|---|---|
| **Amazon** — Product Advertising API 5.0 | Amazon Associates account (approved, with qualifying sales), PA-API access key + secret, partner tag | Prices must be refreshed often; ToS restricts caching. Affiliate link comes from the API. |
| **eBay** — Browse API | eBay developer account, OAuth client id/secret; eBay Partner Network campaign id for affiliate links | Good GTIN/MPN coverage → reliable cross-store matching. |
| **Awin / CJ / Rakuten / Tradedoubler** | Publisher account, approval per advertiser, API token | Product feeds (CSV/XML) per merchant: ingest on the backend, normalize to `Product`. Main route for EU fashion & electronics shops. |
| **AliExpress** — Affiliate API | AliExpress Portals account, app key/secret, tracking id | |
| **Google Shopping / Merchant data** | No public search API for third parties | Not usable as a source; do not scrape. |
| Individual shops (Zalando, MediaMarkt, Otto…) | Usually via the affiliate networks above | Check each programme's rules on price comparison use. |

## AI

| Feature | Option | Needed |
|---|---|---|
| NL query understanding (beyond the rule-based parser) | Claude (Anthropic API) on the backend, returning `ParsedQuery`-like JSON | Anthropic API key (backend) |
| Subjective re-ranking (`/v1/ai/refine`) | LLM over normalized attributes + product images | Same; plus image access rights from sources |
| Visual search (`/v1/visual-search`) | Vision model (Claude vision / Google Cloud Vision) + optional image-embedding index for "visually similar" | API key, embedding store (e.g. pgvector) |

## Notifications

| Feature | Needed |
|---|---|
| Local alerts (done) | Nothing — WorkManager + local notifications |
| Server push | Firebase project, `google-services.json` in the app, FCM service account on the backend; implement `PushTokenRegistrar` |

## Accounts, sync, Pro

| Feature | Needed |
|---|---|
| Accounts & favourites sync | Auth provider (Firebase Auth / Sign in with Google / own OIDC), backend endpoints, implement `FavoritesSyncService` |
| Findly Pro | Google Play Console, Play Billing Library, server-side purchase verification; implement `EntitlementRepository` |

## Image hosting

Demo products have no photos on purpose (no misleading stock images). Real sources provide
image URLs; Coil loads them with memory/disk caching.
