package com.example.diywallpaper.core.utils.ad

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.ads.pro.base.BannerAds
import com.google.ads.pro.base.NativeAds
import com.google.ads.pro.callback.LoadAdsCallback
import com.google.ads.pro.callback.ShowAdsCallback

import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import com.example.diywallpaper.BuildConfig
import com.example.diywallpaper.R
import com.example.diywallpaper.core.constant.Constant.RTDB


import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.nativead.NativeAdOptions

import com.google.firebase.database.FirebaseDatabase

import com.proxglobal.ads.AdsUtils
object MyAdsUtils {
    const val TAG_ADS = "baotq_ads"

    private val listNativeAds: MutableList<MyNativeAds> = arrayListOf()
    var banner: BannerAds<*>? = null
    private var appOpenAd: AppOpenAd? = null
    private var loadedAppOpenId: String? = null
    private var isAppOpenShowing = false
    @Volatile
    var isFullScreenAdShowing = false
    class MyNativeAds(
        var nativeAds: NativeAds<*>? = null,
        var isLoading: Boolean = true,
        var idShowAds: String
    )

     fun logFirebaseAds(eventName: String) {
        Log.d("logTrackingAds", "logTrackingAds: $eventName")
        if (BuildConfig.DEBUG) {
            FirebaseDatabase.getInstance(RTDB)
                .getReference("ads_events/${Build.MANUFACTURER}${Build.DEVICE}")
                .push().setValue(eventName)
            return
        }
    }


    fun Activity.loadAndShowBanner(
        idAds: String,
        frameLayout: FrameLayout,
        onShowSuccess: () -> Unit = {},
        onShowFailed: () -> Unit = {}
    ) {
        try {
            frameLayout.visibility = View.VISIBLE
            frameLayout.removeAllViews()
            banner?.destroyAds()
            banner = null
            logFirebaseAds("loadAndShowBanner $idAds onLoading")
            banner = AdsUtils.loadBannerAds(
                this, frameLayout, idAds, object : LoadAdsCallback() {
                    override fun onLoadSuccess(isMeta: Boolean?) {
                        super.onLoadSuccess(isMeta)
                        Log.d(TAG_ADS, "loadAndShowBanner $idAds onLoadSuccess: ")
                        logFirebaseAds("loadAndShowBanner $idAds Success:")
                        try {
                            frameLayout.removeAllViews()
                            banner?.showAds(frameLayout)
                            onShowSuccess()
                        } catch (_: Exception) {
                        }
                    }

                    override fun onLoadFailed(message: String?) {
                        super.onLoadFailed(message)
                        banner?.destroyAds()
                        banner = null
                        frameLayout.removeAllViews()
                        frameLayout.visibility = View.GONE
                        Log.d(TAG_ADS, "loadAndShowBanner $idAds onLoadFailed: $message")
                        logFirebaseAds("loadAndShowBanner $idAds onLoadFailed: $message")
                        onShowFailed()
                    }
                }, AdsUtils.shimmerBanner, AdsUtils.shimmerBaseColor, AdsUtils.shimmerHighlightColor
            )
        } catch (_: Exception) {
            banner?.destroyAds()
            banner = null
            frameLayout.removeAllViews()
            frameLayout.visibility = View.GONE
        }
    }

    fun Activity.preloadNativeWaterfall(
        primaryId: String,
        fallbackId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val primaryAds =
            getMyNativeAds(primaryId, isAdded = true)
        primaryAds.isLoading = true
        primaryAds.nativeAds = loadNativeAndCallBack(
            idAds = primaryAds.idShowAds,
            onLoadSuccess = {
                primaryAds.isLoading = false
                onResult(true, primaryId)
            },
            onLoadFailed = {
                primaryAds.isLoading = false
                primaryAds.nativeAds = null

                val fallbackAds =getMyNativeAds(
                    fallbackId,
                    isAdded = true
                )
                fallbackAds.isLoading = true
                fallbackAds.nativeAds = loadNativeAndCallBack(
                    idAds = fallbackAds.idShowAds,
                    onLoadSuccess = {
                        fallbackAds.isLoading = false
                        onResult(true, fallbackId)
                    },
                    onLoadFailed = {
                        fallbackAds.isLoading = false
                        fallbackAds.nativeAds = null
                        onResult(false, null)
                    }
                )
            }
        )
    }
    fun showNativeAds(idAds: String, frameLayout: FrameLayout): Boolean {
        val myNativeAds = getMyNativeAds(idAds)
        val rs = myNativeAds.nativeAds != null && !myNativeAds.isLoading
        if (rs) {
            frameLayout.visibility = View.VISIBLE
            frameLayout.removeAllViews()
            myNativeAds.nativeAds?.showAds(frameLayout)
            Log.d(TAG_ADS, "showNativeAds: $idAds success")
            logFirebaseAds("showNativeAds: $idAds success")
        } else {
            frameLayout.visibility = View.GONE
            frameLayout.removeAllViews()
            Log.d(TAG_ADS, "showNativeAds: $idAds failed")
            logFirebaseAds("showNativeAds: $idAds failed")
        }
        return rs
    }

    fun showNativeAdsAlternative(
        idAds1: String,
        idAds2: String,
        frameLayout: FrameLayout
    ): Boolean {
        var rs = showNativeAds(idAds1, frameLayout)
        if (!rs) {
            Log.d(TAG_ADS, "check show fail idAds1: $idAds1")
            rs = showNativeAds(idAds2, frameLayout)
        }
        return rs
    }

    fun Activity.showNativeFullScreenDialog(idAds: String, onClose: () -> Unit) {
        if (!checkNativeLoaded(idAds)) {
            logFirebaseAds("LoadNativeAdsFSC: $idAds fail")
            onClose.invoke()
            return
        }

        // Dùng Theme Translucent để không bị dính nền đen/trắng mặc định của hệ thống
        val dialog = Dialog(this, R.style.Theme_DIYWallPaper)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val frameLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // [QUAN TRỌNG 1] Ép nền trắng cho FrameLayout để che đi bất kỳ khoảng hở nào
            setBackgroundColor(Color.WHITE)
        }
        dialog.setContentView(frameLayout)

        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            decorView.setPadding(0, 0, 0, 0)

            // [QUAN TRỌNG 2] Cho phép tràn vào khu vực "Tai thỏ / Notch"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

            // [QUAN TRỌNG 3] Ẩn hoàn toàn Thanh trạng thái (Pin/Sóng) và Thanh điều hướng
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                insetsController?.let {
                    it.hide(WindowInsets.Type.systemBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
            }
        }

        showNativeAds(idAds, frameLayout)

        var isClosedCalled = false

        Handler(Looper.getMainLooper()).post {
            val btnCloseId = frameLayout.resources.getIdentifier(
                "btnClose",
                "id",
                "com.proxglobal.proxads"
            )
            val btnClose = if (btnCloseId != 0) {
                frameLayout.findViewById<ImageView>(btnCloseId)
            } else {
                null
            }
            if (btnClose != null) {
                btnClose.visibility = View.INVISIBLE
                btnClose.postDelayed({
                    btnClose.visibility = View.VISIBLE
                }, 3000L)
                btnClose.setOnClickListener {
                    dialog.dismiss()
                }
            }
        }

        dialog.setOnDismissListener {
            if (!isClosedCalled) {
                isClosedCalled = true
                onClose.invoke()
            }
        }

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }
    fun Activity.loadOpenAd(
        adUnitId: String,
        idShowAds: String = "O_Resume",
        onLoadSuccess: () -> Unit = {},
        onLoadFailed: () -> Unit = {}
    ) {
       logFirebaseAds("loadOpenAd $idShowAds onLoading")
        if (adUnitId.isBlank()) {
            Log.d(TAG_ADS, "loadOpenAd: missing cached ad unit for $idShowAds")
            logFirebaseAds("loadOpenAd $idShowAds failed: missing_ad_unit")
            onLoadFailed.invoke()
            return
        }

        appOpenAd = null
        loadedAppOpenId = null
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            this,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    loadedAppOpenId = idShowAds
                    Log.d(TAG_ADS, "loadOpenAd $idShowAds success")
                    logFirebaseAds("loadOpenAd $idShowAds success")
                    onLoadSuccess.invoke()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    appOpenAd = null
                    loadedAppOpenId = null
                    val reason = loadAdError.message
                    Log.d(TAG_ADS, "loadOpenAd $idShowAds failed: $reason")
                    logFirebaseAds("loadOpenAd $idShowAds failed: $reason")
                    onLoadFailed.invoke()
                }
            }
        )
    }

    fun Activity.showOpenAd(
        idShowAds: String,
        onShowSuccess: () -> Unit = {},
        onShowFailed: () -> Unit = {},
        onAdClosed: () -> Unit = {}
    ) {
        val ad = appOpenAd
        if (ad == null || loadedAppOpenId != idShowAds || isAppOpenShowing) {
            Log.d(TAG_ADS, "showOpenAd $idShowAds failed: not_ready")
           logFirebaseAds("showOpenAd $idShowAds failed: not_ready")
            onShowFailed.invoke()
            return
        }
        isFullScreenAdShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                isAppOpenShowing = true
                Log.d(TAG_ADS, "showOpenAd $idShowAds success")
                logFirebaseAds("showOpenAd $idShowAds success")
                onShowSuccess.invoke()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                isAppOpenShowing = false
                appOpenAd = null
                isFullScreenAdShowing = false
                loadedAppOpenId = null
                logFirebaseAds("showOpenAd $idShowAds closed")
                onAdClosed.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                isAppOpenShowing = false
                appOpenAd = null
                loadedAppOpenId = null
                isFullScreenAdShowing = false
                val reason = adError.message
                Log.d(TAG_ADS, "showOpenAd $idShowAds failed: $reason")
                logFirebaseAds("showOpenAd $idShowAds failed: $reason")
                onShowFailed.invoke()
            }
        }

        ad.show(this)
    }
    fun Activity.loadNativeAlternate(
        idAds1: String,
        idAds2: String? = null,
        isFullScreen: Boolean = false,
        loadComplete: (Boolean) -> Unit = {}
    ) {
        val ads1 = getMyNativeAds(idAds1, isAdded = true)
        ads1.isLoading = true
        ads1.nativeAds =
            loadNativeAndCallBack(ads1.idShowAds, isFullScreen = isFullScreen, onLoadSuccess = {
                ads1.isLoading = false
                loadComplete.invoke(true)
            }, onLoadFailed = {
                ads1.isLoading = false
                ads1.nativeAds = null
                if (idAds2 != null) {
                    val ads2 = getMyNativeAds(idAds2, isAdded = true)
                    ads2.isLoading = true
                    ads2.nativeAds = loadNativeAndCallBack(
                        ads2.idShowAds,
                        isFullScreen = isFullScreen,
                        onLoadSuccess = {
                            ads2.isLoading = false
                            loadComplete.invoke(true)
                        },
                        onLoadFailed = {
                            ads2.isLoading = false
                            ads2.nativeAds = null
                            loadComplete.invoke(false)
                        })
                } else {
                    loadComplete.invoke(false)
                }
            })
    }
    fun checkNativeLoading(idAds: String): Boolean = getMyNativeAds(idAds).isLoading
    fun checkNativeLoaded(idAds: String): Boolean = getMyNativeAds(idAds).nativeAds != null && !checkNativeLoading(idAds)

    fun getMyNativeAds(idAds: String, isAdded: Boolean = false): MyNativeAds {
        val item = listNativeAds.find { it.idShowAds == idAds }
        if (item != null) return item
        val data = MyNativeAds(idShowAds = idAds)
        if (isAdded) listNativeAds.add(data)
        return data
    }

    private fun Activity.loadNativeAndCallBack(
        idAds: String,
        isFullScreen: Boolean = false,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {}
    ): NativeAds<*>? {
        val adOptions = if (isFullScreen) {
            NativeAdOptions.Builder().setMediaAspectRatio(MediaAspectRatio.PORTRAIT).build()
        } else null
        logFirebaseAds("loadNativeAds $idAds onLoading")
        return AdsUtils.loadNativeAds(
            this,
            null,
            idAds,
            adOptions = adOptions,
            callback = object : LoadAdsCallback() {
                override fun onLoadSuccess(adsMeta: Boolean?) {
                    super.onLoadSuccess(adsMeta)
                    onLoadSuccess.invoke()
                    Log.d(TAG_ADS, "loadNativeAds $idAds success")
                    logFirebaseAds("loadNativeAds $idAds success")
                }

                override fun onLoadFailed(message: String?) {
                    super.onLoadFailed(message)
                    val reason = message ?: "unknown"
                    Log.d(TAG_ADS, "loadNativeAds $idAds failed: $reason")
                    logFirebaseAds("loadNativeAds $idAds failed: $reason")
                    onLoadFailed.invoke()
                }
            })
    }

    fun Activity.loadAndShowNativeAds(
        idAds: String,
        frameLayout: FrameLayout,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {}
    ): NativeAds<*>? { // Thêm return type ở đây
        frameLayout.visibility = View.VISIBLE
        logFirebaseAds("loadAndShowNativeAds $idAds onLoading")
        var native: NativeAds<*>? = null
        runCatching {
            native = this.let { activity ->
                AdsUtils.loadNativeAds(activity, frameLayout, idAds, object : LoadAdsCallback() {
                    override fun onLoadSuccess(isMeta: Boolean?) {
                        super.onLoadSuccess(isMeta)
                        if (activity.isDestroyed || activity.isFinishing) {
                            native?.destroyAds()
                            return
                        }
                        frameLayout.removeAllViews()
                        native?.showAds(frameLayout)
                        Log.d(TAG_ADS, "loadAndShowNativeAds $idAds success")
                        logFirebaseAds("loadAndShowNativeAds $idAds success")
                        onLoadSuccess.invoke()
                    }

                    override fun onLoadFailed(message: String?) {
                        super.onLoadFailed(message)
                        frameLayout.removeAllViews()
                        frameLayout.visibility = View.GONE
                        Log.d(TAG_ADS, "loadAndShowNativeAds $idAds failed: $message")
                        logFirebaseAds("loadAndShowNativeAds $idAds failed: $message")
                        onLoadFailed.invoke()
                    }
                })
            }
        }
        return native
    }

    fun Activity.loadInterAds(
        idAds: String,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {}
    ) {logFirebaseAds("loadinter $idAds onLoading")
        AdsUtils.loadInterstitialAds(this, idAds, object : LoadAdsCallback() {

            override fun onLoadSuccess(adsMeta: Boolean?) {
                super.onLoadSuccess(adsMeta)
                Log.d(TAG_ADS, "loadInterAds $idAds success")
                logFirebaseAds("loadInterAds $idAds success")
                onLoadSuccess.invoke()
            }

            override fun onLoadFailed(message: String?) {
                super.onLoadFailed(message)
                Log.d(TAG_ADS, "loadInterAds $idAds failed: $message")
                logFirebaseAds("loadInterAds $idAds failed: $message")
                onLoadFailed.invoke()
            }
        })
    }

    fun Activity.loadInterAlternate(
        idAdsHigh: String,
        idAdsNormal: String,
        onLoadComplete: (Boolean, String?) -> Unit
    ) {
        loadInterAds(idAdsHigh, onLoadSuccess = {
            onLoadComplete.invoke(true, idAdsHigh)
        }, onLoadFailed = {
            Log.d(TAG_ADS, "Load High Inter Failed, fallback to Normal")
            loadInterAds(idAdsNormal, onLoadSuccess = {
                onLoadComplete.invoke(true, idAdsNormal)
            }, onLoadFailed = {
                onLoadComplete.invoke(false, null)
            })
        })
    }

    fun Activity.loadAndShowInterAds(
        idAds: String,
        onShowFailed: () -> Unit = {},
        onShowSuccess: () -> Unit = {},
        onAdClosed: () -> Unit = {}
    ) {
        logFirebaseAds("loadAndShowInterAds $idAds onLoading")
        isFullScreenAdShowing = true
        AdsUtils.loadAndShowInterstitialAds(this, idAds, object : ShowAdsCallback() {
            override fun onShowSuccess() {
                super.onShowSuccess()
                isFullScreenAdShowing = true
                Log.d(TAG_ADS, "loadAndShowInterAds $idAds success")
                logFirebaseAds("loadAndShowInterAds $idAds success")
                onShowSuccess.invoke()
            }

            override fun onShowFailed(message: String?) {
                super.onShowFailed(message)
                isFullScreenAdShowing = false
                Log.d(TAG_ADS, "loadAndShowInterAds $idAds failed: $message")
                logFirebaseAds("loadAndShowInterAds $idAds failed: $message")
                onShowFailed.invoke()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                isFullScreenAdShowing = false
                onAdClosed.invoke()
            }
        })
    }

    fun Activity.showInterAds(
        idAds: String,
        onShowFailed: () -> Unit = {},
        onShowSuccess: () -> Unit = {},
        onAdClosed: () -> Unit = {}
    ) {
        isFullScreenAdShowing = true
        AdsUtils.showInterstitialAds(this, idAds, object : ShowAdsCallback() {
            override fun onShowSuccess() {
                super.onShowSuccess()
                Log.d(TAG_ADS, "showInterAds $idAds success")
                logFirebaseAds("showInterAds $idAds success")
                onShowSuccess.invoke()
            }

            override fun onShowFailed(message: String?) {
                super.onShowFailed(message)
                isFullScreenAdShowing = false
                Log.d(TAG_ADS, "showInterAds $idAds failed: $message")
                logFirebaseAds("showInterAds $idAds failed: $message")
                onShowFailed.invoke()
            }

            override fun onAdClosed() {
                super.onAdClosed()
                isFullScreenAdShowing = false
                onAdClosed.invoke()
            }
        })
    }


    fun loadRewardAds(
        activity: Activity,
        idShowAds: String,
        onLoadSuccess: () -> Unit,
        onLoadFailed: () -> Unit
    ) {
        AdsUtils.loadRewardAds(activity, idShowAds, object : LoadAdsCallback() {
            override fun onLoadFailed(message: String?) {
                super.onLoadFailed(message)
                Log.d(TAG_ADS, "$idShowAds onLoadFailed: $message")
                logFirebaseAds("$idShowAds onLoadFailed: $message")
                onLoadFailed()
            }

            override fun onLoadSuccess(adsMeta: Boolean?) {
                super.onLoadSuccess(adsMeta)
                Log.d(TAG_ADS, "$idShowAds onLoadSuccess: ")
                logFirebaseAds("$idShowAds onLoadSuccess: ")
                onLoadSuccess()
            }
        })
    }


    fun Activity.loadAndShowRewards(
        idShowAds: String,
        onShowSuccess: () -> Unit,
        onAdsGranted: () -> Unit
    ) {
        logFirebaseAds("loadAndShowRewards $idShowAds onLoading")
        isFullScreenAdShowing = true
        AdsUtils.loadAndShowRewardAds(this, idShowAds, object : ShowAdsCallback() {
            override fun onAdClosed() {
                super.onAdClosed()
                isFullScreenAdShowing = false
                onAdsGranted()
            }

            override fun onShowFailed(message: String?) {
                super.onShowFailed(message)
                isFullScreenAdShowing = false
                Log.d(TAG_ADS, "loadAndShowRewards onShowFailed: $message")
                logFirebaseAds("loadAndShowRewards onShowFailed: $message")
                onAdsGranted()
            }

            override fun onShowSuccess() {
                super.onShowSuccess()
                Log.d(TAG_ADS, "loadAndShowRewards onShowSuccess ")
                logFirebaseAds("loadAndShowRewards onShowSuccess ")
                onShowSuccess()
            }
        })
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
