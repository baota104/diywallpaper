package com.example.diywallpaper.domain.model.design

import kotlin.math.max
import kotlin.math.min

const val DESIGN_RENDER_LAYER_SIDE = 220f
const val DESIGN_RENDER_TEXT_WIDTH_FACTOR = 1.4f
const val DESIGN_RENDER_TEXT_HEIGHT_FACTOR = 0.45f

data class DesignRenderSize(
    val width: Float,
    val height: Float
)

data class DesignRawBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width: Float get() = (maxX - minX).coerceAtLeast(1f)
    val height: Float get() = (maxY - minY).coerceAtLeast(1f)
}

enum class DesignViewportScaleMode {
    Contain,
    Cover
}

data class DesignViewportTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
    val scaledWidth: Float,
    val scaledHeight: Float
) {
    fun modelXToTarget(x: Float): Float = offsetX + x * scale

    fun modelYToTarget(y: Float): Float = offsetY + y * scale

    fun targetXToModel(x: Float): Float = (x - offsetX) / scale.coerceAtLeast(0.0001f)

    fun targetYToModel(y: Float): Float = (y - offsetY) / scale.coerceAtLeast(0.0001f)
}

fun designViewportTransform(
    designWidth: Float,
    designHeight: Float,
    targetWidth: Float,
    targetHeight: Float,
    scaleMode: DesignViewportScaleMode
): DesignViewportTransform {
    val safeDesignWidth = designWidth.coerceAtLeast(1f)
    val safeDesignHeight = designHeight.coerceAtLeast(1f)
    val safeTargetWidth = targetWidth.coerceAtLeast(1f)
    val safeTargetHeight = targetHeight.coerceAtLeast(1f)
    val scale = when (scaleMode) {
        DesignViewportScaleMode.Contain -> min(
            safeTargetWidth / safeDesignWidth,
            safeTargetHeight / safeDesignHeight
        )

        DesignViewportScaleMode.Cover -> max(
            safeTargetWidth / safeDesignWidth,
            safeTargetHeight / safeDesignHeight
        )
    }
    val scaledWidth = safeDesignWidth * scale
    val scaledHeight = safeDesignHeight * scale
    return DesignViewportTransform(
        scale = scale,
        offsetX = (safeTargetWidth - scaledWidth) / 2f,
        offsetY = (safeTargetHeight - scaledHeight) / 2f,
        scaledWidth = scaledWidth,
        scaledHeight = scaledHeight
    )
}

fun stickerRenderSize(): DesignRenderSize {
    return DesignRenderSize(
        width = DESIGN_RENDER_LAYER_SIDE,
        height = DESIGN_RENDER_LAYER_SIDE
    )
}

fun textRenderSize(): DesignRenderSize {
    return DesignRenderSize(
        width = DESIGN_RENDER_LAYER_SIDE * DESIGN_RENDER_TEXT_WIDTH_FACTOR,
        height = DESIGN_RENDER_LAYER_SIDE * DESIGN_RENDER_TEXT_HEIGHT_FACTOR
    )
}

fun photoRenderSize(ratio: CropPresetRatio?): DesignRenderSize {
    val aspectRatio = when (ratio) {
        CropPresetRatio.RATIO_9_16 -> 9f / 16f
        CropPresetRatio.RATIO_3_4 -> 3f / 4f
        CropPresetRatio.RATIO_2_3 -> 2f / 3f
        CropPresetRatio.RATIO_1_1,
        null -> 1f
    }
    return if (aspectRatio >= 1f) {
        DesignRenderSize(
            width = DESIGN_RENDER_LAYER_SIDE,
            height = DESIGN_RENDER_LAYER_SIDE / aspectRatio
        )
    } else {
        DesignRenderSize(
            width = DESIGN_RENDER_LAYER_SIDE * aspectRatio,
            height = DESIGN_RENDER_LAYER_SIDE
        )
    }
}

fun DrawLayerData.renderBounds(): DesignRawBounds? {
    return when (this) {
        is DrawLayerData.FreeStroke -> stroke.points.strokeBounds(stroke.strokeWidth * 2.8f)
        is DrawLayerData.EraseStroke -> stroke.points.strokeBounds(stroke.strokeWidth * 2.8f)
        is DrawLayerData.BrushStack -> items
            .mapNotNull { item ->
                when (item) {
                    is BrushStackItem.Draw -> item.stroke.points.strokeBounds(item.stroke.strokeWidth * 2.8f)
                    is BrushStackItem.Erase -> null
                }
            }
            .reduceOrNull { acc, bounds ->
                DesignRawBounds(
                    minX = min(acc.minX, bounds.minX),
                    minY = min(acc.minY, bounds.minY),
                    maxX = max(acc.maxX, bounds.maxX),
                    maxY = max(acc.maxY, bounds.maxY)
                )
            }

        is DrawLayerData.StickerTrail -> {
            if (points.isEmpty()) return null
            val padding = stampSize * 0.25f
            DesignRawBounds(
                minX = points.minOf { it.x } - padding,
                minY = points.minOf { it.y } - padding,
                maxX = points.maxOf { it.x + stampSize } + padding,
                maxY = points.maxOf { it.y + stampSize } + padding
            )
        }

        is DrawLayerData.TextTrail -> {
            if (points.isEmpty()) return null
            val fontSize = textStyle.fontSizeSp.coerceAtLeast(1f)
            val textWidth = estimateRenderTextWidth(text, fontSize)
            val textHeight = fontSize * 1.75f
            val padding = fontSize * 1.2f
            DesignRawBounds(
                minX = points.minOf { it.x } - padding,
                minY = points.minOf { it.y } - padding,
                maxX = points.maxOf { it.x + textWidth } + padding,
                maxY = points.maxOf { it.y + textHeight } + padding
            )
        }
    }
}

fun TextLayer.renderBounds(
    measuredWidth: Float? = null,
    measuredHeight: Float? = null
): DesignRawBounds {
    val fontSize = style.fontSizeSp.coerceAtLeast(1f)
    val metrics = textFrameMetrics(style.fontFamilyId)
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
    val contentWidth = max(measuredWidth ?: 0f, estimatedWidth).coerceAtLeast(fontSize)
    val contentHeight = max(measuredHeight ?: 0f, estimatedHeight).coerceAtLeast(fontSize)
    val horizontalPadding = (fontSize * metrics.horizontalPaddingFactor).coerceAtLeast(32f)
    val verticalPadding = (fontSize * metrics.verticalPaddingFactor).coerceAtLeast(28f)
    return DesignRawBounds(
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
    val id = fontFamilyId.lowercase()
    return when {
        "script" in id || "dancing" in id || "pacifico" in id || "play" in id -> TextFrameMetrics(
            widthFactor = 0.9f,
            heightFactor = 1.9f,
            horizontalPaddingFactor = 1.85f,
            verticalPaddingFactor = 1.45f
        )

        "bold" in id || "round" in id -> TextFrameMetrics(
            widthFactor = 0.78f,
            heightFactor = 1.7f,
            horizontalPaddingFactor = 1.35f,
            verticalPaddingFactor = 1.2f
        )

        else -> TextFrameMetrics(
            widthFactor = 0.72f,
            heightFactor = 1.6f,
            horizontalPaddingFactor = 1.25f,
            verticalPaddingFactor = 1.15f
        )
    }
}

private fun List<StrokePoint>.strokeBounds(padding: Float): DesignRawBounds? {
    if (isEmpty()) return null
    return DesignRawBounds(
        minX = minOf { it.x } - padding,
        minY = minOf { it.y } - padding,
        maxX = maxOf { it.x } + padding,
        maxY = maxOf { it.y } + padding
    )
}

private fun estimateRenderTextWidth(text: String, fontSize: Float): Float {
    return text.ifBlank { "Text" }.length.coerceAtLeast(1) * fontSize * 0.72f
}
