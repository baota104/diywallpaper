package com.example.diywallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diywallpaper.data.local.entity.DiyTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiyTemplateDao {
    @Query("SELECT * FROM diy_templates ORDER BY rank ASC, id ASC")
    fun observeTemplates(): Flow<List<DiyTemplateEntity>>

    @Query("SELECT id, isFavorite FROM diy_templates")
    suspend fun getFavoriteFlags(): List<DiyFavoriteFlag>

    @Query("UPDATE diy_templates SET isFavorite = NOT isFavorite, updatedAt = :updatedAt WHERE id = :templateId")
    suspend fun toggleFavorite(templateId: String, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<DiyTemplateEntity>): List<Long>

    @Query("DELETE FROM diy_templates")
    suspend fun clearTemplates(): Int
}

data class DiyFavoriteFlag(
    val id: String,
    val isFavorite: Boolean
)
