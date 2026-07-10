package com.example.diywallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diywallpaper.data.local.entity.BackgroundCreateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackgroundCreateDao {
    @Query("SELECT * FROM background_create_items ORDER BY rank ASC, id ASC")
    fun observeItems(): Flow<List<BackgroundCreateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<BackgroundCreateEntity>): List<Long>

    @Query("DELETE FROM background_create_items")
    suspend fun clearItems(): Int
}
