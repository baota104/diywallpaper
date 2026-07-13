package com.example.diywallpaper.core.di

import android.content.Context
import androidx.room.Room
import com.example.diywallpaper.data.local.dao.BackgroundCreateDao
import com.example.diywallpaper.data.local.dao.DiyTemplateDao
import com.example.diywallpaper.data.local.dao.StickerDao
import com.example.diywallpaper.data.local.dao.UserDesignDao
import com.example.diywallpaper.data.local.dao.WallpaperDao
import com.example.diywallpaper.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "diy_wallpaper.db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideWallpaperDao(database: AppDatabase): WallpaperDao = database.wallpaperDao()

    @Provides
    fun provideDiyTemplateDao(database: AppDatabase): DiyTemplateDao = database.diyTemplateDao()

    @Provides
    fun provideBackgroundCreateDao(database: AppDatabase): BackgroundCreateDao =
        database.backgroundCreateDao()

    @Provides
    fun provideStickerDao(database: AppDatabase): StickerDao = database.stickerDao()

    @Provides
    fun provideUserDesignDao(database: AppDatabase): UserDesignDao = database.userDesignDao()
}
