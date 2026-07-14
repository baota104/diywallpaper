package com.example.diywallpaper

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.diywallpaper.core.utils.SharedPrefsHelper
import com.example.diywallpaper.core.utils.manager.LocaleManager
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.proxglobal.ads.AdsUtils
import com.proxglobal.ads.application.ProxApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DIYWallPaperApp : ProxApplication(), ImageLoaderFactory {
    companion object {
        var instance: DIYWallPaperApp? = null
    }

//    override fun attachBaseContext(base: Context) {
//        super.attachBaseContext(LocaleManager.wrapContext(base))
//    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)

        // 1. Áp dụng ngôn ngữ
        val lang = LocaleManager.getLocale(this)
        if (lang.isNotBlank()) {
            LocaleManager.applyLocale(this, lang)
        }

        AdsUtils.setKeyRemoteConfig("config_ads_v${BuildConfig.VERSION_CODE}")
//        AdsUtils.addStyleNative(101,R.layout.layout_collab_native_101)
//        AdsUtils.addStyleNative(102, R.layout.layout_native_fsc_102)
//        AdsUtils.addStyleNative(103,R.layout.layout_native_103)
//        AdsUtils.addStyleNative(104,R.layout.layout_native_104)
//        AdsUtils.addStyleNative(105, R.layout.layout_native_105)
//        AdsUtils.addStyleNative(106, R.layout.layout_native_106)


        AdsUtils.disableOpenAds()
//        cacheOpenAdUnitId()
//        registerResumeAdLifecycle()
//        loadFCMToken()
//        cachePushUpdate()
    }
//    private fun cachePushUpdate(){
//        RemoteConfigManager.fetchRemoteConfig(null) { isSuccess ->
//            if (isSuccess) {
//                val configJson = RemoteConfigManager.getPushUpdateConfig().orEmpty()
//                Log.d("QRScannerApp", "cachePushUpdateConfig: $configJson")
//            }
//        }
//    }
//    private fun cacheOpenAdUnitId() {
//        val openAdUnitId = RemoteConfigManager.getOpenAdsId(OPEN_APP_PLACEMENT_ID).orEmpty()
//        Log.d("QRScannerApp", "cacheOpenAdUnitId: $openAdUnitId")
//    }
//
//    private fun registerResumeAdLifecycle() {
//        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
//            override fun onStop(owner: LifecycleOwner) {
//                ResumeAdNavigationManager.onAppWentToBackground()
//            }
//        })
//
//        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
//            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
//            override fun onActivityStarted(activity: Activity) = Unit
//            override fun onActivityResumed(activity: Activity) {
//
//                ResumeAdNavigationManager.tryOpenLoadingAdOnResume()
//            }
//            override fun onActivityPaused(activity: Activity) = Unit
//            override fun onActivityStopped(activity: Activity) = Unit
//            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
//            override fun onActivityDestroyed(activity: Activity) = Unit
//        })
//    }
//    private fun loadFCMToken() {
//        if (BuildConfig.DEBUG) {
//            FirebaseMessaging.getInstance().token.addOnSuccessListener { token: String ->
//                if (!TextUtils.isEmpty(token)) {
//                    FirebaseDatabase.getInstance()
//                        .getReference("token/${android.os.Build.MANUFACTURER}${android.os.Build.DEVICE}")
//                        .push().setValue(token).addOnFailureListener {
//                            Log.e("baotq_token", "onCreate: push fail token: ${it.message}")
//                        }.addOnSuccessListener {
//                            Log.d("baotq", "token successful : $token")
//                        }
//                    Log.d("baotq", "retrieve token successful : $token")
//                } else {
//                    Log.w("baotq", "token should not be null...")
//                }
//            }.addOnFailureListener { }.addOnCanceledListener {}
//                .addOnSuccessListener {
//                    Log.v("baotq", "This is the token : $it")
//                }
//        }
//    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
    }
}
