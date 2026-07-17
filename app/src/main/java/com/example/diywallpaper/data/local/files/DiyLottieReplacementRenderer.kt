package com.example.diywallpaper.data.local.files

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import kotlin.math.roundToInt

internal fun renderDiyLottieReplacementBitmap(
    element: DiyTemplateElementSnapshot,
    sourceBitmap: Bitmap
): Bitmap {
    val outputWidth = element.width.roundToInt().coerceAtLeast(1)
    val outputHeight = element.height.roundToInt().coerceAtLeast(1)
    val transform = element.contentTransform
    if (transform == null || element.contentBaseWidth == null || element.contentBaseHeight == null) {
        return sourceBitmap
    }

    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val baseWidth = element.contentBaseWidth.coerceAtLeast(1f)
    val baseHeight = element.contentBaseHeight.coerceAtLeast(1f)
    val baseLeft = transform.offsetX - element.x
    val baseTop = transform.offsetY - element.y
    val centerX = baseLeft + baseWidth / 2f
    val centerY = baseTop + baseHeight / 2f
    val scaledWidth = baseWidth * transform.scale
    val scaledHeight = baseHeight * transform.scale
    val contentRect = RectF(
        centerX - scaledWidth / 2f,
        centerY - scaledHeight / 2f,
        centerX + scaledWidth / 2f,
        centerY + scaledHeight / 2f
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    Canvas(output).apply {
        clipRect(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
        val relativeRotation = transform.rotation - element.rotation
        if (relativeRotation != 0f) {
            rotate(relativeRotation, contentRect.centerX(), contentRect.centerY())
        }
        drawBitmap(sourceBitmap, sourceBitmap.cropRect(element.crop), contentRect, paint)
    }
    return output
}

private fun Bitmap.cropRect(crop: CropSpec?): android.graphics.Rect {
    val left = ((crop?.normalizedLeft ?: 0f).coerceIn(0f, 1f) * width)
        .roundToInt()
        .coerceIn(0, width - 1)
    val top = ((crop?.normalizedTop ?: 0f).coerceIn(0f, 1f) * height)
        .roundToInt()
        .coerceIn(0, height - 1)
    val right = ((crop?.normalizedRight ?: 1f).coerceIn(0f, 1f) * width)
        .roundToInt()
        .coerceIn(left + 1, width)
    val bottom = ((crop?.normalizedBottom ?: 1f).coerceIn(0f, 1f) * height)
        .roundToInt()
        .coerceIn(top + 1, height)
    return android.graphics.Rect(left, top, right, bottom)
}
