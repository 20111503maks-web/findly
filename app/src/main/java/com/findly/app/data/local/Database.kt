package com.findly.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Sync state for future account sync (favourites/alerts uploaded to the backend). */
enum class SyncState { LOCAL_ONLY, PENDING_UPLOAD, SYNCED, PENDING_DELETE }

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val productId: String,
    /** JSON snapshot of the Product, so saved items render offline. */
    val productJson: String,
    val savedAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
)

@Entity(tableName = "price_points", indices = [Index("productId"), Index(value = ["productId", "observedAt"])])
data class PricePointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val amountMinor: Long,
    val currency: String,
    val observedAt: Long,
)

@Entity(tableName = "price_alerts", indices = [Index("productId", unique = true)])
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val productJson: String,
    val targetMinor: Long,
    val currency: String,
    val createdAt: Long,
    val isActive: Boolean = true,
    val lastSeenMinor: Long? = null,
    val lastCheckedAt: Long? = null,
    val lastNotifiedMinor: Long? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long,
)

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites WHERE syncState != 'PENDING_DELETE' ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT productId FROM favorites WHERE syncState != 'PENDING_DELETE'")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT * FROM favorites WHERE syncState != 'PENDING_DELETE'")
    suspend fun getAll(): List<FavoriteEntity>

    @Query("SELECT * FROM favorites WHERE productId = :id")
    suspend fun get(id: String): FavoriteEntity?

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE productId = :id")
    suspend fun delete(id: String)
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_points WHERE productId = :productId ORDER BY observedAt ASC")
    fun observe(productId: String): Flow<List<PricePointEntity>>

    @Query("SELECT * FROM price_points WHERE productId = :productId ORDER BY observedAt DESC LIMIT 1")
    suspend fun latest(productId: String): PricePointEntity?

    @Insert
    suspend fun insert(point: PricePointEntity)

    @Query("DELETE FROM price_points WHERE observedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface PriceAlertsDao {
    @Query("SELECT * FROM price_alerts WHERE syncState != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PriceAlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE productId = :productId LIMIT 1")
    fun observeForProduct(productId: String): Flow<PriceAlertEntity?>

    @Query("SELECT * FROM price_alerts WHERE isActive = 1 AND syncState != 'PENDING_DELETE'")
    suspend fun getActive(): List<PriceAlertEntity>

    @Query("SELECT COUNT(*) FROM price_alerts WHERE isActive = 1")
    suspend fun countActive(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PriceAlertEntity): Long

    @Upsert
    suspend fun upsert(entity: PriceAlertEntity)

    @Query("SELECT * FROM price_alerts WHERE id = :id")
    suspend fun get(id: Long): PriceAlertEntity?

    @Query("DELETE FROM price_alerts WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY searchedAt DESC LIMIT :limit")
    fun observe(limit: Int = 8): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Database(
    entities = [FavoriteEntity::class, PricePointEntity::class, PriceAlertEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FindlyDatabase : RoomDatabase() {
    abstract fun favorites(): FavoritesDao
    abstract fun priceHistory(): PriceHistoryDao
    abstract fun alerts(): PriceAlertsDao
    abstract fun recentSearches(): RecentSearchDao
}
