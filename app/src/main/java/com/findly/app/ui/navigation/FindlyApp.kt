package com.findly.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.findly.app.R
import com.findly.app.ui.alerts.AlertsScreen
import com.findly.app.ui.cheaper.FindCheaperScreen
import com.findly.app.ui.home.HomeScreen
import com.findly.app.ui.photo.PhotoSearchScreen
import com.findly.app.ui.product.ProductScreen
import com.findly.app.ui.profile.ProfileScreen
import com.findly.app.ui.saved.SavedScreen
import com.findly.app.ui.search.SearchScreen
import kotlin.reflect.KClass

private data class TopLevel(val route: Any, val routeClass: KClass<*>, val icon: ImageVector, val label: Int)

private val topLevel = listOf(
    TopLevel(HomeRoute, HomeRoute::class, Icons.Filled.Home, R.string.tab_home),
    TopLevel(SearchRoute(), SearchRoute::class, Icons.Filled.Search, R.string.tab_search),
    TopLevel(SavedRoute, SavedRoute::class, Icons.Filled.Favorite, R.string.tab_saved),
    TopLevel(AlertsRoute, AlertsRoute::class, Icons.Filled.Notifications, R.string.tab_alerts),
    TopLevel(ProfileRoute, ProfileRoute::class, Icons.Filled.Person, R.string.tab_profile),
)

/** Something the app was launched with: a notification (product) or a shared link (find cheaper). */
sealed interface LaunchTarget {
    data class Product(val id: String) : LaunchTarget
    data class SharedLink(val url: String) : LaunchTarget
}

@Composable
fun FindlyApp(launchTarget: LaunchTarget?, onLaunchTargetHandled: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination == null || topLevel.any { t -> destination.hierarchy.any { it.hasRoute(t.routeClass) } }

    LaunchedEffect(launchTarget) {
        when (launchTarget) {
            is LaunchTarget.Product -> nav.navigate(ProductRoute(launchTarget.id))
            is LaunchTarget.SharedLink -> nav.navigate(FindCheaperRoute(url = launchTarget.url))
            null -> return@LaunchedEffect
        }
        onLaunchTargetHandled()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Screens handle the status bar themselves (edge-to-edge hero images); only the bottom bar is reserved here.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    topLevel.forEach { item ->
                        val selected = destination?.hierarchy?.any { it.hasRoute(item.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(180)) },
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenSearch = { q -> nav.navigate(SearchRoute(query = q, autoFocus = q == null)) { launchSingleTop = true } },
                    onOpenPhotoSearch = { nav.navigate(PhotoSearchRoute) },
                    onOpenFindCheaper = { nav.navigate(FindCheaperRoute()) },
                    onOpenProduct = { nav.navigate(ProductRoute(it)) },
                )
            }
            composable<SearchRoute> { entry ->
                val route = entry.toRoute<SearchRoute>()
                SearchScreen(
                    initialQuery = route.query,
                    autoFocus = route.autoFocus,
                    onOpenProduct = { nav.navigate(ProductRoute(it)) },
                    onOpenPhotoSearch = { nav.navigate(PhotoSearchRoute) },
                )
            }
            composable<SavedRoute> {
                SavedScreen(onOpenProduct = { nav.navigate(ProductRoute(it)) }, onExplore = { nav.navigateTopLevel(HomeRoute) })
            }
            composable<AlertsRoute> {
                AlertsScreen(onOpenProduct = { nav.navigate(ProductRoute(it)) }, onExplore = { nav.navigateTopLevel(HomeRoute) })
            }
            composable<ProfileRoute> { ProfileScreen() }
            composable<ProductRoute>(
                enterTransition = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)) },
                popExitTransition = { slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(200)) },
            ) { entry ->
                val route = entry.toRoute<ProductRoute>()
                ProductScreen(
                    productId = route.productId,
                    onBack = { nav.popBackStack() },
                    onOpenProduct = { nav.navigate(ProductRoute(it)) },
                )
            }
            composable<PhotoSearchRoute> {
                PhotoSearchScreen(
                    onBack = { nav.popBackStack() },
                    onSearch = { q ->
                        nav.popBackStack()
                        nav.navigate(SearchRoute(query = q)) { launchSingleTop = true }
                    },
                )
            }
            composable<FindCheaperRoute> { entry ->
                val route = entry.toRoute<FindCheaperRoute>()
                FindCheaperScreen(
                    initialProductId = route.productId,
                    initialUrl = route.url,
                    onBack = { nav.popBackStack() },
                    onOpenProduct = { nav.navigate(ProductRoute(it)) },
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
