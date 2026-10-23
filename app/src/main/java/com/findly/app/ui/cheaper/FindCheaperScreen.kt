package com.findly.app.ui.cheaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.findly.app.R
import com.findly.app.data.repository.FavoritesRepository
import com.findly.app.data.repository.ProductRepository
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.components.CircleIconButton
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.InfoBanner
import com.findly.app.ui.components.ProductImage
import com.findly.app.ui.components.SkeletonBlock
import com.findly.app.ui.components.SourceStatusBanner
import com.findly.app.ui.product.OfferRow
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.compare.FindCheaperResult
import com.findly.core.compare.MatchConfidence
import com.findly.core.model.Product
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FindCheaperUiState(
    val url: String = "",
    val loading: Boolean = false,
    val result: FindCheaperResult? = null,
    val failed: Boolean = false,
)

class FindCheaperViewModel(
    private val products: ProductRepository,
    favorites: FavoritesRepository,
    initialProductId: String?,
    initialUrl: String?,
) : ViewModel() {
    private val _state = MutableStateFlow(FindCheaperUiState(url = initialUrl.orEmpty()))
    val state: StateFlow<FindCheaperUiState> = _state.asStateFlow()

    val saved: StateFlow<List<Product>> = favorites.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Domains of connected stores that support link lookup (shown as a hint). */
    val supportedHint: String = products.urlLookupProviders().joinToString(", ") { it.info.displayName }

    init {
        when {
            initialProductId != null -> viewModelScope.launch { products.product(initialProductId)?.let { check(it) } }
            !initialUrl.isNullOrBlank() -> checkUrl()
        }
    }

    fun onUrlChange(v: String) = _state.update { it.copy(url = v, result = null, failed = false) }

    fun checkUrl() {
        val url = extractUrl(_state.value.url) ?: run {
            _state.update { it.copy(result = FindCheaperResult.UnsupportedLink) }
            return
        }
        launchCheck { products.findCheaper(url) }
    }

    fun check(product: Product) = launchCheck { products.findCheaper(product) }

    private fun launchCheck(block: suspend () -> FindCheaperResult) {
        _state.update { it.copy(loading = true, result = null, failed = false) }
        viewModelScope.launch {
            try {
                val r = block()
                _state.update { it.copy(loading = false, result = r) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, failed = true) }
            }
        }
    }

    /** Accepts pasted text like "Look at this https://store/p/1 !" and extracts the https link. */
    private fun extractUrl(text: String): String? = Regex("https://\\S+").find(text.trim())?.value?.trimEnd('.', ',', ')', '!')
}

@Composable
fun FindCheaperScreen(
    initialProductId: String?,
    initialUrl: String?,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    val vm = containerViewModel(key = "cheaper:${initialProductId}:${initialUrl}") {
        FindCheaperViewModel(it.productRepository, it.favoritesRepository, initialProductId, initialUrl)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(Modifier.statusBarsPadding().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.find_cheaper_title), style = MaterialTheme.typography.headlineSmall)
            }
        }
        item {
            Text(stringResource(R.string.find_cheaper_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            OutlinedTextField(
                value = state.url,
                onValueChange = vm::onUrlChange,
                placeholder = { Text(stringResource(R.string.find_cheaper_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_link), contentDescription = null) },
                trailingIcon = {
                    TextButton(onClick = { clipboard.getText()?.text?.let(vm::onUrlChange) }) { Text(stringResource(R.string.action_paste)) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focus.clearFocus()
                    vm.checkUrl()
                }),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = {
                    focus.clearFocus()
                    vm.checkUrl()
                },
                enabled = state.url.isNotBlank() && !state.loading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.action_find_cheaper)) }
        }
        if (vm.supportedHint.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.find_cheaper_supported, vm.supportedHint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (val r = state.result) {
            null -> when {
                state.loading -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SkeletonBlock(Modifier.fillMaxWidth().height(96.dp))
                        SkeletonBlock(Modifier.fillMaxWidth().height(64.dp))
                        SkeletonBlock(Modifier.fillMaxWidth().height(64.dp))
                    }
                }
                state.failed -> item { InfoBanner(stringResource(R.string.find_cheaper_failed), warning = true) }
                saved.isNotEmpty() -> {
                    item { Text(stringResource(R.string.find_cheaper_from_saved), style = MaterialTheme.typography.titleMedium) }
                    items(saved, key = { it.id }) { p -> SavedPickRow(p) { vm.check(p) } }
                }
            }
            FindCheaperResult.UnsupportedLink -> item {
                EmptyState(
                    icon = painterResource(R.drawable.ic_link),
                    title = stringResource(R.string.state_unsupported_link_title),
                    message = stringResource(R.string.state_unsupported_link_message),
                )
            }
            FindCheaperResult.ProductNotFound -> item {
                EmptyState(
                    icon = painterResource(R.drawable.ic_link),
                    title = stringResource(R.string.state_product_missing_title),
                    message = stringResource(R.string.state_product_missing_message),
                )
            }
            is FindCheaperResult.Found -> {
                item { SourceStatusBanner(r.sources) }
                item { ReferenceCard(r.reference) { onOpenProduct(r.reference.id) } }
                item { BestFoundCard(r) }
                val offers = r.comparison.offers.filter { it.confidence != MatchConfidence.SIMILAR }
                items(offers, key = { it.product.id }) { offer ->
                    OfferRow(offer, isCurrent = offer.product.id == r.reference.id, onClick = { onOpenProduct(offer.product.id) })
                }
                if (r.comparison.hasUnknownDelivery) item { InfoBanner(stringResource(R.string.compare_unknown_delivery)) }
            }
        }
    }
}

@Composable
private fun ReferenceCard(product: Product, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(MaterialTheme.shapes.medium)) { ProductImage(product, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.find_cheaper_your_product), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(product.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${formatMoney(product.currentPrice)} · ${product.store.name}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BestFoundCard(r: FindCheaperResult.Found) {
    val best = r.comparison.best
    Column(
        Modifier
            .fillMaxWidth()
            .background(FindlyTheme.extra.bestPrice.copy(alpha = 0.10f), MaterialTheme.shapes.large)
            .padding(18.dp),
    ) {
        Text(stringResource(R.string.best_found_price), style = MaterialTheme.typography.labelLarge, color = FindlyTheme.extra.bestPrice)
        if (best == null || r.comparison.offers.size <= 1) {
            Text(stringResource(R.string.find_cheaper_none), style = MaterialTheme.typography.titleMedium)
            return@Column
        }
        Text(formatMoney(best.effectivePrice), style = MaterialTheme.typography.displaySmall)
        Text(best.product.store.name, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        val savings = r.savings
        Text(
            if (savings != null) stringResource(R.string.savings_value, formatMoney(savings)) else stringResource(R.string.find_cheaper_already_best),
            style = MaterialTheme.typography.bodyMedium,
            color = if (savings != null) FindlyTheme.extra.bestPrice else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedPickRow(product: Product, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small)) { ProductImage(product, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(product.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatMoney(product.currentPrice)} · ${product.store.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
