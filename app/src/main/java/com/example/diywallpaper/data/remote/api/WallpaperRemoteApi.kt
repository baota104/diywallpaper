package com.example.diywallpaper.data.remote.api

import com.example.diywallpaper.data.remote.dto.BackgroundCreateDto
import com.example.diywallpaper.data.remote.dto.DiyTemplateDataDto
import com.example.diywallpaper.data.remote.dto.RemoteCategoryDto
import com.example.diywallpaper.data.remote.dto.StickerDto
import retrofit2.http.GET
import retrofit2.http.Url

interface WallpaperRemoteApi {
    @GET
    suspend fun getDataFull(@Url url: String): List<RemoteCategoryDto>

    @GET
    suspend fun getBackgroundCreate(@Url url: String): List<BackgroundCreateDto>

    @GET
    suspend fun getStickers(@Url url: String): List<StickerDto>

    @GET
    suspend fun getDiyTemplateData(@Url url: String): DiyTemplateDataDto

    @GET
    suspend fun getRawJson(@Url url: String): String
}
