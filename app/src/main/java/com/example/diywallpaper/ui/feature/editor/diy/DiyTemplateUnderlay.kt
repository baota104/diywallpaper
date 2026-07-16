package com.example.diywallpaper.ui.feature.editor.diy

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.LayerTransform
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DiyTemplateUnderlay(
    elements: List<DiyTemplateElementSnapshot>,
    scaleX: Float,
    scaleY: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        elements
            .sortedBy { it.zIndex }
            .forEach { element ->
                when {
                    element.isTextElement() -> {
                        DiyTemplateText(
                            element = element,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )
                    }

                    element.isAssetElement() -> {
                        DiyTemplatePicture(
                            element = element,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )
                    }

                    element.isImageElement() -> {
                        DiySlotPreview(
                            element = element,
                            scaleX = scaleX,
                            scaleY = scaleY
                        )
                    }
                }
            }
    }
}

@Composable
private fun DiyTemplateText(
    element: DiyTemplateElementSnapshot,
    scaleX: Float,
    scaleY: Float
) {
    val transform = element.editTransform()
    val textColor = parseTemplateColor(element.fontColor, Color.Black)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (transform.offsetX * scaleX).roundToInt(),
                    y = (transform.offsetY * scaleY).roundToInt()
                )
            }
            .size(
                width = with(LocalDensity.current) { (element.width * scaleX).toDp() },
                height = with(LocalDensity.current) { (element.height * scaleY).toDp() }
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paint = TextPaint().apply {
                color = textColor.toArgb()
                textSize = max(1f, element.fontSize * ((scaleX + scaleY) / 2f))
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    element.title,
                    0f,
                    paint.textSize,
                    paint
                )
            }
        }
    }
}

@Composable
private fun DiyTemplatePicture(
    element: DiyTemplateElementSnapshot,
    scaleX: Float,
    scaleY: Float
) {
    val transform = element.editTransform()
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (transform.offsetX * scaleX).roundToInt(),
                    y = (transform.offsetY * scaleY).roundToInt()
                )
            }
            .size(
                width = with(LocalDensity.current) { (element.width * scaleX).toDp() },
                height = with(LocalDensity.current) { (element.height * scaleY).toDp() }
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale
            )
    ) {
        AsyncImage(
            model = element.assetUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun DiySlotPreview(
    element: DiyTemplateElementSnapshot,
    scaleX: Float,
    scaleY: Float
) {
    if (element.localImagePath.isNullOrBlank() && element.previewPathOrUrl.isNullOrBlank()) return
    val transform = element.editTransform()
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (element.x * scaleX).roundToInt(),
                    y = (element.y * scaleY).roundToInt()
                )
            }
            .size(
                width = with(LocalDensity.current) { (element.width * scaleX).toDp() },
                height = with(LocalDensity.current) { (element.height * scaleY).toDp() }
            )
            .graphicsLayer(
                rotationZ = element.rotation,
                clip = true,
                compositingStrategy = CompositingStrategy.Offscreen
            )
            .diySlotMask(element.maskPathOrUrl)
    ) {
        if (!element.localImagePath.isNullOrBlank()) {
            DiySlotImageContent(
                element = element,
                transform = transform,
                scaleX = scaleX,
                scaleY = scaleY
            )
        } else {
            AsyncImage(
                model = element.previewPathOrUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun DiySlotImageContent(
    element: DiyTemplateElementSnapshot,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float
) {
    val imagePath = element.localImagePath.orEmpty()
    val imageBitmap = remember(imagePath) {
        BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
    }
    val baseWidth = element.contentBaseWidth ?: element.width
    val baseHeight = element.contentBaseHeight ?: element.height
    val contentModifier = Modifier
        .offset {
            IntOffset(
                x = ((transform.offsetX - element.x) * scaleX).roundToInt(),
                y = ((transform.offsetY - element.y) * scaleY).roundToInt()
            )
        }
        .size(
            width = with(LocalDensity.current) { (baseWidth * scaleX).toDp() },
            height = with(LocalDensity.current) { (baseHeight * scaleY).toDp() }
        )
        .graphicsLayer(
            scaleX = transform.scale,
            scaleY = transform.scale,
            rotationZ = transform.rotation - element.rotation
        )
    if (imageBitmap != null) {
        Canvas(modifier = contentModifier) {
            val crop = element.crop
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
            model = imagePath,
            contentDescription = null,
            modifier = contentModifier,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun Modifier.diySlotMask(maskPathOrUrl: String?): Modifier {
    val maskBitmap = remember(maskPathOrUrl) {
        maskPathOrUrl
            ?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    } ?: return this

    return drawWithContent {
        drawContent()
        drawImage(
            image = maskBitmap,
            dstSize = IntSize(
                width = size.width.roundToInt().coerceAtLeast(1),
                height = size.height.roundToInt().coerceAtLeast(1)
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

private fun DiyTemplateElementSnapshot.isTextElement(): Boolean {
    return type.equals("TEXT", ignoreCase = true) ||
        type.equals("Text", ignoreCase = true)
}

private fun DiyTemplateElementSnapshot.isAssetElement(): Boolean {
    return !isImageElement() &&
        !isTextElement() &&
        !assetUrl.isNullOrBlank()
}

private fun DiyTemplateElementSnapshot.isImageElement(): Boolean {
    return type.equals("IMAGE", ignoreCase = true) ||
        type.equals("Image", ignoreCase = true)
}

private fun parseTemplateColor(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrElse { fallback }
}

private fun DiyTemplateElementSnapshot.editTransform(): LayerTransform {
    if (isFixedTemplateElement()) {
        return LayerTransform(x, y, 1f, rotation)
    }
    return if (isImageElement() && localImagePath != null) {
        contentTransform ?: LayerTransform(x, y, 1f, rotation)
    } else {
        transform ?: LayerTransform(x, y, 1f, rotation)
    }
}

private fun DiyTemplateElementSnapshot.isFixedTemplateElement(): Boolean {
    return type.equals("PICTURE", ignoreCase = true) ||
        type.equals("Picture", ignoreCase = true)
}
