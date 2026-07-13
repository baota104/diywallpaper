package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.EditorFontOption
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary

@Composable
fun TextBrushToolPanel(
    availableFonts: List<EditorFontOption>,
    onApplyTextBrush: (text: String, fontFamilyId: String, colorHex: String, brushSize: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var brushSize by remember { mutableFloatStateOf(14f) }
    var textValue by remember { mutableStateOf("Hello") }
    var selectedFontIndex by remember { mutableIntStateOf(0) }
    val fonts = availableFonts.ifEmpty {
        listOf(
            EditorFontOption("inter", "Inter"),
            EditorFontOption("plus_jakarta_sans", "Plus Jakarta Sans")
        )
    }
    val safeFontIndex = selectedFontIndex.coerceIn(0, fonts.lastIndex)
    val selectedFont = fonts[safeFontIndex]

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.editor_tool_text_brush),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = stringResource(id = R.string.editor_panel_done),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    onApplyTextBrush(
                        textValue,
                        selectedFont.id,
                        "#8B5CF6",
                        brushSize
                    )
                }
            )
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolSectionLabel(text = stringResource(id = R.string.editor_panel_brush_size))
                Text(
                    text = "${brushSize.toInt()}px",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary
                )
            }
            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 8f..40f
            )
        }

        ToolSectionLabel(text = stringResource(id = R.string.editor_panel_font_family))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            fonts.forEachIndexed { index, item ->
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == safeFontIndex) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable { selectedFontIndex = index }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextBrushToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        TextBrushToolPanel(
            availableFonts = emptyList(),
            onApplyTextBrush = { _, _, _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )
    }
}
