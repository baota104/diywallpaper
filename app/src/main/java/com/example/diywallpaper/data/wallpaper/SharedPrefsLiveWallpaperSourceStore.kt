package com.example.diywallpaper.data.wallpaper

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.core.utils.SharedPrefsHelper
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperSourceStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsLiveWallpaperSourceStore @Inject constructor(
    private val sharedPrefsHelper: SharedPrefsHelper
) : LiveWallpaperSourceStore {
    override suspend fun saveVideoPath(path: String): AppResult<Unit> {
        return runCatching {
            sharedPrefsHelper.liveWallpaperVideoPath = path
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = { AppResult.Error(AppError.StorageError(it.message)) }
        )
    }

    override fun getVideoPath(): String? = sharedPrefsHelper.liveWallpaperVideoPath
}
