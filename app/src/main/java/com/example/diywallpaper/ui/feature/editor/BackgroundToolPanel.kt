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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.ui.components.editor.EditorColorPickerPanel
import com.example.diywallpaper.ui.components.editor.EditorColorRow
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft
import kotlin.math.roundToInt

@Composable
fun BackgroundToolPanel(
    availableBackgrounds: List<BackgroundCreateItem>,
    isLoadingCatalog: Boolean,
    onApplySolidBackground: (String) -> Unit,
    onApplyGradientBackground: (List<String>) -> Unit,
    onApplyImageBackground: (BackgroundCreateItem) -> Unit,
    modifier: Modifier = Modifier,
    showImportHint: Boolean = false,
    onImportBackground: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColorHex by remember { mutableStateOf("#FFFFFF") }

    if (showColorPicker) {
        EditorColorPickerPanel(
            selectedColorHex = selectedColorHex,
            onColorSelected = {
                selectedColorHex = it
                onApplySolidBackground(it)
            },
            onDone = { showColorPicker = false },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BackgroundPanelHeader(
            title = stringResource(id = R.string.editor_tool_background),
            onDismiss = onDismiss
        )

        ToolSectionLabel(text = stringResource(id = R.string.editor_panel_colors))

        EditorColorRow(
            selectedColorHex = selectedColorHex,
            onOpenColorPicker = { showColorPicker = true },
            onColorSelected = {
                selectedColorHex = it
                onApplySolidBackground(it)
            }
        )

        ToolSectionLabel(text = stringResource(id = R.string.editor_panel_templates))

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 170.dp, max = 210.dp)
        ) {
            item(key = "import_background") {
                ImportBackgroundTile(onClick = onImportBackground)
            }

            items(availableBackgrounds, key = { it.id }) { background ->
                BackgroundApiTile(
                    background = background,
                    onClick = { onApplyImageBackground(background) }
                )
            }

            if (isLoadingCatalog) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundColorPickerPanel(
    selectedColorHex: String,
    onApplySolidBackground: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedColor by remember {
        mutableStateOf(
            backgroundColorTokens.firstOrNull { it.hex == selectedColorHex }
                ?: backgroundColorTokens.first { it.hex == "#FF6C7C" }
        )
    }
    var hsv by remember { mutableStateOf(selectedColor.swatch.toHsv()) }

    fun applyHsv(nextHsv: HsvColor) {
        hsv = nextHsv
        val hex = nextHsv.toHex()
        selectedColor = BackgroundColorToken(hex, nextHsv.toColor())
        onColorSelected(hex)
        onApplySolidBackground(hex)
    }

    Column(
        modifier = modifier.heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackgroundPanelHeader(
            title = stringResource(id = R.string.editor_panel_colors),
            onDismiss = {
                onApplySolidBackground(selectedColor.hex)
                onDone()
            }
        )

        ColorField(
            hsv = hsv,
            onColorChanged = ::applyHsv,
            modifier = Modifier
                .fillMaxWidth()
                .height(138.dp)
        )

        HueBar(
            hue = hsv.hue,
            onHueChanged = { hue -> applyHsv(hsv.copy(hue = hue)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(backgroundColorTokens, key = { it.hex }) { token ->
                ColorSwatch(
                    color = token.swatch,
                    selected = token.hex == selectedColor.hex,
                    onClick = {
                        selectedColor = token
                        hsv = token.swatch.toHsv()
                        onColorSelected(token.hex)
                        onApplySolidBackground(token.hex)
                    }
                )
            }
        }
    }
}

@Composable
private fun BackgroundPanelHeader(
    title: String,
    onDismiss: () -> Unit
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
                .clickable(onClick = onDismiss)
        )
    }
}

@Composable
private fun ColorField(
    hsv: HsvColor,
    onColorChanged: (HsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHsv by rememberUpdatedState(hsv)
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White, HsvColor(hsv.hue, 1f, 1f).toColor())
                )
            )
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black)
                )
            )
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
            drawCircle(Color.Red, radius = 5f, center = center)
        }
    }
}

@Composable
private fun NoneColorSwatch(
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFFF7F4FA))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ColorPickerSwatch(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(PrimarySoft)
            .border(1.dp, Primary, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ImportBackgroundTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFEEF7))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun BackgroundApiTile(
    background: BackgroundCreateItem,
    onClick: () -> Unit
) {
    AsyncImage(
        model = background.thumbnailUrl.ifBlank { background.imageUrl },
        contentDescription = background.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    )
}

private data class BackgroundColorToken(
    val hex: String,
    val swatch: Color
)

private data class HsvColor(
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

private fun Color.toHsv(): HsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(
        android.graphics.Color.rgb(
            (red * 255).roundToInt().coerceIn(0, 255),
            (green * 255).roundToInt().coerceIn(0, 255),
            (blue * 255).roundToInt().coerceIn(0, 255)
        ),
        hsv
    )
    return HsvColor(hue = hsv[0], saturation = hsv[1], value = hsv[2])
}

private val backgroundColorTokens = listOf(
    BackgroundColorToken("#111111", Color(0xFF111111)),
    BackgroundColorToken("#FFFFFF", Color(0xFFFFFFFF)),
    BackgroundColorToken("#E5E5E5", Color(0xFFE5E5E5)),
    BackgroundColorToken("#FF9AA2", Color(0xFFFF9AA2)),
    BackgroundColorToken("#FF6C7C", Color(0xFFFF6C7C)),
    BackgroundColorToken("#FF1F2D", Color(0xFFFF1F2D)),
    BackgroundColorToken("#FFE8C7", Color(0xFFFFE8C7)),
    BackgroundColorToken("#FFF2A8", Color(0xFFFFF2A8)),
    BackgroundColorToken("#B8F7D4", Color(0xFFB8F7D4)),
    BackgroundColorToken("#6EE7B7", Color(0xFF6EE7B7)),
    BackgroundColorToken("#BFDBFE", Color(0xFFBFDBFE)),
    BackgroundColorToken("#60A5FA", Color(0xFF60A5FA)),
    BackgroundColorToken("#DDD6FE", Color(0xFFDDD6FE)),
    BackgroundColorToken("#8B5CF6", Color(0xFF8B5CF6))
)

@Preview(showBackground = true)
@Composable
private fun BackgroundToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        BackgroundToolPanel(
            availableBackgrounds = emptyList(),
            isLoadingCatalog = false,
            onApplySolidBackground = {},
            onApplyGradientBackground = {},
            onApplyImageBackground = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )
    }
}
