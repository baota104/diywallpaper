package com.example.diywallpaper.ui.preview.core

data class ActivePreviewState(
    val activeVideoIds: Set<String> = emptySet(),
    val activeDiyAnimationIds: Set<String> = emptySet()
)
