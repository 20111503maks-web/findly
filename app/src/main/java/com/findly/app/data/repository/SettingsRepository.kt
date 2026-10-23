package com.findly.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.findly.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Developer option: demo stores that should fail, to preview error states. */
    val failingDemoSources: Set<String> = emptySet(),
    val disabledSources: Set<String> = emptySet(),
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val failing = stringSetPreferencesKey("failing_demo_sources")
        val disabled = stringSetPreferencesKey("disabled_sources")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
            failingDemoSources = p[Keys.failing].orEmpty(),
            disabledSources = p[Keys.disabled].orEmpty(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun setSourceFailing(sourceId: String, failing: Boolean) {
        context.dataStore.edit {
            val current = it[Keys.failing].orEmpty()
            it[Keys.failing] = if (failing) current + sourceId else current - sourceId
        }
    }

    suspend fun setSourceEnabled(sourceId: String, enabled: Boolean) {
        context.dataStore.edit {
            val current = it[Keys.disabled].orEmpty()
            it[Keys.disabled] = if (enabled) current - sourceId else current + sourceId
        }
    }
}
