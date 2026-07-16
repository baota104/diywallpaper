package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Interests
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme

@OptIn(ExperimentalFoundationApi::class)
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
                    .height(96.dp)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                editorToolItems.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clickable { onToolSelected(item.tool) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .width(64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            val title = stringResource(id = item.labelRes)
                            Icon(
                                imageVector = item.icon,
                                contentDescription = title,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE)
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
    EditorToolItem(EditorTool.STICKER, Icons.Outlined.Interests, R.string.editor_tool_sticker),
    EditorToolItem(EditorTool.BRUSH_DRAW, Icons.Outlined.Brush, R.string.editor_tool_brush),
    EditorToolItem(EditorTool.TEXT_BRUSH, Icons.Outlined.Draw, R.string.editor_tool_text_brush)
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
