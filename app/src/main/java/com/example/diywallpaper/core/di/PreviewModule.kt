package com.example.diywallpaper.core.di

import com.example.diywallpaper.domain.usecase.preview.PreviewCarouselPlaybackPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreviewModule {
    @Provides
    @Singleton
    fun providePreviewCarouselPlaybackPolicy(): PreviewCarouselPlaybackPolicy {
        return PreviewCarouselPlaybackPolicy()
    }
}
