package com.example.diywallpaper.ui.feature.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextLayer

@Composable
fun EditorToolSheet(
    selectedTool: EditorTool,
    background: com.example.diywallpaper.domain.model.design.EditorBackground?,
    layers: List<com.example.diywallpaper.domain.model.design.EditorLayer>,
    selectedLayerId: String?,
    availableBackgrounds: List<BackgroundCreateItem>,
    availableStickers: List<StickerItem>,
    availableFonts: List<EditorFontOption>,
    textPresets: List<EditorTextPreset>,
    selectedTextLayer: TextLayer?,
    isLoadingBackgroundCatalog: Boolean,
    isLoadingStickerCatalog: Boolean,
    onApplySolidBackground: (String) -> Unit,
    onApplyGradientBackground: (List<String>) -> Unit,
    onApplyImageBackground: (BackgroundCreateItem) -> Unit,
    onImportBackground: () -> Unit,
    onAddText: (text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onUpdateText: (layerId: String, text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onApplyPreset: (EditorTextPreset) -> Unit,
    onAddSticker: (StickerItem) -> Unit,
    onAddTextBrush: (text: String, fontFamilyId: String, colorHex: String, brushSize: Float) -> Unit,
    onApplyBrush: (erase: Boolean, colorHex: String, brushSize: Float) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onMoveLayer: (layerId: String, targetIndex: Int) -> Unit,
    onRemoveSelectedLayer: () -> Unit,
    onDismissToolSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (selectedTool) {
        EditorTool.LAYERS -> LayersToolPanel(
            background = background,
            layers = layers,
            selectedLayerId = selectedLayerId,
            onSelectLayer = onSelectLayer,
            onMoveLayer = onMoveLayer,
            onRemoveSelectedLayer = onRemoveSelectedLayer,
            modifier = modifier
        )
        EditorTool.BACKGROUND -> BackgroundToolPanel(
            availableBackgrounds = availableBackgrounds,
            isLoadingCatalog = isLoadingBackgroundCatalog,
            modifier = modifier,
            onApplySolidBackground = onApplySolidBackground,
            onApplyGradientBackground = onApplyGradientBackground,
            onApplyImageBackground = onApplyImageBackground,
            onImportBackground = onImportBackground,
            onDismiss = onDismissToolSheet
        )
        EditorTool.IMPORT_PHOTO -> BackgroundToolPanel(
            availableBackgrounds = availableBackgrounds,
            isLoadingCatalog = isLoadingBackgroundCatalog,
            modifier = modifier,
            showImportHint = true,
            onApplySolidBackground = onApplySolidBackground,
            onApplyGradientBackground = onApplyGradientBackground,
            onApplyImageBackground = onApplyImageBackground,
            onImportBackground = onImportBackground,
            onDismiss = onDismissToolSheet
        )
        EditorTool.TEXT -> TextToolPanel(
            availableFonts = availableFonts,
            textPresets = textPresets,
            selectedTextLayer = selectedTextLayer,
            modifier = modifier,
            onAddText = onAddText,
            onUpdateText = onUpdateText,
            onApplyPreset = onApplyPreset,
            onDismiss = onDismissToolSheet
        )
        EditorTool.STICKER -> StickerToolPanel(
            availableStickers = availableStickers,
            isLoadingCatalog = isLoadingStickerCatalog,
            modifier = modifier,
            onAddSticker = onAddSticker
        )
        EditorTool.TEXT_BRUSH -> TextBrushToolPanel(
            availableFonts = availableFonts,
            modifier = modifier,
            onApplyTextBrush = onAddTextBrush
        )
        EditorTool.BRUSH_DRAW,
        EditorTool.BRUSH_ERASE,
        EditorTool.STICKER_BRUSH -> BrushToolPanel(
            modifier = modifier,
            initialErase = selectedTool == EditorTool.BRUSH_ERASE,
            onApplyBrush = onApplyBrush
        )
        EditorTool.PREVIEW -> BackgroundToolPanel(
            availableBackgrounds = availableBackgrounds,
            isLoadingCatalog = isLoadingBackgroundCatalog,
            modifier = modifier,
            onApplySolidBackground = onApplySolidBackground,
            onApplyGradientBackground = onApplyGradientBackground,
            onApplyImageBackground = onApplyImageBackground,
            onDismiss = onDismissToolSheet
        )
    }
}
