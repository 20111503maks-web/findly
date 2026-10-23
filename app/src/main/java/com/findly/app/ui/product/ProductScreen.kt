package com.findly.app.ui.product

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.findly.app.R
import com.findly.app.ui.common.containerViewModel
import com.findly.app.ui.common.formatMoney
import com.findly.app.ui.common.labelRes
import com.findly.app.ui.components.CircleIconButton
import com.findly.app.ui.components.DeliveryLine
import com.findly.app.ui.components.DemoBadge
import com.findly.app.ui.components.DiscountBadge
import com.findly.app.ui.components.EmptyState
import com.findly.app.ui.components.PriceRow
import com.findly.app.ui.components.ProductCard
import com.findly.app.ui.components.ProductCardSkeleton
import com.findly.app.ui.components.ProductImage
import com.findly.app.ui.components.RatingRow
import com.findly.app.ui.components.SkeletonBlock
import com.findly.app.ui.components.SponsoredBadge
import com.findly.app.ui.theme.FindlyTheme
import com.findly.core.model.AttributeKeys
import com.findly.core.model.Product

@Composable
fun ProductScreen(
    productId: String,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
) {
    val vm = containerViewModel(key = "product:$productId") {
        ProductViewModel(productId, it.productRepository, it.favoritesRepository, it.priceTrackingRepository, it.linkResolver)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val isFavorite by vm.isFavorite.collectAsStateWithLifecycle()
    val alert by vm.alert.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val range by vm.historyRange.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var showRedirect by remember { mutableStateOf<Product?>(null) }
    var showAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showCompare by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val alertCreatedMsg = stringResource(R.string.alert_created)
    val alertLimitMsg = stringResource(R.string.alert_limit)
    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                ProductEvent.AlertCreated -> snackbar.showSnackbar(alertCreatedMsg)
                is ProductEvent.AlertLimit -> snackbar.showSnackbar(alertLimitMsg.format(e.limit))
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            state.product?.let { p ->
                BuyBar(p, onBuy = { showRedirect = p })
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.loading -> LoadingSkeleton(onBack)
            state.notFound || state.product == null -> Column(Modifier.statusBarsPadding()) {
                BackRow(onBack)
                EmptyState(
                    icon = painterResource(R.drawable.ic_cloud_off),
                    title = stringResource(R.string.state_product_missing_title),
                    message = stringResource(R.string.state_product_missing_message),
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
            }
            else -> {
                val product = state.product!!
                LazyColumn(
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                            ProductImage(product, Modifier.fillMaxSize(), large = true)
                            Row(
                                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                CircleIconButton(onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                                }
                                CircleIconButton(vm::toggleFavorite) {
                                    Icon(
                                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = stringResource(if (isFavorite) R.string.action_unsave else R.string.action_save),
                                        tint = if (isFavorite) FindlyTheme.extra.discount else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    item { Header(product) }
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            BestPriceCallout(product, state.comparison) { onOpenProduct(it.id) }
                        }
                    }
                    item {
                        Section {
                            OffersSection(
                                current = product,
                                loading = state.offersLoading,
                                comparison = state.comparison,
                                onOpenOffer = { onOpenProduct(it.id) },
                                onCompare = { showCompare = true },
                            )
                        }
                    }
                    item {
                        Section {
                            AlertCard(
                                alertText = alert?.let { stringResource(R.string.alert_active, formatMoney(it.targetPrice)) },
                                onCreate = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    showAlertDialog = true
                                },
                                onDelete = vm::deleteAlert,
                            )
                        }
                    }
                    item {
                        Section {
                            PriceHistorySection(
                                summary = history,
                                range = range,
                                isTracked = isFavorite || alert != null,
                                onRangeChange = vm::setRange,
                                onStartTracking = { vm.toggleFavorite() },
                            )
                        }
                    }
                    if (product.attributes.isNotEmpty()) {
                        item { Section { Attributes(product) } }
                    }
                    product.description?.let { d ->
                        item {
                            Section {
                                Text(stringResource(R.string.product_description), style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                Text(d, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Column(Modifier.padding(top = 28.dp)) {
                            Text(
                                stringResource(R.string.similar_title),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.similarLoading) {
                                    items(3) { ProductCardSkeleton(Modifier.width(160.dp)) }
                                } else if (state.similar.isEmpty()) {
                                    item {
                                        Text(
                                            stringResource(R.string.similar_empty),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    items(state.similar, key = { it.id }) { p ->
                                        ProductCard(
                                            product = p,
                                            isFavorite = p.id in favoriteIds,
                                            onClick = { onOpenProduct(p.id) },
                                            onToggleFavorite = { vm.toggleFavorite(p) },
                                            modifier = Modifier.width(160.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showRedirect?.let { p ->
        val link = remember(p) { vm.link(p) }
        if (link == null) {
            // Source gave no valid https link: never open anything we can't verify.
            LaunchedEffect(p) {
                showRedirect = null
                snackbar.showSnackbar(context.getString(R.string.error_invalid_link))
            }
        } else {
            StoreRedirectDialog(
                product = p,
                link = link,
                onConfirm = {
                    showRedirect = null
                    openUrl(context, link.url)
                },
                onDismiss = { showRedirect = null },
            )
        }
    }
    val product = state.product
    if (showAlertDialog && product != null) {
        PriceAlertDialog(
            product = product,
            onCreate = {
                vm.createAlert(it)
                showAlertDialog = false
            },
            onDismiss = { showAlertDialog = false },
        )
    }
    val comparison = state.comparison
    if (showCompare && product != null && comparison != null) {
        CompareSheet(
            current = product,
            comparison = comparison,
            onOpenOffer = {
                showCompare = false
                if (it.id != product.id) onOpenProduct(it.id)
            },
            onDismiss = { showCompare = false },
        )
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp).padding(top = 28.dp)) {
        content()
    }
}

@Composable
private fun Header(product: Product) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                listOfNotNull(product.brand, product.store.name).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (product.isDemo) DemoBadge()
            if (product.sponsored) SponsoredBadge()
        }
        Spacer(Modifier.height(6.dp))
        Text(product.title, style = MaterialTheme.typography.headlineMedium)
        product.rating?.let {
            Spacer(Modifier.height(8.dp))
            RatingRow(it, product.reviewCount, compact = false)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PriceRow(product, large = true)
            product.discountPercent?.let {
                Spacer(Modifier.width(10.dp))
                DiscountBadge(it)
            }
        }
        Spacer(Modifier.height(6.dp))
        DeliveryLine(product)
        product.availability.labelRes()?.let {
            Text(stringResource(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertCard(alertText: String?, onCreate: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                alertText ?: stringResource(R.string.alert_card_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (alertText == null) {
                Text(stringResource(R.string.alert_card_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (alertText == null) {
            OutlinedButton(onClick = onCreate) { Text(stringResource(R.string.action_track_price)) }
        } else {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.action_stop)) }
        }
    }
}

private val attributeLabels = mapOf(
    AttributeKeys.COLOR to R.string.attr_color,
    AttributeKeys.SIZES to R.string.attr_sizes,
    AttributeKeys.MATERIAL to R.string.attr_material,
    AttributeKeys.STYLE to R.string.attr_style,
    AttributeKeys.LOGO to R.string.attr_logo,
    AttributeKeys.LAYOUT to R.string.attr_layout,
    AttributeKeys.SWITCHES to R.string.attr_switches,
    AttributeKeys.CONNECTIVITY to R.string.attr_connectivity,
    AttributeKeys.STORAGE to R.string.attr_storage,
    AttributeKeys.FIT to R.string.attr_fit,
)

@Composable
private fun Attributes(product: Product) {
    Text(stringResource(R.string.product_specs), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    product.attributes.forEach { (key, value) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text(
                attributeLabels[key]?.let { stringResource(it) } ?: key.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.4f),
            )
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    product.identifiers.gtin?.let {
        Text(
            stringResource(R.string.attr_gtin, it),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun BuyBar(product: Product, onBuy: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(formatMoney(product.currentPrice), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.buy_at, product.store.name),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onBuy,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(stringResource(R.string.action_go_to_store))
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(Modifier.padding(12.dp)) {
        CircleIconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
    }
}

@Composable
private fun LoadingSkeleton(onBack: () -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            SkeletonBlock(Modifier.fillMaxSize())
            Box(Modifier.statusBarsPadding()) { BackRow(onBack) }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.3f).height(12.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.9f).height(24.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.6f).height(24.dp))
            SkeletonBlock(Modifier.fillMaxWidth(0.4f).height(36.dp))
        }
    }
}
