package com.example.diywallpaper.data.local.files

import android.graphics.Bitmap
import android.graphics.Rect
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieImageAsset
import com.example.diywallpaper.domain.model.design.CropSpec
import kotlin.math.roundToInt

data class DiyLottieReplacementImage(
    val bitmap: Bitmap,
    val crop: CropSpec? = null
)

class DiyLottieAssetDelegate(
    private val replacementBitmaps: Map<String, DiyLottieReplacementImage>,
    private val defaultBitmaps: Map<String, Bitmap?>
) : ImageAssetDelegate {
    private val scaledBitmapCache = mutableMapOf<String, Bitmap>()

    override fun fetchBitmap(asset: LottieImageAsset): Bitmap? {
        val fileName = asset.fileName.orEmpty()
        val replacement = replacementBitmaps[fileName]
            ?: replacementBitmaps[fileName.fileNameOnly()]
        if (replacement != null) {
            return replacement.centerCropAndScale(
                cacheKey = "${fileName.fileNameOnly()}:${System.identityHashCode(replacement.bitmap)}:${replacement.crop}:${asset.width}x${asset.height}",
                width = asset.width,
                height = asset.height
            )
        }
        return defaultBitmaps[fileName]
            ?: defaultBitmaps[fileName.fileNameOnly()]
    }

    private fun DiyLottieReplacementImage.centerCropAndScale(
        cacheKey: String,
        width: Int,
        height: Int
    ): Bitmap {
        val targetWidth = width.coerceAtLeast(1)
        val targetHeight = height.coerceAtLeast(1)
        if (bitmap.width == targetWidth && bitmap.height == targetHeight && crop == null) {
            return bitmap
        }
        scaledBitmapCache[cacheKey]?.let { return it }
        val sourceRect = bitmap.cropRect(crop)
        val source = if (
            sourceRect.left == 0 &&
            sourceRect.top == 0 &&
            sourceRect.right == bitmap.width &&
            sourceRect.bottom == bitmap.height
        ) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, sourceRect.left, sourceRect.top, sourceRect.width(), sourceRect.height())
        }
        val scaled = source.centerCropAndScale(targetWidth, targetHeight)
        if (source !== bitmap && source !== scaled && !source.isRecycled) {
            source.recycle()
        }
        scaledBitmapCache[cacheKey] = scaled
        return scaled
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

    private fun Bitmap.centerCropAndScale(targetWidth: Int, targetHeight: Int): Bitmap {
        if (width == targetWidth && height == targetHeight) return this
        val sourceRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat().coerceAtLeast(1f)
        val cropWidth: Int
        val cropHeight: Int
        val left: Int
        val top: Int
        if (sourceRatio > targetRatio) {
            cropHeight = height
            cropWidth = (height * targetRatio).roundToInt().coerceIn(1, width)
            left = (width - cropWidth) / 2
            top = 0
        } else {
            cropWidth = width
            cropHeight = (width / targetRatio).roundToInt().coerceIn(1, height)
            left = 0
            top = (height - cropHeight) / 2
        }
        val cropped = Bitmap.createBitmap(this, left, top, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        if (cropped !== this && cropped !== scaled && !cropped.isRecycled) {
            cropped.recycle()
        }
        return scaled
    }
}

internal fun lottieAssetKeys(srcName: String): List<String> {
    val normalized = srcName.trim()
    if (normalized.isBlank()) return emptyList()
    val fileName = normalized.substringAfterLast("/").substringAfterLast("\\")
    return listOf(normalized, fileName).distinct()
}
