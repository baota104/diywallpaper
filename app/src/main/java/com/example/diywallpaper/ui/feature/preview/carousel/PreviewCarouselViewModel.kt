package com.example.diywallpaper.ui.feature.preview.carousel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.domain.model.preview.primaryAction
import com.example.diywallpaper.domain.usecase.preview.GetPreviewCarouselItemsUseCase
import com.example.diywallpaper.domain.usecase.preview.PreviewCarouselPlaybackPolicy
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PreviewCarouselViewModel @Inject constructor(
    private val getPreviewCarouselItemsUseCase: GetPreviewCarouselItemsUseCase,
    private val playbackPolicy: PreviewCarouselPlaybackPolicy
) : ViewModel() {
    private val _uiState = MutableStateFlow(PreviewCarouselUiState())
    val uiState: StateFlow<PreviewCarouselUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun bind(args: PreviewArgs) {
        if (_uiState.value.categoryId == args.categoryId && _uiState.value.items.isNotEmpty()) return

        observeJob?.cancel()
        _uiState.value = PreviewCarouselUiState(
            isLoading = true,
            categoryId = args.categoryId
        )

        observeJob = viewModelScope.launch {
            getPreviewCarouselItemsUseCase(
                categoryId = args.categoryId,
                initialItemId = args.initialItemId,
                sourceType = args.sourceType
            ).collect { data ->
                val playbackWindow = playbackPolicy.resolve(
                    items = data.items,
                    currentIndex = data.initialIndex
                )
                val currentItem = data.items.getOrNull(data.initialIndex)
                _uiState.value = PreviewCarouselUiState(
                    isLoading = false,
                    categoryId = args.categoryId,
                    currentIndex = data.initialIndex,
                    initialIndex = data.initialIndex,
                    items = data.items,
                    primaryItemId = playbackWindow.primaryItemId,
                    activePlaybackIds = playbackWindow.activeItemIds,
                    neighborPlaybackIds = playbackWindow.neighborItemIds,
                    currentAction = currentItem?.primaryAction()
                )
            }
        }
    }

    fun onPageSettled(index: Int) {
        _uiState.update { state ->
            if (state.items.isEmpty()) return@update state

            val safeIndex = index.coerceIn(state.items.indices)
            val playbackWindow = playbackPolicy.resolve(
                items = state.items,
                currentIndex = safeIndex
            )
            val currentItem = state.items[safeIndex]
            state.copy(
                currentIndex = safeIndex,
                primaryItemId = playbackWindow.primaryItemId,
                activePlaybackIds = playbackWindow.activeItemIds,
                neighborPlaybackIds = playbackWindow.neighborItemIds,
                currentAction = currentItem.primaryAction()
            )
        }
    }

    fun onFavoriteChanged(updatedItemId: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == updatedItemId) {
                        item.copy(isFavorite = !item.isFavorite)
                    } else {
                        item
                    }
                }
            )
        }
    }
}
