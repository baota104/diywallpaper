package com.example.diywallpaper.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import com.example.diywallpaper.domain.wallpaper.SystemWallpaperManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidSystemWallpaperManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemWallpaperManager {
    override suspend fun setStaticWallpaper(
        file: File,
        target: WallpaperTarget
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: throw IllegalStateException("BITMAP_DECODE_FAILED")

            val wallpaperManager = WallpaperManager.getInstance(context)
            when (target) {
                WallpaperTarget.HOME -> {
                    setBitmapCompat(wallpaperManager, bitmap, WallpaperManager.FLAG_SYSTEM)
                }

                WallpaperTarget.LOCK -> {
                    setBitmapCompat(wallpaperManager, bitmap, WallpaperManager.FLAG_LOCK)
                }

                WallpaperTarget.BOTH -> {
                    setBitmapCompat(wallpaperManager, bitmap, WallpaperManager.FLAG_SYSTEM)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setBitmapCompat(wallpaperManager, bitmap, WallpaperManager.FLAG_LOCK)
                    }
                }
            }
        }.fold(
            onSuccess = { AppResult.Success(Unit) },
            onFailure = {
                AppResult.Error(
                    AppError.StorageError(reason = it.message)
                )
            }
        )
    }

    private fun setBitmapCompat(
        wallpaperManager: WallpaperManager,
        bitmap: android.graphics.Bitmap,
        flag: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(bitmap, null, true, flag)
        } else {
            wallpaperManager.setBitmap(bitmap)
        }
    }
}
