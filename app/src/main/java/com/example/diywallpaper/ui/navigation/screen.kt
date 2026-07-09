package com.example.diywallpaper.ui.navigation

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
}
