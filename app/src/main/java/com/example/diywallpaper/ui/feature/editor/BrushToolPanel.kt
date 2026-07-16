package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.components.editor.EditorColorPickerPanel
import com.example.diywallpaper.ui.components.editor.EditorColorRow
import com.example.diywallpaper.ui.components.editor.EditorSizeSlider
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.BrushAccentPink
import com.example.diywallpaper.ui.theme.BrushPanelControl
import com.example.diywallpaper.ui.theme.BrushSelectedSoft
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.Surface
import com.example.diywallpaper.ui.theme.TextSecondary

@Composable
fun BrushToolPanel(
    config: BrushToolConfig,
    onBrushConfigChanged: (erase: Boolean, colorHex: String, brushSize: Float, preset: BrushPresetType, patternBrushName: String?) -> Unit,
    onApplyBrush: (erase: Boolean, colorHex: String, brushSize: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }

    if (showColorPicker) {
        EditorColorPickerPanel(
            selectedColorHex = config.colorHex,
            onColorSelected = { colorHex ->
                onBrushConfigChanged(
                    false,
                    colorHex,
                    config.brushSize,
                    config.preset,
                    config.patternBrushName
                )
            },
            onDone = { showColorPicker = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .background(Surface),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.editor_tool_brush),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    onApplyBrush(config.erase, config.colorHex, config.brushSize)
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        EditorColorRow(
            selectedColorHex = config.colorHex,
            onOpenColorPicker = { showColorPicker = true },
            onColorSelected = { colorHex ->
                onBrushConfigChanged(
                    false,
                    colorHex,
                    config.brushSize,
                    config.preset,
                    config.patternBrushName
                )
            }
        )

        BrushModeAndSizeCard(
            erase = config.erase,
            brushSize = config.brushSize,
            onDrawClick = {
                onBrushConfigChanged(
                    false,
                    config.colorHex,
                    config.brushSize,
                    config.preset,
                    config.patternBrushName
                )
            },
            onEraseClick = {
                onBrushConfigChanged(
                    true,
                    config.colorHex,
                    config.brushSize,
                    BrushPresetType.SOLID,
                    null
                )
            },
            onBrushSizeChanged = {
                onBrushConfigChanged(
                    config.erase,
                    config.colorHex,
                    it,
                    config.preset,
                    config.patternBrushName.takeUnless { config.erase }
                )
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            brushItems.forEach { item ->
                BrushImageItem(
                    item = item,
                    selected = item.isSelected(config),
                    onClick = {
                        onBrushConfigChanged(
                            false,
                            config.colorHex,
                            config.brushSize,
                            item.type,
                            item.patternName
                        )
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun BrushModeIcon(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (selected) Surface else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (selected) Primary else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun BrushModeAndSizeCard(
    erase: Boolean,
    brushSize: Float,
    onDrawClick: () -> Unit,
    onEraseClick: () -> Unit,
    onBrushSizeChanged: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrushPanelControl)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrushModeIcon(
            selected = !erase,
            onClick = onDrawClick,
            icon = Icons.Outlined.Brush,
            contentDescription = stringResource(id = R.string.editor_panel_draw)
        )
        BrushModeIcon(
            selected = erase,
            onClick = onEraseClick,
            icon = Icons.Outlined.FormatColorReset,
            contentDescription = stringResource(id = R.string.editor_panel_erase)
        )
        BrushSizeSlider(
            value = brushSize,
            onValueChange = onBrushSizeChanged,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BrushSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    EditorSizeSlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 6f..56f,
        modifier = modifier
    )
}

@Composable
private fun BrushImageItem(
    item: BrushItemToken,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (selected) BrushSelectedSoft else BrushPanelControl,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) BrushAccentPink else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = item.resId),
            contentDescription = item.name,
            modifier = Modifier.size(48.dp)
        )
    }
}

private data class BrushItemToken(
    val name: String,
    val resId: Int,
    val type: BrushPresetType,
    val patternName: String? = null
) {
    fun isSelected(config: BrushToolConfig): Boolean {
        if (config.erase) return false
        return if (type == BrushPresetType.PATTERN) {
            config.preset == BrushPresetType.PATTERN && config.patternBrushName == patternName
        } else {
            config.preset == type
        }
    }
}

private val brushItems = listOf(
    BrushItemToken("brush1", R.drawable.brush1, BrushPresetType.SOLID),
    BrushItemToken("brush2", R.drawable.brush2, BrushPresetType.DASHED),
    BrushItemToken("brush3", R.drawable.brush3, BrushPresetType.OUTLINE),
    BrushItemToken("brush4", R.drawable.brush4, BrushPresetType.GLOW),
    BrushItemToken("brush5", R.drawable.brush5, BrushPresetType.PATTERN, "brush5"),
    BrushItemToken("brush6", R.drawable.brush6, BrushPresetType.PATTERN, "brush6"),
    BrushItemToken("brush7", R.drawable.brush7, BrushPresetType.PATTERN, "brush7"),
    BrushItemToken("brush8", R.drawable.brush8, BrushPresetType.PATTERN, "brush8"),
    BrushItemToken("brush9", R.drawable.brush9, BrushPresetType.PATTERN, "brush9"),
    BrushItemToken("brush10", R.drawable.brush10, BrushPresetType.PATTERN, "brush10"),
    BrushItemToken("brush11", R.drawable.brush11, BrushPresetType.PATTERN, "brush11"),
    BrushItemToken("brush12", R.drawable.brush12, BrushPresetType.PATTERN, "brush12"),
    BrushItemToken("brush13", R.drawable.brush13, BrushPresetType.PATTERN, "brush13"),
    BrushItemToken("brush14", R.drawable.brush14, BrushPresetType.PATTERN, "brush14"),
    BrushItemToken("brush15", R.drawable.brush15, BrushPresetType.PATTERN, "brush15"),
    BrushItemToken("brush16", R.drawable.brush16, BrushPresetType.PATTERN, "brush16"),
    BrushItemToken("brush17", R.drawable.brush17, BrushPresetType.PATTERN, "brush17"),
    BrushItemToken("brush18", R.drawable.brush18, BrushPresetType.PATTERN, "brush18"),
    BrushItemToken("brush19", R.drawable.brush19, BrushPresetType.PATTERN, "brush19"),
    BrushItemToken("brush20", R.drawable.brush20, BrushPresetType.PATTERN, "brush20")
)

@Preview(showBackground = true)
@Composable
private fun BrushToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        BrushToolPanel(
            config = BrushToolConfig(
                erase = false,
                colorHex = "#1C1527",
                brushSize = 28f,
                preset = BrushPresetType.PATTERN,
                patternBrushName = "brush5"
            ),
            onBrushConfigChanged = { _, _, _, _, _ -> },
            onApplyBrush = { _, _, _ -> },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(18.dp)
        )
    }
}
