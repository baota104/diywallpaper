package com.example.diywallpaper.data.local.files

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject

interface DesignFileStore {
    fun projectFilePath(designId: String): String

    fun thumbnailFilePath(designId: String): String

    fun previewFilePath(designId: String): String

    fun exportedImageFilePath(designId: String): String

    suspend fun writeProject(project: EditorProject): AppResult<String>

    suspend fun readProject(projectFilePath: String): AppResult<EditorProject>

    suspend fun writeBinaryAsset(filePath: String, data: ByteArray): AppResult<String>

    suspend fun deleteDesignFiles(designId: String): AppResult<Unit>
}
