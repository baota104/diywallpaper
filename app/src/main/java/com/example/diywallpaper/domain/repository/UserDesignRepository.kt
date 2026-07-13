package com.example.diywallpaper.domain.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.UserDesign
import kotlinx.coroutines.flow.Flow

interface UserDesignRepository {
    fun observeDesigns(): Flow<List<UserDesign>>

    suspend fun getDesign(designId: String): AppResult<UserDesign>

    suspend fun createDraft(project: EditorProject, title: String? = null): AppResult<String>

    suspend fun getProject(designId: String): AppResult<EditorProject>

    suspend fun saveProject(project: EditorProject, title: String? = null): AppResult<Unit>

    suspend fun renameDesign(designId: String, title: String): AppResult<Unit>

    suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?
    ): AppResult<Unit>

    suspend fun deleteDesign(designId: String): AppResult<Unit>
}
