package com.example.diywallpaper.ui.feature.dashboard.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.HomeFeedCategory
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.usecase.diy.ToggleDiyFavoriteUseCase
import com.example.diywallpaper.domain.usecase.home.GetHomeFeedUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.ToggleWallpaperFavoriteUseCase
import com.example.diywallpaper.ui.preview.core.GridPreviewCoordinator
import com.example.diywallpaper.ui.preview.core.PreviewViewportState
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardHomeViewModel @Inject constructor(
    private val getHomeFeedUseCase: GetHomeFeedUseCase,
    private val toggleWallpaperFavoriteUseCase: ToggleWallpaperFavoriteUseCase,
    private val toggleDiyFavoriteUseCase: ToggleDiyFavoriteUseCase,
    private val previewCoordinator: GridPreviewCoordinator
) : ViewModel() {
    private var allCategories: List<HomeFeedCategory> = emptyList()
    private var viewportState = PreviewViewportState()

    private val _uiState = MutableStateFlow(DashboardHomeUiState())
    val uiState: StateFlow<DashboardHomeUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isInitialLoading = state.categories.isEmpty(),
                    isRefreshing = state.categories.isNotEmpty(),
                    errorMessage = null
                )
            }
            when (val result = getHomeFeedUseCase.refresh()) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }

                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            isInitialLoading = false,
                            isRefreshing = false,
                            errorMessage = result.error.toString().substringBefore("(")
                        )
                    }
                }
            }
        }
    }

    fun onCategorySelected(categoryId: String) {
        _uiState.update { state ->
            val selectedCategoryId = resolveSelectedCategoryId(categoryId)
            state.copy(
                categories = allCategories,
                selectedCategoryId = selectedCategoryId,
                items = buildItems(selectedCategoryId),
                activeVideoIds = buildActivePreviewState(selectedCategoryId).activeVideoIds,
                activeDiyAnimationIds = buildActivePreviewState(selectedCategoryId).activeDiyAnimationIds
            )
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
            val activeState = buildActivePreviewState(state.selectedCategoryId)
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

    private fun observeCategories() {
        viewModelScope.launch {
            getHomeFeedUseCase().collect { categories ->
                allCategories = categories.sortedBy { it.rank }
                _uiState.update { state ->
                    val selectedCategoryId = resolveSelectedCategoryId(state.selectedCategoryId)
                    state.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        categories = allCategories,
                        selectedCategoryId = selectedCategoryId,
                        items = buildItems(selectedCategoryId),
                        activeVideoIds = buildActivePreviewState(selectedCategoryId).activeVideoIds,
                        activeDiyAnimationIds = buildActivePreviewState(selectedCategoryId).activeDiyAnimationIds,
                        errorMessage = if (categories.isNotEmpty()) null else state.errorMessage
                    )
                }
            }
        }
    }

    private fun resolveSelectedCategoryId(requestedCategoryId: String?): String? {
        val availableIds = buildAvailableCategoryIds()
        return when {
            availableIds.isEmpty() -> null
            requestedCategoryId != null && requestedCategoryId in availableIds -> requestedCategoryId
            else -> ALL_CATEGORY_ID
        }
    }

    private fun buildAvailableCategoryIds(): Set<String> {
        if (allCategories.isEmpty()) return emptySet()
        return buildSet {
            add(ALL_CATEGORY_ID)
            allCategories.mapTo(this) { it.id }
        }
    }

    private fun buildItems(selectedCategoryId: String?): List<HomeFeedItem> {
        if (selectedCategoryId == null) return emptyList()

        val items = when (selectedCategoryId) {
            ALL_CATEGORY_ID -> allCategories
                .flatMap { category -> category.items }
            else -> allCategories
                .firstOrNull { it.id == selectedCategoryId }
                ?.items
                .orEmpty()
        }

        return items
            .sortedWith(compareBy<HomeFeedItem> { it.rank }.thenBy { it.id })
    }

    private fun buildActivePreviewState(selectedCategoryId: String?) =
        previewCoordinator.computeActivePreviews(
            items = buildItems(selectedCategoryId),
            viewportState = viewportState
        )

    companion object {
        private const val ALL_CATEGORY_ID = "all"
    }
}
