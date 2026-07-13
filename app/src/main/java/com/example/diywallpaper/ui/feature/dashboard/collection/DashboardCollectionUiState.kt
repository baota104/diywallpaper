package com.example.diywallpaper.ui.feature.dashboard.collection

import com.example.diywallpaper.domain.model.design.UserDesign

data class DashboardCollectionUiState(
    val isLoading: Boolean = true,
    val selectedFilter: CollectionFilter = CollectionFilter.DESIGNS,
    val favoriteCount: Int = 0,
    val designCount: Int = 0,
    val designs: List<UserDesign> = emptyList(),
    val errorMessage: String? = null,
    val pendingOpenDesignId: String? = null
)
