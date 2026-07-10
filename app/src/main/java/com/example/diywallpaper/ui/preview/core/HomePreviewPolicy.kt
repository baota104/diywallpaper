package com.example.diywallpaper.ui.preview.core

data class HomePreviewPolicy(
    val minVisibleFractionIdle: Float = 0.6f,
    val minVisibleFractionScrolling: Float = 0.82f,
    val allowMultipleVideoPreviews: Boolean = true,
    val allowMultipleDiyAnimationPreviews: Boolean = true
)
