package com.example.diywallpaper.core.render

import com.example.diywallpaper.domain.model.design.DesignRawBounds
import com.example.diywallpaper.domain.model.design.LayerTransform
import kotlin.math.cos
import kotlin.math.sin

data class EditorLayerMappedCorners(
    val topLeftX: Float,
    val topLeftY: Float,
    val topRightX: Float,
    val topRightY: Float,
    val bottomRightX: Float,
    val bottomRightY: Float,
    val bottomLeftX: Float,
    val bottomLeftY: Float
)

fun mapLayerBoundsToTargetCorners(
    bounds: DesignRawBounds,
    transform: LayerTransform,
    scaleX: Float,
    scaleY: Float
): EditorLayerMappedCorners {
    val centerX = transform.offsetX + (bounds.minX + bounds.maxX) / 2f
    val centerY = transform.offsetY + (bounds.minY + bounds.maxY) / 2f
    val radians = Math.toRadians(transform.rotation.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()

    fun map(localX: Float, localY: Float): Pair<Float, Float> {
        val modelX = transform.offsetX + localX
        val modelY = transform.offsetY + localY
        val dx = modelX - centerX
        val dy = modelY - centerY
        val scaledDx = dx * transform.scale
        val scaledDy = dy * transform.scale
        val rotatedX = centerX + scaledDx * cos - scaledDy * sin
        val rotatedY = centerY + scaledDx * sin + scaledDy * cos
        return rotatedX * scaleX to rotatedY * scaleY
    }

    val topLeft = map(bounds.minX, bounds.minY)
    val topRight = map(bounds.maxX, bounds.minY)
    val bottomRight = map(bounds.maxX, bounds.maxY)
    val bottomLeft = map(bounds.minX, bounds.maxY)
    return EditorLayerMappedCorners(
        topLeftX = topLeft.first,
        topLeftY = topLeft.second,
        topRightX = topRight.first,
        topRightY = topRight.second,
        bottomRightX = bottomRight.first,
        bottomRightY = bottomRight.second,
        bottomLeftX = bottomLeft.first,
        bottomLeftY = bottomLeft.second
    )
}
