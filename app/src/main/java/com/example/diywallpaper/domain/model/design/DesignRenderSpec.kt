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
