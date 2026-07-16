package com.example.diywallpaper.ui.feature.editor

import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.PathMeasure
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.core.render.AndroidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidDrawLayerRenderBounds
import com.example.diywallpaper.core.render.androidTextTrailStampPoints
import com.example.diywallpaper.core.render.androidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidTextRenderSpec
import com.example.diywallpaper.core.render.buildAndroidTextPaint
import com.example.diywallpaper.core.render.mapLayerBoundsToTargetCorners
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.DesignRawBounds
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorTextAlign
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.model.design.renderSize
import com.example.diywallpaper.domain.model.design.renderBounds
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.ui.feature.editor.diy.DiySlotOverlay
import com.example.diywallpaper.ui.feature.editor.diy.DiyTemplateUnderlay
import com.example.diywallpaper.ui.theme.PlusJakartaSans
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun EditorCanvas(
    uiState: EditorUiState,
    onSelectLayer: (String?) -> Unit,
    onDiySlotSelect: (String?) -> Unit,
    onDiySlotClick: (String) -> Unit,
    onDiySlotTransform: (
        slotId: String,
        panXDelta: Float,
        panYDelta: Float,
        scaleMultiplier: Float,
        rotationDelta: Float
    ) -> Unit,
    onDiySlotRemove: (String) -> Unit,
    onTransformLayer: (
        layerId: String,
        offsetXDelta: Float,
        offsetYDelta: Float,
        scaleMultiplier: Float,
        rotationDelta: Float
    ) -> Unit,
    onCommitCanvasStroke: (List<StrokePoint>) -> Unit,
    onRemoveLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canvasSpec = uiState.canvas
    val aspectRatio = if (canvasSpec != null && canvasSpec.width > 0 && canvasSpec.height > 0) {
        canvasSpec.width.toFloat() / canvasSpec.height.toFloat()
    } else {
        9f / 16f
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val maxCanvasWidth = maxWidth * 0.86f
        val widthThatFitsHeight = maxHeight * aspectRatio
        val canvasWidth = if (widthThatFitsHeight < maxCanvasWidth) {
            widthThatFitsHeight
        } else {
            maxCanvasWidth
        }
        val patternBrushBitmaps = rememberPatternBrushBitmaps()
        BoxWithConstraints(
            modifier = Modifier
                .width(canvasWidth)
                .aspectRatio(aspectRatio)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(34.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(34.dp)
                )
        ) {
            val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            val modelWidth = canvasSpec?.width?.toFloat()?.coerceAtLeast(1f) ?: 1080f
            val modelHeight = canvasSpec?.height?.toFloat()?.coerceAtLeast(1f) ?: 1920f
            val scaleX = canvasWidthPx / modelWidth
            val scaleY = canvasHeightPx / modelHeight
            val context = LocalContext.current
            val textRenderSpecs = remember(context, uiState.layers) {
                uiState.layers
                    .filterIsInstance<TextLayer>()
                    .associate { layer ->
                        layer.id to androidTextLayerRenderSpec(
                            context = context,
                            layer = layer,
                            fontResId = editorFontResId(layer.style.fontFamilyId)
                        )
                    }
            }
            val drawRenderBounds = remember(context, uiState.layers) {
                uiState.layers
                    .filterIsInstance<DrawLayer>()
                    .mapNotNull { layer ->
                        androidDrawLayerRenderBounds(
                            context = context,
                            drawData = layer.drawData,
                            fontResIdFor = ::editorFontResId
                        )?.let { bounds -> layer.id to bounds }
                    }
                    .toMap()
            }
            val activePoints = remember { mutableStateListOf<StrokePoint>() }
            val measuredLayerSizes = remember { mutableStateMapOf<String, IntSize>() }
            val previewTransforms = remember { mutableStateMapOf<String, LayerTransform>() }
            val eraseColor = resolveErasePreviewColor(uiState.background)
            val drawEnabled = uiState.openedToolSheet == EditorTool.BRUSH_DRAW ||
                uiState.openedToolSheet == EditorTool.BRUSH_ERASE ||
                uiState.openedToolSheet == EditorTool.TEXT_BRUSH
            val isDiyTemplateCanvas = uiState.sourceType == DesignSourceType.DIY_TEMPLATE &&
                uiState.templateSnapshot != null
            val selectedDiyFrameId = uiState.selectedDiyElementId?.toDiyFrameId()
            val selectedGestureFrameId = selectedDiyFrameId ?: uiState.selectedLayerId
            val renderDiyElements = if (isDiyTemplateCanvas) {
                uiState.templateSnapshot?.elements.orEmpty().map { element ->
                    val frameId = element.id.toDiyFrameId()
                    element.withPreviewTransform(previewTransforms[frameId])
                }
            } else {
                emptyList()
            }
            val activeBrushStroke = activePoints.takeIf {
                drawEnabled &&
                it.isNotEmpty() &&
                    (uiState.activeTool == EditorTool.BRUSH_DRAW || uiState.activeTool == EditorTool.BRUSH_ERASE)
            }?.let { points ->
                val config = uiState.activeBrushConfig
                if (config != null) {
                    DrawLayerData.FreeStroke(
                        BrushStroke(
                            points = points,
                            colorHex = config.colorHex,
                            brushStyle = config.toBrushStyleSpec(),
                            strokeWidth = config.brushSize
                        )
                    ).takeUnless { config.erase }
                        ?: DrawLayerData.EraseStroke(
                            BrushStroke(
                                points = points,
                                strokeWidth = config.brushSize
                            )
                        )
                } else {
                    null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(34.dp))
            ) {
            EditorCanvasBackground(
                background = uiState.background,
                modifier = Modifier.fillMaxSize()
            )

            if (isDiyTemplateCanvas) {
                    DiyTemplateUnderlay(
                        elements = renderDiyElements,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        modifier = Modifier.fillMaxSize()
                )
            }

            uiState.layers
                .sortedBy { it.zIndex }
                .forEach { layer ->
                    when (layer) {
                        is TextLayer -> TextLayerItem(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            isSelected = uiState.selectedLayerId == layer.id,
                            previewTransform = previewTransforms[layer.id],
                            renderSpec = textRenderSpecs[layer.id]
                        )

                        is StickerLayer -> ImageLayerItem(
                            layerId = layer.id,
                            imageModel = layer.assetPathOrUrl,
                            renderWidth = layer.renderSize().width,
                            renderHeight = layer.renderSize().height,
                            transform = layer.transform,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            isSelected = uiState.selectedLayerId == layer.id,
                            previewTransform = previewTransforms[layer.id]
                        )

                        is PhotoLayer -> PhotoLayerItem(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            isSelected = uiState.selectedLayerId == layer.id,
                            previewTransform = previewTransforms[layer.id]
                        )

                        is DrawLayer -> {
                            if (layer.drawData.isBrushStackRenderable()) {
                                BrushStrokeStackCanvas(
                                    layers = listOf(
                                        layer.copy(transform = previewTransforms[layer.id] ?: layer.transform)
                                    ),
                                    layerBounds = drawRenderBounds,
                                    activeStroke = activeBrushStroke
                                        .takeIf { layer.id == uiState.activeBrushSessionLayerId },
                                    scaleX = scaleX,
                                    scaleY = scaleY,
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                    patternBrushBitmaps = patternBrushBitmaps,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                DrawLayerItem(
                                    layer = layer,
                                    renderBounds = drawRenderBounds[layer.id],
                                    scaleX = scaleX,
                                    scaleY = scaleY,
                                    canvasWidthPx = canvasWidthPx,
                                    canvasHeightPx = canvasHeightPx,
                                    eraseColor = eraseColor,
                                    isSelected = uiState.selectedLayerId == layer.id,
                                    previewTransform = previewTransforms[layer.id],
                                    patternBrushBitmaps = patternBrushBitmaps
                                )
                            }
                        }
                    }
                }

            BrushStrokeStackCanvas(
                layers = emptyList(),
                activeStroke = activeBrushStroke
                    .takeIf {
                        drawEnabled &&
                            uiState.activeBrushSessionLayerId == null &&
                            uiState.activeTool == EditorTool.BRUSH_DRAW
                    },
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                patternBrushBitmaps = patternBrushBitmaps,
                modifier = Modifier.fillMaxSize()
            )

            if (drawEnabled && activePoints.isNotEmpty() && uiState.activeTool == EditorTool.TEXT_BRUSH) {
                ActiveStrokePreview(
                    points = activePoints,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    brushConfig = uiState.activeBrushConfig,
                    textBrushConfig = uiState.activeTextBrushConfig,
                    eraseColor = eraseColor,
                    patternBrushBitmaps = patternBrushBitmaps,
                    activeTool = uiState.activeTool
                )
            }

            if (drawEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            drawEnabled,
                            uiState.activeTool,
                            uiState.activeBrushConfig,
                            uiState.activeTextBrushConfig
                        ) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    activePoints.clear()
                                    activePoints += offset.toStrokePoint(scaleX, scaleY)
                                },
                                onDragEnd = {
                                    onCommitCanvasStroke(activePoints.toList())
                                    activePoints.clear()
                                },
                                onDragCancel = {
                                    activePoints.clear()
                                }
                            ) { change, _ ->
                                change.consume()
                                activePoints += change.position.toStrokePoint(scaleX, scaleY)
                            }
                        }
                )
            } else {
                val layerFrames = uiState.layers
                    .sortedBy { it.zIndex }
                    .mapNotNull { layer ->
                        layer.withRenderTransform(previewTransforms[layer.id]).toFrameSpec(
                            scaleX = scaleX,
                            scaleY = scaleY,
                            measuredSize = measuredLayerSizes[layer.id],
                            textBounds = (textRenderSpecs[layer.id])?.bounds,
                            drawBounds = drawRenderBounds[layer.id]
                        )
                    }
                val diySlotFrames = if (isDiyTemplateCanvas) {
                    renderDiyElements.mapNotNull { element ->
                        element.toFrameSpec(element.id.toDiyFrameId(), scaleX, scaleY)
                    }
                } else {
                    emptyList()
                }
                val gestureFrames = layerFrames + diySlotFrames
                EditorLayerGestureOverlay(
                    frames = gestureFrames,
                    selectedLayerId = selectedGestureFrameId,
                    onSelectLayer = { frameId ->
                        if (frameId == null) {
                            onSelectLayer(null)
                        } else if (frameId.isDiyFrameId()) {
                            onSelectLayer(null)
                            onDiySlotSelect(frameId.toDiySlotId())
                        } else {
                            onSelectLayer(frameId)
                        }
                    },
                    onPreviewTransform = { layerId, transform ->
                        previewTransforms[layerId] = transform
                    },
                    onCommitTransform = { layerId, baseTransform, committedTransform ->
                        previewTransforms.remove(layerId)
                        if (layerId.isDiyFrameId()) {
                            commitTransformDelta(
                                layerId.toDiySlotId(),
                                baseTransform,
                                committedTransform,
                                onDiySlotTransform
                            )
                        } else {
                            commitTransformDelta(layerId, baseTransform, committedTransform, onTransformLayer)
                        }
                    },
                    onCancelTransform = { layerId ->
                        previewTransforms.remove(layerId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isDiyTemplateCanvas && !drawEnabled) {
                DiySlotOverlay(
                    elements = renderDiyElements,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    onSlotClick = onDiySlotClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            uiState.selectedDiyElementId
                ?.let { selectedElementId ->
                    val frameId = selectedElementId.toDiyFrameId()
                    val element = renderDiyElements.firstOrNull { it.id == selectedElementId }
                    element?.toFrameSpec(frameId, scaleX, scaleY)?.let { frame ->
                        SelectionOverlay(
                            frame = frame,
                            showChangePhoto = element.isDiyImageElement() && element.localImagePath != null,
                            onSelect = { onDiySlotSelect(selectedElementId) },
                            onRemove = {
                                if (element.isDiyImageElement() && element.localImagePath != null) {
                                    onDiySlotRemove(selectedElementId)
                                }
                            },
                            onChangePhoto = { onDiySlotClick(selectedElementId) },
                            onPreviewTransform = { previewTransforms[frameId] = it },
                            onCommitTransform = { committedTransform ->
                                previewTransforms.remove(frameId)
                                commitTransformDelta(
                                    selectedElementId,
                                    element.editTransform(),
                                    committedTransform,
                                    onDiySlotTransform
                                )
                            },
                            onCancelTransform = {
                                previewTransforms.remove(frameId)
                            }
                        )
                    }
                }

            uiState.layers
                .firstOrNull { it.id == uiState.selectedLayerId }
                ?.let { selectedLayer ->
                    val renderSelectedLayer = selectedLayer.withRenderTransform(
                        previewTransforms[selectedLayer.id]
                    )
                    renderSelectedLayer.toFrameSpec(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        measuredSize = measuredLayerSizes[selectedLayer.id],
                        textBounds = (textRenderSpecs[selectedLayer.id])?.bounds,
                        drawBounds = drawRenderBounds[selectedLayer.id]
                    )?.let { frame ->
                        SelectionOverlay(
                            frame = frame,
                            showChangePhoto = false,
                            onSelect = { onSelectLayer(frame.layerId) },
                            onRemove = onRemoveLayer,
                            onChangePhoto = {},
                            onPreviewTransform = { previewTransforms[selectedLayer.id] = it },
                            onCommitTransform = { committedTransform ->
                                previewTransforms.remove(selectedLayer.id)
                                commitTransformDelta(
                                    selectedLayer.id,
                                    selectedLayer.transform,
                                    committedTransform,
                                    onTransformLayer
                                )
                            },
                            onCancelTransform = {
                                previewTransforms.remove(selectedLayer.id)
                            }
                        )
                    }
                }

            }
        }
    }
}

@Composable
private fun EditorCanvasBackground(
    background: EditorBackground?,
    modifier: Modifier = Modifier
) {
    when (background) {
        is EditorBackground.ApiImage -> AsyncImage(
            model = background.imageUrl,
            contentDescription = stringResource(id = R.string.editor_panel_background_image),
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.LocalImage -> AsyncImage(
            model = background.localPath,
            contentDescription = stringResource(id = R.string.editor_panel_background_image),
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.Gradient -> Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(
                    background.colors.map { parseColorHex(it, MaterialTheme.colorScheme.surface) }
                )
            )
        )

        is EditorBackground.SolidColor -> Box(
            modifier = modifier.background(parseColorHex(background.colorHex, MaterialTheme.colorScheme.surface))
        )

        null -> Box(modifier = modifier.background(MaterialTheme.colorScheme.surface))
    }
}

@Composable
private fun EmptyCanvasHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFFDF8FF), RoundedCornerShape(24.dp))
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TextLayerItem(
    layer: TextLayer,
    scaleX: Float,
    scaleY: Float,
    isSelected: Boolean,
    previewTransform: LayerTransform?,
    renderSpec: AndroidTextLayerRenderSpec?
) {
    val renderTransform = previewTransform ?: layer.transform
    val context = LocalContext.current
    val spec = renderSpec ?: remember(context, layer.text, layer.style) {
        androidTextLayerRenderSpec(
            context = context,
            layer = layer,
            fontResId = editorFontResId(layer.style.fontFamilyId)
        )
    }
    val bounds = spec.bounds
    val averageScale = ((scaleX + scaleY) / 2f).coerceAtLeast(0.0001f)
    val textColor = resolveTextColor(layer).toArgb()
    val paint = remember(context, layer.style, renderTransform.alpha, averageScale, textColor) {
        buildAndroidTextPaint(
            context = context,
            style = layer.style,
            fontResId = editorFontResId(layer.style.fontFamilyId),
            color = textColor,
            alpha = renderTransform.alpha,
            textScale = averageScale
        )
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = ((bounds.minX + renderTransform.offsetX) * scaleX).roundToInt(),
                    y = ((bounds.minY + renderTransform.offsetY) * scaleY).roundToInt()
                )
            }
            .size(
                width = pxToDp(bounds.width * scaleX),
                height = pxToDp(bounds.height * scaleY)
            )
            .graphicsLayer(
                rotationZ = renderTransform.rotation,
                scaleX = renderTransform.scale,
                scaleY = renderTransform.scale,
                alpha = renderTransform.alpha
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    layer.text,
                    (spec.drawX - bounds.minX) * scaleX,
                    (spec.drawBaselineY - bounds.minY) * scaleY,
                    paint
                )
            }
        }
    }
}

@Composable
private fun PhotoLayerItem(
    layer: PhotoLayer,
    scaleX: Float,
    scaleY: Float,
    isSelected: Boolean,
    previewTransform: LayerTransform?
) {
    val renderLayer = previewTransform?.let { layer.copy(transform = it) } ?: layer
    val renderTransform = renderLayer.transform
    val baseSize = photoRenderSize(layer.crop?.ratio)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (renderTransform.offsetX * scaleX).roundToInt(),
                    y = (renderTransform.offsetY * scaleY).roundToInt()
                )
            }
            .size(
                width = pxToDp(baseSize.width * scaleX),
                height = pxToDp(baseSize.height * scaleY)
            )
            .graphicsLayer(
                rotationZ = renderTransform.rotation,
                scaleX = renderTransform.scale,
                scaleY = renderTransform.scale,
                alpha = renderTransform.alpha
            )
    ) {
        PhotoLayerContent(layer = layer)
    }
}

@Composable
private fun PhotoLayerContent(layer: PhotoLayer) {
    val imageBitmap = remember(layer.localPath) {
        BitmapFactory.decodeFile(layer.localPath)?.asImageBitmap()
    }
    if (imageBitmap != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val crop = layer.crop
            val srcLeft = ((crop?.normalizedLeft ?: 0f).coerceIn(0f, 1f) * imageBitmap.width)
                .roundToInt()
            val srcTop = ((crop?.normalizedTop ?: 0f).coerceIn(0f, 1f) * imageBitmap.height)
                .roundToInt()
            val srcRight = ((crop?.normalizedRight ?: 1f).coerceIn(0f, 1f) * imageBitmap.width)
                .roundToInt()
                .coerceAtLeast(srcLeft + 1)
            val srcBottom = ((crop?.normalizedBottom ?: 1f).coerceIn(0f, 1f) * imageBitmap.height)
                .roundToInt()
                .coerceAtLeast(srcTop + 1)
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(
                    width = (srcRight - srcLeft).coerceAtLeast(1),
                    height = (srcBottom - srcTop).coerceAtLeast(1)
                ),
                dstSize = IntSize(
                    width = size.width.roundToInt().coerceAtLeast(1),
                    height = size.height.roundToInt().coerceAtLeast(1)
                )
            )
        }
    } else {
        AsyncImage(
            model = layer.localPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ImageLayerItem(
    layerId: String,
    imageModel: Any,
    renderWidth: Float,
    renderHeight: Float,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float,
    isSelected: Boolean,
    previewTransform: LayerTransform?
) {
    val renderTransform = previewTransform ?: transform
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (renderTransform.offsetX * scaleX).roundToInt(),
                    y = (renderTransform.offsetY * scaleY).roundToInt()
                )
            }
            .size(
                width = pxToDp(renderWidth * scaleX),
                height = pxToDp(renderHeight * scaleY)
            )
            .graphicsLayer(
                rotationZ = renderTransform.rotation,
                scaleX = renderTransform.scale,
                scaleY = renderTransform.scale,
                alpha = renderTransform.alpha
            )
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun BrushStrokeStackCanvas(
    layers: List<DrawLayer>,
    layerBounds: Map<String, DesignRawBounds> = emptyMap(),
    activeStroke: DrawLayerData?,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>,
    modifier: Modifier = Modifier
) {
    if (layers.isEmpty() && activeStroke == null) return
    Box(modifier = modifier) {
        layers.forEach { layer ->
            val renderTransform = layer.transform
            val bounds = layerBounds[layer.id] ?: layer.drawData.renderBounds() ?: return@forEach
            Canvas(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((bounds.minX + renderTransform.offsetX) * scaleX).roundToInt(),
                            y = ((bounds.minY + renderTransform.offsetY) * scaleY).roundToInt()
                        )
                    }
                    .size(
                        width = pxToDp(bounds.width * scaleX),
                        height = pxToDp(bounds.height * scaleY)
                    )
                    .graphicsLayer(
                        rotationZ = renderTransform.rotation,
                        scaleX = renderTransform.scale,
                        scaleY = renderTransform.scale,
                        alpha = renderTransform.alpha
                    )
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                val data = layer.drawData
                if (data.isBrushStackRenderable()) {
                    withTransform({
                        translate(left = -bounds.minX * scaleX, top = -bounds.minY * scaleY)
                    }) {
                    data.forEachBrushStackItem { item ->
                        drawBrushStackStroke(
                            item = item,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            patternBrushBitmaps = patternBrushBitmaps
                        )
                    }
                    activeStroke?.forEachBrushStackItem { item ->
                        drawBrushStackStroke(
                            item = item,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            patternBrushBitmaps = patternBrushBitmaps
                        )
                    }
                    }
                }
            }
        }
        if (activeStroke != null && layers.isEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                activeStroke.forEachBrushStackItem { item ->
                    drawBrushStackStroke(
                        item = item,
                        scaleX = scaleX,
                    scaleY = scaleY,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    patternBrushBitmaps = patternBrushBitmaps
                )
            }
        }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBrushStackStroke(
    item: BrushStackItem,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>
) {
    val stroke = when (item) {
        is BrushStackItem.Draw -> item.stroke
        is BrushStackItem.Erase -> item.stroke
    }
    if (stroke.points.size < 2) return
    val patternStyle = (item as? BrushStackItem.Draw)?.stroke?.brushStyle as? BrushStyleSpec.Pattern
    val patternBitmap = patternStyle?.let { patternBrushBitmaps[it.drawableName] }
    if (patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
        drawPatternBrushStroke(
            stroke = stroke,
            bitmap = patternBitmap,
            patternStyle = patternStyle,
            scaleX = scaleX,
            scaleY = scaleY
        )
        return
    }
    val style = stroke.brushStyle
    val path = Path().apply {
        moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scaleX, point.y * scaleY)
        }
    }
    if (style is BrushStyleSpec.Outline && item is BrushStackItem.Draw) {
        drawPath(
            path = path,
            color = parseColorHex(style.strokeColorHex, Color.Black),
            style = Stroke(
                width = stroke.strokeWidth * ((scaleX + scaleY) / 2f) * 1.55f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = path,
            color = parseColorHex(style.fillColorHex, Color.Black),
            style = Stroke(
                width = stroke.strokeWidth * ((scaleX + scaleY) / 2f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        return
    }
    if (style is BrushStyleSpec.Glow && item is BrushStackItem.Draw) {
        drawGlowBrushStroke(
            stroke = stroke,
            scaleX = scaleX,
            scaleY = scaleY,
            color = parseColorHex(style.colorHex, Color.White),
            glowColor = parseColorHex(style.glowColorHex, Color.White)
        )
        return
    }
    val brush = when (val style = stroke.brushStyle) {
        is BrushStyleSpec.Gradient -> Brush.linearGradient(
            colors = style.colors.map { parseColorHex(it, Color.Black) },
            start = Offset.Zero,
            end = Offset(canvasWidthPx, canvasHeightPx)
        )

        is BrushStyleSpec.Solid -> Brush.linearGradient(
            listOf(parseColorHex(style.colorHex, Color.Black), parseColorHex(style.colorHex, Color.Black))
        )

        is BrushStyleSpec.Dashed -> Brush.linearGradient(
            listOf(parseColorHex(style.colorHex, Color.Black), parseColorHex(style.colorHex, Color.Black))
        )

        is BrushStyleSpec.Outline -> Brush.linearGradient(
            listOf(parseColorHex(style.fillColorHex, Color.Black), parseColorHex(style.fillColorHex, Color.Black))
        )

        is BrushStyleSpec.Glow -> Brush.linearGradient(
            listOf(parseColorHex(style.colorHex, Color.White), parseColorHex(style.colorHex, Color.White))
        )

        is BrushStyleSpec.Pattern,
        null -> {
            val color = parseColorHex(stroke.colorHex ?: "#1C1527", Color.Black)
            Brush.linearGradient(listOf(color, color))
        }
    }
    drawPath(
        path = path,
        brush = brush,
        style = Stroke(
            width = stroke.strokeWidth * ((scaleX + scaleY) / 2f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = (style as? BrushStyleSpec.Dashed)?.let {
                val width = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
                PathEffect.dashPathEffect(floatArrayOf(width * 1.7f, width * 1.25f), 0f)
            }
        ),
        alpha = 1f,
        blendMode = if (item is BrushStackItem.Erase) BlendMode.Clear else BlendMode.SrcOver
    )
}

@Composable
private fun DrawLayerItem(
    layer: DrawLayer,
    renderBounds: DesignRawBounds?,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    eraseColor: Color,
    isSelected: Boolean,
    previewTransform: LayerTransform?,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>
) {
    val renderTransform = previewTransform ?: layer.transform
    val bounds = renderBounds ?: layer.drawData.renderBounds() ?: return
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = ((bounds.minX + renderTransform.offsetX) * scaleX).roundToInt(),
                    y = ((bounds.minY + renderTransform.offsetY) * scaleY).roundToInt()
                )
            }
            .size(
                width = pxToDp(bounds.width * scaleX),
                height = pxToDp(bounds.height * scaleY)
            )
            .graphicsLayer(
                rotationZ = renderTransform.rotation,
                scaleX = renderTransform.scale,
                scaleY = renderTransform.scale,
                alpha = renderTransform.alpha
            )
    ) {
        when (val drawData = layer.drawData) {
            is DrawLayerData.FreeStroke -> Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({
                    translate(left = -bounds.minX * scaleX, top = -bounds.minY * scaleY)
                }) {
                        drawBrushStackStroke(
                            item = BrushStackItem.Draw(drawData.stroke),
                            scaleX = scaleX,
                            scaleY = scaleY,
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            patternBrushBitmaps = patternBrushBitmaps
                        )
                }
            }

            is DrawLayerData.EraseStroke -> StrokeCanvas(
                stroke = drawData.stroke,
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                eraseColor = eraseColor,
                erase = true,
                patternBrushBitmaps = patternBrushBitmaps
            )

            is DrawLayerData.BrushStack -> Unit

            is DrawLayerData.TextTrail -> TextTrailContent(
                drawData = drawData,
                scaleX = scaleX,
                scaleY = scaleY,
                originX = bounds.minX,
                originY = bounds.minY
            )

            is DrawLayerData.StickerTrail -> {
            StickerTrailContent(
                drawData = drawData,
                scaleX = scaleX,
                scaleY = scaleY,
                originX = bounds.minX,
                originY = bounds.minY
            )
            }
        }
    }
}

@Composable
private fun TextTrailContent(
    drawData: DrawLayerData.TextTrail,
    scaleX: Float,
    scaleY: Float,
    originX: Float = 0f,
    originY: Float = 0f
) {
    if (drawData.points.isEmpty() || drawData.text.isBlank()) return
    val context = LocalContext.current
    val averageScale = ((scaleX + scaleY) / 2f).coerceAtLeast(0.0001f)
    val textColor = resolveTextBrushColor(drawData.textStyle.textBrush, drawData.textStyle.textColorHex).toArgb()
    val spec = remember(context, drawData.text, drawData.textStyle) {
        androidTextRenderSpec(
            context = context,
            text = drawData.text,
            style = drawData.textStyle,
            fontResId = editorFontResId(drawData.textStyle.fontFamilyId)
        )
    }
    val paint = remember(context, drawData.textStyle, averageScale, textColor) {
        buildAndroidTextPaint(
            context = context,
            style = drawData.textStyle,
            fontResId = editorFontResId(drawData.textStyle.fontFamilyId),
            color = textColor,
            alpha = 1f,
            textScale = averageScale
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            androidTextTrailStampPoints(drawData).forEach { point ->
                canvas.nativeCanvas.drawText(
                    drawData.text,
                    (point.x + spec.drawX - originX) * scaleX,
                    (point.y + spec.drawBaselineY - originY) * scaleY,
                    paint
                )
            }
        }
    }
}

@Composable
private fun StickerTrailContent(
    drawData: DrawLayerData.StickerTrail,
    scaleX: Float,
    scaleY: Float,
    originX: Float = 0f,
    originY: Float = 0f
) {
    drawData.points.forEachIndexed { index, point ->
        if (index % 2 == 0) {
            AsyncImage(
                model = drawData.stickerAssetPathOrUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((point.x - originX) * scaleX).roundToInt(),
                            y = ((point.y - originY) * scaleY).roundToInt()
                        )
                    }
                    .size(pxToDp(drawData.stampSize * scaleX))
            )
        }
    }
}

@Composable
private fun ActiveStrokePreview(
    points: List<StrokePoint>,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    brushConfig: BrushToolConfig?,
    textBrushConfig: TextBrushToolConfig?,
    eraseColor: Color,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>,
    activeTool: EditorTool
) {
    when (activeTool) {
        EditorTool.BRUSH_DRAW,
        EditorTool.BRUSH_ERASE -> {
            val config = brushConfig ?: return
            StrokeCanvas(
                stroke = BrushStroke(
                    points = points,
                    colorHex = config.colorHex,
                    brushStyle = config.toBrushStyleSpec(),
                    strokeWidth = config.brushSize
                ),
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                eraseColor = eraseColor,
                erase = config.erase,
                patternBrushBitmaps = patternBrushBitmaps
            )
        }

        EditorTool.TEXT_BRUSH -> {
            val config = textBrushConfig ?: return
            TextTrailContent(
                drawData = DrawLayerData.TextTrail(
                    text = config.text,
                    textStyle = config.style,
                    points = points,
                    spacing = config.spacing
                ),
                scaleX = scaleX,
                scaleY = scaleY
            )
        }

        else -> Unit
    }
}

@Composable
private fun EditorLayerGestureOverlay(
    frames: List<LayerFrameSpec>,
    selectedLayerId: String?,
    onSelectLayer: (String?) -> Unit,
    onPreviewTransform: (String, LayerTransform) -> Unit,
    onCommitTransform: (String, LayerTransform, LayerTransform) -> Unit,
    onCancelTransform: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFrames = rememberUpdatedState(frames)
    val currentSelectedLayerId = rememberUpdatedState(selectedLayerId)
    val currentOnSelectLayer = rememberUpdatedState(onSelectLayer)
    val currentOnPreviewTransform = rememberUpdatedState(onPreviewTransform)
    val currentOnCommitTransform = rememberUpdatedState(onCommitTransform)
    val currentOnCancelTransform = rememberUpdatedState(onCancelTransform)

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val downPosition = down.position
                val gestureFrames = currentFrames.value
                val selectedFrame = gestureFrames.firstOrNull { it.layerId == currentSelectedLayerId.value }
                val handleFrame = selectedFrame?.takeIf { it.handleHitTest(downPosition) }
                val targetFrame = handleFrame ?: gestureFrames
                    .asReversed()
                    .firstOrNull { it.contentHitTest(downPosition) }

                if (targetFrame == null) {
                    currentOnSelectLayer.value(null)
                    return@awaitEachGesture
                }

                down.consume()
                currentOnSelectLayer.value(targetFrame.layerId)

                val mode = if (handleFrame != null) GestureMode.HANDLE else GestureMode.BODY_DRAG
                val baseTransform = targetFrame.transform
                val startCentroid = downPosition
                val startHandleVector = startCentroid - targetFrame.center
                val startHandleDistance = startHandleVector.distance().coerceAtLeast(1f)
                val startHandleAngle = startHandleVector.angleDegrees()
                var transformBase = baseTransform
                var centroidBase = startCentroid
                var distanceBase = 1f
                var rotationBase = 0f
                var gestureMode = if (mode == GestureMode.HANDLE) GestureMode.HANDLE else GestureMode.BODY_DRAG
                var currentTransform = baseTransform
                var committed = false

                while (true) {
                    val event = awaitPointerEvent()
                    val pressedChanges = event.changes.filter { it.pressed }
                    if (pressedChanges.isEmpty()) break

                    val centroid = pressedChanges.centroid()
                    val nextMode = when {
                        mode == GestureMode.HANDLE -> GestureMode.HANDLE
                        pressedChanges.size >= 2 -> GestureMode.BODY_TRANSFORM
                        else -> GestureMode.BODY_DRAG
                    }
                    if (nextMode != gestureMode) {
                        gestureMode = nextMode
                        transformBase = currentTransform
                        centroidBase = centroid
                        distanceBase = pressedChanges.distance().coerceAtLeast(1f)
                        rotationBase = pressedChanges.rotation()
                    }

                    currentTransform = when (gestureMode) {
                        GestureMode.HANDLE -> {
                            val vector = centroid - targetFrame.center
                            val scaleMultiplier = vector.distance().coerceAtLeast(1f) / startHandleDistance
                            val rotationDelta = vector.angleDegrees() - startHandleAngle
                            baseTransform.copy(
                                scale = (baseTransform.scale * scaleMultiplier)
                                    .coerceIn(targetFrame.minScale, targetFrame.maxScale),
                                rotation = baseTransform.rotation + rotationDelta
                            )
                        }

                        GestureMode.BODY_TRANSFORM -> {
                            val distance = pressedChanges.distance().coerceAtLeast(1f)
                            val scaleMultiplier = distance / distanceBase
                            val rotationDelta = pressedChanges.rotation() - rotationBase
                            transformBase.copy(
                                offsetX = transformBase.offsetX + (centroid.x - centroidBase.x) / targetFrame.scaleX,
                                offsetY = transformBase.offsetY + (centroid.y - centroidBase.y) / targetFrame.scaleY,
                                scale = (transformBase.scale * scaleMultiplier)
                                    .coerceIn(targetFrame.minScale, targetFrame.maxScale),
                                rotation = transformBase.rotation + rotationDelta
                            )
                        }

                        GestureMode.BODY_DRAG -> {
                            transformBase.copy(
                                offsetX = transformBase.offsetX + (centroid.x - centroidBase.x) / targetFrame.scaleX,
                                offsetY = transformBase.offsetY + (centroid.y - centroidBase.y) / targetFrame.scaleY
                            )
                        }
                    }

                    currentOnPreviewTransform.value(targetFrame.layerId, currentTransform)
                    event.changes.forEach { it.consume() }
                    committed = true
                }

                if (committed) {
                    currentOnCommitTransform.value(targetFrame.layerId, baseTransform, currentTransform)
                } else {
                    currentOnCancelTransform.value(targetFrame.layerId)
                }
            }
        }
    )
}

private enum class GestureMode {
    BODY_DRAG,
    BODY_TRANSFORM,
    HANDLE
}

@Composable
private fun SelectionOverlay(
    frame: LayerFrameSpec,
    showChangePhoto: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onChangePhoto: () -> Unit,
    onPreviewTransform: (LayerTransform) -> Unit,
    onCommitTransform: (LayerTransform) -> Unit,
    onCancelTransform: () -> Unit
) {
    val corners = frame.visualCorners()
    val handleSize = 32.dp
    val density = LocalDensity.current
    val handleHalfPx = with(density) { handleSize.toPx() / 2f }
    val borderWidthPx = with(density) { 2.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(Color.Black, corners.topLeft, corners.topRight, strokeWidth = borderWidthPx)
            drawLine(Color.Black, corners.topRight, corners.bottomRight, strokeWidth = borderWidthPx)
            drawLine(Color.Black, corners.bottomRight, corners.bottomLeft, strokeWidth = borderWidthPx)
            drawLine(Color.Black, corners.bottomLeft, corners.topLeft, strokeWidth = borderWidthPx)
        }
        OverlayHandle(
            icon = Icons.Outlined.Close,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (corners.topLeft.x - handleHalfPx).roundToInt(),
                        y = (corners.topLeft.y - handleHalfPx).roundToInt()
                    )
                }
                .zIndex(2f),
            onClick = onRemove
        )
        if (showChangePhoto) {
            OverlayHandle(
                icon = Icons.Outlined.AddPhotoAlternate,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (corners.topRight.x - handleHalfPx).roundToInt(),
                            y = (corners.topRight.y - handleHalfPx).roundToInt()
                        )
                    }
                    .zIndex(2f),
                onClick = onChangePhoto
            )
        }
        ResizeHandle(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (corners.bottomRight.x - handleHalfPx).roundToInt(),
                        y = (corners.bottomRight.y - handleHalfPx).roundToInt()
                    )
                }
                .zIndex(2f)
        )
    }
}

@Composable
private fun OverlayHandle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ResizeHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.OpenInFull,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun StrokeCanvas(
    stroke: BrushStroke,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    eraseColor: Color,
    erase: Boolean,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?> = emptyMap()
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (stroke.points.size < 2) return@Canvas
        val patternStyle = stroke.brushStyle as? BrushStyleSpec.Pattern
        val patternBitmap = patternStyle?.let { patternBrushBitmaps[it.drawableName] }
        if (!erase && patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
            drawPatternBrushStroke(
                stroke = stroke,
                bitmap = patternBitmap,
                patternStyle = patternStyle,
                scaleX = scaleX,
                scaleY = scaleY
            )
            return@Canvas
        }
        val style = stroke.brushStyle
        val path = Path().apply {
            moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x * scaleX, point.y * scaleY)
            }
        }
        if (!erase && style is BrushStyleSpec.Outline) {
            drawPath(
                path = path,
                color = parseColorHex(style.strokeColorHex, Color.Black),
                style = Stroke(
                    width = stroke.strokeWidth * ((scaleX + scaleY) / 2f) * 1.55f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            drawPath(
                path = path,
                color = parseColorHex(style.fillColorHex, Color.Black),
                style = Stroke(
                    width = stroke.strokeWidth * ((scaleX + scaleY) / 2f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            return@Canvas
        }
        if (!erase && style is BrushStyleSpec.Glow) {
            drawGlowBrushStroke(
                stroke = stroke,
                scaleX = scaleX,
                scaleY = scaleY,
                color = parseColorHex(style.colorHex, Color.White),
                glowColor = parseColorHex(style.glowColorHex, Color.White)
            )
            return@Canvas
        }
        val brush = when (val style = stroke.brushStyle) {
            is BrushStyleSpec.Gradient -> Brush.linearGradient(
                colors = style.colors.map { parseColorHex(it, Color.Black) },
                start = Offset.Zero,
                end = Offset(canvasWidthPx, canvasHeightPx)
            )

            is BrushStyleSpec.Solid -> Brush.linearGradient(
                listOf(parseColorHex(style.colorHex, Color.Black), parseColorHex(style.colorHex, Color.Black))
            )

            is BrushStyleSpec.Dashed -> Brush.linearGradient(
                listOf(parseColorHex(style.colorHex, Color.Black), parseColorHex(style.colorHex, Color.Black))
            )

            is BrushStyleSpec.Outline -> Brush.linearGradient(
                listOf(parseColorHex(style.fillColorHex, Color.Black), parseColorHex(style.fillColorHex, Color.Black))
            )

            is BrushStyleSpec.Glow -> Brush.linearGradient(
                listOf(parseColorHex(style.colorHex, Color.White), parseColorHex(style.colorHex, Color.White))
            )

            is BrushStyleSpec.Pattern,
            null -> {
                val color = if (erase) eraseColor else parseColorHex(stroke.colorHex ?: "#1C1527", Color.Black)
                Brush.linearGradient(listOf(color, color))
            }
        }
        drawPath(
            path = path,
            brush = brush,
            style = Stroke(
                width = stroke.strokeWidth * ((scaleX + scaleY) / 2f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = (style as? BrushStyleSpec.Dashed)?.let {
                    val width = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
                    PathEffect.dashPathEffect(floatArrayOf(width * 1.7f, width * 1.25f), 0f)
                }
            ),
            alpha = 1f
        )
    }
}

@Composable
private fun rememberPatternBrushBitmaps(): Map<String, android.graphics.Bitmap?> {
    val context = LocalContext.current
    return remember(context) {
        (1..20).associate { index ->
            val name = "brush$index"
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            name to resId.takeIf { it != 0 }?.let {
                BitmapFactory.decodeResource(context.resources, it)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPatternBrushStroke(
    stroke: BrushStroke,
    bitmap: android.graphics.Bitmap,
    patternStyle: BrushStyleSpec.Pattern,
    scaleX: Float,
    scaleY: Float
) {
    val path = android.graphics.Path().apply {
        moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scaleX, point.y * scaleY)
        }
    }
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return
    val averageScale = ((scaleX + scaleY) / 2f).coerceAtLeast(0.0001f)
    val iconScale = (stroke.strokeWidth / bitmap.width.coerceAtLeast(1)) *
        patternStyle.scale.coerceAtLeast(0.1f) *
        averageScale
    val step = (bitmap.width * iconScale * patternStyle.spacingFactor.coerceAtLeast(0.35f))
        .coerceAtLeast(4f)
    val position = FloatArray(2)
    val tangent = FloatArray(2)
    val matrix = Matrix()
    var distance = step / 2f
    drawIntoCanvas { composeCanvas ->
        val nativeCanvas = composeCanvas.nativeCanvas
        while (distance < length && measure.getPosTan(distance, position, tangent)) {
            matrix.reset()
            matrix.setScale(iconScale, iconScale)
            matrix.postTranslate(
                -bitmap.width * iconScale / 2f,
                -bitmap.height * iconScale / 2f
            )
            if (patternStyle.followPath) {
                val angle = Math.toDegrees(kotlin.math.atan2(tangent[1], tangent[0]).toDouble()).toFloat()
                matrix.postRotate(angle)
            }
            matrix.postTranslate(position[0], position[1])
            nativeCanvas.drawBitmap(bitmap, matrix, null)
            distance += step
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGlowBrushStroke(
    stroke: BrushStroke,
    scaleX: Float,
    scaleY: Float,
    color: Color,
    glowColor: Color
) {
    val nativePath = android.graphics.Path().apply {
        moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scaleX, point.y * scaleY)
        }
    }
    val width = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
    drawIntoCanvas { composeCanvas ->
        val nativeCanvas = composeCanvas.nativeCanvas
        val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = glowColor.copy(alpha = 0.45f).toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = width * 1.9f
            maskFilter = BlurMaskFilter(width * 0.9f, BlurMaskFilter.Blur.NORMAL)
        }
        nativeCanvas.drawPath(nativePath, glowPaint)
        val corePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = width
        }
        nativeCanvas.drawPath(nativePath, corePaint)
    }
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
                scale = brushSize / 28f,
                spacingFactor = 0.92f
            )
        }
    }
}

private data class LayerFrameSpec(
    val layerId: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val transform: LayerTransform,
    val scaleX: Float,
    val scaleY: Float,
    val scaleAppliedToFrame: Boolean,
    val minScale: Float = 0.35f,
    val maxScale: Float = 4f,
    val corners: FrameCorners
) {
    val center: Offset get() = Offset(x + width / 2f, y + height / 2f)
    val visualScale: Float get() = if (scaleAppliedToFrame) 1f else transform.scale
}

private data class FrameCorners(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset
)

private fun LayerFrameSpec.visualCorners(): FrameCorners {
    return corners
}

private fun EditorLayer.toFrameSpec(
    scaleX: Float,
    scaleY: Float,
    measuredSize: IntSize?,
    textBounds: DesignRawBounds? = null,
    drawBounds: DesignRawBounds? = null
): LayerFrameSpec? {
    return when (this) {
        is TextLayer -> {
            val bounds = textBounds ?: renderBounds()
            rawFrame(
                layerId = id,
                bounds = bounds,
                x = (transform.offsetX + bounds.minX) * scaleX,
                y = (transform.offsetY + bounds.minY) * scaleY,
                width = bounds.width * scaleX,
                height = bounds.height * scaleY,
                rotation = transform.rotation,
                transform = transform,
                scaleX = scaleX,
                scaleY = scaleY,
                scaleAppliedToFrame = false
            )
        }

        is StickerLayer -> {
            val size = renderSize()
            scaledFrame(
                layerId = id,
                offsetXPx = transform.offsetX * scaleX,
                offsetYPx = transform.offsetY * scaleY,
                baseWidthPx = size.width * scaleX,
                baseHeightPx = size.height * scaleY,
                scale = transform.scale,
                rotation = transform.rotation,
                transform = transform,
                scaleX = scaleX,
                scaleY = scaleY
            )
        }

        is PhotoLayer -> {
            val baseSize = photoRenderSize(crop?.ratio)
            scaledFrame(
                layerId = id,
                offsetXPx = transform.offsetX * scaleX,
                offsetYPx = transform.offsetY * scaleY,
                baseWidthPx = baseSize.width * scaleX,
                baseHeightPx = baseSize.height * scaleY,
                scale = transform.scale,
                rotation = transform.rotation,
                transform = transform,
                scaleX = scaleX,
                scaleY = scaleY
            )
        }

        is DrawLayer -> {
            val bounds = drawBounds ?: drawData.renderBounds() ?: return null
            rawFrame(
                layerId = id,
                bounds = bounds,
                x = (transform.offsetX + bounds.minX) * scaleX,
                y = (transform.offsetY + bounds.minY) * scaleY,
                width = bounds.width * scaleX,
                height = bounds.height * scaleY,
                rotation = transform.rotation,
                transform = transform,
                scaleX = scaleX,
                scaleY = scaleY,
                scaleAppliedToFrame = false
            )
        }
    }
}

private fun DiyTemplateElementSnapshot.toFrameSpec(
    frameId: String,
    scaleX: Float,
    scaleY: Float
): LayerFrameSpec? {
    if (isFixedDiyTemplateElement()) return null
    val transform = editTransform()
    val baseWidth = if (isDiyImageElement() && localImagePath != null) {
        contentBaseWidth ?: width
    } else {
        width
    }
    val baseHeight = if (isDiyImageElement() && localImagePath != null) {
        contentBaseHeight ?: height
    } else {
        height
    }
    if (isDiyImageElement() && localImagePath == null) return null
    return scaledFrame(
        layerId = frameId,
        offsetXPx = transform.offsetX * scaleX,
        offsetYPx = transform.offsetY * scaleY,
        baseWidthPx = baseWidth * scaleX,
        baseHeightPx = baseHeight * scaleY,
        scale = transform.scale,
        rotation = transform.rotation,
        transform = transform,
        scaleX = scaleX,
        scaleY = scaleY,
        minScale = 0.05f,
        maxScale = maxOf(12f, transform.scale * 2f)
    )
}

private fun DiyTemplateElementSnapshot.withPreviewTransform(previewTransform: LayerTransform?): DiyTemplateElementSnapshot {
    if (previewTransform == null) return this
    if (isFixedDiyTemplateElement()) return this
    return if (isDiyImageElement() && localImagePath != null) {
        copy(contentTransform = previewTransform)
    } else {
        copy(transform = previewTransform)
    }
}

private fun DiyTemplateElementSnapshot.editTransform(): LayerTransform {
    if (isFixedDiyTemplateElement()) {
        return LayerTransform(x, y, 1f, rotation)
    }
    return if (isDiyImageElement() && localImagePath != null) {
        contentTransform ?: LayerTransform(x, y, 1f, rotation)
    } else {
        transform ?: LayerTransform(x, y, 1f, rotation)
    }
}

private fun DiyTemplateElementSnapshot.isDiyImageElement(): Boolean {
    return type.equals("IMAGE", ignoreCase = true) ||
        type.equals("Image", ignoreCase = true)
}

private fun DiyTemplateElementSnapshot.isFixedDiyTemplateElement(): Boolean {
    return type.equals("PICTURE", ignoreCase = true) ||
        type.equals("Picture", ignoreCase = true)
}

private fun rawFrame(
    layerId: String,
    bounds: DesignRawBounds,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rotation: Float,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float,
    scaleAppliedToFrame: Boolean
): LayerFrameSpec {
    val mapped = mapLayerBoundsToTargetCorners(
        bounds = bounds,
        transform = transform,
        scaleX = scaleX,
        scaleY = scaleY
    )
    return LayerFrameSpec(
        layerId = layerId,
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
        transform = transform,
        scaleX = scaleX,
        scaleY = scaleY,
        scaleAppliedToFrame = scaleAppliedToFrame,
        corners = mapped.toFrameCorners()
    )
}

private const val DIY_FRAME_PREFIX = "diy_slot:"

private fun String.toDiyFrameId(): String = DIY_FRAME_PREFIX + this

private fun String.isDiyFrameId(): Boolean = startsWith(DIY_FRAME_PREFIX)

private fun String.toDiySlotId(): String = removePrefix(DIY_FRAME_PREFIX)

private fun scaledFrame(
    layerId: String,
    offsetXPx: Float,
    offsetYPx: Float,
    baseWidthPx: Float,
    baseHeightPx: Float,
    scale: Float,
    rotation: Float,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float,
    minScale: Float = 0.35f,
    maxScale: Float = 4f
): LayerFrameSpec {
    val bounds = DesignRawBounds(
        minX = 0f,
        minY = 0f,
        maxX = baseWidthPx / scaleX,
        maxY = baseHeightPx / scaleY
    )
    val mapped = mapLayerBoundsToTargetCorners(
        bounds = bounds,
        transform = transform,
        scaleX = scaleX,
        scaleY = scaleY
    )
    val padding = 8f
    val scaledWidth = baseWidthPx * scale
    val scaledHeight = baseHeightPx * scale
    return LayerFrameSpec(
        layerId = layerId,
        x = offsetXPx - ((scaledWidth - baseWidthPx) / 2f) - padding,
        y = offsetYPx - ((scaledHeight - baseHeightPx) / 2f) - padding,
        width = scaledWidth + padding * 2f,
        height = scaledHeight + padding * 2f,
        rotation = rotation,
        transform = transform,
        scaleX = scaleX,
        scaleY = scaleY,
        scaleAppliedToFrame = true,
        minScale = minScale,
        maxScale = maxScale,
        corners = mapped.toFrameCorners()
    )
}

private fun com.example.diywallpaper.core.render.EditorLayerMappedCorners.toFrameCorners(): FrameCorners {
    return FrameCorners(
        topLeft = Offset(topLeftX, topLeftY),
        topRight = Offset(topRightX, topRightY),
        bottomRight = Offset(bottomRightX, bottomRightY),
        bottomLeft = Offset(bottomLeftX, bottomLeftY)
    )
}

private fun DrawLayerData.isBrushStackRenderable(): Boolean {
    return this is DrawLayerData.FreeStroke ||
        this is DrawLayerData.EraseStroke ||
        this is DrawLayerData.BrushStack
}

private inline fun DrawLayerData.forEachBrushStackItem(block: (BrushStackItem) -> Unit) {
    when (this) {
        is DrawLayerData.FreeStroke -> block(BrushStackItem.Draw(stroke))
        is DrawLayerData.EraseStroke -> block(BrushStackItem.Erase(stroke))
        is DrawLayerData.BrushStack -> items.forEach(block)
        else -> Unit
    }
}

private fun BrushStroke.translate(offsetX: Float, offsetY: Float): BrushStroke {
    return copy(points = points.map { it.copy(x = it.x + offsetX, y = it.y + offsetY) })
}

private fun DrawLayerData.TextTrail.translate(offsetX: Float, offsetY: Float): DrawLayerData.TextTrail {
    return copy(points = points.map { it.copy(x = it.x + offsetX, y = it.y + offsetY) })
}

private fun DrawLayerData.StickerTrail.translate(offsetX: Float, offsetY: Float): DrawLayerData.StickerTrail {
    return copy(points = points.map { it.copy(x = it.x + offsetX, y = it.y + offsetY) })
}

private fun Offset.toStrokePoint(scaleX: Float, scaleY: Float): StrokePoint {
    return StrokePoint(x = x / scaleX, y = y / scaleY)
}

private fun Offset.rotateByDegrees(degrees: Float): Offset {
    if (degrees == 0f) return this
    val radians = Math.toRadians(degrees.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    return Offset(
        x = x * cos - y * sin,
        y = x * sin + y * cos
    )
}

private fun Offset.rotateAround(center: Offset, degrees: Float): Offset {
    return center + (this - center).rotateByDegrees(degrees)
}

private fun LayerFrameSpec.contentHitTest(point: Offset): Boolean {
    val hitSlop = 72f * transform.scale.coerceIn(1f, 2.5f)
    val expanded = corners.expandedFrom(center, hitSlop)
    return point.isInsideQuad(expanded)
}

private fun LayerFrameSpec.handleHitTest(point: Offset): Boolean {
    val handleCenter = corners.bottomRight
    val radius = (72f * transform.scale.coerceIn(1f, 2.5f)).coerceAtLeast(72f)
    return (point - handleCenter).distance() <= radius
}

private fun FrameCorners.expandedFrom(center: Offset, amount: Float): FrameCorners {
    fun Offset.expand(): Offset {
        val vector = this - center
        val length = vector.distance().coerceAtLeast(1f)
        return center + vector * ((length + amount) / length)
    }
    return FrameCorners(
        topLeft = topLeft.expand(),
        topRight = topRight.expand(),
        bottomRight = bottomRight.expand(),
        bottomLeft = bottomLeft.expand()
    )
}

private fun Offset.isInsideQuad(corners: FrameCorners): Boolean {
    fun sameSide(a: Offset, b: Offset, p: Offset, q: Offset): Boolean {
        val crossP = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        val crossQ = (b.x - a.x) * (q.y - a.y) - (b.y - a.y) * (q.x - a.x)
        return crossP * crossQ >= 0f
    }
    val center = Offset(
        x = (corners.topLeft.x + corners.topRight.x + corners.bottomRight.x + corners.bottomLeft.x) / 4f,
        y = (corners.topLeft.y + corners.topRight.y + corners.bottomRight.y + corners.bottomLeft.y) / 4f
    )
    return sameSide(corners.topLeft, corners.topRight, this, center) &&
        sameSide(corners.topRight, corners.bottomRight, this, center) &&
        sameSide(corners.bottomRight, corners.bottomLeft, this, center) &&
        sameSide(corners.bottomLeft, corners.topLeft, this, center)
}

private fun Offset.distance(): Float {
    return sqrt(x * x + y * y)
}

private fun Offset.angleDegrees(): Float {
    return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
}

private fun commitTransformDelta(
    layerId: String,
    baseTransform: LayerTransform,
    committedTransform: LayerTransform,
    onTransformLayer: (
        layerId: String,
        offsetXDelta: Float,
        offsetYDelta: Float,
        scaleMultiplier: Float,
        rotationDelta: Float
    ) -> Unit
) {
    onTransformLayer(
        layerId,
        committedTransform.offsetX - baseTransform.offsetX,
        committedTransform.offsetY - baseTransform.offsetY,
        committedTransform.scale / baseTransform.scale.coerceAtLeast(0.01f),
        committedTransform.rotation - baseTransform.rotation
    )
}

private fun EditorLayer.withRenderTransform(transform: LayerTransform?): EditorLayer {
    if (transform == null) return this
    return when (this) {
        is TextLayer -> copy(transform = transform)
        is StickerLayer -> copy(transform = transform)
        is PhotoLayer -> copy(transform = transform)
        is DrawLayer -> copy(transform = transform)
    }
}

private fun List<PointerInputChange>.centroid(): Offset {
    if (isEmpty()) return Offset.Zero
    val x = sumOf { it.position.x.toDouble() }.toFloat() / size
    val y = sumOf { it.position.y.toDouble() }.toFloat() / size
    return Offset(x, y)
}

private fun List<PointerInputChange>.distance(): Float {
    if (size < 2) return 1f
    val first = this[0].position
    val second = this[1].position
    val dx = first.x - second.x
    val dy = first.y - second.y
    return sqrt(dx * dx + dy * dy)
}

private fun List<PointerInputChange>.rotation(): Float {
    if (size < 2) return 0f
    val first = this[0].position
    val second = this[1].position
    return Math.toDegrees(
        atan2(
            y = (first.y - second.y).toDouble(),
            x = (first.x - second.x).toDouble()
        )
    ).toFloat()
}

private fun EditorTextAlign.toComposeAlign(): TextAlign {
    return when (this) {
        EditorTextAlign.START -> TextAlign.Start
        EditorTextAlign.CENTER -> TextAlign.Center
        EditorTextAlign.END -> TextAlign.End
    }
}

@Composable
private fun resolveTextColor(layer: TextLayer): Color {
    return when (val brush = layer.style.textBrush) {
        is TextBrushStyle.Solid -> parseColorHex(brush.colorHex, MaterialTheme.colorScheme.onSurface)
        is TextBrushStyle.Gradient -> parseColorHex(brush.colors.firstOrNull() ?: "#201A2E", MaterialTheme.colorScheme.onSurface)
        null -> parseColorHex(layer.style.textColorHex ?: "#201A2E", MaterialTheme.colorScheme.onSurface)
    }
}

private fun resolveTextBrushColor(brush: TextBrushStyle?, fallbackHex: String?): Color {
    return when (brush) {
        is TextBrushStyle.Solid -> parseColorHex(brush.colorHex, Color.Black)
        is TextBrushStyle.Gradient -> parseColorHex(brush.colors.firstOrNull() ?: "#8B5CF6", Color.Black)
        null -> parseColorHex(fallbackHex ?: "#8B5CF6", Color.Black)
    }
}

private fun resolveFontFamily(fontFamilyId: String) = editorFontFamily(fontFamilyId)

private fun parseColorHex(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { fallback }
}

@Composable
private fun resolveErasePreviewColor(background: EditorBackground?): Color {
    return when (background) {
        is EditorBackground.SolidColor -> parseColorHex(background.colorHex, MaterialTheme.colorScheme.surface)
        is EditorBackground.Gradient -> parseColorHex(background.colors.firstOrNull() ?: "#FFFFFF", MaterialTheme.colorScheme.surface)
        is EditorBackground.ApiImage,
        is EditorBackground.LocalImage,
        null -> MaterialTheme.colorScheme.surface
    }
}

@Composable
private fun pxToDp(px: Float): Dp {
    return with(LocalDensity.current) { px.toDp() }
}
