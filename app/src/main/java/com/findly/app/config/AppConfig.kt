package com.findly.app.config

import com.findly.app.BuildConfig

/**
 * Public client configuration, injected at build time from env vars / local.properties
 * (see app/build.gradle.kts and docs/CONFIGURATION.md). Contains NO secrets.
 */
object AppConfig {
    /** Findly backend base URL, e.g. "https://api.findly.app/". Empty = backend not connected. */
    val apiBaseUrl: String = BuildConfig.FINDLY_API_BASE_URL.trim().let { if (it.isEmpty() || it.endsWith("/")) it else "$it/" }

    val isBackendConfigured: Boolean get() = apiBaseUrl.startsWith("https://")

    /** Demo stores are on by default until real sources are connected. */
    val demoProvidersEnabled: Boolean = BuildConfig.DEMO_PROVIDERS_ENABLED || !isBackendConfigured
}
