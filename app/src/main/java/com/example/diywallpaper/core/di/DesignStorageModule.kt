package com.example.diywallpaper.core.di

import android.content.Context
import com.example.diywallpaper.data.local.files.AndroidDesignAssetExporter
import com.example.diywallpaper.data.local.files.DesignFileStore
import com.example.diywallpaper.data.local.files.JsonDesignFileStore
import com.example.diywallpaper.domain.repository.DesignAssetExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object DesignStorageModule {
    @Provides
    @Singleton
    fun provideDesignsDirectory(@ApplicationContext context: Context): File {
        return File(context.filesDir, "designs")
    }

    @Provides
    @Singleton
    fun provideDesignFileStore(
        designsDirectory: File,
        json: Json
    ): DesignFileStore {
        return JsonDesignFileStore(
            baseDirectory = designsDirectory,
            json = json
        )
    }

    @Provides
    @Singleton
    fun provideDesignAssetExporter(
        @ApplicationContext context: Context,
        designFileStore: DesignFileStore,
        okHttpClient: OkHttpClient
    ): DesignAssetExporter {
        return AndroidDesignAssetExporter(context, designFileStore, okHttpClient)
    }
}
