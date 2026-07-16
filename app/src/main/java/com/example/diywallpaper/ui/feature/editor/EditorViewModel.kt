package com.example.diywallpaper.ui.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.model.design.hasAnimatedContent
import com.example.diywallpaper.domain.usecase.design.CreateDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.GenerateDesignAssetsUseCase
import com.example.diywallpaper.domain.usecase.design.GetDesignProjectUseCase
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.domain.usecase.design.GetUserDesignUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import com.example.diywallpaper.domain.usecase.design.SaveDesignProjectUseCase
import com.example.diywallpaper.domain.usecase.design.UpdateDesignAssetsUseCase
import com.example.diywallpaper.domain.usecase.sticker.GetStickersUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.GetBackgroundCreateItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val createDesignDraftUseCase: CreateDesignDraftUseCase,
    private val getUserDesignUseCase: GetUserDesignUseCase,
    private val getDesignProjectUseCase: GetDesignProjectUseCase,
    private val saveDesignProjectUseCase: SaveDesignProjectUseCase,
    private val generateDesignAssetsUseCase: GenerateDesignAssetsUseCase,
    private val renameDesignUseCase: RenameDesignUseCase,
    private val updateDesignAssetsUseCase: UpdateDesignAssetsUseCase,
    private val deleteDesignUseCase: DeleteDesignUseCase,
    private val getEditorTextLibraryUseCase: GetEditorTextLibraryUseCase,
    private val getBackgroundCreateItemsUseCase: GetBackgroundCreateItemsUseCase,
    private val getStickersUseCase: GetStickersUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var historyState = EditorHistoryState()
    private var autosaveJob: Job? = null
    private var currentTitle: String? = null
    private var currentThumbnailPath: String? = null
    private var currentPreviewPath: String? = null
    private var currentExportedImagePath: String? = null
    private var activeBrushConfig: BrushToolConfig? = null
    private var activeTextBrushConfig: TextBrushToolConfig? = null
    private var activeBrushSessionLayerId: String? = null

    init {
        loadTextLibrary()
        observeBackgroundCatalog()
        observeStickerCatalog()
        refreshAssetCatalogs()
    }

    fun loadDesign(designId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, saveMessage = null) }
            when (val designResult = getUserDesignUseCase(designId)) {
                is AppResult.Success -> {
                    syncMetadata(
                        title = designResult.data.title,
                        thumbnailPath = designResult.data.thumbnailPath,
                        previewPath = designResult.data.previewPath,
                        exportedImagePath = designResult.data.exportedImagePath
                    )
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = designResult.error.toString().substringBefore("(")
                        )
                    }
                    return@launch
                }
            }
            when (val result = getDesignProjectUseCase(designId)) {
                is AppResult.Success -> {
                    historyState = EditorHistoryState(current = result.data)
                    _uiState.update { it.copy(isPersisted = true) }
                    _uiState.value = result.data.toUiState(historyState)
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.toString().substringBefore("(")
                        )
                    }
                }
            }
        }
    }

    fun startNewProject(
        project: EditorProject,
        title: String? = null
    ) {
        syncMetadata(title = title)
        historyState = EditorHistoryState(current = project)
        _uiState.update { it.copy(isPersisted = false) }
        _uiState.value = project.toUiState(historyState)
    }

    fun bindProject(project: EditorProject) {
        syncMetadata()
        historyState = EditorHistoryState(current = project)
        _uiState.update { it.copy(isPersisted = true) }
        _uiState.value = project.toUiState(historyState)
    }

    fun setActiveTool(tool: EditorTool) {
        val resolvedTool = if (tool == EditorTool.BRUSH_DRAW && activeBrushConfig?.erase == true) {
            EditorTool.BRUSH_ERASE
        } else {
            tool
        }
        val normalizedBrushConfig = when (resolvedTool) {
            EditorTool.BRUSH_DRAW -> (activeBrushConfig ?: defaultBrushConfig()).copy(erase = false)
            EditorTool.BRUSH_ERASE -> (activeBrushConfig ?: defaultBrushConfig()).copy(erase = true)
            else -> activeBrushConfig
        }
        if (normalizedBrushConfig != activeBrushConfig) {
            activeBrushConfig = normalizedBrushConfig
        }
        val nextOpenedToolSheet = when {
            resolvedTool == EditorTool.PREVIEW -> null
            _uiState.value.openedToolSheet == resolvedTool -> null
            else -> resolvedTool
        }
        if (nextOpenedToolSheet != EditorTool.BRUSH_DRAW && nextOpenedToolSheet != EditorTool.BRUSH_ERASE) {
            activeBrushSessionLayerId = null
        }
        _uiState.update { state ->
            state.copy(
                activeTool = resolvedTool,
                openedToolSheet = nextOpenedToolSheet,
                activeBrushSessionLayerId = activeBrushSessionLayerId,
                activeBrushConfig = activeBrushConfig,
                isPreviewMode = false,
                saveMessage = null
            )
        }
    }

    fun dismissToolSheet() {
        activeBrushSessionLayerId = null
        _uiState.update { state ->
            state.copy(
                openedToolSheet = null,
                activeBrushSessionLayerId = null
            )
        }
    }

    fun configureBrushTool(
        erase: Boolean,
        colorHex: String,
        brushSize: Float,
        preset: BrushPresetType = activeBrushConfig?.preset ?: BrushPresetType.SOLID,
        patternBrushName: String? = null
    ) {
        val config = BrushToolConfig(
            erase = erase,
            colorHex = colorHex,
            brushSize = brushSize,
            preset = preset.takeUnless { erase } ?: BrushPresetType.SOLID,
            patternBrushName = patternBrushName
        )
        activeBrushConfig = config
        activeTextBrushConfig = null
        _uiState.update {
            activeBrushSessionLayerId = null
            it.copy(
                activeTool = if (erase) EditorTool.BRUSH_ERASE else EditorTool.BRUSH_DRAW,
                openedToolSheet = null,
                activeBrushSessionLayerId = null,
                activeBrushConfig = activeBrushConfig,
                activeTextBrushConfig = null
            )
        }
    }

    fun updateBrushToolConfig(
        erase: Boolean,
        colorHex: String,
        brushSize: Float,
        preset: BrushPresetType = activeBrushConfig?.preset ?: BrushPresetType.SOLID,
        patternBrushName: String? = activeBrushConfig?.patternBrushName
    ) {
        activeBrushConfig = BrushToolConfig(
            erase = erase,
            colorHex = colorHex,
            brushSize = brushSize,
            preset = preset.takeUnless { erase } ?: BrushPresetType.SOLID,
            patternBrushName = patternBrushName.takeUnless { erase }
        )
        activeTextBrushConfig = null
        _uiState.update {
            it.copy(
                activeTool = if (erase) EditorTool.BRUSH_ERASE else EditorTool.BRUSH_DRAW,
                activeBrushSessionLayerId = activeBrushSessionLayerId,
                activeBrushConfig = activeBrushConfig,
                activeTextBrushConfig = null
            )
        }
    }

    fun configureTextBrushTool(
        text: String,
        style: TextStyleSpec,
        spacing: Float
    ) {
        activeTextBrushConfig = TextBrushToolConfig(
            text = text,
            style = style,
            spacing = spacing
        )
        activeBrushSessionLayerId = null
        activeBrushConfig = null
        _uiState.update {
            it.copy(
                activeTool = EditorTool.TEXT_BRUSH,
                openedToolSheet = null,
                activeBrushSessionLayerId = null,
                activeBrushConfig = null,
                activeTextBrushConfig = activeTextBrushConfig
            )
        }
    }

    fun updateTextBrushToolConfig(
        text: String,
        style: TextStyleSpec,
        spacing: Float
    ) {
        activeTextBrushConfig = TextBrushToolConfig(
            text = text,
            style = style,
            spacing = spacing
        )
        activeBrushSessionLayerId = null
        activeBrushConfig = null
        _uiState.update {
            it.copy(
                activeTool = EditorTool.TEXT_BRUSH,
                activeBrushSessionLayerId = null,
                activeBrushConfig = null,
                activeTextBrushConfig = activeTextBrushConfig
            )
        }
    }

    fun commitCanvasStroke(points: List<StrokePoint>) {
        if (points.size < 2) return
        when (_uiState.value.activeTool) {
            EditorTool.BRUSH_DRAW,
            EditorTool.BRUSH_ERASE -> {
                val config = activeBrushConfig ?: return
                if (config.erase) {
                    addEraseStroke(points = points, strokeWidth = config.brushSize)
                } else {
                    addBrushStroke(
                        points = points,
                        colorHex = config.colorHex,
                        brushStyle = config.toBrushStyleSpec(),
                        strokeWidth = config.brushSize
                    )
                }
            }

            EditorTool.TEXT_BRUSH -> {
                val config = activeTextBrushConfig ?: return
                addTextTrail(
                    text = config.text,
                    style = config.style,
                    points = points,
                    spacing = config.spacing
                )
            }

            else -> Unit
        }
    }

    fun selectLayer(layerId: String?) {
        mutateProject { project ->
            project.copy(selectedLayerId = layerId, updatedAt = now())
        }
    }

    fun updateBackground(background: EditorBackground) {
        mutateProject { project ->
            project.copy(background = background, updatedAt = now())
        }
    }

    fun applyApiBackground(item: BackgroundCreateItem) {
        updateBackground(
            EditorBackground.ApiImage(
                backgroundId = item.id,
                imageUrl = item.imageUrl
            )
        )
    }

    fun applySolidBackground(colorHex: String) {
        updateBackground(EditorBackground.SolidColor(colorHex))
    }

    fun applyGradientBackground(colors: List<String>) {
        updateBackground(EditorBackground.Gradient(colors))
    }

    fun applyLocalImageBackground(localPath: String, crop: CropSpec? = null) {
        updateBackground(
            EditorBackground.LocalImage(
                localPath = localPath,
                crop = crop
            )
        )
    }

    fun upsertLayer(layer: EditorLayer) {
        mutateProject { project ->
            val existingIndex = project.layers.indexOfFirst { it.id == layer.id }
            val updatedLayers = if (existingIndex >= 0) {
                project.layers.toMutableList().apply { set(existingIndex, layer) }
            } else {
                (project.layers + layer).toMutableList()
            }
            project.copy(
                layers = updatedLayers.sortedBy { it.zIndex },
                selectedLayerId = layer.id,
                updatedAt = now()
            )
        }
    }

    fun removeLayer(layerId: String) {
        mutateProject { project ->
            project.copy(
                layers = project.layers.filterNot { it.id == layerId },
                selectedLayerId = project.selectedLayerId.takeUnless { it == layerId },
                updatedAt = now()
            )
        }
    }

    fun removeSelectedLayer() {
        val layerId = _uiState.value.selectedLayerId ?: return
        removeLayer(layerId)
    }

    fun bringSelectedLayerForward() {
        reorderSelectedLayer { layers, selectedIndex ->
            if (selectedIndex >= layers.lastIndex) layers else layers.swap(selectedIndex, selectedIndex + 1)
        }
    }

    fun sendSelectedLayerBackward() {
        reorderSelectedLayer { layers, selectedIndex ->
            if (selectedIndex <= 0) layers else layers.swap(selectedIndex, selectedIndex - 1)
        }
    }

    fun bringSelectedLayerToFront() {
        reorderSelectedLayer { layers, selectedIndex ->
            if (selectedIndex >= layers.lastIndex) {
                layers
            } else {
                buildList {
                    addAll(layers.filterIndexed { index, _ -> index != selectedIndex })
                    add(layers[selectedIndex])
                }
            }
        }
    }

    fun sendSelectedLayerToBack() {
        reorderSelectedLayer { layers, selectedIndex ->
            if (selectedIndex <= 0) {
                layers
            } else {
                buildList {
                    add(layers[selectedIndex])
                    addAll(layers.filterIndexed { index, _ -> index != selectedIndex })
                }
            }
        }
    }

    fun moveLayer(layerId: String, targetIndex: Int) {
        mutateProject { project ->
            val sortedLayers = project.layers.sortedBy { it.zIndex }
            val fromIndex = sortedLayers.indexOfFirst { it.id == layerId }
            if (fromIndex < 0) {
                project
            } else {
                val boundedTargetIndex = targetIndex.coerceIn(0, sortedLayers.lastIndex)
                if (fromIndex == boundedTargetIndex) {
                    project
                } else {
                    val mutable = sortedLayers.toMutableList()
                    val moved = mutable.removeAt(fromIndex)
                    mutable.add(boundedTargetIndex, moved)
                    project.copy(
                        layers = mutable.reindexLayers(),
                        selectedLayerId = layerId,
                        updatedAt = now()
                    )
                }
            }
        }
    }

    fun addTextLayer(
        text: String,
        style: TextStyleSpec,
        transform: LayerTransform = LayerTransform(0f, 0f, 1f, 0f)
    ) {
        val layer = TextLayer(
            id = generateLayerId("text"),
            text = text.take(EDITOR_TEXT_MAX_LENGTH),
            style = style,
            zIndex = nextZIndex(),
            transform = transform,
            isLocked = false,
            isHidden = false
        )
        upsertLayer(layer)
    }

    fun updateTextLayer(
        layerId: String,
        text: String? = null,
        style: TextStyleSpec? = null
    ) {
        mutateProject { project ->
            val updatedLayers = project.layers.map { layer ->
                if (layer is TextLayer && layer.id == layerId) {
                    layer.copy(
                        text = (text ?: layer.text).take(EDITOR_TEXT_MAX_LENGTH),
                        style = style ?: layer.style
                    )
                } else {
                    layer
                }
            }
            project.copy(
                layers = updatedLayers,
                selectedLayerId = layerId,
                updatedAt = now()
            )
        }
    }

    fun updateLayerTransform(
        layerId: String,
        offsetXDelta: Float = 0f,
        offsetYDelta: Float = 0f,
        scaleMultiplier: Float = 1f,
        rotationDelta: Float = 0f
    ) {
        mutateProject { project ->
            val updatedLayers = project.layers.map { layer ->
                if (layer.id == layerId) {
                    layer.withTransform(
                        layer.transform.copy(
                            offsetX = layer.transform.offsetX + offsetXDelta,
                            offsetY = layer.transform.offsetY + offsetYDelta,
                            scale = (layer.transform.scale * scaleMultiplier).coerceIn(0.35f, 4f),
                            rotation = layer.transform.rotation + rotationDelta
                        )
                    )
                } else {
                    layer
                }
            }
            project.copy(
                layers = updatedLayers,
                selectedLayerId = layerId,
                updatedAt = now()
            )
        }
    }

    fun addStickerLayer(
        sticker: StickerItem,
        transform: LayerTransform = LayerTransform(0f, 0f, 1f, 0f)
    ) {
        upsertLayer(
            StickerLayer(
                id = generateLayerId("sticker"),
                stickerId = sticker.id,
                assetPathOrUrl = sticker.thumbnailUrl.ifBlank { sticker.stickerUrl },
                animatedAssetPathOrUrl = sticker.stickerUrl.takeIf { sticker.isAnimated },
                isAnimated = sticker.isAnimated,
                zIndex = nextZIndex(),
                transform = transform,
                isLocked = false,
                isHidden = false
            )
        )
    }

    fun addPhotoLayer(
        localPath: String,
        crop: CropSpec? = null,
        transform: LayerTransform = LayerTransform(120f, 220f, 1f, 0f)
    ) {
        upsertLayer(
            PhotoLayer(
                id = generateLayerId("photo"),
                localPath = localPath,
                crop = crop,
                zIndex = nextZIndex(),
                transform = transform,
                isLocked = false,
                isHidden = false
            )
        )
    }

    fun addTextPresetLayer(preset: EditorTextPreset) {
        addTextLayer(
            text = preset.previewText.take(EDITOR_TEXT_MAX_LENGTH),
            style = preset.style
        )
    }

    fun addBrushStroke(
        points: List<StrokePoint>,
        colorHex: String? = null,
        brushStyle: BrushStyleSpec? = null,
        strokeWidth: Float
    ) {
        val item = BrushStackItem.Draw(
            BrushStroke(
                points = points,
                colorHex = colorHex,
                brushStyle = brushStyle,
                strokeWidth = strokeWidth
            )
        )
        if (appendBrushStackItem(item)) return
        addDrawLayer(
            DrawLayerData.FreeStroke(
                item.stroke
            )
        )
    }

    fun addEraseStroke(
        points: List<StrokePoint>,
        strokeWidth: Float
    ) {
        val item = BrushStackItem.Erase(
            BrushStroke(
                points = points,
                strokeWidth = strokeWidth
            )
        )
        appendBrushStackItem(item)
    }

    fun addStickerTrail(
        sticker: StickerItem,
        points: List<StrokePoint>,
        spacing: Float,
        stampSize: Float,
        rotationMode: StickerTrailRotationMode = StickerTrailRotationMode.FIXED
    ) {
        addDrawLayer(
            DrawLayerData.StickerTrail(
                stickerAssetPathOrUrl = sticker.stickerUrl,
                points = points,
                spacing = spacing,
                stampSize = stampSize,
                rotationMode = rotationMode
            )
        )
    }

    fun addTextTrail(
        text: String,
        style: TextStyleSpec,
        points: List<StrokePoint>,
        spacing: Float
    ) {
        addDrawLayer(
            DrawLayerData.TextTrail(
                text = text,
                textStyle = style,
                points = points,
                spacing = spacing
            )
        )
    }

    fun enterPreviewMode() {
        _uiState.update {
            it.copy(
                activeTool = EditorTool.PREVIEW,
                openedToolSheet = null,
                isPreviewMode = true
            )
        }
    }

    fun exitPreviewMode() {
        _uiState.update {
            it.copy(isPreviewMode = false)
        }
    }

    fun undo() {
        val current = historyState.current ?: return
        val previous = historyState.undoStack.lastOrNull() ?: return
        historyState = historyState.copy(
            undoStack = historyState.undoStack.dropLast(1),
            current = previous,
            redoStack = historyState.redoStack + current
        )
        _uiState.value = previous.toUiState(historyState)
        if (_uiState.value.isPersisted) {
            scheduleAutosave(previous)
        }
    }

    fun redo() {
        val current = historyState.current ?: return
        val next = historyState.redoStack.lastOrNull() ?: return
        historyState = historyState.copy(
            undoStack = historyState.undoStack + current,
            current = next,
            redoStack = historyState.redoStack.dropLast(1)
        )
        _uiState.value = next.toUiState(historyState)
        if (_uiState.value.isPersisted) {
            scheduleAutosave(next)
        }
    }

    fun saveNow() {
        if (_uiState.value.isPersisted) {
            historyState.current?.let(::persistProject)
        }
    }

    fun preparePreviewNavigation() {
        val project = historyState.current ?: return
        viewModelScope.launch {
            autosaveJob?.cancel()
            _uiState.update {
                it.copy(
                    isSaving = true,
                    isGeneratingAssets = true,
                    errorMessage = null,
                    saveMessage = null,
                    pendingPreviewDesignId = null
                )
            }
            val persistedProject = when (val persistResult = ensurePersistedProject(project)) {
                is AppResult.Success -> persistResult.data
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = persistResult.error.toString().substringBefore("(")
                        )
                    }
                    return@launch
                }
            }
            val designId = persistedProject.id

            when (val saveResult = saveDesignProjectUseCase(persistedProject, currentTitle)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = saveResult.error.toString().substringBefore("(")
                        )
                    }
                    return@launch
                }
            }

            if (persistedProject.hasAnimatedContent()) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isGeneratingAssets = false,
                        pendingPreviewDesignId = designId,
                        saveMessage = SAVE_SUCCESS_MESSAGE,
                        errorMessage = null
                    )
                }
                return@launch
            }

            when (val result = generateDesignAssetsUseCase(persistedProject)) {
                is AppResult.Success -> {
                    when (
                        val assetUpdateResult = updateDesignAssetsUseCase(
                            designId = designId,
                            thumbnailPath = result.data.thumbnailPath,
                            previewPath = result.data.previewPath,
                            exportedImagePath = result.data.exportedImagePath
                        )
                    ) {
                        is AppResult.Success -> {
                            syncMetadata(
                                thumbnailPath = result.data.thumbnailPath,
                                previewPath = result.data.previewPath,
                                exportedImagePath = result.data.exportedImagePath
                            )
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isGeneratingAssets = false,
                                    thumbnailPath = result.data.thumbnailPath,
                                    previewPath = result.data.previewPath,
                                    exportedImagePath = result.data.exportedImagePath,
                                    pendingPreviewDesignId = designId,
                                    saveMessage = EXPORT_SUCCESS_MESSAGE,
                                    errorMessage = null
                                )
                            }
                        }

                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isGeneratingAssets = false,
                                    errorMessage = assetUpdateResult.error.toString().substringBefore("(")
                                )
                            }
                        }
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = result.error.toString().substringBefore("(")
                        )
                    }
                }
            }
        }
    }

    fun saveAndExit() {
        val project = historyState.current ?: return
        viewModelScope.launch {
            autosaveJob?.cancel()
            _uiState.update {
                it.copy(
                    isSaving = true,
                    isGeneratingAssets = true,
                    errorMessage = null,
                    saveMessage = null,
                    pendingExitAfterSave = false
                )
            }
            val persistedProject = when (val persistResult = ensurePersistedProject(project)) {
                is AppResult.Success -> persistResult.data
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = persistResult.error.toString().substringBefore("(")
                        )
                    }
                    return@launch
                }
            }
            val designId = persistedProject.id

            when (val saveResult = saveDesignProjectUseCase(persistedProject, currentTitle)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = saveResult.error.toString().substringBefore("(")
                        )
                    }
                    return@launch
                }
            }

            when (val result = generateDesignAssetsUseCase(persistedProject)) {
                is AppResult.Success -> {
                    when (
                        val assetUpdateResult = updateDesignAssetsUseCase(
                            designId = designId,
                            thumbnailPath = result.data.thumbnailPath,
                            previewPath = result.data.previewPath,
                            exportedImagePath = result.data.exportedImagePath
                        )
                    ) {
                        is AppResult.Success -> {
                            syncMetadata(
                                thumbnailPath = result.data.thumbnailPath,
                                previewPath = result.data.previewPath,
                                exportedImagePath = result.data.exportedImagePath
                            )
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isGeneratingAssets = false,
                                    thumbnailPath = result.data.thumbnailPath,
                                    previewPath = result.data.previewPath,
                                    exportedImagePath = result.data.exportedImagePath,
                                    pendingExitAfterSave = true,
                                    saveMessage = EXPORT_SUCCESS_MESSAGE,
                                    errorMessage = null
                                )
                            }
                        }

                        is AppResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isGeneratingAssets = false,
                                    errorMessage = assetUpdateResult.error.toString().substringBefore("(")
                                )
                            }
                        }
                    }
                }

                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isGeneratingAssets = false,
                            errorMessage = result.error.toString().substringBefore("(")
                        )
                    }
                }
            }
        }
    }

    fun consumePendingPreviewNavigation() {
        _uiState.update { it.copy(pendingPreviewDesignId = null) }
    }

    fun consumePendingExitAfterSave() {
        _uiState.update { it.copy(pendingExitAfterSave = false) }
    }

    fun renameDesign(title: String) {
        val designId = historyState.current?.id ?: return
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return

        viewModelScope.launch {
            when (val result = renameDesignUseCase(designId, normalizedTitle)) {
                is AppResult.Success -> {
                    syncMetadata(title = normalizedTitle)
                    _uiState.update {
                        it.copy(title = normalizedTitle, saveMessage = RENAME_SUCCESS_MESSAGE, errorMessage = null)
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun updateGeneratedAssets(
        thumbnailPath: String? = currentThumbnailPath,
        previewPath: String? = currentPreviewPath,
        exportedImagePath: String? = currentExportedImagePath
    ) {
        val designId = historyState.current?.id ?: return
        viewModelScope.launch {
            when (
                val result = updateDesignAssetsUseCase(
                    designId = designId,
                    thumbnailPath = thumbnailPath,
                    previewPath = previewPath,
                    exportedImagePath = exportedImagePath
                )
            ) {
                is AppResult.Success -> {
                    syncMetadata(
                        thumbnailPath = thumbnailPath,
                        previewPath = previewPath,
                        exportedImagePath = exportedImagePath
                    )
                    _uiState.update {
                        it.copy(
                            thumbnailPath = currentThumbnailPath,
                            previewPath = currentPreviewPath,
                            exportedImagePath = currentExportedImagePath,
                            saveMessage = EXPORT_SUCCESS_MESSAGE,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun deleteCurrentDesign() {
        val designId = historyState.current?.id ?: return
        autosaveJob?.cancel()
        viewModelScope.launch {
            when (val result = deleteDesignUseCase(designId)) {
                is AppResult.Success -> {
                    historyState = EditorHistoryState()
                    syncMetadata()
                    _uiState.value = EditorUiState(
                        projectId = designId,
                        isDeleted = true,
                        saveMessage = DELETE_SUCCESS_MESSAGE
                    )
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    private fun observeBackgroundCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBackgroundCatalog = true) }
            getBackgroundCreateItemsUseCase().collect { items ->
                _uiState.update {
                    it.copy(
                        availableBackgrounds = items,
                        isLoadingBackgroundCatalog = false
                    )
                }
            }
        }
    }

    private fun observeStickerCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStickerCatalog = true) }
            getStickersUseCase().collect { items ->
                _uiState.update {
                    it.copy(
                        availableStickers = items,
                        isLoadingStickerCatalog = false
                    )
                }
            }
        }
    }

    private fun refreshAssetCatalogs() {
        viewModelScope.launch {
            getBackgroundCreateItemsUseCase.refresh()
        }
        viewModelScope.launch {
            getStickersUseCase.refresh()
        }
    }

    private fun loadTextLibrary() {
        val library = getEditorTextLibraryUseCase()
        _uiState.update {
            it.copy(
                availableFonts = library.fonts,
                textPresets = library.presets
            )
        }
    }

    private fun mutateProject(transform: (EditorProject) -> EditorProject) {
        val current = historyState.current ?: return
        val updated = transform(current)
        if (updated == current) return

        historyState = historyState.copy(
            undoStack = historyState.undoStack + current,
            current = updated,
            redoStack = emptyList()
        )
        _uiState.value = updated.toUiState(historyState)
        if (_uiState.value.isPersisted) {
            scheduleAutosave(updated)
        }
    }

    private fun scheduleAutosave(project: EditorProject) {
        if (!_uiState.value.isPersisted) return
        autosaveJob?.cancel()
        _uiState.update { it.copy(isSaving = true, saveMessage = null) }
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DELAY_MS)
            persistProject(project)
        }
    }

    private suspend fun ensurePersistedProject(project: EditorProject): AppResult<EditorProject> {
        if (_uiState.value.isPersisted) return AppResult.Success(project)

        return when (val createResult = createDesignDraftUseCase(project, currentTitle)) {
            is AppResult.Success -> {
                val designId = createResult.data
                val persistedProject = project.copy(id = designId)
                historyState = historyState.copy(
                    undoStack = historyState.undoStack.map { it.copy(id = designId) },
                    current = persistedProject,
                    redoStack = historyState.redoStack.map { it.copy(id = designId) }
                )
                _uiState.update { state ->
                    state.copy(
                        projectId = persistedProject.id,
                        isPersisted = true
                    )
                }
                AppResult.Success(persistedProject)
            }

            is AppResult.Error -> createResult
        }
    }

    private fun persistProject(project: EditorProject) {
        autosaveJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveMessage = null, errorMessage = null) }
            when (val result = saveDesignProjectUseCase(project, currentTitle)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveMessage = SAVE_SUCCESS_MESSAGE) }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.error.toString().substringBefore("(")
                        )
                    }
                }
            }
        }
    }

    private fun addDrawLayer(drawData: DrawLayerData) {
        upsertLayer(
            DrawLayer(
                id = generateLayerId("draw"),
                drawData = drawData,
                zIndex = nextZIndex(),
                transform = LayerTransform(0f, 0f, 1f, 0f),
                isLocked = false,
                isHidden = false
            )
        )
    }

    private fun appendBrushStackItem(item: BrushStackItem): Boolean {
        val shouldUseSession = _uiState.value.openedToolSheet == EditorTool.BRUSH_DRAW ||
            _uiState.value.openedToolSheet == EditorTool.BRUSH_ERASE
        if (!shouldUseSession) return false

        mutateProject { project ->
            val existingSessionId = activeBrushSessionLayerId
            val existingSession = project.layers
                .filterIsInstance<DrawLayer>()
                .firstOrNull { layer ->
                    layer.id == existingSessionId && layer.drawData is DrawLayerData.BrushStack
                }

            if (existingSession != null) {
                val updatedLayers = project.layers.map { layer ->
                    if (layer.id == existingSession.id && layer is DrawLayer) {
                        val stack = layer.drawData as DrawLayerData.BrushStack
                        layer.copy(drawData = stack.copy(items = stack.items + item))
                    } else {
                        layer
                    }
                }
                project.copy(layers = updatedLayers, updatedAt = now())
            } else if (item is BrushStackItem.Draw) {
                val layerId = generateLayerId("brush_stack")
                activeBrushSessionLayerId = layerId
                val nextIndex = (project.layers.maxOfOrNull { it.zIndex } ?: 0) + 1
                project.copy(
                    layers = project.layers + DrawLayer(
                        id = layerId,
                        drawData = DrawLayerData.BrushStack(items = listOf(item)),
                        zIndex = nextIndex,
                        transform = LayerTransform(0f, 0f, 1f, 0f),
                        isLocked = false,
                        isHidden = false
                    ),
                    selectedLayerId = layerId,
                    updatedAt = now()
                )
            } else {
                // Eraser only affects the current brush session; it must never create its own layer.
                project
            }
        }
        return true
    }

    private fun reorderSelectedLayer(
        reorder: (layers: List<EditorLayer>, selectedIndex: Int) -> List<EditorLayer>
    ) {
        val selectedLayerId = _uiState.value.selectedLayerId ?: return
        mutateProject { project ->
            val sortedLayers = project.layers.sortedBy { it.zIndex }
            val selectedIndex = sortedLayers.indexOfFirst { it.id == selectedLayerId }
            if (selectedIndex < 0) {
                project
            } else {
                val reorderedLayers = reorder(sortedLayers, selectedIndex).reindexLayers()
                if (reorderedLayers == sortedLayers) {
                    project
                } else {
                    project.copy(
                        layers = reorderedLayers,
                        selectedLayerId = selectedLayerId,
                        updatedAt = now()
                    )
                }
            }
        }
    }

    private fun nextZIndex(): Int {
        return (historyState.current?.layers?.maxOfOrNull { it.zIndex } ?: 0) + 1
    }

    private fun generateLayerId(prefix: String): String {
        return "${prefix}_${UUID.randomUUID()}"
    }

    private fun EditorLayer.withTransform(transform: LayerTransform): EditorLayer {
        return when (this) {
            is TextLayer -> copy(transform = transform)
            is StickerLayer -> copy(transform = transform)
            is DrawLayer -> copy(transform = transform)
            is com.example.diywallpaper.domain.model.design.PhotoLayer -> copy(transform = transform)
        }
    }

    private fun EditorLayer.withZIndex(zIndex: Int): EditorLayer {
        return when (this) {
            is TextLayer -> copy(zIndex = zIndex)
            is StickerLayer -> copy(zIndex = zIndex)
            is DrawLayer -> copy(zIndex = zIndex)
            is com.example.diywallpaper.domain.model.design.PhotoLayer -> copy(zIndex = zIndex)
        }
    }

    private fun List<EditorLayer>.reindexLayers(): List<EditorLayer> {
        return mapIndexed { index, layer -> layer.withZIndex(index + 1) }
    }

    private fun List<EditorLayer>.swap(firstIndex: Int, secondIndex: Int): List<EditorLayer> {
        if (firstIndex == secondIndex) return this
        val mutable = toMutableList()
        val temp = mutable[firstIndex]
        mutable[firstIndex] = mutable[secondIndex]
        mutable[secondIndex] = temp
        return mutable
    }

    private fun EditorProject.toUiState(history: EditorHistoryState): EditorUiState {
        val currentState = _uiState.value
        return EditorUiState(
            projectId = id,
            isPersisted = currentState.isPersisted,
            title = currentTitle,
            sourceType = when (source) {
                is EditorProjectSource.Diy -> DesignSourceType.DIY_TEMPLATE
                EditorProjectSource.Scratch -> DesignSourceType.SCRATCH
            },
            canvas = canvas,
            background = background,
            layers = layers,
            placeholders = placeholders,
            selectedLayerId = selectedLayerId,
            activeBrushSessionLayerId = activeBrushSessionLayerId,
            activeTool = currentState.activeTool,
            openedToolSheet = currentState.openedToolSheet,
            activeBrushConfig = activeBrushConfig,
            activeTextBrushConfig = activeTextBrushConfig,
            canUndo = history.canUndo,
            canRedo = history.canRedo,
            isLoading = false,
            isSaving = currentState.isSaving,
            isGeneratingAssets = currentState.isGeneratingAssets,
            isPreviewMode = currentState.isPreviewMode,
            availableBackgrounds = currentState.availableBackgrounds,
            availableStickers = currentState.availableStickers,
            availableFonts = currentState.availableFonts,
            textPresets = currentState.textPresets,
            isLoadingBackgroundCatalog = currentState.isLoadingBackgroundCatalog,
            isLoadingStickerCatalog = currentState.isLoadingStickerCatalog,
            thumbnailPath = currentThumbnailPath,
            previewPath = currentPreviewPath,
            exportedImagePath = currentExportedImagePath,
            pendingPreviewDesignId = currentState.pendingPreviewDesignId,
            pendingExitAfterSave = currentState.pendingExitAfterSave,
            isDeleted = false,
            errorMessage = currentState.errorMessage,
            saveMessage = currentState.saveMessage
        )
    }

    private fun syncMetadata(
        title: String? = null,
        thumbnailPath: String? = null,
        previewPath: String? = null,
        exportedImagePath: String? = null
    ) {
        currentTitle = title
        currentThumbnailPath = thumbnailPath
        currentPreviewPath = previewPath
        currentExportedImagePath = exportedImagePath
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun defaultBrushConfig(): BrushToolConfig {
        return BrushToolConfig(
            erase = false,
            colorHex = "#1C1527",
            brushSize = 28f
        )
    }

    private fun BrushToolConfig.toBrushStyleSpec(): BrushStyleSpec? {
        return when (preset) {
            BrushPresetType.SOLID -> null
            BrushPresetType.DASHED -> BrushStyleSpec.Dashed(colorHex)
            BrushPresetType.OUTLINE -> BrushStyleSpec.Outline(fillColorHex = colorHex)
            BrushPresetType.GLOW -> BrushStyleSpec.Glow(glowColorHex = colorHex)
            BrushPresetType.PATTERN -> patternBrushName?.let { patternName ->
                BrushStyleSpec.Pattern(
                    drawableName = patternName,
                    scale = brushSize / DEFAULT_BRUSH_SIZE,
                    spacingFactor = 0.92f
                )
            }
        }
    }

    private companion object {
        const val AUTOSAVE_DELAY_MS = 750L
        const val SAVE_SUCCESS_MESSAGE = "Draft saved"
        const val RENAME_SUCCESS_MESSAGE = "Design renamed"
        const val EXPORT_SUCCESS_MESSAGE = "Assets updated"
        const val DELETE_SUCCESS_MESSAGE = "Design deleted"
        const val DEFAULT_BRUSH_SIZE = 28f
    }
}
