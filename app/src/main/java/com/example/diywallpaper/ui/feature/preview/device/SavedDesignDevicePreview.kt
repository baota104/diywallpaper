package com.example.diywallpaper.ui.feature.preview.device

import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.DesignViewportScaleMode
import com.example.diywallpaper.domain.model.design.DesignViewportTransform
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.designViewportTransform
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.model.design.stickerRenderSize
import com.example.diywallpaper.domain.model.design.textRenderSize
import com.example.diywallpaper.ui.feature.editor.editorFontFamily
import com.example.diywallpaper.ui.feature.editor.editorFontResId
import kotlin.math.roundToInt

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
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val modelWidth = project.canvas.width.toFloat().coerceAtLeast(1f)
        val modelHeight = project.canvas.height.toFloat().coerceAtLeast(1f)
        val viewport = designViewportTransform(
            designWidth = modelWidth,
            designHeight = modelHeight,
            targetWidth = constraints.maxWidth.toFloat(),
            targetHeight = constraints.maxHeight.toFloat(),
            scaleMode = DesignViewportScaleMode.Cover
        )
        val eraseColor = resolvePreviewEraseColor(project.background)

        // Fill the whole target first. Layer positions are mapped separately via viewport.
        // This avoids Compose graphicsLayer clipping a transformed virtual child.
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
                        viewport = viewport
                    )

                    is PhotoLayer -> SavedPhotoLayer(
                        layer = layer,
                        viewport = viewport
                    )

                    is TextLayer -> SavedTextLayer(
                        layer = layer,
                        viewport = viewport
                    )

                    is DrawLayer -> SavedDrawLayer(
                        layer = layer,
                        viewport = viewport,
                        eraseColor = eraseColor
                    )
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
    viewport: DesignViewportTransform
) {
    val size = stickerRenderSize()
    SavedLayerBox(
        transform = layer.transform,
        width = size.width,
        height = size.height,
        viewport = viewport
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
    viewport: DesignViewportTransform
) {
    val baseSize = photoRenderSize(layer.crop?.ratio)
    SavedLayerBox(
        transform = layer.transform,
        width = baseSize.width,
        height = baseSize.height,
        viewport = viewport
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
    viewport: DesignViewportTransform
) {
    val size = textRenderSize()
    SavedLayerBox(
        transform = layer.transform,
        width = size.width,
        height = size.height,
        viewport = viewport
    ) {
        Text(
            text = layer.text,
            style = TextStyle(
                color = resolvePreviewTextColor(layer.style.textBrush, layer.style.textColorHex),
                fontFamily = editorFontFamily(layer.style.fontFamilyId),
                fontSize = (layer.style.fontSizeSp * viewport.scale).sp,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun SavedDrawLayer(
    layer: DrawLayer,
    viewport: DesignViewportTransform,
    eraseColor: Color
) {
    val context = LocalContext.current
    val textTrailTypeface = remember(layer.drawData) {
        val textTrail = layer.drawData as? DrawLayerData.TextTrail
        textTrail?.let {
            ResourcesCompat.getFont(context, editorFontResId(it.textStyle.fontFamilyId))
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (val data = layer.drawData) {
                is DrawLayerData.FreeStroke -> drawPreviewStroke(
                    stroke = data.stroke,
                    transform = layer.transform,
                    viewport = viewport,
                    color = parsePreviewColor(data.stroke.colorHex ?: "#1D1726")
                )

                is DrawLayerData.EraseStroke -> drawPreviewStroke(
                    stroke = data.stroke,
                    transform = layer.transform,
                    viewport = viewport,
                    color = eraseColor
                )

                is DrawLayerData.TextTrail -> drawPreviewTextTrail(
                    data = data,
                    transform = layer.transform,
                    viewport = viewport,
                    typeface = textTrailTypeface
                )

                is DrawLayerData.StickerTrail -> Unit
            }
        }
        val stickerTrail = layer.drawData as? DrawLayerData.StickerTrail
        if (stickerTrail != null) {
            DrawPreviewStickerTrail(
                data = stickerTrail,
                transform = layer.transform,
                viewport = viewport
            )
        }
    }
}

@Composable
private fun DrawPreviewStickerTrail(
    data: DrawLayerData.StickerTrail,
    transform: LayerTransform,
    viewport: DesignViewportTransform
) {
    if (data.points.isEmpty()) return
    data.points.forEachIndexed { index, point ->
        if (index % data.spacing.coerceAtLeast(1f).toInt().coerceAtLeast(1) == 0) {
            val rotation = if (data.rotationMode == StickerTrailRotationMode.FOLLOW_PATH && index > 0) {
                val previous = data.points[index - 1]
                kotlin.math.atan2(point.y - previous.y, point.x - previous.x) * 180f / Math.PI.toFloat()
            } else {
                0f
            }
            AsyncImage(
                model = data.stickerAssetPathOrUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = viewport.modelXToTarget(transform.offsetX + point.x - data.stampSize / 2f).roundToInt(),
                            y = viewport.modelYToTarget(transform.offsetY + point.y - data.stampSize / 2f).roundToInt()
                        )
                    }
                    .size(with(LocalDensity.current) { (data.stampSize * viewport.scale).toDp() })
                    .graphicsLayer(
                        rotationZ = transform.rotation + rotation,
                        scaleX = transform.scale,
                        scaleY = transform.scale,
                        alpha = transform.alpha
                    ),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun SavedLayerBox(
    transform: LayerTransform,
    width: Float,
    height: Float,
    viewport: DesignViewportTransform,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = viewport.modelXToTarget(transform.offsetX).roundToInt(),
                    y = viewport.modelYToTarget(transform.offsetY).roundToInt()
                )
            }
            .size(
                width = with(density) { (width * viewport.scale).toDp() },
                height = with(density) { (height * viewport.scale).toDp() }
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
    viewport: DesignViewportTransform,
    color: Color
) {
    if (stroke.points.size < 2) return
    drawContext.canvas.nativeCanvas.apply {
        save()
        translate(
            viewport.modelXToTarget(transform.offsetX),
            viewport.modelYToTarget(transform.offsetY)
        )
        scale(viewport.scale, viewport.scale)
        scale(transform.scale, transform.scale)
        rotate(transform.rotation)
        val path = android.graphics.Path()
        stroke.points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = stroke.strokeWidth
        }
        drawPath(path, paint)
        restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewTextTrail(
    data: DrawLayerData.TextTrail,
    transform: LayerTransform,
    viewport: DesignViewportTransform,
    typeface: Typeface?
) {
    if (data.points.isEmpty() || data.text.isBlank()) return
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = resolvePreviewTextColor(data.textStyle.textBrush, data.textStyle.textColorHex).toArgb()
        textSize = data.textStyle.fontSizeSp * density * viewport.scale
        isFakeBoldText = true
        this.typeface = typeface
    }
    nativeCanvas.save()
    nativeCanvas.translate(
        viewport.modelXToTarget(transform.offsetX),
        viewport.modelYToTarget(transform.offsetY)
    )
    nativeCanvas.scale(transform.scale, transform.scale)
    nativeCanvas.rotate(transform.rotation)
    data.points.forEachIndexed { index, point ->
        if (index % 2 == 0) {
            nativeCanvas.drawText(data.text, point.x * viewport.scale, point.y * viewport.scale, paint)
        }
    }
    nativeCanvas.restore()
}

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
