package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.DiyTemplateDao
import com.example.diywallpaper.data.local.dao.WallpaperDao
import com.example.diywallpaper.data.local.files.DiyTemplateAssetCache
import com.example.diywallpaper.data.remote.dto.DiyTemplateDataDto
import com.example.diywallpaper.data.remote.api.WallpaperRemoteApi
import com.example.diywallpaper.data.remote.dto.toDomain
import com.example.diywallpaper.domain.model.DiyElementType
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.repository.DiyRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Singleton
class DiyRepositoryImpl @Inject constructor(
    private val wallpaperRemoteApi: WallpaperRemoteApi,
    private val wallpaperDao: WallpaperDao,
    private val diyTemplateDao: DiyTemplateDao,
    private val diyTemplateAssetCache: DiyTemplateAssetCache,
    private val json: Json
) : DiyRepository {

    override fun observeDiyTemplates(): Flow<List<DiyTemplate>> {
        return diyTemplateDao.observeTemplates().map { templates ->
            templates.map { entity ->
                DiyTemplate(
                    id = entity.id,
                    type = runCatching { DiyTemplateType.valueOf(entity.type) }
                        .getOrDefault(DiyTemplateType.DIY_STATIC),
                    rank = entity.rank,
                    thumbUrl = entity.thumbUrl,
                    diyDataUrl = entity.diyDataUrl,
                    dataZipUrl = entity.dataZipUrl,
                    diyAnimationUrl = entity.diyAnimationUrl,
                    isFavorite = entity.isFavorite
                )
            }
        }
    }

    override suspend fun refreshDiyTemplates(): AppResult<Unit> {
        val currentCount = wallpaperDao.getCategoryCount()
        return if (currentCount == 0) {
            AppResult.Error(IllegalStateException("Wallpaper metadata must be synced first").toAppError("diy_templates"))
        } else {
            AppResult.Success(Unit)
        }
    }

    override suspend fun toggleFavorite(templateId: String): AppResult<Unit> {
        return runCatching {
            diyTemplateDao.toggleFavorite(
                templateId = templateId,
                updatedAt = System.currentTimeMillis()
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.toAppError("diy_toggle_favorite")) }
        )
    }

    override suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String?
    ): AppResult<DiyTemplateData> {
        return runCatching {
            val cacheResult = diyTemplateAssetCache.preload(
                templateId = templateId,
                diyDataUrl = diyDataUrl,
                dataZipUrl = dataZipUrl
            )
            val dto = json.decodeFromString<DiyTemplateDataDto>(
                cacheResult.dataJsonFile.readText()
            )
            dto.toDomain(diyDataUrl)
                .withLocalAssets(cacheResult.assetDirectory)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(it.toAppError("diy_data")) }
        )
    }

    override suspend fun getDiyAnimationRaw(
        templateId: String,
        animationUrl: String
    ): AppResult<DiyAnimationRaw> {
        return runCatching {
            DiyAnimationRaw(
                templateId = templateId,
                animationUrl = animationUrl,
                rawJson = wallpaperRemoteApi.getRawJson(animationUrl)
            )
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(it.toAppError("diy_animation")) }
        )
    }
}

private fun DiyTemplateData.withLocalAssets(assetDirectory: File?): DiyTemplateData {
    if (assetDirectory == null || !assetDirectory.exists()) return this

    return copy(
        background = background.toLocalBackground(assetDirectory),
        elements = elements.map { element ->
            val localAssetUrl = if (
                element.type != DiyElementType.IMAGE &&
                element.type != DiyElementType.TEXT &&
                element.srcName.isNotBlank()
            ) {
                assetDirectory.findAsset(element.srcName)?.let { localAsset ->
                    localAsset.absolutePath
                }
            } else {
                null
            }
            val localMaskUrl = element.maskName
                .takeIf { it.isNotBlank() }
                ?.let { assetDirectory.findAsset(it)?.absolutePath }
            element.copy(
                assetUrl = localAssetUrl ?: element.assetUrl,
                maskUrl = localMaskUrl ?: element.maskUrl
            )
        },
        placeholders = placeholders.map { placeholder ->
            val localMaskUrl = placeholder.maskName
                .takeIf { it.isNotBlank() }
                ?.let { assetDirectory.findAsset(it)?.absolutePath }
            val localPreviewUrl = placeholder.previewName
                .takeIf { it.isNotBlank() }
                ?.let { assetDirectory.findAsset(it)?.absolutePath }
            placeholder.copy(
                maskUrl = localMaskUrl ?: placeholder.maskUrl,
                previewUrl = localPreviewUrl ?: placeholder.previewUrl
            )
        }
    )
}

private fun String.toLocalBackground(assetDirectory: File): String {
    val value = trim()
    if (value.isBlank() || value.startsWith("#")) return this
    if (value.startsWith("http://") || value.startsWith("https://")) return this
    return assetDirectory.findAsset(value)?.absolutePath ?: this
}

private fun File.findAsset(relativePath: String): File? {
    val root = canonicalFile
    val direct = File(this, relativePath).canonicalFile
        .takeIf { it.path.startsWith(root.path + File.separator) && it.exists() }
    if (direct != null) return direct

    val fileName = relativePath.substringAfterLast("/")
        .substringAfterLast("\\")
        .takeIf { it.isNotBlank() }
        ?: return null
    return walkTopDown()
        .firstOrNull { it.isFile && it.name == fileName }
}
