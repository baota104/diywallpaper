package com.example.diywallpaper.core.utils.trackings

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.example.diywallpaper.BuildConfig
import com.example.diywallpaper.DIYWallPaperApp
import com.example.diywallpaper.core.constant.Constant.RTDB

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase

object Trackings {
    fun logFirebaseTracking(eventName: String,bundle: Bundle? = null) {
        Log.d("DIYWallPaper_tracking", "logFirebaseEvent: :$eventName")

        if (BuildConfig.DEBUG) {
            try {
                FirebaseDatabase.getInstance(RTDB)
                    .getReference("screen_tracking/${Build.MANUFACTURER}${Build.DEVICE}")
                    .push().setValue(eventName)
            } catch (e: Exception) {
                Log.e("DIYWallPaper_tracking", "Firebase RTDB tracking failed: ${e.message}", e)
            }
            return
        }
        else {
            DIYWallPaperApp.instance?.let { app ->
                FirebaseAnalytics.getInstance(app).logEvent(eventName, bundle)
            }
        }
    }
}
