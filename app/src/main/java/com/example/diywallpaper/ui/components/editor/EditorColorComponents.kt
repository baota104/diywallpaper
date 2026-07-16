package com.example.diywallpaper.ui.components.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.theme.Primary
import kotlin.math.roundToInt

data class EditorColorToken(
    val hex: String,
    val swatch: Color
)

val editorColorTokens = listOf(
    EditorColorToken("#111111", Color(0xFF111111)),
    EditorColorToken("#FFFFFF", Color(0xFFFFFFFF)),
    EditorColorToken("#E5E5E5", Color(0xFFE5E5E5)),
    EditorColorToken("#FF9AA2", Color(0xFFFF9AA2)),
    EditorColorToken("#FF6C7C", Color(0xFFFF6C7C)),
    EditorColorToken("#FF1F2D", Color(0xFFFF1F2D)),
    EditorColorToken("#FFE8C7", Color(0xFFFFE8C7)),
    EditorColorToken("#FFF2A8", Color(0xFFFFF2A8)),
    EditorColorToken("#B8F7D4", Color(0xFFB8F7D4)),
    EditorColorToken("#6EE7B7", Color(0xFF6EE7B7)),
    EditorColorToken("#BFDBFE", Color(0xFFBFDBFE)),
    EditorColorToken("#60A5FA", Color(0xFF60A5FA)),
    EditorColorToken("#DDD6FE", Color(0xFFDDD6FE)),
    EditorColorToken("#8B5CF6", Color(0xFF8B5CF6))
)

@Composable
fun EditorColorRow(
    selectedColorHex: String,
    onOpenColorPicker: () -> Unit,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: List<EditorColorToken> = editorColorTokens
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        EditorCustomColorSwatch(onClick = onOpenColorPicker)
        colors.forEach { token ->
            EditorColorSwatch(
                color = token.swatch,
                selected = token.hex.equals(selectedColorHex, ignoreCase = true),
                onClick = { onColorSelected(token.hex) }
            )
        }
    }
}

@Composable
fun EditorColorPickerPanel(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    colors: List<EditorColorToken> = editorColorTokens
) {
    var selectedColor by remember(selectedColorHex) {
        mutableStateOf(
            colors.firstOrNull { it.hex.equals(selectedColorHex, ignoreCase = true) }
                ?: EditorColorToken(selectedColorHex, parseEditorHexColor(selectedColorHex, colors.first().swatch))
        )
    }
    var hsv by remember(selectedColorHex) { mutableStateOf(selectedColor.swatch.toEditorHsv()) }

    fun applyHsv(nextHsv: EditorHsvColor) {
        hsv = nextHsv
        val hex = nextHsv.toHex()
        selectedColor = EditorColorToken(hex, nextHsv.toColor())
        onColorSelected(hex)
    }

    Column(
        modifier = modifier.heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorColorPanelHeader(
            title = stringResource(id = R.string.editor_panel_colors),
            onDone = onDone
        )
        EditorColorField(
            hsv = hsv,
            onColorChanged = ::applyHsv,
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
        )
        EditorHueBar(
            hue = hsv.hue,
            onHueChanged = { hue -> applyHsv(hsv.copy(hue = hue)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(colors, key = { it.hex }) { token ->
                EditorColorSwatch(
                    color = token.swatch,
                    selected = token.hex.equals(selectedColor.hex, ignoreCase = true),
                    onClick = {
                        selectedColor = token
                        hsv = token.swatch.toEditorHsv()
                        onColorSelected(token.hex)
                    }
                )
            }
        }
    }
}

@Composable
fun EditorColorPanelHeader(
    title: String,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onDone)
        )
    }
}

@Composable
private fun EditorCustomColorSwatch(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EditorColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .padding(if (selected) 4.dp else 0.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (color.editorLuminanceSafe() > 0.62f) MaterialTheme.colorScheme.onSurface else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EditorColorField(
    hsv: EditorHsvColor,
    onColorChanged: (EditorHsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHsv by rememberUpdatedState(hsv)
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White, EditorHsvColor(hsv.hue, 1f, 1f).toColor())
                )
            )
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    currentOnColorChanged(
                        currentHsv.copy(
                            saturation = (position.x / size.width).coerceIn(0f, 1f),
                            value = (1f - position.y / size.height).coerceIn(0f, 1f)
                        )
                    )
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnColorChanged(
                        currentHsv.copy(
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f),
                            value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        )
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val marker = Offset(
                x = hsv.saturation * size.width,
                y = (1f - hsv.value) * size.height
            )
            drawCircle(Color.White, radius = 8f, center = marker, style = Stroke(width = 3f))
            drawCircle(Color.Black.copy(alpha = 0.35f), radius = 9f, center = marker, style = Stroke(width = 1.5f))
        }
    }
}

@Composable
private fun EditorHueBar(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnHueChanged by rememberUpdatedState(onHueChanged)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    )
                )
            )
            .pointerInput(Unit) {
                fun update(position: Offset) {
                    currentOnHueChanged((position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
                detectTapGestures { update(it) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    currentOnHueChanged((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val markerX = (hue / 360f).coerceIn(0f, 1f) * size.width
            val center = Offset(markerX, size.height / 2f)
            drawCircle(Color.White, radius = 7f, center = center)
            drawCircle(Color.Red, radius = 5f, center = center)
        }
    }
}

data class EditorHsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float
) {
    fun toColor(): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    fun toHex(): String {
        val intColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val red = android.graphics.Color.red(intColor)
        val green = android.graphics.Color.green(intColor)
        val blue = android.graphics.Color.blue(intColor)
        return "#%02X%02X%02X".format(red, green, blue)
    }
}

fun Color.toEditorHsv(): EditorHsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (red * 255).roundToInt().coerceIn(0, 255),
            (green * 255).roundToInt().coerceIn(0, 255),
            (blue * 255).roundToInt().coerceIn(0, 255)
        ),
        hsv
    )
    return EditorHsvColor(hue = hsv[0], saturation = hsv[1], value = hsv[2])
}

fun parseEditorHexColor(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

fun Color.editorLuminanceSafe(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue).coerceIn(0f, 1f)
}
