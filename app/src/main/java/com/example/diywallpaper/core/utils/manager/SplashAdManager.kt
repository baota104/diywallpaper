package com.example.diywallpaper.core.utils.manager

object SplashAdManager {
    var shouldShowNativeFullAfterInter: Boolean = false
    var fallbackAdId: String? = null

    fun resetSplashFlow() {
        shouldShowNativeFullAfterInter = false
        fallbackAdId = null
    }
}
object LanguageNativeAdManager {
    var fallbackNativeAdId: String? = null
}
