package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.ui.feature.preview.device.SavedDesignDevicePreview
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    args: PreviewArgs,
    uiState: EditorUiState,
    onBackClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onExitPreviewClick: () -> Unit,
    onNextClick: () -> Unit,
    onToolSelected: (EditorTool) -> Unit,
    onDismissToolSheet: () -> Unit,
    onRemoveSelectedLayer: () -> Unit,
    onOpenLayers: () -> Unit,
    onMoveLayer: (layerId: String, targetIndex: Int) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onTransformLayer: (
        layerId: String,
        offsetXDelta: Float,
        offsetYDelta: Float,
        scaleMultiplier: Float,
        rotationDelta: Float
    ) -> Unit,
    onCommitCanvasStroke: (List<com.example.diywallpaper.domain.model.design.StrokePoint>) -> Unit,
    onApplySolidBackground: (String) -> Unit,
    onApplyGradientBackground: (List<String>) -> Unit,
    onApplyImageBackground: (com.example.diywallpaper.domain.model.BackgroundCreateItem) -> Unit,
    onImportBackground: () -> Unit,
    onImportPhoto: () -> Unit,
    onAddText: (text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onUpdateText: (layerId: String, text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onApplyTextPreset: (EditorTextPreset) -> Unit,
    onAddSticker: (com.example.diywallpaper.domain.model.StickerItem) -> Unit,
    onAddTextBrush: (text: String, fontFamilyId: String, colorHex: String, brushSize: Float) -> Unit,
    onApplyBrush: (erase: Boolean, colorHex: String, brushSize: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTextLayer = uiState.layers
        .firstOrNull { it.id == uiState.selectedLayerId } as? TextLayer
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val previewProject = uiState.toPreviewProject()

    if (uiState.isPreviewMode && previewProject != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            SavedDesignDevicePreview(
                project = previewProject,
                isChromeVisible = true,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 12.dp, end = 16.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.24f)
            ) {
                IconButton(onClick = onExitPreviewClick) {
                    Icon(
                        imageVector = Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(id = R.string.editor_preview),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.statusBarsPadding().navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EditorTopBar(
                onBackClick = onBackClick,
                onUndoClick = onUndoClick,
                onRedoClick = onRedoClick,
                onPreviewClick = onPreviewClick,
                onNextClick = onNextClick
            )
        },
        bottomBar = {
            EditorBottomToolbar(
                selectedTool = uiState.activeTool,
                onToolSelected = { tool ->
                    if (tool == EditorTool.IMPORT_PHOTO) {
                        onImportPhoto()
                    } else {
                        onToolSelected(tool)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EditorCanvas(
                    uiState = uiState,
                    onSelectLayer = onSelectLayer,
                    onTransformLayer = onTransformLayer,
                    onCommitCanvasStroke = onCommitCanvasStroke,
                    onRemoveLayer = onRemoveSelectedLayer,
                    modifier = Modifier.fillMaxWidth()
                )

                FloatingActionButton(
                    onClick = onOpenLayers,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = stringResource(id = R.string.editor_tool_layers)
                    )
                }

            }
        }
    }

    val openedToolSheet = uiState.openedToolSheet
    if (openedToolSheet != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissToolSheet,
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .padding(bottom = 24.dp)
            ) {
                EditorToolSheet(
                    selectedTool = openedToolSheet,
                    background = uiState.background,
                    layers = uiState.layers,
                    selectedLayerId = uiState.selectedLayerId,
                    availableBackgrounds = uiState.availableBackgrounds,
                    availableStickers = uiState.availableStickers,
                    availableFonts = uiState.availableFonts,
                    textPresets = uiState.textPresets,
                    selectedTextLayer = selectedTextLayer,
                    isLoadingBackgroundCatalog = uiState.isLoadingBackgroundCatalog,
                    isLoadingStickerCatalog = uiState.isLoadingStickerCatalog,
                    onApplySolidBackground = onApplySolidBackground,
                    onApplyGradientBackground = onApplyGradientBackground,
                    onApplyImageBackground = onApplyImageBackground,
                    onImportBackground = onImportBackground,
                    onAddText = onAddText,
                    onUpdateText = onUpdateText,
                    onApplyPreset = onApplyTextPreset,
                    onAddSticker = onAddSticker,
                    onAddTextBrush = onAddTextBrush,
                    onApplyBrush = onApplyBrush,
                    onSelectLayer = onSelectLayer,
                    onMoveLayer = onMoveLayer,
                    onRemoveSelectedLayer = onRemoveSelectedLayer,
                    onDismissToolSheet = onDismissToolSheet,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditorScreenPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        EditorScreen(
            args = PreviewArgs(
                categoryId = "diy",
                initialItemId = "1",
                sourceType = PreviewSourceType.DIY
            ),
            uiState = EditorUiState(activeTool = EditorTool.TEXT),
            onBackClick = {},
            onUndoClick = {},
            onRedoClick = {},
            onPreviewClick = {},
            onExitPreviewClick = {},
            onNextClick = {},
            onToolSelected = {},
            onDismissToolSheet = {},
            onRemoveSelectedLayer = {},
            onOpenLayers = {},
            onMoveLayer = { _, _ -> },
            onSelectLayer = {},
            onTransformLayer = { _, _, _, _, _ -> },
            onCommitCanvasStroke = {},
            onApplySolidBackground = {},
            onApplyGradientBackground = {},
            onApplyImageBackground = {},
            onImportBackground = {},
            onImportPhoto = {},
            onAddText = { _, _, _ -> },
            onUpdateText = { _, _, _, _ -> },
            onApplyTextPreset = {},
            onAddSticker = {},
            onAddTextBrush = { _, _, _, _ -> },
            onApplyBrush = { _, _, _ -> }
        )
    }
}

private fun EditorUiState.toPreviewProject(): EditorProject? {
    val canvasSpec = canvas ?: return null
    val backgroundSpec = background ?: return null
    val now = System.currentTimeMillis()
    return EditorProject(
        id = projectId ?: "editor_preview",
        source = EditorProjectSource.Scratch,
        canvas = canvasSpec,
        background = backgroundSpec,
        layers = layers,
        placeholders = placeholders,
        selectedLayerId = selectedLayerId,
        createdAt = now,
        updatedAt = now,
        schemaVersion = 1
    )
}
