package com.example.diywallpaper.ui.feature.dashboard.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush as BrushIcon
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.ui.common.CommonConfirmDialog
import com.example.diywallpaper.ui.components.AppImageBackground
import com.example.diywallpaper.ui.feature.dashboard.home.HomeWallpaperGrid
import com.example.diywallpaper.ui.preview.core.PreviewVisibilityInfo
import com.example.diywallpaper.ui.theme.AuroraGradient
import com.example.diywallpaper.ui.theme.SkyBlue
import com.example.diywallpaper.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCollectionScreen(
    onOpenDesign: (String) -> Unit = {},
    onOpenPreview: (sourceType: PreviewSourceType, categoryId: String, itemId: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: DashboardCollectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteDesignId by rememberSaveable { mutableStateOf<String?>(null) }

    DashboardCollectionContent(
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onOpenDesign = onOpenDesign,
        onDeleteDesignClick = { designId -> pendingDeleteDesignId = designId },
        onOpenPreview = onOpenPreview,
        onViewportChanged = viewModel::onViewportChanged,
        onWallpaperFavoriteClick = viewModel::onWallpaperFavoriteClick,
        onDiyFavoriteClick = viewModel::onDiyFavoriteClick,
        modifier = modifier
    )

    pendingDeleteDesignId?.let { designId ->
        CommonConfirmDialog(
            title = stringResource(id = R.string.collection_delete_design_title),
            message = stringResource(id = R.string.collection_delete_design_message),
            confirmText = stringResource(id = R.string.collection_delete_design_confirm),
            dismissText = stringResource(id = R.string.collection_delete_design_cancel),
            onConfirmClick = {
                pendingDeleteDesignId = null
                viewModel.deleteDesign(designId)
            },
            onDismissRequest = {
                pendingDeleteDesignId = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardCollectionContent(
    uiState: DashboardCollectionUiState,
    onFilterSelected: (CollectionFilter) -> Unit,
    onOpenDesign: (String) -> Unit,
    onDeleteDesignClick: (String) -> Unit,
    onOpenPreview: (sourceType: PreviewSourceType, categoryId: String, itemId: String) -> Unit,
    onViewportChanged: (Map<String, PreviewVisibilityInfo>, Boolean) -> Unit,
    onWallpaperFavoriteClick: (String) -> Unit,
    onDiyFavoriteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            text = stringResource(id = R.string.collection_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Rounded.BrushIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 22.dp)
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AppImageBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CollectionSummaryCard(
                    favoriteCount = uiState.favoriteCount,
                    designCount = uiState.designCount,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        uiState.selectedFilter == CollectionFilter.FAVORITES -> {
                            if (uiState.favorites.isEmpty()) {
                                CollectionStatusMessage(
                                    text = stringResource(id = R.string.collection_favorites_empty)
                                )
                            } else {
                                HomeWallpaperGrid(
                                    feedItems = uiState.favorites,
                                    activeVideoIds = uiState.activeVideoIds,
                                    activeDiyAnimationIds = uiState.activeDiyAnimationIds,
                                    onViewportChanged = onViewportChanged,
                                    onWallpaperFavoriteClick = onWallpaperFavoriteClick,
                                    onDiyFavoriteClick = onDiyFavoriteClick,
                                    onOpenPreview = onOpenPreview,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        uiState.designs.isEmpty() -> {
                            CollectionStatusMessage(
                                text = stringResource(id = R.string.collection_designs_empty)
                            )
                        }

                        else -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 148.dp),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(
                                    count = uiState.designs.size,
                                    key = { index -> uiState.designs[index].id }
                                ) { index ->
                                    UserDesignCard(
                                        design = uiState.designs[index],
                                        onClick = { onOpenDesign(uiState.designs[index].id) },
                                        onDeleteClick = { onDeleteDesignClick(uiState.designs[index].id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionSummaryCard(
    favoriteCount: Int,
    designCount: Int,
    selectedFilter: CollectionFilter,
    onFilterSelected: (CollectionFilter) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(AuroraGradient))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.collection_saved_for_you),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.collection_saved_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CollectionFilterChip(
                        text = stringResource(id = R.string.collection_favorites_count, favoriteCount),
                        isSelected = selectedFilter == CollectionFilter.FAVORITES,
                        onClick = { onFilterSelected(CollectionFilter.FAVORITES) }
                    )
                    CollectionFilterChip(
                        text = stringResource(id = R.string.collection_designs_count, designCount),
                        isSelected = selectedFilter == CollectionFilter.DESIGNS,
                        onClick = { onFilterSelected(CollectionFilter.DESIGNS) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun CollectionFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) SkyBlue else TextPrimary,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )
    }
}

@Composable
private fun UserDesignCard(
    design: UserDesign,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = design.previewPath ?: design.exportedImagePath ?: design.thumbnailPath,
                contentDescription = stringResource(id = R.string.collection_design_preview),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onDeleteClick)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(id = R.string.collection_delete_design),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun CollectionStatusMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
