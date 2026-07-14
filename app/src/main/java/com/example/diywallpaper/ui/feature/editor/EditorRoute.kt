package com.example.diywallpaper.ui.feature.editor

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diywallpaper.domain.model.design.CropPresetRatio
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import java.io.File
import java.util.UUID

@Composable
fun EditorRoute(
    args: PreviewArgs,
    existingDesignId: String? = null,
    pendingImportPhotoUri: String? = null,
    pendingImportPhotoLeft: String? = null,
    pendingImportPhotoTop: String? = null,
    pendingImportPhotoRight: String? = null,
    pendingImportPhotoBottom: String? = null,
    pendingImportPhotoRatio: String? = null,
    onImportPhotoConsumed: () -> Unit = {},
    onOpenImportPhotoCrop: (String) -> Unit = {},
    onBackClick: () -> Unit,
    onNextClick: (String) -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.applyLocalImageBackground(uri.toString())
        }
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                copyImportedPhotoToInternalFiles(context, uri)
            }.onSuccess { localPath ->
                onOpenImportPhotoCrop(localPath)
            }
        }
    }
    val runtimeProjectId = rememberSaveable(args.sourceType, args.categoryId, args.initialItemId) {
        "design_${UUID.randomUUID()}"
    }

    LaunchedEffect(runtimeProjectId, existingDesignId) {
        if (uiState.projectId != null) return@LaunchedEffect

        if (existingDesignId != null) {
            viewModel.loadDesign(existingDesignId)
        } else {
            viewModel.startNewProject(
                project = createStarterProject(
                    projectId = runtimeProjectId,
                    args = args
                ),
                title = starterTitle(args)
            )
        }
    }

    LaunchedEffect(uiState.pendingPreviewDesignId) {
        val designId = uiState.pendingPreviewDesignId ?: return@LaunchedEffect
        onNextClick(designId)
        viewModel.consumePendingPreviewNavigation()
    }

    LaunchedEffect(
        pendingImportPhotoUri,
        pendingImportPhotoLeft,
        pendingImportPhotoTop,
        pendingImportPhotoRight,
        pendingImportPhotoBottom,
        pendingImportPhotoRatio
    ) {
        val photoUri = pendingImportPhotoUri ?: return@LaunchedEffect
        val ratio = pendingImportPhotoRatio
            ?.let { runCatching { CropPresetRatio.valueOf(it) }.getOrNull() }
            ?: CropPresetRatio.RATIO_9_16
        viewModel.addPhotoLayer(
            localPath = photoUri,
            crop = CropSpec(
                normalizedLeft = pendingImportPhotoLeft.toNormalizedOrDefault(0f),
                normalizedTop = pendingImportPhotoTop.toNormalizedOrDefault(0f),
                normalizedRight = pendingImportPhotoRight.toNormalizedOrDefault(1f),
                normalizedBottom = pendingImportPhotoBottom.toNormalizedOrDefault(1f),
                ratio = ratio
            )
        )
        onImportPhotoConsumed()
    }

    EditorScreen(
        args = args,
        uiState = uiState,
        onBackClick = onBackClick,
        onUndoClick = viewModel::undo,
        onRedoClick = viewModel::redo,
        onPreviewClick = viewModel::enterPreviewMode,
        onExitPreviewClick = viewModel::exitPreviewMode,
        onNextClick = viewModel::preparePreviewNavigation,
        onToolSelected = viewModel::setActiveTool,
        onDismissToolSheet = viewModel::dismissToolSheet,
        onRemoveSelectedLayer = viewModel::removeSelectedLayer,
        onOpenLayers = { viewModel.setActiveTool(EditorTool.LAYERS) },
        onMoveLayer = viewModel::moveLayer,
        onSelectLayer = viewModel::selectLayer,
        onTransformLayer = viewModel::updateLayerTransform,
        onCommitCanvasStroke = viewModel::commitCanvasStroke,
        onApplySolidBackground = viewModel::applySolidBackground,
        onApplyGradientBackground = viewModel::applyGradientBackground,
        onApplyImageBackground = viewModel::applyApiBackground,
        onImportBackground = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onImportPhoto = {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onAddText = { text, fontFamilyId, colorHex ->
            val fontOption = uiState.availableFonts.firstOrNull { it.id == fontFamilyId }
            viewModel.addTextLayer(
                text = text,
                style = TextStyleSpec(
                    fontFamilyId = fontFamilyId,
                    fontDisplayName = fontOption?.displayName ?: fontFamilyId,
                    fontSizeSp = 28f,
                    textColorHex = colorHex
                )
            )
        },
        onUpdateText = { layerId, text, fontFamilyId, colorHex ->
            val currentLayer = uiState.layers.firstOrNull { it.id == layerId } as? com.example.diywallpaper.domain.model.design.TextLayer
            val fontOption = uiState.availableFonts.firstOrNull { it.id == fontFamilyId }
            viewModel.updateTextLayer(
                layerId = layerId,
                text = text,
                style = (currentLayer?.style ?: TextStyleSpec(
                    fontFamilyId = fontFamilyId,
                    fontDisplayName = fontOption?.displayName ?: fontFamilyId,
                    fontSizeSp = 28f,
                    textColorHex = colorHex
                )).copy(
                    fontFamilyId = fontFamilyId,
                    fontDisplayName = fontOption?.displayName ?: fontFamilyId,
                    textColorHex = colorHex
                )
            )
        },
        onApplyTextPreset = viewModel::addTextPresetLayer,
        onAddSticker = viewModel::addStickerLayer,
        onAddTextBrush = { text, fontFamilyId, colorHex, brushSize ->
            val fontOption = uiState.availableFonts.firstOrNull { it.id == fontFamilyId }
            viewModel.configureTextBrushTool(
                text = text,
                style = TextStyleSpec(
                    fontFamilyId = fontFamilyId,
                    fontDisplayName = fontOption?.displayName ?: fontFamilyId,
                    fontSizeSp = brushSize + 8f,
                    textColorHex = colorHex,
                    textBrush = TextBrushStyle.Solid(colorHex)
                ),
                spacing = brushSize * 1.8f
            )
        },
        onApplyBrush = { erase, colorHex, brushSize ->
            viewModel.configureBrushTool(
                erase = erase,
                colorHex = colorHex,
                brushSize = brushSize
            )
        }
    )
}

private fun copyImportedPhotoToInternalFiles(
    context: Context,
    uri: Uri
): String {
    val directory = File(context.filesDir, "editor_imports/photos").apply {
        if (!exists()) mkdirs()
    }
    val extension = context.contentResolver.getType(uri).toImageExtension()
    val target = File(directory, "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    } ?: error("Cannot open selected photo")
    return target.absolutePath
}

private fun String?.toImageExtension(): String {
    return when (this) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/jpeg",
        "image/jpg" -> "jpg"
        else -> "jpg"
    }
}

private fun String?.toNormalizedOrDefault(default: Float): Float {
    return this?.toFloatOrNull()?.coerceIn(0f, 1f) ?: default
}

private fun starterTitle(args: PreviewArgs): String {
    return when (args.sourceType) {
        com.example.diywallpaper.domain.model.preview.PreviewSourceType.DIY -> "DIY ${args.initialItemId}"
        com.example.diywallpaper.domain.model.preview.PreviewSourceType.WALLPAPER -> "Wallpaper ${args.initialItemId}"
        com.example.diywallpaper.domain.model.preview.PreviewSourceType.CREATE_FROM_SCRATCH -> "Create from Scratch"
    }
}

private fun createStarterProject(
    projectId: String,
    args: PreviewArgs
): EditorProject {
    val now = System.currentTimeMillis()
    return EditorProject(
        id = projectId,
        source = when (args.sourceType) {
            com.example.diywallpaper.domain.model.preview.PreviewSourceType.DIY -> {
                EditorProjectSource.Diy(
                    templateId = args.initialItemId,
                    templateSnapshot = com.example.diywallpaper.domain.model.design.DiyTemplateSnapshot(
                        width = 1080,
                        height = 1920
                    )
                )
            }
            com.example.diywallpaper.domain.model.preview.PreviewSourceType.WALLPAPER,
            com.example.diywallpaper.domain.model.preview.PreviewSourceType.CREATE_FROM_SCRATCH -> {
                EditorProjectSource.Scratch
            }
        },
        canvas = EditorCanvasSpec(1080, 1920),
        background = EditorBackground.SolidColor("#FFFFFF"),
        layers = emptyList(),
        placeholders = emptyList(),
        selectedLayerId = null,
        createdAt = now,
        updatedAt = now,
        schemaVersion = 1
    )
}
