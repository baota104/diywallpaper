package com.example.diywallpaper.ui.feature.editor

import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer

data class EditorUiState(
    val projectId: String? = null,
    val isPersisted: Boolean = false,
    val title: String? = null,
    val sourceType: DesignSourceType? = null,
    val canvas: EditorCanvasSpec? = null,
    val background: EditorBackground? = null,
    val layers: List<EditorLayer> = emptyList(),
    val placeholders: List<PhotoPlaceholderLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeBrushSessionLayerId: String? = null,
    val activeTool: EditorTool = EditorTool.PREVIEW,
    val openedToolSheet: EditorTool? = null,
    val activeBrushConfig: BrushToolConfig? = null,
    val activeTextBrushConfig: TextBrushToolConfig? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isGeneratingAssets: Boolean = false,
    val isPreviewMode: Boolean = false,
    val availableBackgrounds: List<BackgroundCreateItem> = emptyList(),
    val availableStickers: List<StickerItem> = emptyList(),
    val availableFonts: List<EditorFontOption> = emptyList(),
    val textPresets: List<EditorTextPreset> = emptyList(),
    val isLoadingBackgroundCatalog: Boolean = false,
    val isLoadingStickerCatalog: Boolean = false,
    val thumbnailPath: String? = null,
    val previewPath: String? = null,
    val exportedImagePath: String? = null,
    val pendingPreviewDesignId: String? = null,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null
)

data class BrushToolConfig(
    val erase: Boolean,
    val colorHex: String,
    val brushSize: Float
)

data class TextBrushToolConfig(
    val text: String,
    val style: TextStyleSpec,
    val spacing: Float
)
