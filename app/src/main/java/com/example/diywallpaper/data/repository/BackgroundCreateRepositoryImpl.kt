package com.example.diywallpaper.data.repository

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.BackgroundCreateDao
import com.example.diywallpaper.data.local.datastore.SyncPreferencesDataStore
import com.example.diywallpaper.data.local.entity.BackgroundCreateEntity
import com.example.diywallpaper.data.remote.api.WallpaperRemoteApi
import com.example.diywallpaper.data.remote.dto.toDomainOrNull
import com.example.diywallpaper.data.remote.ndk.EndpointProvider
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class BackgroundCreateRepositoryImpl @Inject constructor(
    private val wallpaperRemoteApi: WallpaperRemoteApi,
    private val endpointProvider: EndpointProvider,
    private val backgroundCreateDao: BackgroundCreateDao,
    private val syncPreferencesDataStore: SyncPreferencesDataStore
) : BackgroundCreateRepository {

    override fun observeBackgrounds(): Flow<List<BackgroundCreateItem>> {
        return backgroundCreateDao.observeItems().map { items ->
            items.map { entity ->
                BackgroundCreateItem(
                    id = entity.id,
                    rank = entity.rank,
                    name = entity.name,
                    imageUrl = entity.imageUrl,
                    thumbnailUrl = entity.thumbnailUrl
                )
            }
        }
    }

    override suspend fun refreshBackgrounds(): AppResult<Unit> {
        if (!isStale(syncPreferencesDataStore.lastSyncedAtValue())) return AppResult.Success(Unit)

        return runCatching {
            val items = wallpaperRemoteApi
                .getBackgroundCreate(endpointProvider.getBgCreateUrl())
                .mapNotNull { it.toDomainOrNull() }
            val entities = items.map { item ->
                BackgroundCreateEntity(
                    id = item.id,
                    rank = item.rank,
                    name = item.name,
                    imageUrl = item.imageUrl,
                    thumbnailUrl = item.thumbnailUrl,
                    localPath = null,
                    rawJson = null
                )
            }
            backgroundCreateDao.clearItems()
            backgroundCreateDao.insertItems(entities)
            syncPreferencesDataStore.updateLastSyncedAt(System.currentTimeMillis())
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(it.toAppError("bgcreate")) }
        )
    }
}
