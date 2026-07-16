package com.example.diywallpaper.ui.feature.dashboard.collection

import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo

data class DashboardCollectionUiState(
    val isLoading: Boolean = true,
    val selectedFilter: CollectionFilter = CollectionFilter.DESIGNS,
    val favoriteCount: Int = 0,
    val designCount: Int = 0,
    val favorites: List<HomeFeedItem> = emptyList(),
    val designs: List<UserDesign> = emptyList(),
    val activeVideoIds: Set<String> = emptySet(),
    val activeDiyAnimationIds: Set<String> = emptySet(),
    val visibleItems: Map<String, PreviewVisibilityInfo> = emptyMap(),
    val isGridScrolling: Boolean = false,
    val errorMessage: String? = null,
    val pendingOpenDesignId: String? = null
)
