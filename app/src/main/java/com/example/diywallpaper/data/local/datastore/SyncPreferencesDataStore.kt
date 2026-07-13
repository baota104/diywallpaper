package com.example.diywallpaper.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncPreferencesDataStore by preferencesDataStore(name = "sync_preferences")

@Singleton
class SyncPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val LAST_SYNCED_AT_WALLPAPERS = longPreferencesKey("last_synced_at_wallpapers")
        val LAST_SYNCED_AT_BACKGROUNDS = longPreferencesKey("last_synced_at_backgrounds")
        val LAST_SYNCED_AT_STICKERS = longPreferencesKey("last_synced_at_stickers")
    }

    val wallpaperLastSyncedAt: Flow<Long> = context.syncPreferencesDataStore.data
        .map { preferences: Preferences -> preferences[LAST_SYNCED_AT_WALLPAPERS] ?: 0L }

    val backgroundLastSyncedAt: Flow<Long> = context.syncPreferencesDataStore.data
        .map { preferences: Preferences -> preferences[LAST_SYNCED_AT_BACKGROUNDS] ?: 0L }

    val stickerLastSyncedAt: Flow<Long> = context.syncPreferencesDataStore.data
        .map { preferences: Preferences -> preferences[LAST_SYNCED_AT_STICKERS] ?: 0L }

    suspend fun updateWallpaperLastSyncedAt(value: Long) {
        context.syncPreferencesDataStore.edit { preferences ->
            preferences[LAST_SYNCED_AT_WALLPAPERS] = value
        }
    }

    suspend fun updateBackgroundLastSyncedAt(value: Long) {
        context.syncPreferencesDataStore.edit { preferences ->
            preferences[LAST_SYNCED_AT_BACKGROUNDS] = value
        }
    }

    suspend fun updateStickerLastSyncedAt(value: Long) {
        context.syncPreferencesDataStore.edit { preferences ->
            preferences[LAST_SYNCED_AT_STICKERS] = value
        }
    }

    suspend fun wallpaperLastSyncedAtValue(): Long = wallpaperLastSyncedAt.first()

    suspend fun backgroundLastSyncedAtValue(): Long = backgroundLastSyncedAt.first()

    suspend fun stickerLastSyncedAtValue(): Long = stickerLastSyncedAt.first()
}
