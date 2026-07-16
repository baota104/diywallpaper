package com.example.diywallpaper.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.diywallpaper.data.local.dao.BackgroundCreateDao
import com.example.diywallpaper.data.local.dao.DiyTemplateDao
import com.example.diywallpaper.data.local.dao.StickerDao
import com.example.diywallpaper.data.local.dao.UserDesignDao
import com.example.diywallpaper.data.local.dao.WallpaperDao
import com.example.diywallpaper.data.local.entity.BackgroundCreateEntity
import com.example.diywallpaper.data.local.entity.DiyTemplateEntity
import com.example.diywallpaper.data.local.entity.StickerItemEntity
import com.example.diywallpaper.data.local.entity.UserDesignEntity
import com.example.diywallpaper.data.local.entity.WallpaperCategoryEntity
import com.example.diywallpaper.data.local.entity.WallpaperItemEntity

@Database(
    entities = [
        WallpaperCategoryEntity::class,
        WallpaperItemEntity::class,
        DiyTemplateEntity::class,
        BackgroundCreateEntity::class,
        StickerItemEntity::class,
        UserDesignEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wallpaperDao(): WallpaperDao
    abstract fun diyTemplateDao(): DiyTemplateDao
    abstract fun backgroundCreateDao(): BackgroundCreateDao
    abstract fun stickerDao(): StickerDao
    abstract fun userDesignDao(): UserDesignDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE wallpaper_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE diy_templates ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS user_designs (" +
                        "id TEXT NOT NULL, " +
                        "sourceType TEXT NOT NULL, " +
                        "title TEXT, " +
                        "thumbnailPath TEXT, " +
                        "previewPath TEXT, " +
                        "templateId TEXT, " +
                        "projectFilePath TEXT NOT NULL, " +
                        "canvasWidth INTEGER NOT NULL, " +
                        "canvasHeight INTEGER NOT NULL, " +
                        "exportedImagePath TEXT, " +
                        "createdAt INTEGER NOT NULL, " +
                        "updatedAt INTEGER NOT NULL, " +
                        "lastOpenedAt INTEGER NOT NULL, " +
                        "isDeleted INTEGER NOT NULL, " +
                        "schemaVersion INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE diy_templates ADD COLUMN dataZipUrl TEXT"
                )
            }
        }
    }
}
