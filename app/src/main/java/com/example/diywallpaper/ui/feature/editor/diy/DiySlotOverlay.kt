package com.example.diywallpaper.ui.feature.editor.diy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import kotlin.math.roundToInt

@Composable
fun DiySlotOverlay(
    elements: List<DiyTemplateElementSnapshot>,
    scaleX: Float,
    scaleY: Float,
    onSlotClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        elements
            .filter { it.isImageElement() && it.localImagePath.isNullOrBlank() }
            .sortedBy { it.zIndex }
            .forEach { element ->
                DiyEmptySlotHotspot(
                    element = element,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    onClick = { onSlotClick(element.id) }
                )
            }
    }
}

@Composable
private fun DiyEmptySlotHotspot(
    element: DiyTemplateElementSnapshot,
    scaleX: Float,
    scaleY: Float,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (element.x * scaleX).roundToInt(),
                    y = (element.y * scaleY).roundToInt()
                )
            }
            .size(
                width = with(density) { (element.width * scaleX).toDp() },
                height = with(density) { (element.height * scaleY).toDp() }
            )
            .graphicsLayer(rotationZ = element.rotation)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val arm = size.minDimension * 0.24f
            drawLine(
                color = Color.White.copy(alpha = 0.86f),
                start = Offset(center.x - arm, center.y),
                end = Offset(center.x + arm, center.y),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.86f),
                start = Offset(center.x, center.y - arm),
                end = Offset(center.x, center.y + arm),
                strokeWidth = 2.4f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DiyTemplateElementSnapshot.isImageElement(): Boolean {
    return type.equals("IMAGE", ignoreCase = true) ||
        type.equals("Image", ignoreCase = true)
}
