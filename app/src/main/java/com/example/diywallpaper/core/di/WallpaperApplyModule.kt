package com.example.diywallpaper.core.di

import com.example.diywallpaper.data.wallpaper.AndroidSystemWallpaperManager
import com.example.diywallpaper.data.wallpaper.AndroidLiveWallpaperLauncher
import com.example.diywallpaper.data.wallpaper.OkHttpWallpaperAssetResolver
import com.example.diywallpaper.data.wallpaper.SharedPrefsLiveWallpaperSourceStore
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperLauncher
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperSourceStore
import com.example.diywallpaper.domain.wallpaper.SystemWallpaperManager
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WallpaperApplyModule {
    @Binds
    @Singleton
    abstract fun bindWallpaperAssetResolver(
        resolver: OkHttpWallpaperAssetResolver
    ): WallpaperAssetResolver

    @Binds
    @Singleton
    abstract fun bindSystemWallpaperManager(
        manager: AndroidSystemWallpaperManager
    ): SystemWallpaperManager

    @Binds
    @Singleton
    abstract fun bindLiveWallpaperSourceStore(
        store: SharedPrefsLiveWallpaperSourceStore
    ): LiveWallpaperSourceStore

    @Binds
    @Singleton
    abstract fun bindLiveWallpaperLauncher(
        launcher: AndroidLiveWallpaperLauncher
    ): LiveWallpaperLauncher
}
