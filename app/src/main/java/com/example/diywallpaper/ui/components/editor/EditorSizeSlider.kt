package com.example.diywallpaper.ui.components.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.ui.theme.BrushAccentPink
import com.example.diywallpaper.ui.theme.Surface

@Composable
fun EditorSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    fun valueToFraction(nextValue: Float): Float {
        return ((nextValue - valueRange.start) / (valueRange.endInclusive - valueRange.start))
            .coerceIn(0f, 1f)
    }

    fun fractionToValue(fraction: Float): Float {
        return valueRange.start +
            (valueRange.endInclusive - valueRange.start) * fraction.coerceIn(0f, 1f)
    }

    Canvas(
        modifier = modifier
            .height(36.dp)
            .pointerInput(valueRange) {
                fun updateFromX(x: Float) {
                    onValueChange(fractionToValue(x / size.width.coerceAtLeast(1)))
                }
                detectTapGestures { offset -> updateFromX(offset.x) }
            }
            .pointerInput(valueRange) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange(fractionToValue(change.position.x / size.width.coerceAtLeast(1)))
                }
            }
    ) {
        val thumbRadius = 14.dp.toPx()
        val trackStart = thumbRadius
        val trackEnd = size.width - thumbRadius
        val trackY = size.height / 2f
        val usableWidth = (trackEnd - trackStart).coerceAtLeast(1f)
        val thumbX = trackStart + usableWidth * valueToFraction(value)

        drawLine(
            color = BrushAccentPink.copy(alpha = 0.14f),
            start = Offset(trackStart, trackY),
            end = Offset(trackEnd, trackY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = BrushAccentPink,
            start = Offset(trackStart, trackY),
            end = Offset(thumbX, trackY),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = Surface,
            radius = thumbRadius,
            center = Offset(thumbX, trackY)
        )
        drawCircle(
            color = BrushAccentPink,
            radius = thumbRadius,
            center = Offset(thumbX, trackY),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
