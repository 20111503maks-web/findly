package com.findly.app.data.repository

import com.findly.app.data.local.FavoriteEntity
import com.findly.app.data.local.FavoritesDao
import com.findly.app.data.local.ProductJson
import com.findly.app.data.local.SyncState
import com.findly.core.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Favourites are stored locally first. [FavoritesSyncService] is the seam for account sync:
 * rows carry a [SyncState] so a future sync job can upload/delete pending changes.
 */
class FavoritesRepository(
    private val dao: FavoritesDao,
    private val sync: FavoritesSyncService = NoOpFavoritesSyncService,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val favorites: Flow<List<Product>> = dao.observeAll().map { list -> list.mapNotNull { ProductJson.decode(it.productJson) } }

    val favoriteIds: Flow<Set<String>> = dao.observeIds().map { it.toSet() }

    suspend fun get(id: String): Product? = dao.get(id)?.let { ProductJson.decode(it.productJson) }

    suspend fun all(): List<Product> = dao.getAll().mapNotNull { ProductJson.decode(it.productJson) }

    suspend fun isFavorite(id: String): Boolean = dao.get(id) != null

    suspend fun add(product: Product) {
        val now = clock()
        dao.upsert(
            FavoriteEntity(
                productId = product.id,
                productJson = ProductJson.encode(product),
                savedAt = now,
                updatedAt = now,
                syncState = if (sync.isEnabled) SyncState.PENDING_UPLOAD else SyncState.LOCAL_ONLY,
            ),
        )
    }

    /** Refreshes the saved snapshot with fresh source data (keeps the original save time). */
    suspend fun updateSnapshot(product: Product) {
        val existing = dao.get(product.id) ?: return
        dao.upsert(existing.copy(productJson = ProductJson.encode(product), updatedAt = clock()))
    }

    suspend fun remove(id: String) = dao.delete(id)

    suspend fun toggle(product: Product): Boolean =
        if (isFavorite(product.id)) {
            remove(product.id)
            false
        } else {
            add(product)
            true
        }
}

/** Future: sync favourites with a Findly account via the backend. */
interface FavoritesSyncService {
    val isEnabled: Boolean
    suspend fun sync()
}

object NoOpFavoritesSyncService : FavoritesSyncService {
    override val isEnabled = false
    override suspend fun sync() = Unit
}
