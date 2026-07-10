package com.example.diywallpaper.ui.feature.preview.carousel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.common.VitalityPrimaryButton
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import com.example.diywallpaper.ui.feature.preview.PreviewCarouselCard
import com.example.diywallpaper.ui.feature.preview.PreviewStateBox
import com.example.diywallpaper.ui.feature.preview.toLabelRes
import com.example.diywallpaper.ui.preview.video.rememberVideoPreviewManager
import com.example.diywallpaper.ui.theme.PrimaryGradient
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PreviewCarouselScreen(
    args: PreviewArgs,
    onBackClick: () -> Unit,
    onOpenDevicePreview: (PreviewArgs) -> Unit,
    onEditRequested: (PreviewArgs) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviewCarouselViewModel = hiltViewModel()
) {
    LaunchedEffect(args) {
        viewModel.bind(args)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PreviewCarouselContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onPageSettled = viewModel::onPageSettled,
        onPrimaryActionClick = {
            val currentItemId = uiState.currentItem?.id
            if (currentItemId != null) {
                val nextArgs = args.copy(initialItemId = currentItemId)
                when (uiState.currentAction) {
                    com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction.SET_WALLPAPER,
                    com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction.SET_LIVE_WALLPAPER -> onOpenDevicePreview(nextArgs)
                    com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction.EDIT_DIY,
                    com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction.CREATE_FROM_SCRATCH -> onEditRequested(nextArgs)
                    null -> Unit
                }
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PreviewCarouselContent(
    uiState: PreviewCarouselUiState,
    onBackClick: () -> Unit,
    onPageSettled: (Int) -> Unit,
    onPrimaryActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoPreviewManager = rememberVideoPreviewManager()
    val pagerState = rememberPagerState(
        initialPage = uiState.initialIndex,
        pageCount = { uiState.items.size.coerceAtLeast(1) }
    )

    LaunchedEffect(uiState.currentIndex, uiState.items.size) {
        if (uiState.items.isNotEmpty() && pagerState.currentPage != uiState.currentIndex) {
            pagerState.scrollToPage(uiState.currentIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect(onPageSettled)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.preview_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(id = R.string.preview_back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        if (!uiState.isLoading && uiState.currentAction != null && uiState.items.isNotEmpty()) {
                            Box(modifier = Modifier.padding(end = 14.dp)) {
                                Button(
                                        onClick = onPrimaryActionClick,
                                    modifier = Modifier
                                        .height(36.dp)
                                        .wrapContentWidth(),
                                    colors = buttonColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                brush = horizontalGradient(
                                                    PrimaryGradient
                                                ),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(id = uiState.currentAction.toLabelRes()),
                                            color = androidx.compose.ui.graphics.Color.White,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                PreviewStateBox(
                    text = stringResource(id = R.string.preview_title),
                    showLoading = true,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            uiState.items.isEmpty() -> {
                PreviewStateBox(
                    text = stringResource(id = R.string.preview_empty),
                    modifier = Modifier.padding(paddingValues)
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(top = 10.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 28.dp),
                        pageSpacing = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        val item = uiState.items[page]
                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val clampedOffset = pageOffset.coerceIn(0f, 1f)
                        val scale = 1f - (clampedOffset * 0.08f)
                        val alpha = 1f - (clampedOffset * 0.25f)
                        PreviewCarouselCard(
                            item = item,
                            isPlaybackActive = item.id in uiState.activePlaybackIds,
                            videoPreviewManager = videoPreviewManager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .aspectRatio(0.59f)
                        )
                    }
                }
            }
        }
    }
}
