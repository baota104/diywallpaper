package com.example.diywallpaper.ui.components.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.ui.preview.video.VideoPreviewManager
import com.example.diywallpaper.ui.preview.video.VideoPreviewSurface
import com.example.diywallpaper.ui.theme.PrimaryGradient

@Composable
fun WallpaperPreviewCard(
    item: WallpaperItem,
    isPlaying: Boolean,
    videoPreviewManager: VideoPreviewManager,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.thumbUrl.ifBlank { item.imageUrl.orEmpty() },
                contentDescription = stringResource(id = R.string.home_wallpaper_preview),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (item.type == WallpaperType.LIVE_VIDEO && isPlaying && !item.videoUrl.isNullOrBlank()) {
                VideoPreviewSurface(
                    itemId = item.id,
                    videoUrl = item.videoUrl,
                    manager = videoPreviewManager,
                    isActive = isPlaying,
                    modifier = Modifier.fillMaxSize()
                )
            }

            FavoriteOverlayButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
                isSelected = isFavorite,
                onClick = onFavoriteClick
            )

            if (item.type == WallpaperType.LIVE_VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Brush.linearGradient(PrimaryGradient))
                ) {
                    Text(
                        text = stringResource(id = R.string.home_live_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun FavoriteOverlayButton(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    Color.White.copy(alpha = 0.2f)
                }
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(id = R.string.home_favorite),
            tint = if (isSelected) MaterialTheme.colorScheme.secondary else Color.White
        )
    }
}
