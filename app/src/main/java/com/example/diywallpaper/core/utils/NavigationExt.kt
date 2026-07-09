package com.example.diywallpaper.core.utils

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

fun NavController.popBackStackSafely() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

fun NavController.popBackStackSafely(route: String, inclusive: Boolean, saveState: Boolean = false) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack(route, inclusive, saveState)
    }
}
