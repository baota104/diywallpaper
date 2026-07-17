package com.example.diywallpaper.data.local.files

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieImageAsset
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import kotlin.math.abs
import kotlin.math.roundToInt

data class DiyLottieReplacementImage(
    val bitmap: Bitmap,
    val crop: CropSpec? = null
)

class DiyLottieAssetDelegate(
    private val replacementBitmaps: Map<String, DiyLottieReplacementImage>,
    private val defaultBitmaps: Map<String, Bitmap?>
) : ImageAssetDelegate {
    override fun fetchBitmap(asset: LottieImageAsset): Bitmap? {
        val fileName = asset.fileName.orEmpty()
        val replacement = replacementBitmaps[fileName]
            ?: replacementBitmaps[fileName.fileNameOnly()]
        if (replacement != null) {
            return replacement.fitToAsset(
                width = asset.width,
                height = asset.height
            )
        }
        return defaultBitmaps[fileName]
            ?: defaultBitmaps[fileName.fileNameOnly()]
    }

    private fun DiyLottieReplacementImage.fitToAsset(
        width: Int,
        height: Int
    ): Bitmap {
        val targetWidth = width.coerceAtLeast(1)
        val targetHeight = height.coerceAtLeast(1)
        if (bitmap.width == targetWidth && bitmap.height == targetHeight && crop == null) {
            return bitmap
        }
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            bitmap,
            bitmap.cropRect(crop),
            RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat()),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return output
    }

    private fun Bitmap.cropRect(crop: CropSpec?): Rect {
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
        return Rect(left, top, right, bottom)
    }

    private fun String.fileNameOnly(): String {
        return substringAfterLast("/").substringAfterLast("\\")
    }
}

internal fun lottieAssetKeys(srcName: String): List<String> {
    val normalized = srcName.trim()
    if (normalized.isBlank()) return emptyList()
    val fileName = normalized.substringAfterLast("/").substringAfterLast("\\")
    return listOf(normalized, fileName).distinct()
}

internal fun DiyTemplateElementSnapshot.effectiveLottieCrop(): CropSpec? {
    val transform = contentTransform ?: return crop
    val baseWidth = contentBaseWidth?.takeIf { it > 0f } ?: return crop
    val baseHeight = contentBaseHeight?.takeIf { it > 0f } ?: return crop
    val scale = transform.scale.takeIf { it > 0.0001f } ?: return crop
    if (abs(transform.rotation - rotation) > 0.5f) return crop

    val visibleLeftInContent = ((x - transform.offsetX) / scale).coerceIn(0f, baseWidth)
    val visibleTopInContent = ((y - transform.offsetY) / scale).coerceIn(0f, baseHeight)
    val visibleRightInContent = ((x + width - transform.offsetX) / scale).coerceIn(0f, baseWidth)
    val visibleBottomInContent = ((y + height - transform.offsetY) / scale).coerceIn(0f, baseHeight)
    if (visibleRightInContent <= visibleLeftInContent || visibleBottomInContent <= visibleTopInContent) {
        return crop
    }

    val baseCropLeft = (crop?.normalizedLeft ?: 0f).coerceIn(0f, 1f)
    val baseCropTop = (crop?.normalizedTop ?: 0f).coerceIn(0f, 1f)
    val baseCropRight = (crop?.normalizedRight ?: 1f).coerceIn(baseCropLeft, 1f)
    val baseCropBottom = (crop?.normalizedBottom ?: 1f).coerceIn(baseCropTop, 1f)
    val cropWidth = (baseCropRight - baseCropLeft).takeIf { it > 0.0001f } ?: return crop
    val cropHeight = (baseCropBottom - baseCropTop).takeIf { it > 0.0001f } ?: return crop

    val nextLeft = baseCropLeft + (visibleLeftInContent / baseWidth) * cropWidth
    val nextTop = baseCropTop + (visibleTopInContent / baseHeight) * cropHeight
    val nextRight = baseCropLeft + (visibleRightInContent / baseWidth) * cropWidth
    val nextBottom = baseCropTop + (visibleBottomInContent / baseHeight) * cropHeight
    if (nextRight <= nextLeft || nextBottom <= nextTop) return crop

    return CropSpec(
        normalizedLeft = nextLeft.coerceIn(0f, 1f),
        normalizedTop = nextTop.coerceIn(0f, 1f),
        normalizedRight = nextRight.coerceIn(0f, 1f),
        normalizedBottom = nextBottom.coerceIn(0f, 1f),
        ratio = crop?.ratio
    )
}
