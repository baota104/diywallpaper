package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.DiyTemplateDao
import com.example.diywallpaper.data.local.dao.WallpaperDao
import com.example.diywallpaper.data.remote.api.WallpaperRemoteApi
import com.example.diywallpaper.data.remote.dto.toDomain
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.repository.DiyRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DiyRepositoryImpl @Inject constructor(
    private val wallpaperRemoteApi: WallpaperRemoteApi,
    private val wallpaperDao: WallpaperDao,
    private val diyTemplateDao: DiyTemplateDao
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
        diyDataUrl: String
    ): AppResult<DiyTemplateData> {
        return runCatching {
            wallpaperRemoteApi.getDiyTemplateData(diyDataUrl).toDomain(diyDataUrl)
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
