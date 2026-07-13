package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft
import kotlin.math.roundToInt

private val CardWidth = 88.dp
private val CardHeight = 124.dp
private val CardSpacing = 12.dp

@Composable
fun LayersToolPanel(
    background: EditorBackground?,
    layers: List<EditorLayer>,
    selectedLayerId: String?,
    onSelectLayer: (String?) -> Unit,
    onMoveLayer: (layerId: String, targetIndex: Int) -> Unit,
    onRemoveSelectedLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedContentLayers = layers.sortedBy { it.zIndex }
    val contentOrder = remember { mutableStateListOf<String>() }
    LaunchedEffect(orderedContentLayers.map { it.id }) {
        contentOrder.clear()
        contentOrder.addAll(orderedContentLayers.map { it.id })
    }
    val contentById = orderedContentLayers.associateBy { it.id }
    val orderedItems = buildList {
        add(LayerPreviewItem.Background(background))
        contentOrder.forEach { id ->
            contentById[id]?.let { add(LayerPreviewItem.Content(it)) }
        }
    }
    val itemStepPx = with(LocalDensity.current) {
        (CardWidth + CardSpacing).toPx()
    }

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(CardSpacing)) {
            itemsIndexed(orderedItems, key = { _, item -> item.stableKey() }) { index, item ->
                when (item) {
                    is LayerPreviewItem.Background -> {
                        LayerThumbnailCard(
                            item = item,
                            selected = selectedLayerId == null,
                            dragOffsetPx = 0f,
                            onClick = { onSelectLayer(null) }
                        )
                    }

                    is LayerPreviewItem.Content -> {
                        val currentIndex = contentOrder.indexOf(item.layer.id)
                        var dragOffsetPx by remember(item.layer.id) { mutableFloatStateOf(0f) }
                        var startIndex by remember(item.layer.id) { mutableIntStateOf(currentIndex) }
                        LayerThumbnailCard(
                            item = item,
                            selected = item.layer.id == selectedLayerId,
                            dragOffsetPx = dragOffsetPx,
                            onClick = { onSelectLayer(item.layer.id) },
                            onRemove = if (item.layer.id == selectedLayerId) onRemoveSelectedLayer else null,
                            onHandleDragStart = {
                                startIndex = contentOrder.indexOf(item.layer.id)
                                dragOffsetPx = 0f
                                onSelectLayer(item.layer.id)
                            },
                            onHandleDrag = { dragAmountX ->
                                val maxLeft = -startIndex * itemStepPx
                                val maxRight = (contentOrder.lastIndex - startIndex) * itemStepPx
                                dragOffsetPx = (dragOffsetPx + dragAmountX).coerceIn(maxLeft, maxRight)
                            },
                            onHandleDragEnd = {
                                val shift = (dragOffsetPx / itemStepPx).roundToInt()
                                val targetIndex = (startIndex + shift).coerceIn(0, contentOrder.lastIndex)
                                dragOffsetPx = 0f
                                if (targetIndex != startIndex) {
                                    onMoveLayer(item.layer.id, targetIndex)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerThumbnailCard(
    item: LayerPreviewItem,
    selected: Boolean,
    dragOffsetPx: Float,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onHandleDragStart: (() -> Unit)? = null,
    onHandleDrag: ((Float) -> Unit)? = null,
    onHandleDragEnd: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (selected) PrimarySoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(width = CardWidth, height = CardHeight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (item) {
                    is LayerPreviewItem.Background -> BackgroundThumbnail(item.background, onClick = onClick)
                    is LayerPreviewItem.Content -> LayerThumbnail(item.layer, onClick = onClick)
                }

                if (selected && onRemove != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(22.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.86f))
                            .pointerInput(onRemove) { detectTapGestures(onTap = { onRemove() }) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
                .pointerInput(item.stableKey(), onHandleDragStart, onHandleDrag, onHandleDragEnd) {
                    if (onHandleDragStart != null && onHandleDrag != null && onHandleDragEnd != null) {
                        detectDragGestures(
                            onDragStart = { onHandleDragStart() },
                            onDragEnd = { onHandleDragEnd() },
                            onDragCancel = { onHandleDragEnd() }
                        ) { change, dragAmount ->
                            change.consume()
                            onHandleDrag(dragAmount.x)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (item) {
                    is LayerPreviewItem.Background -> Icons.Outlined.Lock
                    is LayerPreviewItem.Content -> Icons.Outlined.Menu
                },
                contentDescription = null,
                tint = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BackgroundThumbnail(
    background: EditorBackground?,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        when (background) {
            is EditorBackground.ApiImage -> AsyncImage(
            model = background.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentScale = ContentScale.Crop
        )

            is EditorBackground.LocalImage -> AsyncImage(
                model = background.localPath,
                contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentScale = ContentScale.Crop
        )

            is EditorBackground.Gradient -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        background.colors.map { parseColorHex(it, MaterialTheme.colorScheme.surface) }
                    )
                )
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            )

            is EditorBackground.SolidColor -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(parseColorHex(background.colorHex, MaterialTheme.colorScheme.surface))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            )

            null -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) }
            )
        }
    }
}

@Composable
private fun LayerThumbnail(
    layer: EditorLayer,
    onClick: () -> Unit
) {
    when (layer) {
        is TextLayer -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = layer.text.ifBlank { "Text" },
                style = MaterialTheme.typography.labelLarge,
                color = when (val brush = layer.style.textBrush) {
                    is TextBrushStyle.Solid -> parseColorHex(brush.colorHex, Primary)
                    is TextBrushStyle.Gradient -> parseColorHex(brush.colors.firstOrNull() ?: "#201A2E", Primary)
                    null -> parseColorHex(layer.style.textColorHex ?: "#201A2E", Primary)
                }
            )
        }

        is StickerLayer -> AsyncImage(
            model = layer.assetPathOrUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentScale = ContentScale.Crop
        )

        is PhotoLayer -> AsyncImage(
            model = layer.localPath,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentScale = ContentScale.Crop
        )

        is DrawLayer -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
            contentAlignment = Alignment.Center
        ) {
            DrawLayerThumbnail(layer = layer)
        }
    }
}

private sealed interface LayerPreviewItem {
    data class Background(val background: EditorBackground?) : LayerPreviewItem
    data class Content(val layer: EditorLayer) : LayerPreviewItem
}

private fun LayerPreviewItem.stableKey(): String {
    return when (this) {
        is LayerPreviewItem.Background -> "background"
        is LayerPreviewItem.Content -> layer.id
    }
}

@Composable
private fun DrawLayerThumbnail(layer: DrawLayer) {
    when (val drawData = layer.drawData) {
        is DrawLayerData.FreeStroke -> StrokeThumbnail(
            points = drawData.stroke.points,
            color = parseColorHex(drawData.stroke.colorHex ?: "#1C1527", Primary),
            strokeWidth = drawData.stroke.strokeWidth,
            erase = false
        )

        is DrawLayerData.EraseStroke -> StrokeThumbnail(
            points = drawData.stroke.points,
            color = MaterialTheme.colorScheme.outlineVariant,
            strokeWidth = drawData.stroke.strokeWidth,
            erase = true
        )

        is DrawLayerData.TextTrail -> TextTrailThumbnail(drawData)

        is DrawLayerData.StickerTrail -> AsyncImage(
            model = drawData.stickerAssetPathOrUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StrokeThumbnail(
    points: List<StrokePoint>,
    color: Color,
    strokeWidth: Float,
    erase: Boolean
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        if (points.size < 2) return@Canvas
        val bounds = points.toThumbnailBounds()
        val path = Path().apply {
            val first = points.first().mapToThumbnail(bounds, size.width, size.height)
            moveTo(first.x, first.y)
            points.drop(1).forEach { point ->
                val mapped = point.mapToThumbnail(bounds, size.width, size.height)
                lineTo(mapped.x, mapped.y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = (strokeWidth / 3f).coerceIn(3f, 12f),
                cap = StrokeCap.Round
            ),
            alpha = if (erase) 0.72f else 1f
        )
    }
}

@Composable
private fun TextTrailThumbnail(drawData: DrawLayerData.TextTrail) {
    Box(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        val points = drawData.points.ifEmpty { listOf(StrokePoint(0f, 0f)) }
        val bounds = points.toThumbnailBounds()
        points.take(6).forEachIndexed { index, point ->
            val mapped = point.mapToThumbnail(bounds, 72f, 92f)
            Text(
                text = drawData.text.ifBlank { "Text" },
                color = when (val brush = drawData.textStyle.textBrush) {
                    is TextBrushStyle.Solid -> parseColorHex(brush.colorHex, Primary)
                    is TextBrushStyle.Gradient -> parseColorHex(brush.colors.firstOrNull() ?: "#8B5CF6", Primary)
                    null -> parseColorHex(drawData.textStyle.textColorHex ?: "#8B5CF6", Primary)
                },
                fontSize = 11.sp,
                modifier = Modifier.offset {
                    IntOffset(
                        x = mapped.x.roundToInt(),
                        y = (mapped.y + index % 2 * 4f).roundToInt()
                    )
                }
            )
        }
    }
}

private data class ThumbnailBounds(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float
)

private fun List<StrokePoint>.toThumbnailBounds(): ThumbnailBounds {
    val minX = minOf { it.x }
    val minY = minOf { it.y }
    val maxX = maxOf { it.x }
    val maxY = maxOf { it.y }
    return ThumbnailBounds(
        minX = minX,
        minY = minY,
        width = (maxX - minX).coerceAtLeast(1f),
        height = (maxY - minY).coerceAtLeast(1f)
    )
}

private fun StrokePoint.mapToThumbnail(bounds: ThumbnailBounds, width: Float, height: Float): Offset {
    val horizontalPadding = 10f
    val verticalPadding = 10f
    return Offset(
        x = horizontalPadding + ((x - bounds.minX) / bounds.width) * (width - horizontalPadding * 2f).coerceAtLeast(1f),
        y = verticalPadding + ((y - bounds.minY) / bounds.height) * (height - verticalPadding * 2f).coerceAtLeast(1f)
    )
}

private fun parseColorHex(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { fallback }
}
