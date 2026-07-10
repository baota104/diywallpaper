package com.example.diywallpaper.ui.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.preview.PreviewCatalogItem
import com.example.diywallpaper.domain.model.preview.PreviewItemKind
import com.example.diywallpaper.ui.preview.video.VideoPreviewManager
import com.example.diywallpaper.ui.preview.video.VideoPreviewSurface
import com.example.diywallpaper.ui.theme.PrimaryGradient

@Composable
fun PreviewStateBox(
    text: String,
    modifier: Modifier = Modifier,
    showLoading: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PreviewCarouselCard(
    item: PreviewCatalogItem,
    isPlaybackActive: Boolean,
    videoPreviewManager: VideoPreviewManager,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 34.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
        ) {
            when (item.kind) {
                PreviewItemKind.WALLPAPER_LIVE -> {
                    AsyncImage(
                        model = item.thumbUrl,
                        contentDescription = stringResource(id = R.string.home_wallpaper_preview),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    val videoUrl = item.wallpaperSource?.videoUrl
                    if (isPlaybackActive && !videoUrl.isNullOrBlank()) {
                        VideoPreviewSurface(
                            itemId = item.id,
                            videoUrl = videoUrl,
                            manager = videoPreviewManager,
                            isActive = isPlaybackActive,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                PreviewItemKind.DIY_LIVE -> {
                    val animationUrl = item.diySource?.diyAnimationUrl
                    val composition = if (isPlaybackActive && !animationUrl.isNullOrBlank()) {
                        rememberLottieComposition(LottieCompositionSpec.Url(animationUrl))
                    } else {
                        rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.ic_select_lg))
                    }
                    val progress = animateLottieCompositionAsState(
                        composition = composition.value,
                        iterations = LottieConstants.IterateForever,
                        isPlaying = isPlaybackActive && composition.value != null
                    )
                    if (isPlaybackActive && composition.value != null) {
                        LottieAnimation(
                            composition = composition.value,
                            progress = { progress.value },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = item.thumbUrl,
                            contentDescription = stringResource(id = R.string.home_wallpaper_preview),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                else -> {
                    AsyncImage(
                        model = item.thumbUrl.ifBlank {
                            item.wallpaperSource?.imageUrl
                                ?: item.scratchSource?.templateBackgroundUrl
                                .orEmpty()
                        },
                        contentDescription = stringResource(id = R.string.home_wallpaper_preview),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceMockPreview(
    item: PreviewCatalogItem,
    isChromeVisible: Boolean,
    videoPreviewManager: VideoPreviewManager,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp
) {
    Box(modifier = modifier.fillMaxSize()) {
        PreviewCarouselCard(
            item = item,
            isPlaybackActive = true,
            videoPreviewManager = videoPreviewManager,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = cornerRadius
        )

        if (isChromeVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 74.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.preview_mock_date),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.preview_mock_large_time),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewOverlayTopBar(
    isChromeVisible: Boolean,
    onBackClick: () -> Unit,
    onToggleChrome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(id = R.string.preview_back),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.scale(0.88f)
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleChrome)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isChromeVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                contentDescription = stringResource(id = R.string.preview_toggle_chrome),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PreviewBottomActionContainer(
    ctaText: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(PrimaryGradient)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.preview_apply_loading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            } else {
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun PreviewFeedbackChip(
    text: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 8.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}
