package com.findly.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.findly.app.BuildConfig
import com.findly.app.R
import com.findly.app.config.AppConfig
import com.findly.app.data.repository.AppSettings
import com.findly.app.data.repository.SettingsRepository
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.components.DemoBadge
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.theme.FindlyTheme
import com.findly.app.ui.theme.ThemeMode
import com.findly.core.provider.ProviderInfo
import com.findly.core.provider.ProviderKind
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val settingsRepo: SettingsRepository,
    val settings: StateFlow<AppSettings>,
    val sources: List<ProviderInfo>,
) : ViewModel() {
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    fun setSourceEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setSourceEnabled(id, enabled) }
    }

    fun setSourceFailing(id: String, failing: Boolean) {
        viewModelScope.launch { settingsRepo.setSourceFailing(id, failing) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val vm = containerViewModel { c ->
        ProfileViewModel(c.settingsRepository, c.settings, c.productRepository.providers().map { it.info })
    }
    val settings by vm.settings.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.tab_profile), style = MaterialTheme.typography.displaySmall)

        Card {
            Text(stringResource(R.string.profile_account), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.profile_account_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Card {
            Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            val modes = listOf(ThemeMode.SYSTEM to R.string.theme_system, ThemeMode.LIGHT to R.string.theme_light, ThemeMode.DARK to R.string.theme_dark)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { i, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { vm.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }

        ProCard()

        Card {
            Text(stringResource(R.string.profile_sources), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.profile_sources_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            vm.sources.forEach { source ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(source.displayName, style = MaterialTheme.typography.titleSmall)
                            if (source.kind == ProviderKind.DEMO) DemoBadge()
                        }
                        Text(
                            stringResource(
                                when (source.kind) {
                                    ProviderKind.DEMO -> R.string.source_kind_demo
                                    ProviderKind.FINDLY_BACKEND -> R.string.source_kind_backend
                                    ProviderKind.OFFICIAL_API -> R.string.source_kind_api
                                    ProviderKind.AFFILIATE_FEED -> R.string.source_kind_feed
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = source.id !in settings.disabledSources,
                        onCheckedChange = { vm.setSourceEnabled(source.id, it) },
                    )
                }
            }
            if (!AppConfig.isBackendConfigured) {
                Spacer(Modifier.height(6.dp))
                InfoBanner(stringResource(R.string.profile_backend_not_configured))
            }
        }

        Card {
            Text(stringResource(R.string.profile_transparency), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.profile_transparency_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        val demoSources = vm.sources.filter { it.kind == ProviderKind.DEMO }
        if (demoSources.isNotEmpty()) {
            Card {
                Text(stringResource(R.string.profile_developer), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.profile_developer_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                demoSources.forEach { source ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.profile_simulate_failure, source.displayName),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = source.id in settings.failingDemoSources,
                            onCheckedChange = { vm.setSourceFailing(source.id, it) },
                        )
                    }
                }
            }
        }

        Text(
            stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun ProCard() {
    val accent = MaterialTheme.colorScheme.secondary
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(accent.copy(alpha = 0.22f), FindlyTheme.extra.demo.copy(alpha = 0.10f))),
                MaterialTheme.shapes.large,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.pro_features), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.pro_free_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.pro_coming_soon), style = MaterialTheme.typography.labelLarge, color = accent)
    }
}
