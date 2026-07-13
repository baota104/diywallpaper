package com.example.diywallpaper.ui.feature.preview.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewItemKind
import com.example.diywallpaper.domain.model.preview.WallpaperSource
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import com.example.diywallpaper.domain.model.preview.primaryAction
import com.example.diywallpaper.domain.model.preview.toPlayableSource
import com.example.diywallpaper.domain.usecase.design.GetUserDesignUseCase
import com.example.diywallpaper.domain.usecase.preview.GetPreviewCarouselItemsUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveWallpaperUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetStaticWallpaperUseCase
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
class DevicePreviewViewModel @Inject constructor(
    private val getUserDesignUseCase: GetUserDesignUseCase,
    private val getPreviewCarouselItemsUseCase: GetPreviewCarouselItemsUseCase,
    private val setStaticWallpaperUseCase: SetStaticWallpaperUseCase,
    private val setLiveWallpaperUseCase: SetLiveWallpaperUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DevicePreviewUiState())
    val uiState: StateFlow<DevicePreviewUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun bind(args: PreviewArgs) {
        observeJob?.cancel()
        _uiState.value = DevicePreviewUiState(
            isLoading = true,
            categoryId = args.categoryId,
            itemId = args.initialItemId
        )

        if (args.categoryId == COLLECTION_PREVIEW_CATEGORY_ID) {
            bindSavedDesign(args.initialItemId)
            return
        }

        observeJob = viewModelScope.launch {
            getPreviewCarouselItemsUseCase(
                categoryId = args.categoryId,
                initialItemId = args.initialItemId,
                sourceType = args.sourceType
            ).collect { data ->
                val currentItem = data.items.firstOrNull { it.id == args.initialItemId }
                    ?: data.items.getOrNull(data.initialIndex)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentItem = currentItem,
                    playableSource = currentItem?.toPlayableSource(),
                    primaryAction = currentItem?.primaryAction(),
                    designExportPath = null,
                    launchIntent = null,
                    successMessage = null,
                    errorMessage = if (currentItem == null) "Preview item is no longer available." else null
                )
            }
        }
    }

    fun onToggleChrome() {
        _uiState.update { state ->
            state.copy(isChromeVisible = !state.isChromeVisible)
        }
    }

    fun onApplyStarted() {
        _uiState.update { state ->
            state.copy(
                isApplyingWallpaper = true,
                launchIntent = null,
                successMessage = null,
                errorMessage = null
            )
        }
    }

    fun onApplyFinished(
        successMessage: String? = null,
        errorMessage: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                isApplyingWallpaper = false,
                successMessage = successMessage,
                errorMessage = errorMessage
            )
        }
    }

    fun onLaunchIntentConsumed() {
        _uiState.update { state ->
            state.copy(launchIntent = null)
        }
    }

    fun dismissTargetDialog() {
        _uiState.update { it.copy(showTargetDialog = false) }
    }

    fun onApplyClick() {
        val state = _uiState.value
        val action = state.primaryAction ?: return

        when (action) {
            PreviewPrimaryAction.SET_WALLPAPER -> {
                _uiState.update { it.copy(showTargetDialog = true) }
            }

            PreviewPrimaryAction.SET_LIVE_WALLPAPER -> {
                val currentItem = state.currentItem ?: return
                val videoUrl = currentItem.wallpaperSource?.videoUrl
                    ?: run {
                        onApplyFinished(
                            errorMessage = "Live wallpaper video is unavailable for this item."
                        )
                        return
                    }

                viewModelScope.launch {
                    onApplyStarted()
                    when (
                        val result = setLiveWallpaperUseCase(
                            source = WallpaperApplySource.LiveVideo(
                                itemId = currentItem.id,
                                videoUrl = videoUrl
                            )
                        )
                    ) {
                        is AppResult.Success -> {
                            _uiState.update { currentState ->
                                currentState.copy(
                                    isApplyingWallpaper = false,
                                    launchIntent = result.data,
                                    successMessage = "Live wallpaper is ready. Choose where to apply it.",
                                    errorMessage = null
                                )
                            }
                        }

                        is AppResult.Error -> onApplyFinished(
                            errorMessage = result.error.toPreviewMessage()
                        )
                    }
                }
            }

            PreviewPrimaryAction.EDIT_DIY,
            PreviewPrimaryAction.CREATE_FROM_SCRATCH -> {
                onApplyFinished(errorMessage = "This action is not available yet.")
            }
        }
    }

    fun applyStaticWallpaper(target: WallpaperTarget) {
        val state = _uiState.value
        val currentItem = state.currentItem ?: return
        val imageUrl = currentItem.wallpaperSource?.imageUrl
            ?: currentItem.thumbUrl.takeIf { it.isNotBlank() }
            ?: run {
                onApplyFinished(
                    errorMessage = "Wallpaper image is unavailable for this item."
                )
                return
            }

        _uiState.update { it.copy(showTargetDialog = false) }

        viewModelScope.launch {
            onApplyStarted()
            when (
                val result = setStaticWallpaperUseCase(
                    source = WallpaperApplySource.StaticImage(
                        itemId = currentItem.id,
                        imageUrl = imageUrl,
                        localPath = state.designExportPath
                    ),
                    target = target
                )
            ) {
                is AppResult.Success -> onApplyFinished(
                    successMessage = "Wallpaper applied successfully."
                )
                is AppResult.Error -> onApplyFinished(
                    errorMessage = result.error.toPreviewMessage()
                )
            }
        }
    }

    private fun bindSavedDesign(designId: String) {
        observeJob = viewModelScope.launch {
            when (val result = getUserDesignUseCase(designId)) {
                is AppResult.Success -> {
                    val design = result.data
                    val previewPath = design.previewPath ?: design.thumbnailPath ?: design.exportedImagePath
                    val exportPath = design.exportedImagePath ?: previewPath
                    val currentItem = if (previewPath != null && exportPath != null) {
                        PreviewCatalogItem(
                            id = design.id,
                            categoryId = COLLECTION_PREVIEW_CATEGORY_ID,
                            rank = 0,
                            title = design.title ?: "My Design",
                            kind = PreviewItemKind.WALLPAPER_STATIC,
                            thumbUrl = previewPath,
                            isFavorite = false,
                            wallpaperSource = WallpaperSource(
                                imageUrl = previewPath,
                                videoUrl = null
                            )
                        )
                    } else {
                        null
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentItem = currentItem,
                        playableSource = currentItem?.toPlayableSource(),
                        primaryAction = currentItem?.primaryAction(),
                        designExportPath = exportPath,
                        launchIntent = null,
                        successMessage = null,
                        errorMessage = if (currentItem == null) {
                            "Design preview is not ready yet."
                        } else {
                            null
                        }
                    )
                }

                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentItem = null,
                        playableSource = null,
                        primaryAction = null,
                        designExportPath = null,
                        errorMessage = result.error.toPreviewMessage()
                    )
                }
            }
        }
    }

    private fun AppError.toPreviewMessage(): String {
        return when (this) {
            AppError.NetworkUnavailable -> "No internet connection. Please try again."
            AppError.Timeout -> "The wallpaper request timed out. Please try again."
            AppError.EmptyResponse -> "Wallpaper data is empty. Please try another item."
            is AppError.HttpError -> "Could not load wallpaper from server."
            is AppError.JsonParseError -> "Wallpaper data is invalid. Please try another item."
            is AppError.InvalidDataContract -> "Wallpaper data is incomplete. Please try another item."
            is AppError.AssetLoadError -> "Unable to download wallpaper image."
            is AppError.VideoLoadError -> "Unable to prepare live wallpaper video."
            is AppError.ExportError -> "Unable to prepare wallpaper for this device."
            is AppError.StorageError -> "Unable to save wallpaper on this device."
            is AppError.Unknown -> "Something went wrong while applying wallpaper."
        }
    }

    private companion object {
        const val COLLECTION_PREVIEW_CATEGORY_ID = "collection"
    }
}
