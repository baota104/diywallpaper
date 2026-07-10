package com.example.diywallpaper.data.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLiveWallpaperLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) : LiveWallpaperLauncher {
    override fun createLaunchIntent(): AppResult<Intent> {
        val serviceComponent = ComponentName(context, VideoLiveWallpaperService::class.java)
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, serviceComponent)
            }
        } else {
            Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return AppResult.Success(intent)
    }
}
