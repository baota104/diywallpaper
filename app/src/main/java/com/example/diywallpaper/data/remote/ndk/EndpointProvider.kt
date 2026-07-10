package com.example.diywallpaper.data.remote.ndk

interface EndpointProvider {
    fun getDataFullUrl(): String
    fun getBgCreateUrl(): String
    fun getStickersUrl(): String
}
