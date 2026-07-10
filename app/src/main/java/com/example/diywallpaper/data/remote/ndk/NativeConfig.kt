package com.example.diywallpaper.data.remote.ndk

object NativeConfig {
    init {
        runCatching { System.loadLibrary("native_config") }
    }

    external fun getDataFullUrl(): String
    external fun getBgCreateUrl(): String
    external fun getStickersUrl(): String
}
