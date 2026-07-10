package com.example.diywallpaper.domain.model

sealed interface DiyBackgroundValue {
    data class ColorHex(val value: String) : DiyBackgroundValue
    data class RemoteUrl(val url: String) : DiyBackgroundValue
    data class AssetUrl(val url: String) : DiyBackgroundValue
    data object Empty : DiyBackgroundValue
}
