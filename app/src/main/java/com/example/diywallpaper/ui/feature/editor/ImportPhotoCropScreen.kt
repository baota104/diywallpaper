package com.example.diywallpaper.ui.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush.Companion.horizontalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.diywallpaper.R
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.CropPresetRatio
import com.example.diywallpaper.ui.theme.Border
import com.example.diywallpaper.ui.theme.PinkStrong
import com.example.diywallpaper.ui.theme.Primary
import com.example.diywallpaper.ui.theme.PrimaryGradient
import com.example.diywallpaper.ui.theme.Surface
import com.example.diywallpaper.ui.theme.TextSecondary
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPhotoCropScreen(
    imageUri: String,
    onBackClick: () -> Unit,
    onNextClick: (CropSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRatio by remember { mutableStateOf(CropPresetRatio.RATIO_9_16) }
    var imageBoundsSize by remember { mutableStateOf(IntSize.Zero) }
    var cropRect by remember { mutableStateOf(CropRect.Zero) }

    LaunchedEffect(selectedRatio, imageBoundsSize) {
        if (imageBoundsSize.width > 0 && imageBoundsSize.height > 0) {
            cropRect = fitCropRect(
                boundsWidth = imageBoundsSize.width.toFloat(),
                boundsHeight = imageBoundsSize.height.toFloat(),
                ratio = selectedRatio.aspectRatio
            )
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Text(
                        text = stringResource(R.string.crop_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    GradientCropNextButton(
                        onClick = {
                            onNextClick(
                                cropRect.toCropSpec(
                                    boundsWidth = imageBoundsSize.width.toFloat().coerceAtLeast(1f),
                                    boundsHeight = imageBoundsSize.height.toFloat().coerceAtLeast(1f),
                                    ratio = selectedRatio
                                )
                            )
                        },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        bottomBar = {
            CropRatioBottomPanel(
                selectedRatio = selectedRatio,
                onRatioSelected = { selectedRatio = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            CropPreviewFrame(
                imageUri = imageUri,
                ratio = selectedRatio,
                cropRect = cropRect,
                onBoundsChanged = { imageBoundsSize = it },
                onCropRectChanged = { cropRect = it }
            )
        }
    }
}

@Composable
private fun GradientCropNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .wrapContentWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = horizontalGradient(PrimaryGradient),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.crop_next),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun CropRatioBottomPanel(
    selectedRatio: CropPresetRatio,
    onRatioSelected: (CropPresetRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Surface)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.crop_ratio_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            cropRatioItems.forEach { item ->
                CropRatioOption(
                    label = item.label,
                    ratio = item.ratio.aspectRatio,
                    selected = selectedRatio == item.ratio,
                    onClick = { onRatioSelected(item.ratio) }
                )
            }
        }
    }
}

@Composable
private fun CropPreviewFrame(
    imageUri: String,
    ratio: CropPresetRatio,
    cropRect: CropRect,
    onBoundsChanged: (IntSize) -> Unit,
    onCropRectChanged: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(maxHeight * 0.86f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onSizeChanged(onBoundsChanged)
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            CropOverlay(
                cropRect = cropRect,
                ratio = ratio.aspectRatio,
                onCropRectChanged = onCropRectChanged,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CropOverlay(
    cropRect: CropRect,
    ratio: Float,
    onCropRectChanged: (CropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var previewRect by remember { mutableStateOf(cropRect) }

    LaunchedEffect(cropRect) {
        if (cropRect != previewRect) {
            previewRect = cropRect
        }
    }

    Box(
        modifier = modifier.pointerInput(ratio) {
            var activeCropTouchArea = TouchArea.OUTSIDE
            var activeRect = previewRect
            detectDragGestures(
                onDragStart = { offset ->
                    activeRect = previewRect
                    activeCropTouchArea = previewRect.detectTouchArea(offset)
                },
                onDragEnd = { activeCropTouchArea = TouchArea.OUTSIDE },
                onDragCancel = { activeCropTouchArea = TouchArea.OUTSIDE }
            ) { change, dragAmount ->
                change.consume()
                val boundsWidth = size.width.toFloat()
                val boundsHeight = size.height.toFloat()
                val nextRect = when (activeCropTouchArea) {
                    TouchArea.CENTER -> activeRect.moveBy(
                        dx = dragAmount.x,
                        dy = dragAmount.y,
                        boundsWidth = boundsWidth,
                        boundsHeight = boundsHeight
                    )

                    TouchArea.LEFT_TOP -> activeRect.resizeFixed(
                        deltaX = dragAmount.x,
                        deltaY = dragAmount.y,
                        isLeft = true,
                        isTop = true,
                        ratio = ratio,
                        boundsWidth = boundsWidth,
                        boundsHeight = boundsHeight
                    )

                    TouchArea.RIGHT_TOP -> activeRect.resizeFixed(
                        deltaX = dragAmount.x,
                        deltaY = dragAmount.y,
                        isLeft = false,
                        isTop = true,
                        ratio = ratio,
                        boundsWidth = boundsWidth,
                        boundsHeight = boundsHeight
                    )

                    TouchArea.LEFT_BOTTOM -> activeRect.resizeFixed(
                        deltaX = dragAmount.x,
                        deltaY = dragAmount.y,
                        isLeft = true,
                        isTop = false,
                        ratio = ratio,
                        boundsWidth = boundsWidth,
                        boundsHeight = boundsHeight
                    )

                    TouchArea.RIGHT_BOTTOM -> activeRect.resizeFixed(
                        deltaX = dragAmount.x,
                        deltaY = dragAmount.y,
                        isLeft = false,
                        isTop = false,
                        ratio = ratio,
                        boundsWidth = boundsWidth,
                        boundsHeight = boundsHeight
                    )

                    TouchArea.OUTSIDE -> activeRect
                }
                activeRect = nextRect
                previewRect = nextRect
                onCropRectChanged(nextRect)
            }
        }
    ) {
        CropScrimAndGrid(
            cropRect = previewRect,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .padding(
                    start = with(density) { previewRect.left.toDp() },
                    top = with(density) { previewRect.top.toDp() }
                )
                .size(
                    width = with(density) { previewRect.width.toDp() },
                    height = with(density) { previewRect.height.toDp() }
                )
                .border(1.5.dp, Surface)
        ) {
            CropHandle(Modifier.align(Alignment.TopStart))
            CropHandle(Modifier.align(Alignment.TopEnd))
            CropHandle(Modifier.align(Alignment.BottomStart))
            CropHandle(Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun CropScrimAndGrid(
    cropRect: CropRect,
    modifier: Modifier = Modifier
) {
    val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.36f)
    val gridColor = Surface.copy(alpha = 0.88f)
    Canvas(modifier = modifier) {
        val left = cropRect.left
        val top = cropRect.top
        val right = cropRect.right
        val bottom = cropRect.bottom
        drawRect(scrimColor, topLeft = Offset.Zero, size = Size(size.width, top.coerceAtLeast(0f)))
        drawRect(scrimColor, topLeft = Offset(0f, bottom), size = Size(size.width, (size.height - bottom).coerceAtLeast(0f)))
        drawRect(scrimColor, topLeft = Offset(0f, top), size = Size(left.coerceAtLeast(0f), cropRect.height))
        drawRect(scrimColor, topLeft = Offset(right, top), size = Size((size.width - right).coerceAtLeast(0f), cropRect.height))
        val thirdW = cropRect.width / 3f
        val thirdH = cropRect.height / 3f
        repeat(2) { index ->
            val x = left + thirdW * (index + 1)
            drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1.2f)
            val y = top + thirdH * (index + 1)
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1.2f)
        }
        drawRect(
            color = gridColor,
            topLeft = Offset(left, top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
private fun CropHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(14.dp)
            .background(Surface, CircleShape)
            .border(1.5.dp, PinkStrong, CircleShape)
    )
}

@Composable
private fun CropRatioOption(
    label: String,
    ratio: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) PinkStrong else Border
    val labelColor = if (selected) PinkStrong else TextSecondary
    Column(
        modifier = modifier
            .width(58.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .aspectRatio(ratio)
                .border(
                    width = if (selected) 1.8.dp else 1.4.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp)
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private data class CropRatioItem(
    val ratio: CropPresetRatio,
    val label: String
)

private val cropRatioItems = listOf(
    CropRatioItem(CropPresetRatio.RATIO_9_16, "9:16"),
    CropRatioItem(CropPresetRatio.RATIO_3_4, "3:4"),
    CropRatioItem(CropPresetRatio.RATIO_2_3, "2:3"),
    CropRatioItem(CropPresetRatio.RATIO_1_1, "1:1")
)

private val CropPresetRatio.aspectRatio: Float
    get() = when (this) {
        CropPresetRatio.RATIO_9_16 -> 9f / 16f
        CropPresetRatio.RATIO_3_4 -> 3f / 4f
        CropPresetRatio.RATIO_2_3 -> 2f / 3f
        CropPresetRatio.RATIO_1_1 -> 1f
    }

private enum class TouchArea {
    OUTSIDE,
    CENTER,
    LEFT_TOP,
    RIGHT_TOP,
    LEFT_BOTTOM,
    RIGHT_BOTTOM
}

private data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)

    fun detectTouchArea(offset: Offset): TouchArea {
        val cornerTouchSlop = 52f
        val centerTouchSlop = 10f
        return when {
            offset.near(left, top, cornerTouchSlop) -> TouchArea.LEFT_TOP
            offset.near(right, top, cornerTouchSlop) -> TouchArea.RIGHT_TOP
            offset.near(left, bottom, cornerTouchSlop) -> TouchArea.LEFT_BOTTOM
            offset.near(right, bottom, cornerTouchSlop) -> TouchArea.RIGHT_BOTTOM
            offset.x in (left - centerTouchSlop)..(right + centerTouchSlop) &&
                offset.y in (top - centerTouchSlop)..(bottom + centerTouchSlop) -> TouchArea.CENTER
            else -> TouchArea.OUTSIDE
        }
    }

    fun moveBy(
        dx: Float,
        dy: Float,
        boundsWidth: Float,
        boundsHeight: Float
    ): CropRect {
        val nextLeft = (left + dx).coerceIn(0f, (boundsWidth - width).coerceAtLeast(0f))
        val nextTop = (top + dy).coerceIn(0f, (boundsHeight - height).coerceAtLeast(0f))
        return copy(
            left = nextLeft,
            top = nextTop,
            right = nextLeft + width,
            bottom = nextTop + height
        )
    }

    fun resizeFixed(
        deltaX: Float,
        deltaY: Float,
        isLeft: Boolean,
        isTop: Boolean,
        ratio: Float,
        boundsWidth: Float,
        boundsHeight: Float
    ): CropRect {
        val safeRatio = ratio.coerceAtLeast(0.01f)
        val anchorX = if (isLeft) right else left
        val anchorY = if (isTop) bottom else top
        val draggedX = if (isLeft) left + deltaX else right + deltaX
        val draggedY = if (isTop) top + deltaY else bottom + deltaY
        val widthByX = abs(anchorX - draggedX)
        val widthByY = abs(anchorY - draggedY) * safeRatio
        val maxWidth = if (isLeft) anchorX else boundsWidth - anchorX
        val maxHeight = if (isTop) anchorY else boundsHeight - anchorY
        val maxAllowedWidth = min(maxWidth, maxHeight * safeRatio).coerceAtLeast(MinCropSide)
        val nextWidth = maxOf(widthByX, widthByY, MinCropSide)
            .coerceAtMost(maxAllowedWidth)
        val nextHeight = nextWidth / safeRatio
        val nextLeft = if (isLeft) anchorX - nextWidth else anchorX
        val nextRight = if (isLeft) anchorX else anchorX + nextWidth
        val nextTop = if (isTop) anchorY - nextHeight else anchorY
        val nextBottom = if (isTop) anchorY else anchorY + nextHeight
        return CropRect(
            left = nextLeft.coerceIn(0f, boundsWidth),
            top = nextTop.coerceIn(0f, boundsHeight),
            right = nextRight.coerceIn(0f, boundsWidth),
            bottom = nextBottom.coerceIn(0f, boundsHeight)
        )
    }

    fun toCropSpec(
        boundsWidth: Float,
        boundsHeight: Float,
        ratio: CropPresetRatio
    ): CropSpec {
        return CropSpec(
            normalizedLeft = (left / boundsWidth).coerceIn(0f, 1f),
            normalizedTop = (top / boundsHeight).coerceIn(0f, 1f),
            normalizedRight = (right / boundsWidth).coerceIn(0f, 1f),
            normalizedBottom = (bottom / boundsHeight).coerceIn(0f, 1f),
            ratio = ratio
        )
    }

    companion object {
        val Zero = CropRect(0f, 0f, 0f, 0f)
    }
}

private fun fitCropRect(
    boundsWidth: Float,
    boundsHeight: Float,
    ratio: Float
): CropRect {
    val safeRatio = ratio.coerceAtLeast(0.01f)
    val maxWidth = boundsWidth * 0.86f
    val maxHeight = boundsHeight * 0.86f
    val width = min(maxWidth, maxHeight * safeRatio).coerceAtLeast(MinCropSide)
    val height = (width / safeRatio).coerceAtMost(maxHeight)
    val left = (boundsWidth - width) / 2f
    val top = (boundsHeight - height) / 2f
    return CropRect(
        left = left,
        top = top,
        right = left + width,
        bottom = top + height
    )
}

private fun Offset.near(x: Float, y: Float, touchSlop: Float): Boolean {
    return abs(this.x - x) <= touchSlop && abs(this.y - y) <= touchSlop
}

private const val MinCropSide = 72f
