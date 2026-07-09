package com.example.diywallpaper.core.utils.manager

import android.app.Activity
import com.example.diywallpaper.core.constant.Constant.N_FS_12
import com.example.diywallpaper.core.constant.Constant.N_FS_23
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.checkNativeLoaded
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.loadNativeAlternate

object OnboardAdManager {

    enum class FullscreenPreloadState {
        IDLE,
        LOADING,
        SUCCESS,
        FAILED
    }

    var fs12LoadedAdId: String? = null
    var fs12PreloadState = FullscreenPreloadState.IDLE
    private var fs12PreloadSessionId = 0
    private var isFs12PreloadStarted = false

    var isFs12Shown = false

    var fs23LoadedAdId: String? = null
    var isFs23Shown = false
    private var isFs23PreloadStarted = false

    var fallbackOb1NativeAdId: String? = null
    var isOb1NativeActuallyShown = false

    fun reset() {
        fs12LoadedAdId = null
        fs12PreloadState = FullscreenPreloadState.IDLE
        fs12PreloadSessionId++
        isFs12PreloadStarted = false
        isFs12Shown = false

        fs23LoadedAdId = null
        isFs23Shown = false
        isFs23PreloadStarted = false

        fallbackOb1NativeAdId = null
        isOb1NativeActuallyShown = false
    }

    fun startFs12Preload(): Int {
        isFs12PreloadStarted = true
        fs12PreloadState = FullscreenPreloadState.LOADING
        return fs12PreloadSessionId
    }

    fun completeFs12Preload(sessionId: Int, loadedId: String?) {
        if (sessionId != fs12PreloadSessionId) return
        fs12LoadedAdId = loadedId
        fs12PreloadState = if (loadedId != null) FullscreenPreloadState.SUCCESS else FullscreenPreloadState.FAILED
    }

    fun ignoreFs12Preload() {
        fs12PreloadState = FullscreenPreloadState.FAILED
    }

    fun preloadFs12(activity: Activity) {
        if (isFs12PreloadStarted) return
        val sessionId = startFs12Preload()

        activity.loadNativeAlternate(
            idAds1 = N_FS_12,
            isFullScreen = true,
            loadComplete = { success ->
                val loadedId = if (success) {
                    when {
                        checkNativeLoaded(N_FS_12) -> N_FS_12
                        else -> null
                    }
                } else null
                completeFs12Preload(sessionId, loadedId)
            }
        )
    }

    fun preloadFs23(activity: Activity) {
        if (isFs23PreloadStarted) return
        isFs23PreloadStarted = true
        activity.loadNativeAlternate(
            idAds1 = N_FS_23,
            isFullScreen = true,
            loadComplete = { success ->
                fs23LoadedAdId = if (success && checkNativeLoaded(N_FS_23)) N_FS_23 else null
            }
        )
    }
}
