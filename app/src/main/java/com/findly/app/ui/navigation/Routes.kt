package com.findly.app.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute

@Serializable data class SearchRoute(val query: String? = null, val autoFocus: Boolean = false)

@Serializable data object SavedRoute

@Serializable data object AlertsRoute

@Serializable data object ProfileRoute

@Serializable data class ProductRoute(val productId: String)

@Serializable data object PhotoSearchRoute

@Serializable data class FindCheaperRoute(val productId: String? = null, val url: String? = null)
