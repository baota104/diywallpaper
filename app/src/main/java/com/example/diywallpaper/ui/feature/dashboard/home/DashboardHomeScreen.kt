package com.example.diywallpaper.ui.feature.dashboard.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diywallpaper.domain.model.preview.PreviewSourceType

@Composable
fun DashboardHomeScreen(
    modifier: Modifier = Modifier,
    onOpenPreview: (sourceType: PreviewSourceType, categoryId: String, itemId: String) -> Unit = { _, _, _ -> },
    onCreateFromScratch: () -> Unit = {},
    viewModel: DashboardHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardHomeContent(
        modifier = modifier,
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onCategorySelected = viewModel::onCategorySelected,
        onViewportChanged = viewModel::onViewportChanged,
        onWallpaperFavoriteClick = viewModel::onWallpaperFavoriteClick,
        onDiyFavoriteClick = viewModel::onDiyFavoriteClick,
        onOpenPreview = onOpenPreview,
        onCreateFromScratch = onCreateFromScratch
    )
}
