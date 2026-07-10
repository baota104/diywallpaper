package com.example.diywallpaper.ui.preview.core

data class PreviewViewportState(
    val visibleItems: Map<String, PreviewVisibilityInfo> = emptyMap(),
    val isScrolling: Boolean = false
)

data class PreviewVisibilityInfo(
    val visibleFraction: Float,
    val isFullyVisible: Boolean
)
