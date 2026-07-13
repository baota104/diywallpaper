package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft

@Composable
fun BrushToolPanel(
    onApplyBrush: (erase: Boolean, colorHex: String, brushSize: Float) -> Unit,
    modifier: Modifier = Modifier,
    initialErase: Boolean = false
) {
    var brushSize by remember { mutableFloatStateOf(28f) }
    var selectedColorIndex by remember { mutableIntStateOf(1) }
    var isEraseMode by remember(initialErase) { mutableStateOf(initialErase) }

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(id = R.string.editor_tool_brush),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            brushColors.forEachIndexed { index, color ->
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(color = color.swatch, shape = CircleShape)
                        .border(
                            width = if (index == selectedColorIndex) 2.dp else 0.dp,
                            color = if (index == selectedColorIndex) Primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedColorIndex = index }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrushModeChip(
                text = stringResource(id = R.string.editor_panel_draw),
                selected = !isEraseMode,
                onClick = { isEraseMode = false }
            )
            BrushModeChip(
                text = stringResource(id = R.string.editor_panel_erase),
                selected = isEraseMode,
                onClick = { isEraseMode = true }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolSectionLabel(text = stringResource(id = R.string.editor_panel_brush_size))
            Slider(
                value = brushSize,
                onValueChange = { brushSize = it },
                valueRange = 6f..48f
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            brushStamps.forEach { stamp ->
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onApplyBrush(
                                isEraseMode,
                                brushColors[selectedColorIndex].hex,
                                brushSize
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stamp,
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun BrushModeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) PrimarySoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class BrushColorToken(
    val hex: String,
    val swatch: Color
)

private val brushColors = listOf(
    BrushColorToken("#FFFFFF", Color(0xFFFFFFFF)),
    BrushColorToken("#1C1527", Color(0xFF1C1527)),
    BrushColorToken("#D1D5DD", Color(0xFFD1D5DD)),
    BrushColorToken("#FF97A7", Color(0xFFFF97A7)),
    BrushColorToken("#FF6C7C", Color(0xFFFF6C7C)),
    BrushColorToken("#FF3548", Color(0xFFFF3548))
)

private val brushStamps = listOf("~", "w", "[]", "_", "*")

@Preview(showBackground = true)
@Composable
private fun BrushToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        BrushToolPanel(
            onApplyBrush = { _, _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )
    }
}
