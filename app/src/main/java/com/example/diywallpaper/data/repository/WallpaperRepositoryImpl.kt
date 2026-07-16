package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.DiyTemplateDao
import com.example.diywallpaper.data.local.dao.WallpaperDao
import com.example.diywallpaper.data.local.datastore.SyncPreferencesDataStore
import com.example.diywallpaper.data.local.entity.DiyTemplateEntity
import com.example.diywallpaper.data.local.entity.WallpaperCategoryEntity
import com.example.diywallpaper.data.local.entity.WallpaperItemEntity
import com.example.diywallpaper.data.remote.api.WallpaperRemoteApi
import com.example.diywallpaper.data.remote.dto.toDiyTemplates
import com.example.diywallpaper.data.remote.dto.toWallpaperCategoryOrNull
import com.example.diywallpaper.data.remote.ndk.EndpointProvider
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class WallpaperRepositoryImpl @Inject constructor(
    private val wallpaperRemoteApi: WallpaperRemoteApi,
    private val endpointProvider: EndpointProvider,
    private val wallpaperDao: WallpaperDao,
    private val diyTemplateDao: DiyTemplateDao,
    private val syncPreferencesDataStore: SyncPreferencesDataStore,
    private val json: Json
) : WallpaperRepository {

    override fun observeWallpaperCategories(): Flow<List<WallpaperCategory>> {
        return combine(
            wallpaperDao.observeCategories(),
            wallpaperDao.observeItems()
        ) { categories, items ->
            categories.map { category ->
                category.toDomain(items.filter { it.categoryId == category.id })
            }
        }
    }

    override suspend fun refreshWallpaperCategories(): AppResult<Unit> {
        val shouldRefresh =
            wallpaperDao.getCategoryCount() == 0 || isStale(syncPreferencesDataStore.wallpaperLastSyncedAtValue())
        if (!shouldRefresh) return AppResult.Success(Unit)

        return runCatching {
            val wallpaperFavoriteFlags = wallpaperDao.getFavoriteFlags()
                .associate { it.id to it.isFavorite }
            val diyFavoriteFlags = diyTemplateDao.getFavoriteFlags()
                .associate { it.id to it.isFavorite }
            val remoteCategories = wallpaperRemoteApi.getDataFull(endpointProvider.getDataFullUrl())
            val categoryEntities = mutableListOf<WallpaperCategoryEntity>()
            val wallpaperEntities = mutableListOf<WallpaperItemEntity>()
            val diyEntities = mutableListOf<DiyTemplateEntity>()
            val now = System.currentTimeMillis()

            remoteCategories.forEach { categoryDto ->
                categoryDto.toWallpaperCategoryOrNull()?.let { category ->
                    categoryEntities += WallpaperCategoryEntity(
                        id = category.id,
                        name = category.name,
                        iconUrl = category.iconUrl,
                        rank = category.rank,
                        rawJson = json.encodeToString(categoryDto)
                    )

                    wallpaperEntities += category.items.map { item ->
                        WallpaperItemEntity(
                            id = item.id,
                            categoryId = item.categoryId,
                            type = item.type.toStorageValue(),
                            rank = item.rank,
                            thumbUrl = item.thumbUrl,
                            imageUrl = item.imageUrl,
                            videoUrl = item.videoUrl,
                            isFavorite = wallpaperFavoriteFlags[item.id] ?: item.isFavorite,
                            localPath = null,
                            rawJson = null,
                            createdAt = now,
                            updatedAt = now
                        )
                    }
                }

                diyEntities += categoryDto.toDiyTemplates().map { template ->
                    DiyTemplateEntity(
                        id = template.id,
                        type = template.type.name,
                        rank = template.rank,
                        thumbUrl = template.thumbUrl,
                        diyDataUrl = template.diyDataUrl,
                        dataZipUrl = template.dataZipUrl,
                        diyAnimationUrl = template.diyAnimationUrl,
                        isFavorite = diyFavoriteFlags[template.id] ?: template.isFavorite,
                        diyDataLocalPath = null,
                        diyAnimationLocalPath = null,
                        rawJson = null,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }

            wallpaperDao.clearItems()
            wallpaperDao.clearCategories()
            wallpaperDao.insertCategories(categoryEntities)
            wallpaperDao.insertItems(wallpaperEntities)

            diyTemplateDao.clearTemplates()
            diyTemplateDao.insertTemplates(diyEntities)

            syncPreferencesDataStore.updateWallpaperLastSyncedAt(now)
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.toAppError("data_url_full")) }
        )
    }

    override suspend fun toggleFavorite(itemId: String): AppResult<Unit> {
        return runCatching {
            wallpaperDao.toggleFavorite(
                itemId = itemId,
                updatedAt = System.currentTimeMillis()
            )
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.toAppError("wallpaper_toggle_favorite")) }
        )
    }
}
