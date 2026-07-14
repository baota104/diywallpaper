package com.example.diywallpaper.ui.feature.editor

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.DESIGN_RENDER_LAYER_SIDE
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorTextAlign
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
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

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(34.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
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
            val activePoints = remember { mutableStateListOf<StrokePoint>() }
            val measuredLayerSizes = remember { mutableStateMapOf<String, IntSize>() }
            val previewTransforms = remember { mutableStateMapOf<String, LayerTransform>() }
            val eraseColor = resolveErasePreviewColor(uiState.background)
            val drawEnabled = uiState.activeTool == EditorTool.BRUSH_DRAW ||
                uiState.activeTool == EditorTool.BRUSH_ERASE ||
                uiState.activeTool == EditorTool.TEXT_BRUSH

            EditorCanvasBackground(
                background = uiState.background,
                modifier = Modifier.fillMaxSize()
            )

            uiState.placeholders
                .sortedBy { it.zIndex }
                .forEach { placeholder ->
                    PlaceholderOverlay(
                        placeholder = placeholder,
                        scaleX = scaleX,
                        scaleY = scaleY
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
                            onMeasured = { size -> measuredLayerSizes[layer.id] = size }
                        )

                        is StickerLayer -> ImageLayerItem(
                            layerId = layer.id,
                            imageModel = layer.assetPathOrUrl,
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

                        is DrawLayer -> DrawLayerItem(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            canvasWidthPx = canvasWidthPx,
                            canvasHeightPx = canvasHeightPx,
                            eraseColor = eraseColor,
                            isSelected = uiState.selectedLayerId == layer.id,
                            previewTransform = previewTransforms[layer.id]
                        )
                    }
                }

            if (activePoints.isNotEmpty()) {
                ActiveStrokePreview(
                    points = activePoints,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    canvasWidthPx = canvasWidthPx,
                    canvasHeightPx = canvasHeightPx,
                    brushConfig = uiState.activeBrushConfig,
                    textBrushConfig = uiState.activeTextBrushConfig,
                    eraseColor = eraseColor,
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
                            measuredSize = measuredLayerSizes[layer.id]
                        )
                    }
                EditorLayerGestureOverlay(
                    frames = layerFrames,
                    selectedLayerId = uiState.selectedLayerId,
                    onSelectLayer = onSelectLayer,
                    onPreviewTransform = { layerId, transform ->
                        previewTransforms[layerId] = transform
                    },
                    onCommitTransform = { layerId, baseTransform, committedTransform ->
                        previewTransforms.remove(layerId)
                        commitTransformDelta(layerId, baseTransform, committedTransform, onTransformLayer)
                    },
                    onCancelTransform = { layerId ->
                        previewTransforms.remove(layerId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
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
                        measuredSize = measuredLayerSizes[selectedLayer.id]
                    )?.let { frame ->
                        SelectionOverlay(
                            frame = frame,
                            onSelect = { onSelectLayer(frame.layerId) },
                            onRemove = onRemoveLayer,
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

            if (uiState.layers.isEmpty() && uiState.placeholders.isEmpty() && activePoints.isEmpty()) {
                EmptyCanvasHint(modifier = Modifier.align(Alignment.Center))
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
private fun PlaceholderOverlay(
    placeholder: PhotoPlaceholderLayer,
    scaleX: Float,
    scaleY: Float
) {
    val width = pxToDp(placeholder.width * scaleX)
    val height = pxToDp(placeholder.height * scaleY)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (placeholder.x * scaleX).roundToInt(),
                    y = (placeholder.y * scaleY).roundToInt()
                )
            }
            .size(width = width, height = height)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    size = size,
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 3f)
                )
            }
            .background(Color(0x66D6F1FF), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
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
    onMeasured: (IntSize) -> Unit
) {
    val renderTransform = previewTransform ?: layer.transform
    val textStyle = MaterialTheme.typography.bodyLarge.merge(
        TextStyle(
            color = resolveTextColor(layer),
            fontSize = layer.style.fontSizeSp.sp,
            fontFamily = resolveFontFamily(layer.style.fontFamilyId),
            textAlign = layer.style.textAlign.toComposeAlign(),
            letterSpacing = layer.style.letterSpacing.sp
        )
    )
    val textMeasurer = rememberTextMeasurer()
    val measuredTextSize = remember(layer.text, layer.style, textStyle) {
        textMeasurer.measure(
            text = layer.text.ifBlank { " " },
            style = textStyle,
            softWrap = false,
            maxLines = 1
        ).size
    }
    val measuredModelTextSize = IntSize(
        width = (measuredTextSize.width / scaleX).roundToInt().coerceAtLeast(1),
        height = (measuredTextSize.height / scaleY).roundToInt().coerceAtLeast(1)
    )
    val bounds = layer.rawBounds(measuredModelTextSize)

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
            .onGloballyPositioned {
                onMeasured(measuredTextSize)
            }
            .graphicsLayer(
                rotationZ = renderTransform.rotation,
                scaleX = renderTransform.scale,
                scaleY = renderTransform.scale,
                alpha = renderTransform.alpha
            )
    ) {
        Text(
            text = layer.text,
            style = textStyle,
            fontWeight = FontWeight.SemiBold,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.offset {
                IntOffset(
                    x = (-bounds.minX * scaleX).roundToInt(),
                    y = (-bounds.minY * scaleY).roundToInt()
                )
            }
        )
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
    val renderTransform = previewTransform ?: layer.transform
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
}

@Composable
private fun ImageLayerItem(
    layerId: String,
    imageModel: Any,
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
            .size(pxToDp(DESIGN_RENDER_LAYER_SIDE * scaleX), pxToDp(DESIGN_RENDER_LAYER_SIDE * scaleY))
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
private fun DrawLayerItem(
    layer: DrawLayer,
    scaleX: Float,
    scaleY: Float,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    eraseColor: Color,
    isSelected: Boolean,
    previewTransform: LayerTransform?
) {
    val renderTransform = previewTransform ?: layer.transform
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = renderTransform.offsetX * scaleX
                translationY = renderTransform.offsetY * scaleY
                rotationZ = renderTransform.rotation
                this.scaleX = renderTransform.scale
                this.scaleY = renderTransform.scale
                alpha = renderTransform.alpha
                transformOrigin = TransformOrigin(0f, 0f)
            }
    ) {
        when (val drawData = layer.drawData) {
            is DrawLayerData.FreeStroke -> StrokeCanvas(
                stroke = drawData.stroke,
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                eraseColor = eraseColor,
                erase = false
            )

            is DrawLayerData.EraseStroke -> StrokeCanvas(
                stroke = drawData.stroke,
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                eraseColor = eraseColor,
                erase = true
            )

            is DrawLayerData.TextTrail -> TextTrailContent(
                drawData = drawData,
                scaleX = scaleX,
                scaleY = scaleY
            )

            is DrawLayerData.StickerTrail -> StickerTrailContent(
                drawData = drawData,
                scaleX = scaleX,
                scaleY = scaleY
            )
        }
    }
}

@Composable
private fun TextTrailContent(
    drawData: DrawLayerData.TextTrail,
    scaleX: Float,
    scaleY: Float
) {
    drawData.points.forEachIndexed { index, point ->
        if (index % 2 == 0) {
            Text(
                text = drawData.text,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = resolveTextBrushColor(drawData.textStyle.textBrush, drawData.textStyle.textColorHex),
                    fontFamily = resolveFontFamily(drawData.textStyle.fontFamilyId),
                    fontSize = drawData.textStyle.fontSizeSp.sp
                ),
                modifier = Modifier.offset {
                    IntOffset(
                        x = (point.x * scaleX).roundToInt(),
                        y = (point.y * scaleY).roundToInt()
                    )
                }
            )
        }
    }
}

@Composable
private fun StickerTrailContent(
    drawData: DrawLayerData.StickerTrail,
    scaleX: Float,
    scaleY: Float
) {
    drawData.points.forEachIndexed { index, point ->
        if (index % 2 == 0) {
            AsyncImage(
                model = drawData.stickerAssetPathOrUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (point.x * scaleX).roundToInt(),
                            y = (point.y * scaleY).roundToInt()
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
                    strokeWidth = config.brushSize
                ),
                scaleX = scaleX,
                scaleY = scaleY,
                canvasWidthPx = canvasWidthPx,
                canvasHeightPx = canvasHeightPx,
                eraseColor = eraseColor,
                erase = config.erase
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
                                scale = (baseTransform.scale * scaleMultiplier).coerceIn(0.35f, 4f),
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
                                scale = (transformBase.scale * scaleMultiplier).coerceIn(0.35f, 4f),
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
    onSelect: () -> Unit,
    onRemove: () -> Unit,
    onPreviewTransform: (LayerTransform) -> Unit,
    onCommitTransform: (LayerTransform) -> Unit,
    onCancelTransform: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(frame.x.roundToInt(), frame.y.roundToInt()) }
            .size(pxToDp(frame.width), pxToDp(frame.height))
            .graphicsLayer(
                rotationZ = frame.rotation,
                scaleX = frame.visualScale,
                scaleY = frame.visualScale
            )
            .border(2.dp, Color.Black, RoundedCornerShape(10.dp))
    ) {
        OverlayHandle(
            icon = Icons.Outlined.Close,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-12).dp, y = (-12).dp),
            onClick = onRemove
        )
        ResizeHandle(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
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
            .size(28.dp)
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
            .size(30.dp)
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
    erase: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (stroke.points.size < 2) return@Canvas
        val path = Path().apply {
            moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x * scaleX, point.y * scaleY)
            }
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
                join = StrokeJoin.Round
            ),
            alpha = 1f
        )
    }
}

private data class RawBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
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
    val scaleAppliedToFrame: Boolean
) {
    val center: Offset get() = Offset(x + width / 2f, y + height / 2f)
    val visualScale: Float get() = if (scaleAppliedToFrame) 1f else transform.scale
}

private fun EditorLayer.toFrameSpec(
    scaleX: Float,
    scaleY: Float,
    measuredSize: IntSize?
): LayerFrameSpec? {
    return when (this) {
        is TextLayer -> {
            val measuredModelSize = measuredSize?.let { size ->
                IntSize(
                    width = (size.width / scaleX).roundToInt().coerceAtLeast(1),
                    height = (size.height / scaleY).roundToInt().coerceAtLeast(1)
                )
            }
            val bounds = rawBounds(measuredModelSize)
            rawFrame(
                layerId = id,
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
            scaledFrame(
                layerId = id,
                offsetXPx = transform.offsetX * scaleX,
                offsetYPx = transform.offsetY * scaleY,
                baseWidthPx = DESIGN_RENDER_LAYER_SIDE * scaleX,
                baseHeightPx = DESIGN_RENDER_LAYER_SIDE * scaleY,
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
            val bounds = drawData.rawBounds() ?: return null
            rawFrame(
                layerId = id,
                x = (bounds.minX + transform.offsetX) * scaleX,
                y = (bounds.minY + transform.offsetY) * scaleY,
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

private fun rawFrame(
    layerId: String,
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
        scaleAppliedToFrame = scaleAppliedToFrame
    )
}

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
    scaleY: Float
): LayerFrameSpec {
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
        scaleAppliedToFrame = true
    )
}

private fun DrawLayerData.rawBounds(): RawBounds? {
    return when (this) {
        is DrawLayerData.FreeStroke -> stroke.points.strokeBounds(stroke.strokeWidth * 2.8f)
        is DrawLayerData.EraseStroke -> stroke.points.strokeBounds(stroke.strokeWidth * 2.8f)
        is DrawLayerData.StickerTrail -> {
            if (points.isEmpty()) return null
            val padding = stampSize * 0.25f
            RawBounds(
                minX = points.minOf { it.x } - padding,
                minY = points.minOf { it.y } - padding,
                maxX = points.maxOf { it.x + stampSize } + padding,
                maxY = points.maxOf { it.y + stampSize } + padding
            )
        }

        is DrawLayerData.TextTrail -> {
            if (points.isEmpty()) return null
            val fontSize = textStyle.fontSizeSp.coerceAtLeast(1f)
            val textWidth = estimateTextWidth(text, fontSize)
            val textHeight = fontSize * 1.75f
            val padding = fontSize * 1.2f
            RawBounds(
                minX = points.minOf { it.x } - padding,
                minY = points.minOf { it.y } - padding,
                maxX = points.maxOf { it.x + textWidth } + padding,
                maxY = points.maxOf { it.y + textHeight } + padding
            )
        }
    }
}

private fun List<StrokePoint>.strokeBounds(padding: Float): RawBounds? {
    if (isEmpty()) return null
    return RawBounds(
        minX = minOf { it.x } - padding,
        minY = minOf { it.y } - padding,
        maxX = maxOf { it.x } + padding,
        maxY = maxOf { it.y } + padding
    )
}

private fun estimateTextWidth(text: String, fontSize: Float): Float {
    return text.ifBlank { "Text" }.length.coerceAtLeast(1) * fontSize * 0.72f
}

private fun TextLayer.rawBounds(measuredSize: IntSize? = null): RawBounds {
    val fontSize = style.fontSizeSp.coerceAtLeast(1f)
    val metrics = textFrameMetrics(style.fontFamilyId)
    val measuredWidthModel = measuredSize?.width?.toFloat()
    val measuredHeightModel = measuredSize?.height?.toFloat()
    val estimatedWidth = text.ifBlank { "Text" }
        .lineSequence()
        .maxOf { line ->
            line.length.coerceAtLeast(1) * fontSize * metrics.widthFactor +
                style.letterSpacing * line.length.coerceAtLeast(1)
        }
    val estimatedHeight = text.ifBlank { "Text" }
        .lineSequence()
        .count()
        .coerceAtLeast(1) * (style.lineHeight ?: (fontSize * metrics.heightFactor))
    val contentWidth = maxOf(measuredWidthModel ?: 0f, estimatedWidth).coerceAtLeast(fontSize)
    val contentHeight = maxOf(measuredHeightModel ?: 0f, estimatedHeight).coerceAtLeast(fontSize)
    val horizontalPadding = (fontSize * metrics.horizontalPaddingFactor).coerceAtLeast(32f)
    val verticalPadding = (fontSize * metrics.verticalPaddingFactor).coerceAtLeast(28f)
    return RawBounds(
        minX = -horizontalPadding,
        minY = -verticalPadding,
        maxX = contentWidth + horizontalPadding,
        maxY = contentHeight + verticalPadding
    )
}

private data class TextFrameMetrics(
    val widthFactor: Float,
    val heightFactor: Float,
    val horizontalPaddingFactor: Float,
    val verticalPaddingFactor: Float
)

private fun textFrameMetrics(fontFamilyId: String): TextFrameMetrics {
    return when (fontFamilyId) {
        "allura",
        "arizonia",
        "dawning_of_a_new_day",
        "licorice",
        "lieckerli_one",
        "meow_script",
        "sansita_swashed" -> TextFrameMetrics(
            widthFactor = 0.94f,
            heightFactor = 1.9f,
            horizontalPaddingFactor = 2.4f,
            verticalPaddingFactor = 1.8f
        )

        "are_you_serious",
        "londrina_shadow",
        "ma_shan_zheng" -> TextFrameMetrics(
            widthFactor = 0.82f,
            heightFactor = 1.7f,
            horizontalPaddingFactor = 1.7f,
            verticalPaddingFactor = 1.5f
        )

        else -> TextFrameMetrics(
            widthFactor = 0.72f,
            heightFactor = 1.6f,
            horizontalPaddingFactor = 1.25f,
            verticalPaddingFactor = 1.15f
        )
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
    val unrotated = center + (point - center).rotateByDegrees(-rotation)
    val visualWidth = width * visualScale
    val visualHeight = height * visualScale
    val visualLeft = center.x - visualWidth / 2f
    val visualTop = center.y - visualHeight / 2f
    return unrotated.x in (visualLeft - hitSlop)..(visualLeft + visualWidth + hitSlop) &&
        unrotated.y in (visualTop - hitSlop)..(visualTop + visualHeight + hitSlop)
}

private fun LayerFrameSpec.handleHitTest(point: Offset): Boolean {
    val handleCenter = Offset(
        x = center.x + (width * visualScale) / 2f,
        y = center.y + (height * visualScale) / 2f
    ).rotateAround(center, rotation)
    val radius = (72f * transform.scale.coerceIn(1f, 2.5f)).coerceAtLeast(72f)
    return (point - handleCenter).distance() <= radius
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
