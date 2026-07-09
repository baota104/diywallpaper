package com.example.diywallpaper.core.utils.remoteconfig


import android.annotation.SuppressLint
import android.app.Activity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.example.diywallpaper.BuildConfig
import org.json.JSONObject
import kotlin.getOrDefault
import kotlin.runCatching
import kotlin.takeIf
import kotlin.text.isNotBlank

object RemoteConfigManager {
    @SuppressLint("StaticFieldLeak")
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    private val adsConfigKey = "config_ads_v${BuildConfig.VERSION_CODE}"

    fun fetchRemoteConfig(activity: Activity? = null, onComplete: (isSuccess: Boolean) -> Unit) {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 0)
            .setFetchTimeoutInSeconds(10)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        val task = remoteConfig.fetchAndActivate()
        if (activity != null) {
            task.addOnCompleteListener(activity) {
                onComplete(it.isSuccessful && it.isComplete)
            }
        } else {
            task.addOnCompleteListener {
                onComplete(it.isSuccessful && it.isComplete)
            }
        }
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return runCatching {
            if (remoteConfig.all.containsKey(key)) {
                remoteConfig.getBoolean(key)
            } else {
                defaultValue
            }
        }.getOrDefault(defaultValue)
    }

    fun getAdsConfigJson(): String? {
        return runCatching {
            if (!remoteConfig.all.containsKey(adsConfigKey)) return null
            remoteConfig.getString(adsConfigKey).takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun getOpenAdsId(idShowAds: String): String? {
        return runCatching {
            val configJson = getAdsConfigJson() ?: return null
            val root = JSONObject(configJson)
            val appOpen = root.optJSONObject("app_open") ?: return null
            if (!appOpen.optBoolean("status", false)) return null
            if (appOpen.optString("id_show_ads") != idShowAds) return null

            val idAds = appOpen.optJSONObject("id_ads") ?: return null
            idAds.optString("id_admob").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun getPushUpdateConfig(): String? {
        return runCatching {
            remoteConfig.getString("config_pushupdate").takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
