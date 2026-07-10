package com.example.diywallpaper.data.remote.ndk

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeEndpointProvider @Inject constructor() : EndpointProvider {
    override fun getDataFullUrl(): String = runCatching {
        NativeConfig.getDataFullUrl()
    }.getOrDefault("https://cdn.leansoft-ai.com/ls36-diy-wallpaper/json/data_20260227_1339.json")

    override fun getBgCreateUrl(): String = runCatching {
        NativeConfig.getBgCreateUrl()
    }.getOrDefault("https://cdn.leansoft-ai.com/ls36-diy-wallpaper/data/bgcreate.json")

    override fun getStickersUrl(): String = runCatching {
        NativeConfig.getStickersUrl()
    }.getOrDefault("https://cdn.leansoft-ai.com/ls36-diy-wallpaper/data/stickers.json")
}
