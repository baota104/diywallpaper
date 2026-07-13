package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft

@Composable
fun EditorBottomToolbar(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                editorToolItems.forEach { item ->
                    val isSelected = item.tool == selectedTool ||
                        (item.tool == EditorTool.BRUSH_DRAW && selectedTool == EditorTool.BRUSH_ERASE)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .background(
                                color = if (isSelected) PrimarySoft else Color.Transparent,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { onToolSelected(item.tool) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = stringResource(id = item.labelRes),
                                tint = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(id = item.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

private data class EditorToolItem(
    val tool: EditorTool,
    val icon: ImageVector,
    val labelRes: Int
)

private val editorToolItems = listOf(
    EditorToolItem(EditorTool.BACKGROUND, Icons.Outlined.Image, R.string.editor_tool_background),
    EditorToolItem(EditorTool.IMPORT_PHOTO, Icons.Outlined.AddPhotoAlternate, R.string.editor_tool_import),
    EditorToolItem(EditorTool.TEXT, Icons.Outlined.TextFields, R.string.editor_tool_text),
    EditorToolItem(EditorTool.STICKER, Icons.Outlined.StickyNote2, R.string.editor_tool_sticker),
    EditorToolItem(EditorTool.TEXT_BRUSH, Icons.Outlined.BorderColor, R.string.editor_tool_text_brush),
    EditorToolItem(EditorTool.BRUSH_DRAW, Icons.Outlined.Brush, R.string.editor_tool_brush)
)

@Preview(showBackground = true)
@Composable
private fun EditorBottomToolbarPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        EditorBottomToolbar(
            selectedTool = EditorTool.STICKER,
            onToolSelected = {}
        )
    }
}
