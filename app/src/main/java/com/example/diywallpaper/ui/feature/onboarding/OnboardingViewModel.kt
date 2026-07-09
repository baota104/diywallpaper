package com.example.diywallpaper.ui.feature.onboarding

import androidx.lifecycle.ViewModel
import com.example.diywallpaper.core.utils.SharedPrefsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val sharedPrefsHelper: SharedPrefsHelper
) : ViewModel() {

    fun completeOnboarding() {
        sharedPrefsHelper.isOnboardingCompleted = true
    }
}
