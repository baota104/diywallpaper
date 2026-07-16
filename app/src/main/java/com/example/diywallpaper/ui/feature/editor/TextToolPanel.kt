package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.domain.model.design.EditorTextPreset
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.ui.components.editor.EditorColorPickerPanel
import com.example.diywallpaper.ui.components.editor.EditorColorRow
import com.example.diywallpaper.ui.components.editor.EditorColorToken
import com.example.diywallpaper.ui.components.editor.EditorFontSampleRow
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.Surface
import kotlin.math.roundToInt

const val EDITOR_TEXT_MAX_LENGTH = 60

@Composable
fun TextToolPanel(
    availableFonts: List<EditorFontOption>,
    textPresets: List<EditorTextPreset>,
    selectedTextLayer: TextLayer?,
    onAddText: (text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onUpdateText: (layerId: String, text: String, fontFamilyId: String, colorHex: String) -> Unit,
    onApplyPreset: (EditorTextPreset) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val visibleFonts = remember(availableFonts) {
        availableFonts
            .filterNot { it.id in hiddenTextPanelFontIds }
            .ifEmpty { fallbackVisibleFonts }
    }
    var textValue by remember { mutableStateOf("Hello") }
    var selectedFontId by remember(visibleFonts) {
        mutableStateOf(visibleFonts.firstOrNull()?.id ?: GetEditorTextLibraryUseCase.FONT_INTER)
    }
    var selectedColorHex by remember { mutableStateOf(textColorTokens.first().hex) }
    var selectedPresetId by remember { mutableStateOf<String?>(null) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showTextLibrary by remember { mutableStateOf(false) }
    val editTargetId = remember { selectedTextLayer?.id }
    val editTargetLayer = selectedTextLayer?.takeIf { it.id == editTargetId }

    fun resetForNextText() {
        textValue = "Hello"
        selectedFontId = visibleFonts.firstOrNull()?.id ?: GetEditorTextLibraryUseCase.FONT_INTER
        selectedColorHex = textColorTokens.first().hex
        selectedPresetId = null
        showColorPicker = false
        showTextLibrary = false
    }

    LaunchedEffect(editTargetLayer?.id, visibleFonts) {
        if (editTargetLayer != null) {
            textValue = editTargetLayer.text
            selectedFontId = visibleFonts.firstOrNull { it.id == editTargetLayer.style.fontFamilyId }?.id
                ?: visibleFonts.firstOrNull()?.id
                    ?: GetEditorTextLibraryUseCase.FONT_INTER
            selectedColorHex = editTargetLayer.resolveColorHex()
        }
    }

    when {
        showColorPicker -> TextColorPickerPanel(
            selectedColorHex = selectedColorHex,
            onColorSelected = { selectedColorHex = it },
            onDone = { showColorPicker = false },
            modifier = modifier
        )

        showTextLibrary -> TextLibraryPanel(
            textPresets = textPresets,
            selectedPresetId = selectedPresetId,
            onPresetSelected = { preset ->
                selectedPresetId = preset.id
                textValue = preset.previewText.take(EDITOR_TEXT_MAX_LENGTH)
                selectedFontId = visibleFonts.firstOrNull { it.id == preset.style.fontFamilyId }?.id
                    ?: selectedFontId
                selectedColorHex = preset.style.textColorHex
                    ?: (preset.style.textBrush as? TextBrushStyle.Solid)?.colorHex
                        ?: selectedColorHex
            },
            onDone = { showTextLibrary = false },
            modifier = modifier
        )

        else -> TextMainPanel(
            textValue = textValue,
            selectedFontId = selectedFontId,
            selectedColorHex = selectedColorHex,
            visibleFonts = visibleFonts,
            onTextChanged = { textValue = it.take(EDITOR_TEXT_MAX_LENGTH) },
            onFontSelected = { selectedFontId = it },
            onColorSelected = { selectedColorHex = it },
            onOpenColorPicker = { showColorPicker = true },
            onOpenTextLibrary = { showTextLibrary = true },
            onDone = {
                if (editTargetLayer != null) {
                    onUpdateText(editTargetLayer.id, textValue.take(EDITOR_TEXT_MAX_LENGTH), selectedFontId, selectedColorHex)
                    onDismiss()
                } else {
                    onAddText(textValue.take(EDITOR_TEXT_MAX_LENGTH), selectedFontId, selectedColorHex)
                    resetForNextText()
                    onDismiss()
                }
            },
            modifier = modifier
        )
    }
}

@Composable
private fun TextMainPanel(
    textValue: String,
    selectedFontId: String,
    selectedColorHex: String,
    visibleFonts: List<EditorFontOption>,
    onTextChanged: (String) -> Unit,
    onFontSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onOpenColorPicker: () -> Unit,
    onOpenTextLibrary: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorTextPanelHeader(
            title = stringResource(id = R.string.editor_tool_text),
            onDone = onDone
        )

        TextInputWithLibrary(
            value = textValue,
            selectedFontId = selectedFontId,
            selectedColorHex = selectedColorHex,
            onValueChange = onTextChanged,
            onOpenTextLibrary = onOpenTextLibrary,
            modifier = Modifier.fillMaxWidth()
        )

        EditorFontSampleRow(
            fonts = visibleFonts,
            selectedFontId = selectedFontId,
            onFontSelected = onFontSelected
        )

        TextColorRow(
            selectedColorHex = selectedColorHex,
            onOpenColorPicker = onOpenColorPicker,
            onColorSelected = onColorSelected
        )
    }
}

@Composable
private fun EditorTextPanelHeader(
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
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
private fun TextInputWithLibrary(
    value: String,
    selectedFontId: String,
    selectedColorHex: String,
    onValueChange: (String) -> Unit,
    onOpenTextLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp)),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = editorFontFamily(selectedFontId),
            color = parseHexColor(selectedColorHex, MaterialTheme.colorScheme.onSurface)
        ),
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onOpenTextLibrary)
                    .padding(end = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(id = R.string.editor_panel_library),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun TextColorRow(
    selectedColorHex: String,
    onOpenColorPicker: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    EditorColorRow(
        selectedColorHex = selectedColorHex,
        onOpenColorPicker = onOpenColorPicker,
        onColorSelected = onColorSelected,
        colors = textColorTokens.map { EditorColorToken(it.hex, it.swatch) }
    )
}

@Composable
private fun TextColorPickerPanel(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditorColorPickerPanel(
        selectedColorHex = selectedColorHex,
        onColorSelected = onColorSelected,
        onDone = onDone,
        modifier = modifier
            .navigationBarsPadding()
            .heightIn(max = 360.dp),
        colors = textColorTokens.map { EditorColorToken(it.hex, it.swatch) }
    )
}

@Composable
private fun TextLibraryPanel(
    textPresets: List<EditorTextPreset>,
    selectedPresetId: String?,
    onPresetSelected: (EditorTextPreset) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditorTextPanelHeader(
            title = stringResource(id = R.string.editor_text_library_title),
            onDone = onDone
        )
        if (textPresets.isEmpty()) {
            Text(
                text = stringResource(id = R.string.editor_text_library_empty),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(textPresets, key = { it.id }) { preset ->
                    val selected = preset.id == selectedPresetId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.75f else 0.42f))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) Primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                onPresetSelected(preset)
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = preset.previewText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = editorFontFamily(preset.style.fontFamilyId),
                                fontWeight = FontWeight.Bold
                            ),
                            color = parseHexColor(
                                preset.style.textColorHex ?: "#201A2E",
                                MaterialTheme.colorScheme.onSurface
                            ),
                            softWrap = true,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorField(
    hsv: TextHsvColor,
    onColorChanged: (TextHsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHsv by rememberUpdatedState(hsv)
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White, TextHsvColor(hsv.hue, 1f, 1f).toColor())
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
private fun HueBar(
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
            drawCircle(Primary, radius = 5f, center = center)
        }
    }
}

private fun TextLayer.resolveColorHex(): String {
    return style.textColorHex
        ?: when (val brush = style.textBrush) {
            is TextBrushStyle.Solid -> brush.colorHex
            is TextBrushStyle.Gradient -> brush.colors.firstOrNull()
            null -> null
        }
        ?: textColorTokens.first().hex
}

private data class TextColorToken(
    val hex: String,
    val swatch: Color
)

private data class TextHsvColor(
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

private fun Color.toTextHsv(): TextHsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (red * 255).roundToInt().coerceIn(0, 255),
            (green * 255).roundToInt().coerceIn(0, 255),
            (blue * 255).roundToInt().coerceIn(0, 255)
        ),
        hsv
    )
    return TextHsvColor(hue = hsv[0], saturation = hsv[1], value = hsv[2])
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
}

private fun Color.luminanceSafe(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue).coerceIn(0f, 1f)
}

private val hiddenTextPanelFontIds = setOf(
    GetEditorTextLibraryUseCase.FONT_INTER,
    GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS
)

private val fallbackVisibleFonts = listOf(
    EditorFontOption("allura", "Allura"),
    EditorFontOption("arizonia", "Arizonia"),
    EditorFontOption("fredoka_condensed", "Fredoka"),
    EditorFontOption("sora", "Sora")
)

private val textColorTokens = listOf(
    TextColorToken("#201A2E", Color(0xFF201A2E)),
    TextColorToken("#FFFFFF", Color(0xFFFFFFFF)),
    TextColorToken("#E5E5E5", Color(0xFFE5E5E5)),
    TextColorToken("#FF8FA3", Color(0xFFFF8FA3)),
    TextColorToken("#FF6C7C", Color(0xFFFF6C7C)),
    TextColorToken("#FF1F2D", Color(0xFFFF1F2D)),
    TextColorToken("#43B5F5", Color(0xFF43B5F5)),
    TextColorToken("#8B5CF6", Color(0xFF8B5CF6)),
    TextColorToken("#6EE7B7", Color(0xFF6EE7B7))
)

@Preview(showBackground = true)
@Composable
private fun TextToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        TextToolPanel(
            availableFonts = fallbackVisibleFonts,
            textPresets = emptyList(),
            selectedTextLayer = null,
            onAddText = { _, _, _ -> },
            onUpdateText = { _, _, _, _ -> },
            onApplyPreset = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(16.dp)
        )
    }
}
