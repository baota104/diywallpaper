package com.example.diywallpaper.ui.feature.preview

import com.example.diywallpaper.domain.model.preview.PreviewSourceType

data class PreviewArgs(
    val categoryId: String,
    val initialItemId: String,
    val sourceType: PreviewSourceType
)
