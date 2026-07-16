package com.example.diywallpaper.domain.model.design

import kotlin.math.cos
import kotlin.math.sin

fun DiyTemplateElementSnapshot.diyRenderTransform(): LayerTransform {
    return if (isDiyImageElement() && !localImagePath.isNullOrBlank()) {
        contentTransform ?: LayerTransform(x, y, 1f, rotation)
    } else {
        transform ?: LayerTransform(x, y, 1f, rotation)
    }
}

fun DiyTemplateElementSnapshot.proceduralAnimatedTransform(
    templateId: String,
    timeMs: Long,
    enabled: Boolean
): LayerTransform {
    val base = diyRenderTransform()
    if (!enabled || isFixedDiyPictureElement()) return base

    val phase = proceduralPhase(templateId, id)
    val seconds = timeMs / 1000f
    val typeProfile = proceduralProfile()
    val driftX = sin(seconds * typeProfile.speedX + phase) * typeProfile.offsetAmplitude
    val driftY = cos(seconds * typeProfile.speedY + phase * 0.7f) * typeProfile.offsetAmplitude
    val scale = base.scale * (1f + sin(seconds * typeProfile.scaleSpeed + phase) * typeProfile.scaleAmplitude)
    val rotation = base.rotation + sin(seconds * typeProfile.rotationSpeed + phase) * typeProfile.rotationAmplitude

    return base.copy(
        offsetX = base.offsetX + driftX,
        offsetY = base.offsetY + driftY,
        scale = scale.coerceAtLeast(0.01f),
        rotation = rotation
    )
}

fun DiyTemplateElementSnapshot.isDiyImageElement(): Boolean {
    return type.equals("IMAGE", ignoreCase = true) ||
        type.equals("Image", ignoreCase = true)
}

fun DiyTemplateElementSnapshot.isFixedDiyPictureElement(): Boolean {
    return type.equals("PICTURE", ignoreCase = true) ||
        type.equals("Picture", ignoreCase = true)
}

private fun DiyTemplateElementSnapshot.proceduralProfile(): ProceduralProfile {
    return when {
        isDiyImageElement() -> ProceduralProfile(
            offsetAmplitude = 4f,
            scaleAmplitude = 0.018f,
            rotationAmplitude = 0.35f,
            speedX = 0.55f,
            speedY = 0.45f,
            scaleSpeed = 0.38f,
            rotationSpeed = 0.28f
        )
        type.equals("TEXT", ignoreCase = true) ||
            type.equals("Text", ignoreCase = true) -> ProceduralProfile(
                offsetAmplitude = 2f,
                scaleAmplitude = 0.006f,
                rotationAmplitude = 0.18f,
                speedX = 0.42f,
                speedY = 0.35f,
                scaleSpeed = 0.3f,
                rotationSpeed = 0.22f
            )
        else -> ProceduralProfile(
            offsetAmplitude = 3f,
            scaleAmplitude = 0.01f,
            rotationAmplitude = 0.22f,
            speedX = 0.48f,
            speedY = 0.4f,
            scaleSpeed = 0.32f,
            rotationSpeed = 0.24f
        )
    }
}

private fun proceduralPhase(templateId: String, elementId: String): Float {
    val seed = (templateId.hashCode() * 31 + elementId.hashCode()).and(0x7fffffff)
    return (seed % 6283) / 1000f
}

private data class ProceduralProfile(
    val offsetAmplitude: Float,
    val scaleAmplitude: Float,
    val rotationAmplitude: Float,
    val speedX: Float,
    val speedY: Float,
    val scaleSpeed: Float,
    val rotationSpeed: Float
)
