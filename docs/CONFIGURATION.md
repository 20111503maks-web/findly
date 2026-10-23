# Configuration & secrets

## Rule #1: the APK contains no secrets

Anything compiled into an Android app can be extracted from the APK. So:

| Value | Where it lives |
|---|---|
| `FINDLY_API_BASE_URL` (public URL of our backend) | Env var or `local.properties` → `BuildConfig` |
| `FINDLY_DEMO_PROVIDERS` | Env var or `local.properties` → `BuildConfig` |
| Store / affiliate API keys, AI keys, FCM credentials, DB URL | **Backend only** — env vars / secret manager (e.g. GCP Secret Manager, AWS Secrets Manager, Doppler) |

The Android client only ever talks to the Findly backend. The backend holds the partner
credentials, calls the official APIs, normalizes results into the `Product` contract
(`backend/openapi.yaml`) and returns them.

## Local Android setup

`local.properties` (git-ignored, created by Android Studio):

```properties
sdk.dir=/path/to/Android/sdk
# optional
FINDLY_API_BASE_URL=https://api.findly.example/
FINDLY_DEMO_PROVIDERS=true
```

Or use environment variables with the same names (CI does this). See `.env.example` for the
full list, including backend-only secrets.

## CI

`.github/workflows/android.yml` builds the debug APK, runs `:core` unit tests, app unit tests and
lint. Add `FINDLY_API_BASE_URL` as a repository *variable* (not a secret — it is public) when the
backend exists. Backend secrets go to the backend's deployment, not to this workflow.
