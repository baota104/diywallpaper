package com.example.diywallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diywallpaper.data.local.entity.StickerItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerDao {
    @Query("SELECT * FROM sticker_items ORDER BY rank ASC, id ASC")
    fun observeItems(): Flow<List<StickerItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<StickerItemEntity>): List<Long>

    @Query("DELETE FROM sticker_items")
    suspend fun clearItems(): Int
}
