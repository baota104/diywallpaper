package com.example.diywallpaper.ui.feature.dashboard.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.preview.PreviewSourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeContent(
    uiState: DashboardHomeUiState,
    onRefresh: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onViewportChanged: (Map<String, com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo>, Boolean) -> Unit,
    onWallpaperFavoriteClick: (String) -> Unit,
    onDiyFavoriteClick: (String) -> Unit,
    onOpenPreview: (sourceType: PreviewSourceType, categoryId: String, itemId: String) -> Unit,
    onCreateFromScratch: () -> Unit,
    onNotificationClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp)
            ) {
                CenterAlignedTopAppBar(
                    colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.dashboard_tab_home),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {},
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = androidx.compose.material3.SnackbarHostState()) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeCreateFromScratchCard(onClick = onCreateFromScratch)

            Text(
                text = stringResource(id = R.string.home_explore_wallpapers),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            HomeCategoryChips(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = onCategorySelected
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when {
                    uiState.isInitialLoading && uiState.items.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    uiState.errorMessage != null && uiState.items.isEmpty() -> {
                        HomeStatusMessage(
                            text = uiState.errorMessage,
                            actionText = stringResource(id = R.string.home_retry),
                            onAction = onRefresh
                        )
                    }

                    uiState.items.isEmpty() -> {
                        HomeStatusMessage(
                            text = stringResource(id = R.string.home_empty_title)
                        )
                    }

                    else -> {
                        HomeWallpaperGrid(
                            feedItems = uiState.items,
                            activeVideoIds = uiState.activeVideoIds,
                            activeDiyAnimationIds = uiState.activeDiyAnimationIds,
                            onViewportChanged = onViewportChanged,
                            onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                            onDiyFavoriteClick = onDiyFavoriteClick,
                            onOpenPreview = onOpenPreview,
                            contentPadding = PaddingValues(bottom = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
