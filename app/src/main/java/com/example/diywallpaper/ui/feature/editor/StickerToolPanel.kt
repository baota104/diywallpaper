package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.ui.theme.DIYWallpaperTheme
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimarySoft

@Composable
fun StickerToolPanel(
    availableStickers: List<StickerItem>,
    isLoadingCatalog: Boolean,
    onAddSticker: (StickerItem) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedStickerId by remember { mutableStateOf<String?>(null) }
    val selectedSticker = availableStickers.firstOrNull { it.id == selectedStickerId }

    LaunchedEffect(availableStickers) {
        if (selectedStickerId == null || availableStickers.none { it.id == selectedStickerId }) {
            selectedStickerId = availableStickers.firstOrNull()?.id
        }
    }

    Column(
        modifier = modifier.heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.editor_panel_add_sticker),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(id = R.string.editor_panel_done),
                tint = if (selectedSticker != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(enabled = selectedSticker != null) {
                        selectedSticker?.let(onAddSticker)
                        onDismiss()
                    }
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 156.dp, max = 236.dp)
        ) {
            items(availableStickers, key = { it.id }) { sticker ->
                val selected = sticker.id == selectedStickerId
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            color = if (selected) PrimarySoft else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(6.dp)
                        .clickable { selectedStickerId = sticker.id },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedStickerPreview(
                        sticker = sticker,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (!isLoadingCatalog && availableStickers.isEmpty()) {
            Text(
                text = stringResource(id = R.string.editor_sticker_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoadingCatalog) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AnimatedStickerPreview(
    sticker: StickerItem,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = if (sticker.isAnimated) {
            sticker.stickerUrl
        } else {
            sticker.thumbnailUrl.ifBlank { sticker.stickerUrl }
        },
        contentDescription = sticker.id,
        contentScale = ContentScale.Fit,
        modifier = modifier.aspectRatio(1f)
    )
}

@Preview(showBackground = true)
@Composable
private fun StickerToolPanelPreview() {
    DIYWallpaperTheme(dynamicColor = false) {
        StickerToolPanel(
            availableStickers = emptyList(),
            isLoadingCatalog = false,
            onAddSticker = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )
    }
}
