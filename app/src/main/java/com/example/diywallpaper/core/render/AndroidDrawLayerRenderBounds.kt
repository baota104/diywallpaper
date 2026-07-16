package com.example.diywallpaper.core.render

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Path
import android.graphics.PathMeasure
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.DesignRawBounds
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.StrokePoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_VISUAL_BOUNDS_SAFETY = 112f
private const val PATTERN_VISUAL_BOUNDS_SAFETY = 144f
private const val TEXT_TRAIL_VISUAL_BOUNDS_SAFETY = 128f

fun androidDrawLayerRenderBounds(
    context: Context,
    drawData: DrawLayerData,
    fontResIdFor: (String) -> Int
): DesignRawBounds? {
    return when (drawData) {
        is DrawLayerData.FreeStroke -> drawData.stroke.androidStrokeBounds(context)
        is DrawLayerData.EraseStroke -> drawData.stroke.androidStrokeBounds(context)
        is DrawLayerData.BrushStack -> drawData.items
            .mapNotNull { item ->
                when (item) {
                    is BrushStackItem.Draw -> item.stroke.androidStrokeBounds(context)
                    is BrushStackItem.Erase -> null
                }
            }
            .reduceOrNull { acc, bounds -> acc.union(bounds) }

        is DrawLayerData.TextTrail -> drawData.androidTextTrailBounds(context, fontResIdFor)
        is DrawLayerData.StickerTrail -> drawData.androidStickerTrailBounds()
    }
}

private fun BrushStroke.androidStrokeBounds(context: Context): DesignRawBounds? {
    if (points.isEmpty()) return null
    val patternStyle = brushStyle as? BrushStyleSpec.Pattern
    if (patternStyle != null) {
        patternStrokeBounds(context, patternStyle)?.let { return it }
    }
    return points.strokeBounds(androidStrokePadding(context))
        ?.expand(visualStrokeSafetyPadding())
}

private fun BrushStroke.androidStrokePadding(context: Context): Float {
    val base = strokeWidth.coerceAtLeast(1f)
    return when (val style = brushStyle) {
        is BrushStyleSpec.Pattern -> {
            val bitmapSize = style.drawableName.androidDrawableSize(context)
            val iconRadius = if (bitmapSize != null) {
                val iconScale = (base / bitmapSize.first.coerceAtLeast(1)) *
                    style.scale.coerceAtLeast(0.1f)
                sqrt(
                    (bitmapSize.first * iconScale) * (bitmapSize.first * iconScale) +
                        (bitmapSize.second * iconScale) * (bitmapSize.second * iconScale)
                ) / 2f
            } else {
                base * style.scale.coerceAtLeast(1f)
            }
            max(base * 5.8f, iconRadius + base * 3.2f)
        }

        is BrushStyleSpec.Glow -> {
            val glowStrokeRadius = (base * 1.9f) / 2f
            val blurRadius = base * 0.9f
            glowStrokeRadius + blurRadius * 3.2f + base * 2.4f
        }

        is BrushStyleSpec.Outline -> base * 5.2f
        is BrushStyleSpec.Dashed -> base * 4.6f
        else -> base * 4.4f
    }
}

private fun BrushStroke.visualStrokeSafetyPadding(): Float {
    val base = strokeWidth.coerceAtLeast(1f)
    return when (brushStyle) {
        is BrushStyleSpec.Pattern -> max(PATTERN_VISUAL_BOUNDS_SAFETY, base * 6f)
        is BrushStyleSpec.Glow -> max(MIN_VISUAL_BOUNDS_SAFETY, base * 5.5f)
        is BrushStyleSpec.Outline -> max(MIN_VISUAL_BOUNDS_SAFETY, base * 4f)
        is BrushStyleSpec.Dashed -> max(MIN_VISUAL_BOUNDS_SAFETY, base * 3.8f)
        else -> max(MIN_VISUAL_BOUNDS_SAFETY, base * 3.6f)
    }
}

private fun BrushStroke.patternStrokeBounds(
    context: Context,
    style: BrushStyleSpec.Pattern
): DesignRawBounds? {
    if (points.size < 2) return points.strokeBounds(androidStrokePadding(context))
    val bitmapSize = style.drawableName.androidDrawableSize(context)
        ?: return points.strokeBounds(androidStrokePadding(context))
    val path = points.toAndroidPath()
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return points.strokeBounds(androidStrokePadding(context))
    val iconScale = (strokeWidth.coerceAtLeast(1f) / bitmapSize.first.coerceAtLeast(1)) *
        style.scale.coerceAtLeast(0.1f)
    val iconWidth = bitmapSize.first * iconScale
    val iconHeight = bitmapSize.second * iconScale
    val step = (bitmapSize.first * iconScale * style.spacingFactor.coerceAtLeast(0.35f))
        .coerceAtLeast(4f)
    val position = FloatArray(2)
    val tangent = FloatArray(2)
    var distance = step / 2f
    var bounds: DesignRawBounds? = null
    while (distance < length && measure.getPosTan(distance, position, tangent)) {
        val angle = if (style.followPath) {
            kotlin.math.atan2(tangent[1], tangent[0])
        } else {
            0f
        }
        val itemBounds = rotatedRectBounds(
            centerX = position[0],
            centerY = position[1],
            width = iconWidth,
            height = iconHeight,
            radians = angle
        )
        bounds = bounds?.union(itemBounds) ?: itemBounds
        distance += step
    }
    return (bounds ?: points.strokeBounds(androidStrokePadding(context)))
        ?.expand(visualStrokeSafetyPadding())
}

private fun DrawLayerData.TextTrail.androidTextTrailBounds(
    context: Context,
    fontResIdFor: (String) -> Int
): DesignRawBounds? {
    if (points.isEmpty() || text.isBlank()) return null
    val textSpec = androidTextRenderSpec(
        context = context,
        text = text,
        style = textStyle,
        fontResId = fontResIdFor(textStyle.fontFamilyId)
    )
    val fontSize = textStyle.fontSizeSp.coerceAtLeast(1f) *
        context.resources.displayMetrics.scaledDensity
    val safetyPadding = max(TEXT_TRAIL_VISUAL_BOUNDS_SAFETY, fontSize * 1.6f)
    return androidTextTrailStampPoints(this)
        .map { point ->
            DesignRawBounds(
                minX = point.x + textSpec.bounds.minX,
                minY = point.y + textSpec.bounds.minY,
                maxX = point.x + textSpec.bounds.maxX,
                maxY = point.y + textSpec.bounds.maxY
            )
        }
        .reduceOrNull { acc, bounds -> acc.union(bounds) }
        ?.expand(safetyPadding)
}

fun androidTextTrailStampPoints(data: DrawLayerData.TextTrail): List<StrokePoint> {
    if (data.points.isEmpty()) return emptyList()
    if (data.points.size < 2) return data.points
    val path = data.points.toAndroidPath()
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return data.points
    val spacing = data.spacing.coerceAtLeast(1f)
    val position = FloatArray(2)
    val stamps = mutableListOf<StrokePoint>()
    var distance = 0f
    while (distance <= length && measure.getPosTan(distance, position, null)) {
        stamps += StrokePoint(position[0], position[1])
        distance += spacing
    }
    if (stamps.isEmpty()) {
        stamps += data.points.first()
    }
    return stamps
}

private fun DrawLayerData.StickerTrail.androidStickerTrailBounds(): DesignRawBounds? {
    if (points.isEmpty()) return null
    val padding = stampSize.coerceAtLeast(1f) * 0.75f
    return DesignRawBounds(
        minX = points.minOf { it.x } - padding,
        minY = points.minOf { it.y } - padding,
        maxX = points.maxOf { it.x + stampSize } + padding,
        maxY = points.maxOf { it.y + stampSize } + padding
    )
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

private fun List<StrokePoint>.toAndroidPath(): Path {
    return Path().apply {
        moveTo(first().x, first().y)
        drop(1).forEach { point -> lineTo(point.x, point.y) }
    }
}

private fun rotatedRectBounds(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    radians: Float
): DesignRawBounds {
    val halfWidth = width / 2f
    val halfHeight = height / 2f
    val cos = cos(radians)
    val sin = sin(radians)
    val extentX = abs(halfWidth * cos) + abs(halfHeight * sin)
    val extentY = abs(halfWidth * sin) + abs(halfHeight * cos)
    return DesignRawBounds(
        minX = centerX - extentX,
        minY = centerY - extentY,
        maxX = centerX + extentX,
        maxY = centerY + extentY
    )
}

private fun DesignRawBounds.union(other: DesignRawBounds): DesignRawBounds {
    return DesignRawBounds(
        minX = min(minX, other.minX),
        minY = min(minY, other.minY),
        maxX = max(maxX, other.maxX),
        maxY = max(maxY, other.maxY)
    )
}

private fun DesignRawBounds.expand(padding: Float): DesignRawBounds {
    return DesignRawBounds(
        minX = minX - padding,
        minY = minY - padding,
        maxX = maxX + padding,
        maxY = maxY + padding
    )
}

private fun String.androidDrawableSize(context: Context): Pair<Int, Int>? {
    val resId = context.resources.getIdentifier(this, "drawable", context.packageName)
    if (resId == 0) return null
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, resId, options)
    return options.outWidth.takeIf { it > 0 }?.let { width ->
        options.outHeight.takeIf { it > 0 }?.let { height -> width to height }
    }
}
