package com.example.diywallpaper.core.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PREFS_NAME = "diy_wallpaper_prefs"
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var appOpenCount: Int
        get() = sharedPrefs.getInt("app_open_count", 0)
        set(value) = sharedPrefs.edit().putInt("app_open_count", value).apply()

    var isOnboardingCompleted: Boolean
        get() = sharedPrefs.getBoolean("onboarding_completed", false)
        set(value) = sharedPrefs.edit().putBoolean("onboarding_completed", value).apply()

    var hasLoadedCmp: Boolean
        get() = sharedPrefs.getBoolean("has_loaded_cmp", false)
        set(value) = sharedPrefs.edit().putBoolean("has_loaded_cmp", value).apply()

    fun getEffectiveAppOpenCount(): Int {
        return appOpenCount
    }

    fun getEffectiveOnboardingCompleted(): Boolean {
        return isOnboardingCompleted
    }

}

