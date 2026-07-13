package com.example.diywallpaper.ui.navigation

import android.net.Uri
import com.example.diywallpaper.domain.model.preview.PreviewSourceType

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Language : Screen("language")
    object Onboarding : Screen("onboarding")
    object Welcome : Screen("welcome")
    object ApplyingLanguage : Screen("applying_language/{source}/{languageCode}") {
        fun createRoute(source: String, languageCode: String) = "applying_language/$source/$languageCode"
    }
    object DashBoard : Screen("dashboard?tab={tab}") {
        fun createRoute(tab: String) = "dashboard?tab=$tab"
    }

    object LoadingAd : Screen("loading_advertisement")

    object PreviewCarousel :
        Screen("preview/carousel/{sourceType}/{categoryId}/{itemId}") {
        fun createRoute(sourceType: PreviewSourceType, categoryId: String, itemId: String): String {
            return "preview/carousel/${sourceType.name}/${Uri.encode(categoryId)}/${Uri.encode(itemId)}"
        }
    }

    object DevicePreview :
        Screen("preview/device/{sourceType}/{categoryId}/{itemId}") {
        fun createRoute(sourceType: PreviewSourceType, categoryId: String, itemId: String): String {
            return "preview/device/${sourceType.name}/${Uri.encode(categoryId)}/${Uri.encode(itemId)}"
        }
    }

    object DevicePreviewDesign :
        Screen("preview/device/design/{designId}") {
        fun createRoute(designId: String): String {
            return "preview/device/design/${Uri.encode(designId)}"
        }
    }

    object Editor :
        Screen("editor/{sourceType}/{categoryId}/{itemId}") {
        fun createRoute(sourceType: PreviewSourceType, categoryId: String, itemId: String): String {
            return "editor/${sourceType.name}/${Uri.encode(categoryId)}/${Uri.encode(itemId)}"
        }
    }

    object EditorDesign :
        Screen("editor/design/{designId}") {
        fun createRoute(designId: String): String {
            return "editor/design/${Uri.encode(designId)}"
        }
    }

    object ImportPhotoCrop :
        Screen("editor/import-photo-crop/{imageUri}") {
        fun createRoute(imageUri: String): String {
            return "editor/import-photo-crop/${Uri.encode(imageUri)}"
        }
    }
}
