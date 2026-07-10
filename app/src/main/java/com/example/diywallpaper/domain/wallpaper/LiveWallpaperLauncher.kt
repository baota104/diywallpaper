package com.example.diywallpaper.domain.wallpaper

import android.content.Intent
import com.example.diywallpaper.core.result.AppResult

interface LiveWallpaperLauncher {
    fun createLaunchIntent(): AppResult<Intent>
}
