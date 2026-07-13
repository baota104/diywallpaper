package com.example.diywallpaper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.diywallpaper.data.local.entity.UserDesignEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDesignDao {
    @Query(
        "SELECT * FROM user_designs " +
            "WHERE isDeleted = 0 " +
            "ORDER BY updatedAt DESC, createdAt DESC, id DESC"
    )
    fun observeActiveDesigns(): Flow<List<UserDesignEntity>>

    @Query("SELECT * FROM user_designs WHERE id = :designId LIMIT 1")
    suspend fun getDesignById(designId: String): UserDesignEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDesign(entity: UserDesignEntity): Long

    @Query(
        "UPDATE user_designs SET " +
            "title = :title, " +
            "updatedAt = :updatedAt " +
            "WHERE id = :designId"
    )
    suspend fun renameDesign(
        designId: String,
        title: String,
        updatedAt: Long
    ): Int

    @Query(
        "UPDATE user_designs SET " +
            "title = :title, " +
            "thumbnailPath = :thumbnailPath, " +
            "previewPath = :previewPath, " +
            "exportedImagePath = :exportedImagePath, " +
            "updatedAt = :updatedAt, " +
            "lastOpenedAt = :lastOpenedAt, " +
            "canvasWidth = :canvasWidth, " +
            "canvasHeight = :canvasHeight, " +
            "schemaVersion = :schemaVersion " +
            "WHERE id = :designId"
    )
    suspend fun updateDesignSnapshot(
        designId: String,
        title: String?,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?,
        updatedAt: Long,
        lastOpenedAt: Long,
        canvasWidth: Int,
        canvasHeight: Int,
        schemaVersion: Int
    ): Int

    @Query(
        "UPDATE user_designs SET " +
            "thumbnailPath = :thumbnailPath, " +
            "previewPath = :previewPath, " +
            "exportedImagePath = :exportedImagePath, " +
            "updatedAt = :updatedAt " +
            "WHERE id = :designId"
    )
    suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?,
        updatedAt: Long
    ): Int

    @Query(
        "UPDATE user_designs SET " +
            "isDeleted = 1, " +
            "updatedAt = :updatedAt " +
            "WHERE id = :designId"
    )
    suspend fun markDeleted(
        designId: String,
        updatedAt: Long
    ): Int
}
