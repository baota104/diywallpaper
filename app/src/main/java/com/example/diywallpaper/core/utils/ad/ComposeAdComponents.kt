package com.example.diywallpaper.core.utils.ad

import android.app.Activity
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.diywallpaper.R
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.loadAndShowBanner
import com.example.diywallpaper.core.utils.ad.MyAdsUtils.loadAndShowNativeAds


@Composable
fun BannerAdView(
    idAds: String,
    modifier: Modifier = Modifier,
    onShowSuccess: () -> Unit = {},
    onShowFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.White)
        ,
        factory = { ctx ->
            Log.d("BannerAd", "1. Bắt đầu khởi tạo FrameLayout cho Banner: $idAds")
            val themedContext = ContextThemeWrapper(ctx, R.style.Theme_DIYWallPaper)
            FrameLayout(themedContext).apply {
                activity?.loadAndShowBanner(
                    idAds = idAds,
                    frameLayout = this,
                    onShowSuccess = {
                        Log.d("BannerAd", "3. SUCCESS: $idAds")
                        onShowSuccess()
                    },
                    onShowFailed = {
                        Log.e("BannerAd", "3. FAILED: $idAds")
                        onShowFailed()
                    }
                )
            }
        },
        onRelease = {
//            MyAdsUtils.banner?.destroyAds()
//            MyAdsUtils.banner = null
        }
    )
}

//
//@Composable
//fun NativeAdWaterfallView(
//    idAdsMedium: String,
//    idAdsAll: String,
//    modifier: Modifier = Modifier,
//    reloadKey: Int = 0, // Mặc định = 0 cho các màn không cần reload
//    onLoadSuccessId: (String) -> Unit = {},
//    onLoadSuccess: () -> Unit = {},
//    onLoadFailedAll: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    val activity = context.findActivity()
//
//    key(reloadKey) {
//        AndroidView(
//            modifier = modifier
//                .fillMaxWidth()
//                .wrapContentHeight(),
//            factory = { ctx ->
//                val themedContext = ContextThemeWrapper(
//                    ctx,
//                    R.style.Theme_DIYWallPaper
//                )
//                val frameLayout = FrameLayout(themedContext)
//
//                val handleAdLoaded: (String) -> Unit = { loadedId ->
//                    // Ánh xạ các View từ file XML của Native Ad
//                    val adContainer = frameLayout.findViewById<ConstraintLayout>(R.id.native_ad_container)
//                    val btnCollapse = frameLayout.findViewById<ImageView>(R.id.iv_collapse)
//                    val mediaView = frameLayout.findViewById<View>(R.id.ad_media)
//                    val bodyView = frameLayout.findViewById<View>(R.id.ad_body)
//                    val btnCta = frameLayout.findViewById<AppCompatButton>(R.id.ad_call_to_action)
//                    // Bắt sự kiện Click vào nút Mũi tên thu gọn
//                    btnCollapse?.setOnClickListener {
//                        // BƯỚC 1: Ẩn Media (Ảnh/Video), Body Text và chính nút Collapse
//                        mediaView?.visibility = View.GONE
//                        bodyView?.visibility = View.GONE
//                        btnCollapse.visibility = View.GONE
//                        btnCta?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
//                        btnCta?.setPadding(32, 0, 32, 0)
//                        // BƯỚC 2: Định hình lại Nút CTA (Từ full-width ở đáy lên nằm bên phải)
//                        if (adContainer != null) {
//                            val constraintSet = ConstraintSet()
//                            constraintSet.clone(adContainer)
//
//                            // a. Xóa liên kết cũ của nút CTA
//                            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.TOP)
//                            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.BOTTOM)
//                            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.START)
//
//                            // b. Bóp chiều ngang nút CTA từ match_parent thành wrap_content
//                            constraintSet.constrainWidth(R.id.ad_call_to_action, ConstraintLayout.LayoutParams.WRAP_CONTENT)
//                            // Thu nhỏ chiều cao của nút lại một chút cho phù hợp với banner ngang (vd: 36dp)
//                            constraintSet.constrainHeight(R.id.ad_call_to_action, (36 * ctx.resources.displayMetrics.density).toInt())
//
//                            // c. Neo nút CTA sang bên phải màn hình, căn giữa theo chiều dọc với cái App Icon
//                            constraintSet.connect(R.id.ad_call_to_action, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
//                            constraintSet.connect(R.id.ad_call_to_action, ConstraintSet.TOP, R.id.cv_ad_app_icon, ConstraintSet.TOP)
//                            constraintSet.connect(R.id.ad_call_to_action, ConstraintSet.BOTTOM, R.id.cv_ad_app_icon, ConstraintSet.BOTTOM)
//
//                            // d. Cập nhật lại liên kết của Headline để nó không bị chữ đè lên nút CTA mới
//                            constraintSet.connect(R.id.ad_headline, ConstraintSet.END, R.id.ad_call_to_action, ConstraintSet.START)
//
//                            // e. Áp dụng toàn bộ thay đổi lên UI
//                            constraintSet.applyTo(adContainer)
//                        }
//                    }
//
//                    // Gọi callback gốc để báo ra bên ngoài màn hình
//                    onLoadSuccessId(loadedId)
//                    onLoadSuccess()
//                }
//
//                activity?.loadAndShowNativeAds(
//                    idAds = idAdsMedium,
//                    frameLayout = frameLayout,
//                    onLoadSuccess = { handleAdLoaded(idAdsMedium) }, // Gắn logic UI vào Medium
//                    onLoadFailed = {
//                        activity.loadAndShowNativeAds(
//                            idAds = idAdsAll,
//                            frameLayout = frameLayout,
//                            onLoadSuccess = { handleAdLoaded(idAdsAll) }, // Gắn logic UI vào All
//                            onLoadFailed = onLoadFailedAll
//                        )
//                    }
//                )
//                frameLayout
//            }
//        )
//    }
//}

/**
 * Load Native Ad đơn lẻ (Dùng khi chỉ có 1 ID, không có Waterfall)
 */
@Composable
fun NativeAdView(
    idAds: String,
    modifier: Modifier = Modifier,
    onLoadSuccess: () -> Unit = {},
    onLoadFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { ctx ->
            val themedContext = ContextThemeWrapper(ctx, R.style.Theme_DIYWallPaper)
            val frameLayout = FrameLayout(themedContext)

            // Lưu idAds vào tag để biết đang chứa quảng cáo nào
            frameLayout.tag = idAds

            activity?.loadAndShowNativeAds(
                idAds = idAds,
                frameLayout = frameLayout,
                onLoadSuccess = onLoadSuccess,
                onLoadFailed = onLoadFailed
            )
            frameLayout
        },
        onRelease = { view ->
            // [TỐI ƯU]: Destroy theo ID thông qua MyAdsUtils sẽ an toàn hơn
            // tránh lỗi biến đối tượng nativeAd bị null
            val storedId = view.tag as? String
            if (storedId != null) {
                // Giả định thư viện của bạn có hàm dọn dẹp theo ID, hoặc tự xử lý
                // MyAdsUtils.destroyNativeAd(storedId)
            }
        }
    )
}

/**
 * Hiển thị Native Ad đã được load ngầm (Cache) từ trước
 */
@Composable
fun PreLoadedNativeAdView(
    idAds: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { ctx ->
            val themedContext = ContextThemeWrapper(ctx, R.style.Theme_DIYWallPaper)
            FrameLayout(themedContext).apply {
                MyAdsUtils.showNativeAds(idAds, this)
            }
        }
    )
}
//
//private fun applyCollapsibleNativeLayout(frameLayout: FrameLayout, density: Float) {
//    val adContainer = frameLayout.findViewById<ConstraintLayout>(R.id.native_ad_container)
//    val btnCollapse = frameLayout.findViewById<ImageView>(R.id.iv_collapse)
//    val mediaView = frameLayout.findViewById<View>(R.id.ad_media)
//    val bodyView = frameLayout.findViewById<View>(R.id.ad_body)
//    val btnCta = frameLayout.findViewById<AppCompatButton>(R.id.ad_call_to_action)
//
//    btnCollapse?.setOnClickListener {
//        mediaView?.visibility = View.GONE
//        bodyView?.visibility = View.GONE
//        btnCollapse.visibility = View.GONE
//        btnCta?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
//        btnCta?.setPadding(32, 0, 32, 0)
//        if (adContainer != null) {
//            val constraintSet = ConstraintSet()
//            constraintSet.clone(adContainer)
//            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.TOP)
//            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.BOTTOM)
//            constraintSet.clear(R.id.ad_call_to_action, ConstraintSet.START)
//            constraintSet.constrainWidth(
//                R.id.ad_call_to_action,
//                ConstraintLayout.LayoutParams.WRAP_CONTENT
//            )
//            constraintSet.constrainHeight(
//                R.id.ad_call_to_action,
//                (36 * density).toInt()
//            )
//            constraintSet.connect(
//                R.id.ad_call_to_action,
//                ConstraintSet.END,
//                ConstraintSet.PARENT_ID,
//                ConstraintSet.END
//            )
//            constraintSet.connect(
//                R.id.ad_call_to_action,
//                ConstraintSet.TOP,
//                R.id.cv_ad_app_icon,
//                ConstraintSet.TOP
//            )
//            constraintSet.connect(
//                R.id.ad_call_to_action,
//                ConstraintSet.BOTTOM,
//                R.id.cv_ad_app_icon,
//                ConstraintSet.BOTTOM
//            )
//            constraintSet.connect(
//                R.id.ad_headline,
//                ConstraintSet.END,
//                R.id.ad_call_to_action,
//                ConstraintSet.START
//            )
//            constraintSet.applyTo(adContainer)
//        }
//    }
//}

//@Composable
//fun PreLoadedCollapsibleNativeAdView(
//    idAds: String,
//    renderVersion: Int,
//    alreadyShown: Boolean,
//    modifier: Modifier = Modifier,
//    onShowSuccess: () -> Unit = {},
//    onShowFailed: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    AndroidView(
//        modifier = modifier
//            .fillMaxWidth()
//            .wrapContentHeight(),
//        factory = { ctx ->
//            val themedContext =
//                ContextThemeWrapper(ctx, R.style.Theme_QRScanner)
//            FrameLayout(themedContext).apply {
//                if (!alreadyShown) {
//                    val isShown = MyAdsUtils.showNativeAds(idAds, this)
//                    if (isShown) {
//                        applyCollapsibleNativeLayout(
//                            frameLayout = this,
//                            density = context.resources.displayMetrics.density
//                        )
//                        onShowSuccess()
//                    } else {
//                        onShowFailed()
//                    }
//                }
//            }
//        }
//    )
//}

//@Composable
//fun CollapsibleNativeAdView(
//    idAds: String,
//    modifier: Modifier = Modifier,
//    reloadKey: Int = 0,
//    onLoadSuccess: () -> Unit = {},
//    onLoadFailed: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    val activity = context as? Activity
//
//    key(reloadKey) {
//        AndroidView(
//            modifier = modifier
//                .fillMaxWidth()
//                .wrapContentHeight(),
//            factory = { ctx ->
//                val themedContext = ContextThemeWrapper(
//                    ctx,
//                    R.style.Theme_QRScanner
//                )
//                val frameLayout = FrameLayout(themedContext)
//
//                activity?.loadAndShowNativeAds(
//                    idAds = idAds,
//                    frameLayout = frameLayout,
//                    onLoadSuccess = {
//                        applyCollapsibleNativeLayout(
//                            frameLayout = frameLayout,
//                            density = ctx.resources.displayMetrics.density
//                        )
//                        onLoadSuccess()
//                    },
//                    onLoadFailed = onLoadFailed
//                )
//                frameLayout
//            }
//        )
//    }
//}
