package com.example.diywallpaper.core.utils.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.diywallpaper.core.utils.ad.MyAdsUtils

object ResumeAdNavigationManager {
    private val blockedRoutes = listOf(
        "splash",
        "language",
        "applying_language",
        "loading_advertisement"
    )

    private val _openLoadingAdEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val openLoadingAdEvents = _openLoadingAdEvents.asSharedFlow()

    @Volatile
    private var shouldShowResumeAdOnNextResume = false

    @Volatile
    private var isResumeAdFlowActive = false

    @Volatile
    private var currentRoute: String? = "splash"

    fun onAppWentToBackground() {
        shouldShowResumeAdOnNextResume = true
    }

    fun updateCurrentRoute(route: String?) {
        if (route != null) {
            currentRoute = route
        }
        else{
            currentRoute = "splash"
        }
    }

    fun tryOpenLoadingAdOnResume() {
        if (!shouldShowResumeAdOnNextResume) return
        if (isResumeAdFlowActive) return
        if (MyAdsUtils.isFullScreenAdShowing) return
//        if(currentRoute.contains("splash")) return
        for (blockedRoute in blockedRoutes) {
            if (currentRoute?.contains(blockedRoute) == true) return
        }
        shouldShowResumeAdOnNextResume = false
        isResumeAdFlowActive = true
        _openLoadingAdEvents.tryEmit(Unit)
    }

    fun markLoadingAdClosed() {
        isResumeAdFlowActive = false
        MyAdsUtils.isFullScreenAdShowing = false
    }
}
