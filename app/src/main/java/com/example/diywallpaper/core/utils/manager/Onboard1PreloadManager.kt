package com.example.diywallpaper.core.utils.manager

import android.app.Activity
import com.example.diywallpaper.core.constant.Constant.N_ONBOARD1_A
import com.example.diywallpaper.core.constant.Constant.N_ONBOARD1_M
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.checkNativeLoaded
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.loadNativeAlternate

object OnboardOb1AdManager {

    enum class PreloadState {
        IDLE,
        LOADING,
        SUCCESS,
        FAILED
    }

    private var preloadStarted = false
    private var preloadCompleted = false
    private var preloadedAdId: String? = null
    private var preloadSessionId = 0
    private var preloadState = PreloadState.IDLE

    fun resetForApplyingLanguage() {
        preloadStarted = false
        preloadCompleted = false
        preloadedAdId = null
        preloadSessionId++
        preloadState = PreloadState.IDLE
    }

    fun preloadForOb1(activity: Activity) {
        if (preloadStarted) return
        val sessionId = preloadSessionId
        preloadStarted = true
        preloadCompleted = false
        preloadedAdId = null
        preloadState = PreloadState.LOADING

        activity.loadNativeAlternate(
            idAds1 = N_ONBOARD1_M,
            idAds2 = N_ONBOARD1_A,
            loadComplete = { success ->
                if (sessionId != preloadSessionId) return@loadNativeAlternate
                preloadedAdId = when {
                    success && checkNativeLoaded(N_ONBOARD1_M) -> N_ONBOARD1_M
                    success && checkNativeLoaded(N_ONBOARD1_A) -> N_ONBOARD1_A
                    else -> null
                }
                preloadCompleted = true
                preloadState = if (preloadedAdId != null) PreloadState.SUCCESS else PreloadState.FAILED
            }
        )
    }

    fun getPreloadedAdId(): String? = preloadedAdId

    fun hasCompletedPreload(): Boolean = preloadCompleted

    fun getPreloadState(): PreloadState = preloadState

    fun consumePreloadedAdId(): String? {
        val adId = preloadedAdId
        preloadedAdId = null
        return adId
    }
}
