package com.example.diywallpaper.core.utils.manager

import android.content.Context
import android.content.res.Configuration
import com.example.diywallpaper.core.utils.SharedPrefsHelper
import java.util.Locale

object LocaleManager {
    private const val SELECTED_LANGUAGE = "selected_language"

    fun wrapContext(context: Context): Context {
        val savedLang = getLocale(context)
        if (savedLang.isBlank()) return context
        val locale = Locale(savedLang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun applyLocale(context: Context, lang: String) {
        if (lang.isBlank()) return
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun getLocale(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(SharedPrefsHelper.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(SELECTED_LANGUAGE, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun setLocale(context: Context, lang: String) {
        try {
            val prefs = context.getSharedPreferences(SharedPrefsHelper.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(SELECTED_LANGUAGE, lang).apply()
            applyLocale(context, lang)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createConfiguration(baseConfig: Configuration, languageCode: String): Configuration {
        val locale = Locale(languageCode)
        return Configuration(baseConfig).apply {
            setLocale(locale)
        }
    }
}
