package com.example.diywallpaper.ui.feature.preview.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.diywallpaper.R
import com.example.diywallpaper.ui.feature.preview.DeviceMockPreview
import com.example.diywallpaper.ui.feature.preview.PreviewBottomActionContainer
import com.example.diywallpaper.ui.feature.preview.PreviewFeedbackChip
import com.example.diywallpaper.ui.feature.preview.PreviewOverlayTopBar
import com.example.diywallpaper.ui.feature.preview.PreviewArgs
import com.example.diywallpaper.ui.feature.preview.PreviewStateBox
import com.example.diywallpaper.ui.feature.preview.toDeviceLabelRes
import com.example.diywallpaper.ui.preview.video.rememberVideoPreviewManager

@Composable
fun DevicePreviewScreen(
    args: PreviewArgs,
    onBackClick: () -> Unit,
    onApplyClick: (PreviewArgs) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DevicePreviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(args) {
        viewModel.bind(args)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.launchIntent) {
        val intent = uiState.launchIntent ?: return@LaunchedEffect
        context.startActivity(intent)
        viewModel.onLaunchIntentConsumed()
    }

    DevicePreviewContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onToggleChrome = viewModel::onToggleChrome,
        onApplyClick = {
            onApplyClick(args)
            viewModel.onApplyClick()
        },
        onSelectTarget = viewModel::applyStaticWallpaper,
        onDismissDialog = viewModel::dismissTargetDialog,
        modifier = modifier.statusBarsPadding()
    )
}

@Composable
fun DevicePreviewContent(
    uiState: DevicePreviewUiState,
    onBackClick: () -> Unit,
    onToggleChrome: () -> Unit,
    onApplyClick: () -> Unit,
    onSelectTarget: (com.example.diywallpaper.domain.model.preview.WallpaperTarget) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoPreviewManager = rememberVideoPreviewManager()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        when {
            uiState.isLoading -> PreviewStateBox(
                text = stringResource(id = R.string.preview_title),
                showLoading = true,
                modifier = Modifier.padding(paddingValues)
            )

            uiState.currentItem == null -> PreviewStateBox(
                text = stringResource(id = R.string.preview_empty),
                modifier = Modifier.padding(paddingValues)
            )

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                ) {
                    if (uiState.designProject != null) {
                        SavedDesignDevicePreview(
                            project = uiState.designProject,
                            isChromeVisible = uiState.isChromeVisible,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        DeviceMockPreview(
                            item = uiState.currentItem,
                            isChromeVisible = uiState.isChromeVisible,
                            videoPreviewManager = videoPreviewManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    PreviewOverlayTopBar(
                        isChromeVisible = uiState.isChromeVisible,
                        onBackClick = onBackClick,
                        onToggleChrome = onToggleChrome,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )

                    if (uiState.primaryAction != null) {
                        PreviewBottomActionContainer(
                            ctaText = stringResource(id = uiState.primaryAction.toDeviceLabelRes()),
                            onClick = onApplyClick,
                            enabled = !uiState.isApplyingWallpaper,
                            isLoading = uiState.isApplyingWallpaper,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp)
                        )
                    }

                    val feedbackMessage = uiState.errorMessage ?: uiState.successMessage
                    if (feedbackMessage != null) {
                        PreviewFeedbackChip(
                            text = feedbackMessage,
                            isError = uiState.errorMessage != null,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = 24.dp, end = 24.dp, bottom = 92.dp)
                        )
                    }

                    if (uiState.showTargetDialog) {
                        WallpaperTargetDialog(
                            onSelectTarget = onSelectTarget,
                            onDismissRequest = onDismissDialog
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperTargetDialog(
    onSelectTarget: (com.example.diywallpaper.domain.model.preview.WallpaperTarget) -> Unit,
    onDismissRequest: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_set_wallpaper_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                androidx.compose.material3.TextButton(
                    onClick = { onSelectTarget(com.example.diywallpaper.domain.model.preview.WallpaperTarget.HOME) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_set_wallpaper_home),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                androidx.compose.material3.TextButton(
                    onClick = { onSelectTarget(com.example.diywallpaper.domain.model.preview.WallpaperTarget.LOCK) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_set_wallpaper_lock),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                androidx.compose.material3.TextButton(
                    onClick = { onSelectTarget(com.example.diywallpaper.domain.model.preview.WallpaperTarget.BOTH) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_set_wallpaper_both),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_set_wallpaper_cancel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
