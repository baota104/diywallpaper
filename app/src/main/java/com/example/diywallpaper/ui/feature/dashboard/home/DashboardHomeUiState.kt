package com.example.diywallpaper.ui.feature.dashboard.home

import com.example.diywallpaper.domain.model.HomeFeedCategory
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo

data class DashboardHomeUiState(
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val unreadNotificationsCount: Int = 0,
    val categories: List<HomeFeedCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val items: List<HomeFeedItem> = emptyList(),
    val activeVideoIds: Set<String> = emptySet(),
    val activeDiyAnimationIds: Set<String> = emptySet(),
    val visibleItems: Map<String, PreviewVisibilityInfo> = emptyMap(),
    val isGridScrolling: Boolean = false,
    val errorMessage: String? = null
)
