package com.qubix.stream.db

import androidx.room.*

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val detailUrl: String,
    val title: String,
    val posterUrl: String,
    val type: String,
    val providerId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val episodeUrl: String,
    val title: String,
    val episodeTitle: String,
    val posterUrl: String,
    val detailUrl: String,
    val providerId: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val watchedAt: Long = System.currentTimeMillis()
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    suspend fun all(): List<BookmarkEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE detailUrl = :url)")
    suspend fun exists(url: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(b: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE detailUrl = :url")
    suspend fun delete(url: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY watchedAt DESC LIMIT 50")
    suspend fun recent(): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(h: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Database(entities = [BookmarkEntity::class, HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarks(): BookmarkDao
    abstract fun history(): HistoryDao
}
