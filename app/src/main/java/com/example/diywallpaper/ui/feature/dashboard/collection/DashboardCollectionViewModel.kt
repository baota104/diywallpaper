package com.example.diywallpaper.ui.feature.dashboard.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.usecase.collection.ObserveFavoriteFeedItemsUseCase
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.ObserveUserDesignsUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import com.example.diywallpaper.domain.usecase.diy.ToggleDiyFavoriteUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.ToggleWallpaperFavoriteUseCase
import com.example.diywallpaper.ui.preview.core.GridPreviewCoordinator
import com.example.diywallpaper.ui.preview.core.PreviewViewportState
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardCollectionViewModel @Inject constructor(
    private val observeUserDesignsUseCase: ObserveUserDesignsUseCase,
    private val observeFavoriteFeedItemsUseCase: ObserveFavoriteFeedItemsUseCase,
    private val renameDesignUseCase: RenameDesignUseCase,
    private val deleteDesignUseCase: DeleteDesignUseCase,
    private val toggleWallpaperFavoriteUseCase: ToggleWallpaperFavoriteUseCase,
    private val toggleDiyFavoriteUseCase: ToggleDiyFavoriteUseCase,
    private val previewCoordinator: GridPreviewCoordinator
) : ViewModel() {
    private var favoriteItems: List<HomeFeedItem> = emptyList()
    private var viewportState = PreviewViewportState()

    private val _uiState = MutableStateFlow(DashboardCollectionUiState())
    val uiState: StateFlow<DashboardCollectionUiState> = _uiState.asStateFlow()

    init {
        observeDesigns()
    }

    fun onFilterSelected(filter: CollectionFilter) {
        _uiState.update { state ->
            state.copy(selectedFilter = filter)
        }
    }

    fun onDesignSelected(designId: String) {
        _uiState.update { state ->
            state.copy(pendingOpenDesignId = designId)
        }
    }

    fun consumePendingOpenDesign() {
        _uiState.update { state ->
            state.copy(pendingOpenDesignId = null)
        }
    }

    fun onViewportChanged(
        visibleItems: Map<String, PreviewVisibilityInfo>,
        isScrolling: Boolean
    ) {
        viewportState = PreviewViewportState(
            visibleItems = visibleItems,
            isScrolling = isScrolling
        )
        _uiState.update { state ->
            val activeState = previewCoordinator.computeActivePreviews(
                items = favoriteItems,
                viewportState = viewportState
            )
            state.copy(
                visibleItems = visibleItems,
                isGridScrolling = isScrolling,
                activeVideoIds = activeState.activeVideoIds,
                activeDiyAnimationIds = activeState.activeDiyAnimationIds
            )
        }
    }

    fun onWallpaperFavoriteClick(itemId: String) {
        viewModelScope.launch {
            when (val result = toggleWallpaperFavoriteUseCase(itemId)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun onDiyFavoriteClick(templateId: String) {
        viewModelScope.launch {
            when (val result = toggleDiyFavoriteUseCase(templateId)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun renameDesign(designId: String, title: String) {
        viewModelScope.launch {
            when (val result = renameDesignUseCase(designId, title.trim())) {
                is AppResult.Success -> {
                    _uiState.update { state -> state.copy(errorMessage = null) }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun deleteDesign(designId: String) {
        viewModelScope.launch {
            when (val result = deleteDesignUseCase(designId)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = null,
                            pendingOpenDesignId = state.pendingOpenDesignId.takeUnless { it == designId }
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    private fun observeDesigns() {
        viewModelScope.launch {
            combine(
                observeUserDesignsUseCase(),
                observeFavoriteFeedItemsUseCase()
            ) { designs, favorites -> designs to favorites }
                .collect { (designs, favorites) ->
                favoriteItems = favorites
                val activeState = previewCoordinator.computeActivePreviews(
                    items = favorites,
                    viewportState = viewportState
                )
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        designCount = designs.size,
                        favoriteCount = favorites.size,
                        favorites = favorites,
                        designs = designs,
                        activeVideoIds = activeState.activeVideoIds,
                        activeDiyAnimationIds = activeState.activeDiyAnimationIds,
                        errorMessage = null
                    )
                }
            }
        }
    }
}
