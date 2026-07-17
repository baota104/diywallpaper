package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.ui.common.consumeTapForKeyboardDismiss
import com.example.diywallpaper.ui.common.hideKeyboardOnTapOutside
import com.example.diywallpaper.ui.components.editor.EditorColorPickerPanel
import com.example.diywallpaper.ui.components.editor.EditorColorRow
import com.example.diywallpaper.ui.components.editor.EditorFontSampleRow
import com.example.diywallpaper.ui.components.editor.EditorSizeSlider
import com.example.diywallpaper.ui.components.editor.parseEditorHexColor
import com.example.diywallpaper.ui.theme.BrushPanelControl
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Surface
import com.example.diywallpaper.ui.theme.TextPrimary
import com.example.diywallpaper.ui.theme.TextSecondary

const val EDITOR_TEXT_BRUSH_MAX_LENGTH = 16

@Composable
fun TextBrushToolPanel(
    availableFonts: List<EditorFontOption>,
    config: TextBrushToolConfig?,
    onTextBrushConfigChanged: (text: String, fontFamilyId: String, colorHex: String, brushSize: Float) -> Unit,
    onApplyTextBrush: (text: String, fontFamilyId: String, colorHex: String, brushSize: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val fonts = availableFonts
        .filterNot { it.id in hiddenTextBrushFontIds }
        .ifEmpty { fallbackTextBrushFonts }
    val currentText = config?.text ?: "Hello"
    val currentFontId = config?.style?.fontFamilyId
        ?.takeIf { id -> fonts.any { it.id == id } }
        ?: fonts.first().id
    val currentColorHex = config?.style?.textColorHex
        ?: (config?.style?.textBrush as? TextBrushStyle.Solid)?.colorHex
        ?: "#8B5CF6"
    val currentBrushSize = ((config?.style?.fontSizeSp ?: 22f) - 8f).coerceIn(8f, 40f)
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        EditorColorPickerPanel(
            selectedColorHex = currentColorHex,
            onColorSelected = { colorHex ->
                onTextBrushConfigChanged(
                    currentText,
                    currentFontId,
                    colorHex,
                    currentBrushSize
                )
            },
            onDone = { showColorPicker = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .heightIn(max = 308.dp)
            .hideKeyboardOnTapOutside()
            .background(Surface),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextBrushHeader(
            onDone = {
                onApplyTextBrush(
                    currentText.take(EDITOR_TEXT_BRUSH_MAX_LENGTH),
                    currentFontId,
                    currentColorHex,
                    currentBrushSize
                )
            }
        )

        TextBrushInput(
            value = currentText,
            selectedFontId = currentFontId,
            colorHex = currentColorHex,
            onValueChange = { text ->
                onTextBrushConfigChanged(
                    text.take(EDITOR_TEXT_BRUSH_MAX_LENGTH),
                    currentFontId,
                    currentColorHex,
                    currentBrushSize
                )
            },
            onClearClick = {
                onTextBrushConfigChanged(
                    "",
                    currentFontId,
                    currentColorHex,
                    currentBrushSize
                )
            },
            modifier = Modifier.fillMaxWidth().consumeTapForKeyboardDismiss()
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.editor_panel_brush_size),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = "${currentBrushSize.toInt()}px",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrushPanelControl)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
            EditorSizeSlider(
                value = currentBrushSize,
                onValueChange = { brushSize ->
                    onTextBrushConfigChanged(
                        currentText,
                        currentFontId,
                        currentColorHex,
                        brushSize
                    )
                },
                valueRange = 8f..40f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        EditorFontSampleRow(
            fonts = fonts,
            selectedFontId = currentFontId,
            onFontSelected = { fontId ->
                onTextBrushConfigChanged(
                    currentText,
                    fontId,
                    currentColorHex,
                    currentBrushSize
                )
            }
        )

        EditorColorRow(
            selectedColorHex = currentColorHex,
            onOpenColorPicker = { showColorPicker = true },
            onColorSelected = { colorHex ->
                onTextBrushConfigChanged(
                    currentText,
                    currentFontId,
                    colorHex,
                    currentBrushSize
                )
            }
        )
    }
}

@Composable
private fun TextBrushHeader(onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.editor_tool_text_brush),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = stringResource(id = R.string.editor_panel_done),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onDone)
        )
    }
}

@Composable
private fun TextBrushInput(
    value: String,
    selectedFontId: String,
    colorHex: String,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BrushPanelControl)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(EDITOR_TEXT_BRUSH_MAX_LENGTH)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = editorFontFamily(selectedFontId),
                color = parseEditorHexColor(colorHex, MaterialTheme.colorScheme.onSurface)
            ),
            cursorBrush = SolidColor(
                parseEditorHexColor(
                    colorHex,
                    MaterialTheme.colorScheme.primary
                )
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onClearClick)
        )
    }
}

private val hiddenTextBrushFontIds = setOf(
    GetEditorTextLibraryUseCase.FONT_INTER,
    GetEditorTextLibraryUseCase.FONT_PLUS_JAKARTA_SANS
)

private val fallbackTextBrushFonts = listOf(
    EditorFontOption("allura", "Allura"),
    EditorFontOption("arizonia", "Arizonia"),
    EditorFontOption("fredoka_condensed", "Fredoka"),
    EditorFontOption("sora", "Sora")
)

@Preview(showBackground = true)
@Composable
private fun TextBrushToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        TextBrushToolPanel(
            availableFonts = emptyList(),
            config = null,
            onTextBrushConfigChanged = { _, _, _, _ -> },
            onApplyTextBrush = { _, _, _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(16.dp)
        )
    }
}
