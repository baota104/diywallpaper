package com.example.diywallpaper.ui.feature.welcome

import androidx.lifecycle.ViewModel
import com.example.diywallpaper.core.utils.SharedPrefsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val sharedPrefsHelper: SharedPrefsHelper
) : ViewModel() {

    fun completeWelcome() {
        // Có thể lưu cờ đánh dấu nếu cần
    }
}
