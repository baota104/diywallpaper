package com.example.diywallpaper.core.di

import com.example.diywallpaper.data.remote.ndk.EndpointProvider
import com.example.diywallpaper.data.remote.ndk.NativeEndpointProvider
import com.example.diywallpaper.data.repository.BackgroundCreateRepositoryImpl
import com.example.diywallpaper.data.repository.DiyRepositoryImpl
import com.example.diywallpaper.data.repository.StickerRepositoryImpl
import com.example.diywallpaper.data.repository.WallpaperRepositoryImpl
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.StickerRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEndpointProvider(provider: NativeEndpointProvider): EndpointProvider

    @Binds
    @Singleton
    abstract fun bindWallpaperRepository(repository: WallpaperRepositoryImpl): WallpaperRepository

    @Binds
    @Singleton
    abstract fun bindDiyRepository(repository: DiyRepositoryImpl): DiyRepository

    @Binds
    @Singleton
    abstract fun bindBackgroundCreateRepository(repository: BackgroundCreateRepositoryImpl): BackgroundCreateRepository

    @Binds
    @Singleton
    abstract fun bindStickerRepository(repository: StickerRepositoryImpl): StickerRepository
}
