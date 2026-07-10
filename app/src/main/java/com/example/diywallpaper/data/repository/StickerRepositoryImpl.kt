package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.StickerDao
import com.example.diywallpaper.data.local.datastore.SyncPreferencesDataStore
import com.example.diywallpaper.data.local.entity.StickerItemEntity
import com.example.diywallpaper.data.remote.api.WallpaperRemoteApi
import com.example.diywallpaper.data.remote.dto.toDomainOrNull
import com.example.diywallpaper.data.remote.ndk.EndpointProvider
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.repository.StickerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StickerRepositoryImpl @Inject constructor(
    private val wallpaperRemoteApi: WallpaperRemoteApi,
    private val endpointProvider: EndpointProvider,
    private val stickerDao: StickerDao,
    private val syncPreferencesDataStore: SyncPreferencesDataStore
) : StickerRepository {

    override fun observeStickers(): Flow<List<StickerItem>> {
        return stickerDao.observeItems().map { items ->
            items.map { entity ->
                StickerItem(
                    id = entity.id,
                    rank = entity.rank,
                    stickerUrl = entity.stickerUrl,
                    thumbnailUrl = entity.thumbnailUrl,
                    isAnimated = entity.isAnimated
                )
            }
        }
    }

    override suspend fun refreshStickers(): AppResult<Unit> {
        if (!isStale(syncPreferencesDataStore.lastSyncedAtValue())) return AppResult.Success(Unit)

        return runCatching {
            val items = wallpaperRemoteApi
                .getStickers(endpointProvider.getStickersUrl())
                .mapNotNull { it.toDomainOrNull() }
            val entities = items.map { item ->
                StickerItemEntity(
                    id = item.id,
                    rank = item.rank,
                    stickerUrl = item.stickerUrl,
                    thumbnailUrl = item.thumbnailUrl,
                    localPath = null,
                    isAnimated = item.isAnimated,
                    rawJson = null
                )
            }
            stickerDao.clearItems()
            stickerDao.insertItems(entities)
            syncPreferencesDataStore.updateLastSyncedAt(System.currentTimeMillis())
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.toAppError("stickers")) }
        )
    }
}
