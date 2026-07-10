package com.example.diywallpaper.ui.navigation

import android.app.Activity
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.ui.feature.language.ApplyingLanguageRoute
import com.example.diywallpaper.ui.feature.language.ApplyingLanguageSource
import com.example.diywallpaper.ui.feature.language.LanguageScreen
import com.example.diywallpaper.ui.feature.onboarding.OnboardingScreen
import com.example.diywallpaper.ui.feature.splash.SplashLaunchState
import com.example.diywallpaper.ui.feature.splash.SplashScreen
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import com.example.diywallpaper.ui.feature.preview.carousel.PreviewCarouselScreen
import com.example.diywallpaper.ui.feature.preview.device.DevicePreviewScreen
import com.example.diywallpaper.ui.feature.welcome.WelcomeScreen
import com.example.diywallpaper.core.utils.popBackStackSafely

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    onLanguageChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var lastLoadedKey by remember { mutableStateOf(-1) }
    val bannerContainer = remember { FrameLayout(context) }
    var bannerReloadKey by remember { mutableStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { launchState: SplashLaunchState ->
                    val destination = when {
                        launchState.isFirstAppLaunch -> Screen.Language.route
                        !launchState.isOnboardingCompleted -> Screen.Language.route
                        else -> Screen.Welcome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Language Screen
        composable(Screen.Language.route) {
            LanguageScreen(
                onLanguageSelected = { selectedLang ->
                    onLanguageChanged(selectedLang)
                    val source =
                        if (navController.previousBackStackEntry?.destination?.route == Screen.DashBoard.route) {
                            ApplyingLanguageSource.SETTINGS.name
                        } else {
                            ApplyingLanguageSource.FIRST_OPEN.name
                        }
                    navController.navigate(
                        Screen.ApplyingLanguage.createRoute(
                            source = source,
                            languageCode = selectedLang
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.ApplyingLanguage.route,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("languageCode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sourceArg = backStackEntry.arguments?.getString("source")
                ?: ApplyingLanguageSource.FIRST_OPEN.name
            val languageCode = backStackEntry.arguments?.getString("languageCode").orEmpty()
            val source = runCatching { ApplyingLanguageSource.valueOf(sourceArg) }
                .getOrDefault(ApplyingLanguageSource.FIRST_OPEN)

            ApplyingLanguageRoute(
                languageCode = languageCode,
                source = source,
                onFinished = {
                    when (source) {
                        ApplyingLanguageSource.FIRST_OPEN -> {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(Screen.Language.route) { inclusive = true }
                            }
                        }

                        ApplyingLanguageSource.SETTINGS -> {
                            navController.popBackStackSafely(
                                Screen.Language.route,
                                inclusive = true
                            )
                        }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingFinished = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateNext = {
                    navController.navigate(Screen.DashBoard.createRoute("home")) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.DashBoard.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "home"
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: "home"
            com.example.diywallpaper.ui.feature.dashboard.DashboardScreen(
                initialTab = tab,
                onOpenPreview = { sourceType, categoryId, itemId ->
                    navController.navigate(
                        Screen.PreviewCarousel.createRoute(
                            sourceType = sourceType,
                            categoryId = categoryId,
                            itemId = itemId
                        )
                    )
                },
                onCreateFromScratch = {
                    navController.navigate(
                        Screen.PreviewCarousel.createRoute(
                            sourceType = PreviewSourceType.CREATE_FROM_SCRATCH,
                            categoryId = "create_from_scratch",
                            itemId = "create_from_scratch"
                        )
                    )
                }
            )
        }

        composable(
            route = Screen.PreviewCarousel.route,
            arguments = listOf(
                navArgument("sourceType") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.toPreviewArgs()
            PreviewCarouselScreen(
                args = args,
                onBackClick = { navController.popBackStack() },
                onOpenDevicePreview = { previewArgs ->
                    navController.navigate(
                        Screen.DevicePreview.createRoute(
                            sourceType = previewArgs.sourceType,
                            categoryId = previewArgs.categoryId,
                            itemId = previewArgs.initialItemId
                        )
                    )
                },
                onEditRequested = { _ -> }
            )
        }

        composable(
            route = Screen.DevicePreview.route,
            arguments = listOf(
                navArgument("sourceType") { type = NavType.StringType },
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.toPreviewArgs()
            DevicePreviewScreen(
                args = args,
                onBackClick = { navController.popBackStack() },
                onApplyClick = { _ -> }
            )
        }
    }
}

private fun androidx.navigation.NavBackStackEntry.toPreviewArgs(): PreviewArgs {
    val sourceType = arguments?.getString("sourceType")
        ?.let { runCatching { PreviewSourceType.valueOf(it) }.getOrNull() }
        ?: PreviewSourceType.WALLPAPER
    val categoryId = Uri.decode(arguments?.getString("categoryId").orEmpty())
    val itemId = Uri.decode(arguments?.getString("itemId").orEmpty())
    return PreviewArgs(
        categoryId = categoryId,
        initialItemId = itemId,
        sourceType = sourceType
    )
}
