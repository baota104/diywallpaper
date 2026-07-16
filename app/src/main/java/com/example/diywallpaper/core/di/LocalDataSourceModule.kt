package com.example.diywallpaper.core.di

import com.example.diywallpaper.data.local.datasource.RawSpecialTextLocalDataSource
import com.example.diywallpaper.data.local.datasource.SpecialTextLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocalDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSpecialTextLocalDataSource(
        impl: RawSpecialTextLocalDataSource
    ): SpecialTextLocalDataSource
}
