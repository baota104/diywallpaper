package com.example.diywallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diywallpaper.data.local.entity.WallpaperCategoryEntity
import com.example.diywallpaper.data.local.entity.WallpaperItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {
    @Query("SELECT * FROM wallpaper_categories ORDER BY rank ASC, name ASC")
    fun observeCategories(): Flow<List<WallpaperCategoryEntity>>

    @Query("SELECT * FROM wallpaper_items ORDER BY rank ASC, id ASC")
    fun observeItems(): Flow<List<WallpaperItemEntity>>

    @Query("SELECT id, isFavorite FROM wallpaper_items")
    suspend fun getFavoriteFlags(): List<WallpaperFavoriteFlag>

    @Query("UPDATE wallpaper_items SET isFavorite = NOT isFavorite, updatedAt = :updatedAt WHERE id = :itemId")
    suspend fun toggleFavorite(itemId: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<WallpaperCategoryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<WallpaperItemEntity>): List<Long>

    @Query("DELETE FROM wallpaper_categories")
    suspend fun clearCategories(): Int

    @Query("DELETE FROM wallpaper_items")
    suspend fun clearItems(): Int

    @Query("SELECT COUNT(*) FROM wallpaper_categories")
    suspend fun getCategoryCount(): Int
}

data class WallpaperFavoriteFlag(
    val id: String,
    val isFavorite: Boolean
)
