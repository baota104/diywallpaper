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
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }

    val lastSyncedAt: Flow<Long> = context.syncPreferencesDataStore.data
        .map { preferences: Preferences -> preferences[LAST_SYNCED_AT] ?: 0L }

    suspend fun updateLastSyncedAt(value: Long) {
        context.syncPreferencesDataStore.edit { preferences ->
            preferences[LAST_SYNCED_AT] = value
        }
    }

    suspend fun lastSyncedAtValue(): Long = lastSyncedAt.first()
}
