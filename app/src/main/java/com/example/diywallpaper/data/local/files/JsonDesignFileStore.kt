package com.example.diywallpaper.data.local.files

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorProject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Singleton
class JsonDesignFileStore @Inject constructor(
    private val baseDirectory: File,
    private val json: Json
) : DesignFileStore {

    override fun projectFilePath(designId: String): String {
        return File(resolveDesignDirectory(designId), PROJECT_FILE_NAME).absolutePath
    }

    override fun thumbnailFilePath(designId: String): String {
        return File(resolveDesignDirectory(designId), THUMBNAIL_FILE_NAME).absolutePath
    }

    override fun previewFilePath(designId: String): String {
        return File(resolveDesignDirectory(designId), PREVIEW_FILE_NAME).absolutePath
    }

    override fun exportedImageFilePath(designId: String): String {
        return File(resolveDesignDirectory(designId), EXPORTED_FILE_NAME).absolutePath
    }

    override suspend fun writeProject(project: EditorProject): AppResult<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val projectFile = File(projectFilePath(project.id))
                projectFile.parentFile?.mkdirs()
                projectFile.writeText(json.encodeToString(EditorProject.serializer(), project))
                projectFile.absolutePath
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
            )
        }
    }

    override suspend fun readProject(projectFilePath: String): AppResult<EditorProject> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val projectFile = File(projectFilePath)
                val content = projectFile.readText()
                json.decodeFromString(EditorProject.serializer(), content)
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = {
                    val error = if (it is SerializationException) {
                        AppError.JsonParseError(source = "editor_project", reason = it.message)
                    } else {
                        AppError.StorageError(it.message)
                    }
                    AppResult.Error(error)
                }
            )
        }
    }

    override suspend fun writeBinaryAsset(filePath: String, data: ByteArray): AppResult<String> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val assetFile = File(filePath)
                assetFile.parentFile?.mkdirs()
                assetFile.writeBytes(data)
                assetFile.absolutePath
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
            )
        }
    }

    override suspend fun deleteDesignFiles(designId: String): AppResult<Unit> {
        return withContext(Dispatchers.IO) {
            runCatching {
                resolveDesignDirectory(designId).takeIf(File::exists)?.deleteRecursively()
            }.fold(
                onSuccess = { AppResult.Success(Unit) },
                onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
            )
        }
    }

    private fun resolveDesignDirectory(designId: String): File {
        return File(baseDirectory, designId)
    }

    private companion object {
        const val PROJECT_FILE_NAME = "project.json"
        const val THUMBNAIL_FILE_NAME = "thumbnail.webp"
        const val PREVIEW_FILE_NAME = "preview.webp"
        const val EXPORTED_FILE_NAME = "exported.png"
    }
}
