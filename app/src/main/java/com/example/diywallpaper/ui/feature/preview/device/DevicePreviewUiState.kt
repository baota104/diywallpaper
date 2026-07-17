package com.example.diywallpaper.ui.feature.preview.device

import android.content.Intent
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewPlayableSource
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction

data class DevicePreviewUiState(
    val isLoading: Boolean = true,
    val categoryId: String = "",
    val itemId: String = "",
    val currentItem: PreviewCatalogItem? = null,
    val designProject: EditorProject? = null,
    val playableSource: PreviewPlayableSource? = null,
    val primaryAction: PreviewPrimaryAction? = null,
    val isChromeVisible: Boolean = true,
    val isApplyingWallpaper: Boolean = false,
    val showTargetDialog: Boolean = false,
    val launchIntent: Intent? = null,
    val designExportPath: String? = null,
    val navigateHomeAfterApply: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
