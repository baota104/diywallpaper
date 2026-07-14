package com.example.diywallpaper.ui.feature.preview.device

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.CropPresetRatio
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.ui.feature.editor.editorFontFamily
import kotlin.math.max
import kotlin.math.roundToInt

private const val BasePreviewLayerSide = 220f

@Composable
fun SavedDesignDevicePreview(
    project: EditorProject,
    isChromeVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        SavedDesignProjectCanvas(
            project = project,
            modifier = Modifier.fillMaxSize()
        )

        if (isChromeVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 94.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.preview_mock_date),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = stringResource(id = R.string.preview_mock_large_time),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SavedDesignProjectCanvas(
    project: EditorProject,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(0.dp))
                .clipToBounds()
        ) {
            val modelWidth = project.canvas.width.toFloat().coerceAtLeast(1f)
            val modelHeight = project.canvas.height.toFloat().coerceAtLeast(1f)
            val scaleX = constraints.maxWidth.toFloat() / modelWidth
            val scaleY = constraints.maxHeight.toFloat() / modelHeight
            val eraseColor = resolvePreviewEraseColor(project.background)

            SavedDesignBackground(
                background = project.background,
                modifier = Modifier.fillMaxSize()
            )

            project.layers
                .filterNot { it.isHidden }
                .sortedBy { it.zIndex }
                .forEach { layer ->
                    when (layer) {
                        is StickerLayer -> SavedStickerLayer(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )

                        is PhotoLayer -> SavedPhotoLayer(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )

                        is TextLayer -> SavedTextLayer(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )

                        is DrawLayer -> SavedDrawLayer(
                            layer = layer,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            eraseColor = eraseColor
                        )
                    }
                }
        }
    }
}

@Composable
private fun SavedDesignBackground(
    background: EditorBackground,
    modifier: Modifier = Modifier
) {
    when (background) {
        is EditorBackground.ApiImage -> AsyncImage(
            model = background.imageUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.LocalImage -> AsyncImage(
            model = background.localPath,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.Gradient -> Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(background.colors.map { parsePreviewColor(it) })
            )
        )

        is EditorBackground.SolidColor -> Box(
            modifier = modifier.background(parsePreviewColor(background.colorHex))
        )
    }
}

@Composable
private fun SavedStickerLayer(
    layer: StickerLayer,
    scaleX: Float,
    scaleY: Float
) {
    SavedLayerBox(
        transform = layer.transform,
        width = BasePreviewLayerSide,
        height = BasePreviewLayerSide,
        scaleX = scaleX,
        scaleY = scaleY
    ) {
        AsyncImage(
            model = layer.animatedAssetPathOrUrl ?: layer.assetPathOrUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SavedPhotoLayer(
    layer: PhotoLayer,
    scaleX: Float,
    scaleY: Float
) {
    val baseSize = photoPreviewBaseSize(layer.crop?.ratio)
    SavedLayerBox(
        transform = layer.transform,
        width = baseSize.width,
        height = baseSize.height,
        scaleX = scaleX,
        scaleY = scaleY
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
                    srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
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
private fun SavedTextLayer(
    layer: TextLayer,
    scaleX: Float,
    scaleY: Float
) {
    SavedLayerBox(
        transform = layer.transform,
        width = BasePreviewLayerSide * 1.4f,
        height = BasePreviewLayerSide * 0.45f,
        scaleX = scaleX,
        scaleY = scaleY
    ) {
        Text(
            text = layer.text,
            style = TextStyle(
                color = resolvePreviewTextColor(layer.style.textBrush, layer.style.textColorHex),
                fontFamily = editorFontFamily(layer.style.fontFamilyId),
                fontSize = layer.style.fontSizeSp.sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SavedDrawLayer(
    layer: DrawLayer,
    scaleX: Float,
    scaleY: Float,
    eraseColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (val data = layer.drawData) {
            is DrawLayerData.FreeStroke -> drawPreviewStroke(
                stroke = data.stroke,
                transform = layer.transform,
                scaleX = scaleX,
                scaleY = scaleY,
                color = parsePreviewColor(data.stroke.colorHex ?: "#1D1726")
            )

            is DrawLayerData.EraseStroke -> drawPreviewStroke(
                stroke = data.stroke,
                transform = layer.transform,
                scaleX = scaleX,
                scaleY = scaleY,
                color = eraseColor
            )

            is DrawLayerData.StickerTrail -> Unit
            is DrawLayerData.TextTrail -> drawPreviewTextTrail(
                data = data,
                transform = layer.transform,
                scaleX = scaleX,
                scaleY = scaleY
            )
        }
    }
}

@Composable
private fun SavedLayerBox(
    transform: LayerTransform,
    width: Float,
    height: Float,
    scaleX: Float,
    scaleY: Float,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (transform.offsetX * scaleX).roundToInt(),
                    y = (transform.offsetY * scaleY).roundToInt()
                )
            }
            .size(
                width = with(density) { (width * scaleX).toDp() },
                height = with(density) { (height * scaleY).toDp() }
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale,
                alpha = transform.alpha
            )
    ) {
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewStroke(
    stroke: BrushStroke,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float,
    color: Color
) {
    if (stroke.points.size < 2) return
    val path = Path()
    stroke.points.forEachIndexed { index, point ->
        val x = (point.x + transform.offsetX) * scaleX
        val y = (point.y + transform.offsetY) * scaleY
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = stroke.strokeWidth * ((scaleX + scaleY) / 2f) * transform.scale,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewTextTrail(
    data: DrawLayerData.TextTrail,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float
) {
    if (data.points.isEmpty() || data.text.isBlank()) return
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = resolvePreviewTextColor(data.textStyle.textBrush, data.textStyle.textColorHex).toArgb()
        textSize = data.textStyle.fontSizeSp * density * ((scaleX + scaleY) / 2f)
        isFakeBoldText = true
    }
    nativeCanvas.save()
    nativeCanvas.translate(transform.offsetX * scaleX, transform.offsetY * scaleY)
    nativeCanvas.scale(transform.scale, transform.scale)
    nativeCanvas.rotate(transform.rotation)
    data.points.forEachIndexed { index, point ->
        if (index % 2 == 0) {
            nativeCanvas.drawText(data.text, point.x * scaleX, point.y * scaleY, paint)
        }
    }
    nativeCanvas.restore()
}

private fun photoPreviewBaseSize(ratio: CropPresetRatio?): PreviewLayerSize {
    return when (ratio) {
        CropPresetRatio.RATIO_9_16, null -> PreviewLayerSize(220f, 391f)
        CropPresetRatio.RATIO_3_4 -> PreviewLayerSize(220f, 293f)
        CropPresetRatio.RATIO_2_3 -> PreviewLayerSize(220f, 330f)
        CropPresetRatio.RATIO_1_1 -> PreviewLayerSize(220f, 220f)
    }
}

private data class PreviewLayerSize(
    val width: Float,
    val height: Float
)

private fun resolvePreviewTextColor(
    brush: TextBrushStyle?,
    fallbackHex: String?
): Color {
    return when (brush) {
        is TextBrushStyle.Solid -> parsePreviewColor(brush.colorHex)
        is TextBrushStyle.Gradient -> parsePreviewColor(brush.colors.firstOrNull() ?: fallbackHex ?: "#1D1726")
        null -> parsePreviewColor(fallbackHex ?: "#1D1726")
    }
}

private fun resolvePreviewEraseColor(background: EditorBackground): Color {
    return when (background) {
        is EditorBackground.SolidColor -> parsePreviewColor(background.colorHex)
        else -> Color.White
    }
}

private fun parsePreviewColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color.White)
}
