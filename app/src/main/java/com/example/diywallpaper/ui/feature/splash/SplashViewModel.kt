package com.example.diywallpaper.ui.feature.splash

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.utils.SharedPrefsHelper

import com.example.diywallpaper.core.utils.trackings.Trackings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import javax.inject.Inject

private const val TAG = "SplashViewModel"

data class SplashLaunchState(
    val isFirstAppLaunch: Boolean,
    val isOnboardingCompleted: Boolean
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sharedPrefsHelper: SharedPrefsHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isSdkInitialized = MutableStateFlow(true)
    val isSdkInitialized = _isSdkInitialized.asStateFlow()

    fun consumeSplashLaunchState(): SplashLaunchState {
        val appOpenCount = sharedPrefsHelper.getEffectiveAppOpenCount()
        val launchState = SplashLaunchState(
            isFirstAppLaunch = appOpenCount == 0,
            isOnboardingCompleted = sharedPrefsHelper.getEffectiveOnboardingCompleted()
        )
        sharedPrefsHelper.appOpenCount = appOpenCount + 1
        return launchState
    }


//    fun startInitFlow(activity: Activity) {
//        viewModelScope.launch {
//            try {
//                withTimeoutOrNull(30_000L) {
//                    supervisorScope {
//                        val shouldLoadCmp = !sharedPrefsHelper.hasLoadedCmp
//                        val remoteConfigTask = async { fetchRemoteConfig() }
//                        val cmpTask = async {
//                            if (shouldLoadCmp) {
//                                withTimeoutOrNull(15_000L) {
//                                    loadCmpTask(activity)
//                                } ?: run {
//                                    Log.w(TAG, "CMP timeout after 15_000ms")
//                                    Trackings.logFirebaseTracking(TrackingEvents.splCmpLoad("fail"))
//                                    false
//                                }
//                            } else {
//                                Log.d(TAG, "Skip CMP because it was loaded before")
//                                true
//                            }
//                        }
//
//                        val isCmpLoaded = cmpTask.await()
//                        remoteConfigTask.await()
//
//                        if (isCmpLoaded) {
//                            if (shouldLoadCmp) {
//                                sharedPrefsHelper.hasLoadedCmp = true
//                            }
//                            initAdsTask()
//                        } else {
//                            Log.w(TAG, "Skip init ads because CMP did not load successfully")
//                        }
//                    }
//
//                    delay(500L)
//                    _isSdkInitialized.value = true
//                }
//            } catch (e: Exception) {
//                Trackings.logFirebaseTracking(TrackingEvents.splAdsLibInit("fail"))
//                Log.e(TAG, "Lỗi Init Splash", e)
//                _isSdkInitialized.value = true
//            }
//        }
//    }
//
//    private suspend fun initAdsTask() = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
//        var isResumed = false
//        Log.d(TAG, "init ad start")
//        Trackings.logFirebaseTracking(TrackingEvents.splAdsLibInit("start"))
//
//        try {
//            viewModelScope.launch {
//                withTimeoutOrNull(10000L) {
//                    com.google.ads.pro.manager.AdsManager.initAds(context) {
//                        Log.d(TAG, "init ad done")
//                        Trackings.logFirebaseTracking(TrackingEvents.splAdsLibInit("done"))
//                        if (!isResumed && cont.isActive) {
//                            Log.d(TAG, "init ad resume")
//                            isResumed = true
//                            runCatching { cont.resume(Unit) }
//                        }
//                    }
//                }
//                if (!isResumed && cont.isActive) {
//                    isResumed = true
//                    runCatching { cont.resume(Unit) }
//                }
//            }
//        } catch (e: Exception) {
//            Trackings.logFirebaseTracking(TrackingEvents.splAdsLibInit("fail"))
//            Log.e(TAG, "Missing AdsManager", e)
//            if (!isResumed && cont.isActive) {
//                isResumed = true
//                runCatching { cont.resume(Unit) }
//            }
//        }
//    }
//
//    private suspend fun fetchRemoteConfig(): Unit = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
//        var isResumed = false
//        runCatching {
//            com.proxglobal.ads.AdsUtils.observeLoadRemoteConfigAds(androidx.lifecycle.ProcessLifecycleOwner.get()) {
//                Trackings.logFirebaseTracking(TrackingEvents.splMConfigLoad("done"))
//                if (!isResumed && cont.isActive) {
//                    isResumed = true
//                    runCatching { cont.resume(Unit) }
//                }
//            }
//        }.onFailure {
//            Trackings.logFirebaseTracking(TrackingEvents.splMConfigLoad("fail"))
//            if (!isResumed && cont.isActive) {
//                isResumed = true
//                runCatching { cont.resume(Unit) }
//            }
//        }
//    }
//
//    @android.annotation.SuppressLint("HardwareIds")
//    private suspend fun loadCmpTask(activity: Activity): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
//        var isResumed = false
//        fun resumeTask(isSuccess: Boolean) {
//            if (!isResumed && cont.isActive) {
//                isResumed = true
//                runCatching { cont.resume(isSuccess) }
//            }
//        }
//
//        try {
//            val consentManager = com.consent.ConsentManager(activity).apply {
//                if (BuildConfig.DEBUG) {
//                    val deviceID = android.provider.Settings.Secure.getString(
//                        activity.contentResolver,
//                        android.provider.Settings.Secure.ANDROID_ID
//                    )
//                    addTestDeviceId(deviceID)
//                }
//            }
//
//            Trackings.logFirebaseTracking(TrackingEvents.splCmpLoad("start"))
//
//            consentManager.request(
//                context = activity,
//                onLoadSuccessCMP = { consentForm ->
//                    Log.d(TAG, "CMP Load success")
//                    Trackings.logFirebaseTracking(TrackingEvents.splCmpLoad("done"))
//
//                    if (consentForm != null) {
//                        consentManager.showCMP(consentForm) {
//                            Trackings.logFirebaseTracking(TrackingEvents.splCmpShow("done"))
//                            resumeTask(true)
//                        }
//                    } else {
//                        resumeTask(true)
//                    }
//                },
//                onConsentResult = { consentResult ->
//                    Log.d(TAG, "CMP result: $consentResult")
//                    resumeTask(true)
//                }
//            )
//        } catch (e: Exception) {
//            Trackings.logFirebaseTracking(TrackingEvents.splCmpLoad("fail"))
//            Log.e(TAG, "Missing ConsentManager", e)
//            resumeTask(false)
//        }
//    }
}
