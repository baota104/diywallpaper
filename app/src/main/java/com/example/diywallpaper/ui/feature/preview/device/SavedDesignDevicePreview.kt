package com.example.diywallpaper.ui.feature.preview.device

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.example.diywallpaper.R
import com.example.diywallpaper.core.render.AndroidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidDrawLayerRenderBounds
import com.example.diywallpaper.core.render.androidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidTextRenderSpec
import com.example.diywallpaper.core.render.androidTextTrailStampPoints
import com.example.diywallpaper.core.render.buildAndroidTextPaint
import com.example.diywallpaper.data.local.files.DiyLottieAssetDelegate
import com.example.diywallpaper.data.local.files.DiyLottieReplacementImage
import com.example.diywallpaper.data.local.files.effectiveLottieCrop
import com.example.diywallpaper.data.local.files.lottieAssetKeys
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.DesignRawBounds
import com.example.diywallpaper.domain.model.design.DesignViewportScaleMode
import com.example.diywallpaper.domain.model.design.DesignViewportTransform
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DiyTemplateSnapshot
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.designViewportTransform
import com.example.diywallpaper.domain.model.design.proceduralAnimatedTransform
import com.example.diywallpaper.domain.model.design.requiresLiveExport
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.model.design.renderBounds
import com.example.diywallpaper.domain.model.design.renderSize
import com.example.diywallpaper.domain.model.design.stickerRenderSize
import com.example.diywallpaper.ui.feature.editor.editorFontFamily
import com.example.diywallpaper.ui.feature.editor.editorFontResId
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SavedDesignDevicePreview(
    project: EditorProject,
    isChromeVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val shouldAnimate = project.requiresLiveExport()
    var frameTimeMs by remember(project.id, shouldAnimate) { mutableStateOf(0L) }
    LaunchedEffect(project.id, project.updatedAt, shouldAnimate) {
        if (!shouldAnimate) {
            frameTimeMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            frameTimeMs = withFrameMillis { it }
        }
    }
    Box(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        SavedDesignProjectCanvas(
            project = project,
            frameTimeMs = frameTimeMs,
            isAnimating = shouldAnimate,
            modifier = Modifier.fillMaxSize()
        )

        if (isChromeVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 94.dp),
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

@Composable
private fun SavedDesignProjectCanvas(
    project: EditorProject,
    frameTimeMs: Long,
    isAnimating: Boolean,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        val modelWidth = project.canvas.width.toFloat().coerceAtLeast(1f)
        val modelHeight = project.canvas.height.toFloat().coerceAtLeast(1f)
        val viewport = designViewportTransform(
            designWidth = modelWidth,
            designHeight = modelHeight,
            targetWidth = constraints.maxWidth.toFloat(),
            targetHeight = constraints.maxHeight.toFloat(),
            scaleMode = DesignViewportScaleMode.Cover
        )
        val eraseColor = resolvePreviewEraseColor(project.background)

        // Fill the whole target first. Layer positions are mapped separately via viewport.
        // This avoids Compose graphicsLayer clipping a transformed virtual child.
        SavedDesignBackground(
            background = project.background,
            modifier = Modifier.fillMaxSize()
        )

        val diySource = project.source as? EditorProjectSource.Diy
        val diyLottiePreviewState = diySource
            ?.takeIf { it.isLive }
            ?.let { rememberDiyLottiePreviewData(it) }
        if (diySource != null) {
            val diyLottiePreview = diyLottiePreviewState?.value
            if (diyLottiePreview != null) {
                SavedDiyLottieTemplateContent(
                    source = diySource,
                    previewData = diyLottiePreview,
                    viewport = viewport,
                    frameTimeMs = frameTimeMs
                )
                SavedDiyTemplateContent(
                    templateId = diySource.templateId,
                    templateSnapshot = diySource.templateSnapshot,
                    placeholders = project.placeholders,
                    viewport = viewport,
                    frameTimeMs = frameTimeMs,
                    isAnimating = false,
                    shouldDrawElement = { element ->
                        element.shouldDrawOverLottie(diyLottiePreview.defaultBitmaps.keys)
                    }
                )
            } else {
                SavedDiyTemplateContent(
                    templateId = diySource.templateId,
                    templateSnapshot = diySource.templateSnapshot,
                    placeholders = project.placeholders,
                    viewport = viewport,
                    frameTimeMs = frameTimeMs,
                    isAnimating = isAnimating && diySource.isLive
                )
            }
        }

        project.layers
            .filterNot { it.isHidden }
            .sortedBy { it.zIndex }
            .forEach { layer ->
                when (layer) {
                    is StickerLayer -> SavedStickerLayer(
                        layer = layer,
                        viewport = viewport
                    )

                    is PhotoLayer -> SavedPhotoLayer(
                        layer = layer,
                        viewport = viewport
                    )

                    is TextLayer -> SavedTextLayer(
                        layer = layer,
                        viewport = viewport
                    )

                    is DrawLayer -> {
                        if (layer.drawData.isBrushStackRenderable()) {
                            SavedBrushStrokeStack(
                                layers = listOf(layer),
                                viewport = viewport,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SavedDrawLayer(
                                layer = layer,
                                viewport = viewport,
                                eraseColor = eraseColor
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun rememberDiyLottiePreviewData(
    source: EditorProjectSource.Diy
): State<DiyLottiePreviewData?> {
    val context = LocalContext.current
    return produceState<DiyLottiePreviewData?>(
        initialValue = null,
        context,
        source.templateId,
        source.diyAnimationPathOrUrl,
        source.templateSnapshot
    ) {
        value = withContext(Dispatchers.IO) {
            loadDiyLottiePreviewData(context, source)
        }
    }
}

@Composable
private fun SavedDiyLottieTemplateContent(
    source: EditorProjectSource.Diy,
    previewData: DiyLottiePreviewData,
    viewport: DesignViewportTransform,
    frameTimeMs: Long
) {
    val drawable = remember(previewData) {
        LottieDrawable().apply {
            setComposition(previewData.composition)
            setImageAssetDelegate(
                DiyLottieAssetDelegate(
                    replacementBitmaps = previewData.replacementBitmaps,
                    defaultBitmaps = previewData.defaultBitmaps
                )
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val durationMs = previewData.composition.duration
            .roundToInt()
            .coerceAtLeast(1)
        drawable.bounds = Rect(
            0,
            0,
            source.templateSnapshot.width,
            source.templateSnapshot.height
        )
        drawable.progress = ((frameTimeMs % durationMs).toFloat() / durationMs)
            .coerceIn(0f, 1f)
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            nativeCanvas.translate(viewport.offsetX, viewport.offsetY)
            nativeCanvas.scale(viewport.scale, viewport.scale)
            drawable.draw(nativeCanvas)
            nativeCanvas.restore()
        }
    }
}

private data class DiyLottiePreviewData(
    val composition: LottieComposition,
    val replacementBitmaps: Map<String, DiyLottieReplacementImage>,
    val defaultBitmaps: Map<String, Bitmap?>
)

private fun loadDiyLottiePreviewData(
    context: android.content.Context,
    source: EditorProjectSource.Diy
): DiyLottiePreviewData? {
    val animationFile = resolveDiyLottieAnimationFile(context, source) ?: return null
    val composition = LottieCompositionFactory
        .fromJsonStringSync(
            animationFile.readText(),
            source.diyLottieCompositionCacheKey(animationFile.absolutePath)
        )
        .value
        ?: return null
    return DiyLottiePreviewData(
        composition = composition,
        replacementBitmaps = buildPreviewLottieReplacementBitmapMap(source),
        defaultBitmaps = loadPreviewLottieDefaultImageAssets(animationFile)
    )
}

private fun resolveDiyLottieAnimationFile(
    context: android.content.Context,
    source: EditorProjectSource.Diy
): File? {
    source.diyAnimationPathOrUrl
        ?.takeIf { it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://") }
        ?.let(::File)
        ?.takeIf { it.exists() && it.isFile }
        ?.let { return it }

    val assetDirectory = File(context.filesDir, "diy_templates/${source.templateId}/assets")
        .takeIf { it.exists() && it.isDirectory }
        ?: return null
    File(assetDirectory, "animation/data.json")
        .takeIf { it.exists() && it.isFile }
        ?.let { return it }
    return File(assetDirectory, "animation")
        .takeIf { it.exists() && it.isDirectory }
        ?.walkTopDown()
        ?.firstOrNull { it.isFile && it.extension.equals("json", ignoreCase = true) }
}

private fun loadPreviewLottieDefaultImageAssets(animationFile: File): Map<String, Bitmap?> {
    val animationDirectory = animationFile.parentFile ?: return emptyMap()
    return sequenceOf(
        File(animationDirectory, "images"),
        animationDirectory
    )
        .filter { it.exists() && it.isDirectory }
        .flatMap { directory -> directory.walkTopDown().filter { it.isFile } }
        .filter { file -> file.extension.lowercase() in LOTTIE_IMAGE_EXTENSIONS }
        .flatMap { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            listOf(
                file.name to bitmap,
                file.relativeToOrSelf(animationDirectory).invariantSeparatorsPath to bitmap
            )
        }
        .toMap()
}

private fun buildPreviewLottieReplacementBitmapMap(
    source: EditorProjectSource.Diy
): Map<String, DiyLottieReplacementImage> {
    return source.templateSnapshot.elements
        .filter { it.isImageElement() }
        .map { element -> element to element.lottieAssetKeys() }
        .filter { (element, keys) -> !element.localImagePath.isNullOrBlank() && keys.isNotEmpty() }
        .flatMap { (element, keys) ->
            val localImagePath = element.localImagePath ?: return@flatMap emptyList()
            val bitmap = BitmapFactory.decodeFile(localImagePath) ?: return@flatMap emptyList()
            val replacement = DiyLottieReplacementImage(bitmap = bitmap, crop = element.effectiveLottieCrop())
            keys.map { key -> key to replacement }
        }
        .toMap()
}

@Composable
private fun SavedDiyTemplateContent(
    templateId: String,
    templateSnapshot: DiyTemplateSnapshot,
    placeholders: List<PhotoPlaceholderLayer>,
    viewport: DesignViewportTransform,
    frameTimeMs: Long,
    isAnimating: Boolean,
    shouldDrawElement: (DiyTemplateElementSnapshot) -> Boolean = { true }
) {
    templateSnapshot.elements
        .sortedBy { it.zIndex }
            .filter(shouldDrawElement)
            .forEach { element ->
                when {
                element.isTextElement() -> {
                    SavedDiyTemplateText(
                        templateId = templateId,
                        element = element,
                        viewport = viewport,
                        frameTimeMs = frameTimeMs,
                        isAnimating = isAnimating
                    )
                }

                element.isAssetElement() -> {
                    SavedDiyTemplatePicture(
                        templateId = templateId,
                        element = element,
                        viewport = viewport,
                        frameTimeMs = frameTimeMs,
                        isAnimating = isAnimating
                    )
                }

                element.isImageElement() -> {
                    SavedDiyPlaceholderPreview(
                        templateId = templateId,
                        element = element,
                        viewport = viewport,
                        frameTimeMs = frameTimeMs,
                        isAnimating = isAnimating
                    )
                }
            }
        }
}

@Composable
private fun SavedDiyTemplateText(
    templateId: String,
    element: DiyTemplateElementSnapshot,
    viewport: DesignViewportTransform,
    frameTimeMs: Long,
    isAnimating: Boolean
) {
    val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
    val textColor = runCatching { parsePreviewColor(element.fontColor) }
        .getOrDefault(Color.Black)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = viewport.modelXToTarget(transform.offsetX).roundToInt(),
                    y = viewport.modelYToTarget(transform.offsetY).roundToInt()
                )
            }
            .size(
                width = pxToDp(element.width * viewport.scale),
                height = pxToDp(element.height * viewport.scale)
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paint = TextPaint().apply {
                color = textColor.toArgb()
                textSize = max(1f, element.fontSize * viewport.scale)
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    element.title,
                    0f,
                    paint.textSize,
                    paint
                )
            }
        }
    }
}

@Composable
private fun SavedDiyTemplatePicture(
    templateId: String,
    element: DiyTemplateElementSnapshot,
    viewport: DesignViewportTransform,
    frameTimeMs: Long,
    isAnimating: Boolean
) {
    val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = viewport.modelXToTarget(transform.offsetX).roundToInt(),
                    y = viewport.modelYToTarget(transform.offsetY).roundToInt()
                )
            }
            .size(
                width = pxToDp(element.width * viewport.scale),
                height = pxToDp(element.height * viewport.scale)
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale
            )
    ) {
        AsyncImage(
            model = element.assetUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun SavedDiyPlaceholderPreview(
    templateId: String,
    element: DiyTemplateElementSnapshot,
    viewport: DesignViewportTransform,
    frameTimeMs: Long,
    isAnimating: Boolean
) {
    if (element.localImagePath.isNullOrBlank() && element.previewPathOrUrl.isNullOrBlank()) return
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = viewport.modelXToTarget(element.x).roundToInt(),
                    y = viewport.modelYToTarget(element.y).roundToInt()
                )
            }
            .size(
                width = pxToDp(element.width * viewport.scale),
                height = pxToDp(element.height * viewport.scale)
            )
            .graphicsLayer(
                rotationZ = element.rotation,
                clip = true,
                compositingStrategy = CompositingStrategy.Offscreen
            )
            .placeholderMask(element.maskPathOrUrl)
    ) {
        if (!element.localImagePath.isNullOrBlank()) {
            SavedDiyElementImageContent(
                templateId = templateId,
                element = element,
                viewport = viewport,
                frameTimeMs = frameTimeMs,
                isAnimating = isAnimating
            )
        } else {
            AsyncImage(
                model = element.previewPathOrUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun SavedDiyElementImageContent(
    templateId: String,
    element: DiyTemplateElementSnapshot,
    viewport: DesignViewportTransform,
    frameTimeMs: Long,
    isAnimating: Boolean
) {
    val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
    val baseWidth = element.contentBaseWidth ?: element.width
    val baseHeight = element.contentBaseHeight ?: element.height
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = ((transform.offsetX - element.x) * viewport.scale).roundToInt(),
                    y = ((transform.offsetY - element.y) * viewport.scale).roundToInt()
                )
            }
            .size(
                width = pxToDp(baseWidth * viewport.scale),
                height = pxToDp(baseHeight * viewport.scale)
            )
            .graphicsLayer(
                rotationZ = transform.rotation - element.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale
            )
    ) {
        SavedPhotoLayerContent(
            layer = PhotoLayer(
                id = element.id,
                localPath = element.localImagePath.orEmpty(),
                crop = element.crop,
                zIndex = element.zIndex,
                transform = LayerTransform(0f, 0f, 1f, 0f),
                isLocked = false,
                isHidden = false
            )
        )
    }
}

@Composable
private fun SavedDesignBackground(
    background: EditorBackground,
    modifier: Modifier = Modifier
) {
    when (background) {
        is EditorBackground.ApiImage -> AsyncImage(
            model = background.imageUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.LocalImage -> AsyncImage(
            model = background.localPath,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )

        is EditorBackground.Gradient -> Box(
            modifier = modifier.background(
                brush = Brush.linearGradient(background.colors.map { parsePreviewColor(it) })
            )
        )

        is EditorBackground.SolidColor -> Box(
            modifier = modifier.background(parsePreviewColor(background.colorHex))
        )
    }
}

@Composable
private fun SavedStickerLayer(
    layer: StickerLayer,
    viewport: DesignViewportTransform
) {
    val size = layer.renderSize()
    SavedLayerBox(
        transform = layer.transform,
        width = size.width,
        height = size.height,
        viewport = viewport
    ) {
        AsyncImage(
            model = layer.animatedAssetPathOrUrl ?: layer.assetPathOrUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SavedPhotoLayer(
    layer: PhotoLayer,
    viewport: DesignViewportTransform
) {
    val baseSize = photoRenderSize(layer.crop?.ratio)
    SavedLayerBox(
        transform = layer.transform,
        width = baseSize.width,
        height = baseSize.height,
        viewport = viewport
    ) {
        SavedPhotoLayerContent(layer = layer)
    }
}

@Composable
private fun SavedPhotoLayerContent(layer: PhotoLayer) {
    val imageBitmap = remember(layer.localPath) {
        BitmapFactory.decodeFile(layer.localPath)?.asImageBitmap()
    }
    if (imageBitmap != null) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val crop = layer.crop
            val srcLeft = ((crop?.normalizedLeft ?: 0f).coerceIn(0f, 1f) * imageBitmap.width)
                .roundToInt()
            val srcTop = ((crop?.normalizedTop ?: 0f).coerceIn(0f, 1f) * imageBitmap.height)
                .roundToInt()
            val srcRight = ((crop?.normalizedRight ?: 1f).coerceIn(0f, 1f) * imageBitmap.width)
                .roundToInt()
                .coerceAtLeast(srcLeft + 1)
            val srcBottom = ((crop?.normalizedBottom ?: 1f).coerceIn(0f, 1f) * imageBitmap.height)
                .roundToInt()
                .coerceAtLeast(srcTop + 1)
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(srcLeft, srcTop),
                srcSize = IntSize(srcRight - srcLeft, srcBottom - srcTop),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )
        }
    } else {
        AsyncImage(
            model = layer.localPath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun Modifier.placeholderMask(maskPathOrUrl: String?): Modifier {
    val maskBitmap = remember(maskPathOrUrl) {
        maskPathOrUrl
            ?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    } ?: return this

    return drawWithContent {
        drawContent()
        drawImage(
            image = maskBitmap,
            dstSize = IntSize(
                width = size.width.roundToInt().coerceAtLeast(1),
                height = size.height.roundToInt().coerceAtLeast(1)
            ),
            blendMode = BlendMode.DstIn
        )
    }
}

private fun DiyTemplateElementSnapshot.isTextElement(): Boolean {
    return type.equals("TEXT", ignoreCase = true) ||
        type.equals("Text", ignoreCase = true)
}

private fun DiyTemplateElementSnapshot.isAssetElement(): Boolean {
    return !isImageElement() &&
        !isTextElement() &&
        !assetUrl.isNullOrBlank()
}

private fun DiyTemplateElementSnapshot.isImageElement(): Boolean {
    return type.equals("IMAGE", ignoreCase = true) ||
        type.equals("Image", ignoreCase = true)
}

private fun DiyTemplateElementSnapshot.lottieAssetKeys(): List<String> {
    return lottieAssetKeys(srcName)
        .ifEmpty { previewPathOrUrl?.let(::lottieAssetKeys).orEmpty() }
        .ifEmpty { assetUrl?.let(::lottieAssetKeys).orEmpty() }
}

private fun DiyTemplateElementSnapshot.shouldDrawOverLottie(lottieDefaultKeys: Set<String>): Boolean {
    if (isImageElement()) return false
    val keys = lottieAssetKeys()
    return keys.isEmpty() || keys.none { it in lottieDefaultKeys }
}

private fun EditorProjectSource.Diy.diyLottieCompositionCacheKey(animationSource: String): String {
    val replacementFingerprint = templateSnapshot.elements
        .filter { it.isImageElement() }
        .joinToString(separator = "|") { element ->
            listOf(
                element.id,
                element.srcName,
                element.localImagePath.orEmpty(),
                element.crop?.normalizedLeft,
                element.crop?.normalizedTop,
                element.crop?.normalizedRight,
                element.crop?.normalizedBottom
            ).joinToString(separator = ":")
        }
    return "diy-lottie-preview:$templateId:$animationSource:$replacementFingerprint"
}

private fun DiyTemplateElementSnapshot.matchPlaceholder(
    placeholders: List<PhotoPlaceholderLayer>
): PhotoPlaceholderLayer? {
    return placeholders.firstOrNull { placeholder ->
        placeholder.zIndex == zIndex &&
            placeholder.x == x &&
            placeholder.y == y &&
            placeholder.width == width &&
            placeholder.height == height
    } ?: placeholders.firstOrNull { it.zIndex == zIndex }
}

private fun DiyTemplateElementSnapshot.editTransform(): LayerTransform {
    if (isFixedTemplateElement()) {
        return LayerTransform(x, y, 1f, rotation)
    }
    return if (isImageElement() && localImagePath != null) {
        contentTransform ?: LayerTransform(x, y, 1f, rotation)
    } else {
        transform ?: LayerTransform(x, y, 1f, rotation)
    }
}

private fun DiyTemplateElementSnapshot.isFixedTemplateElement(): Boolean {
    return type.equals("PICTURE", ignoreCase = true) ||
        type.equals("Picture", ignoreCase = true)
}

@Composable
private fun SavedTextLayer(
    layer: TextLayer,
    viewport: DesignViewportTransform
) {
    val context = LocalContext.current
    val spec = remember(context, layer.text, layer.style) {
        androidTextLayerRenderSpec(
            context = context,
            layer = layer,
            fontResId = editorFontResId(layer.style.fontFamilyId)
        )
    }
    val bounds = spec.bounds
    val textColor = resolvePreviewTextColor(layer.style.textBrush, layer.style.textColorHex).toArgb()
    val paint = remember(context, layer.style, layer.transform.alpha, viewport.scale, textColor) {
        buildAndroidTextPaint(
            context = context,
            style = layer.style,
            fontResId = editorFontResId(layer.style.fontFamilyId),
            color = textColor,
            alpha = layer.transform.alpha,
            textScale = viewport.scale
        )
    }
    SavedLayerBox(
        transform = layer.transform.copy(
            offsetX = layer.transform.offsetX + bounds.minX,
            offsetY = layer.transform.offsetY + bounds.minY
        ),
        width = bounds.width,
        height = bounds.height,
        viewport = viewport
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    layer.text,
                    (spec.drawX - bounds.minX) * viewport.scale,
                    (spec.drawBaselineY - bounds.minY) * viewport.scale,
                    paint
                )
            }
        }
    }
}

@Composable
private fun SavedBrushStrokeStack(
    layers: List<DrawLayer>,
    viewport: DesignViewportTransform,
    modifier: Modifier = Modifier
) {
    if (layers.isEmpty()) return
    val context = LocalContext.current
    val patternBrushBitmaps = rememberPatternBrushBitmaps()
    Box(modifier = modifier) {
        layers.forEach { layer ->
            val bounds = remember(context, layer.drawData) {
                androidDrawLayerRenderBounds(
                    context = context,
                    drawData = layer.drawData,
                    fontResIdFor = ::editorFontResId
                )
            } ?: layer.drawData.renderBounds() ?: return@forEach
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = layer.transform.alpha)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                withSavedDrawExporterTransform(
                    bounds = bounds,
                    transform = layer.transform,
                    viewport = viewport
                ) {
                    layer.drawData.forEachBrushStackItem { item ->
                        drawSavedBrushStackStroke(
                            item = item,
                            scale = viewport.scale,
                            patternBrushBitmaps = patternBrushBitmaps
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSavedBrushStackStroke(
    item: BrushStackItem,
    scale: Float,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>
) {
    val stroke = when (item) {
        is BrushStackItem.Draw -> item.stroke
        is BrushStackItem.Erase -> item.stroke
    }
    if (stroke.points.size < 2) return
    val patternStyle = (item as? BrushStackItem.Draw)?.stroke?.brushStyle as? BrushStyleSpec.Pattern
    val patternBitmap = patternStyle?.let { patternBrushBitmaps[it.drawableName] }
    if (patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
        drawSavedPatternBrushStroke(
            stroke = stroke,
            bitmap = patternBitmap,
            patternStyle = patternStyle,
            scale = scale
        )
        return
    }
    val style = stroke.brushStyle
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(stroke.points.first().x * scale, stroke.points.first().y * scale)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scale, point.y * scale)
        }
    }
    if (style is BrushStyleSpec.Outline && item is BrushStackItem.Draw) {
        drawPath(
            path = path,
            color = parsePreviewColor(style.strokeColorHex),
            style = Stroke(
                width = stroke.strokeWidth * scale * 1.55f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        drawPath(
            path = path,
            color = parsePreviewColor(style.fillColorHex),
            style = Stroke(
                width = stroke.strokeWidth * scale,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        return
    }
    if (style is BrushStyleSpec.Glow && item is BrushStackItem.Draw) {
        drawSavedGlowBrushStroke(
            stroke = stroke,
            scale = scale,
            color = parsePreviewColor(style.colorHex),
            glowColor = parsePreviewColor(style.glowColorHex)
        )
        return
    }
    val brush = when (val style = stroke.brushStyle) {
        is BrushStyleSpec.Gradient -> Brush.linearGradient(
            colors = style.colors.map { parsePreviewColor(it) }
        )

        is BrushStyleSpec.Solid -> Brush.linearGradient(
            listOf(parsePreviewColor(style.colorHex), parsePreviewColor(style.colorHex))
        )

        is BrushStyleSpec.Dashed -> Brush.linearGradient(
            listOf(parsePreviewColor(style.colorHex), parsePreviewColor(style.colorHex))
        )

        is BrushStyleSpec.Outline -> Brush.linearGradient(
            listOf(parsePreviewColor(style.fillColorHex), parsePreviewColor(style.fillColorHex))
        )

        is BrushStyleSpec.Glow -> Brush.linearGradient(
            listOf(parsePreviewColor(style.colorHex), parsePreviewColor(style.colorHex))
        )

        is BrushStyleSpec.Pattern,
        null -> {
            val color = parsePreviewColor(stroke.colorHex ?: "#1D1726")
            Brush.linearGradient(listOf(color, color))
        }
    }
    drawPath(
        path = path,
        brush = brush,
        style = Stroke(
            width = stroke.strokeWidth * scale,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
            pathEffect = (style as? BrushStyleSpec.Dashed)?.let {
                val width = stroke.strokeWidth * scale
                PathEffect.dashPathEffect(floatArrayOf(width * 1.7f, width * 1.25f), 0f)
            }
        ),
        blendMode = if (item is BrushStackItem.Erase) BlendMode.Clear else BlendMode.SrcOver
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.withSavedDrawExporterTransform(
    bounds: DesignRawBounds,
    transform: LayerTransform,
    viewport: DesignViewportTransform,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    val pivot = Offset(
        x = viewport.modelXToTarget(transform.offsetX + bounds.minX + bounds.width / 2f),
        y = viewport.modelYToTarget(transform.offsetY + bounds.minY + bounds.height / 2f)
    )
    withTransform({
        rotate(degrees = transform.rotation, pivot = pivot)
        scale(scaleX = transform.scale, scaleY = transform.scale, pivot = pivot)
        translate(
            left = viewport.offsetX + transform.offsetX * viewport.scale,
            top = viewport.offsetY + transform.offsetY * viewport.scale
        )
    }) {
        block()
    }
}

@Composable
private fun rememberPatternBrushBitmaps(): Map<String, android.graphics.Bitmap?> {
    val context = LocalContext.current
    return remember(context) {
        (1..20).associate { index ->
            val name = "brush$index"
            val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
            name to resId.takeIf { it != 0 }?.let {
                BitmapFactory.decodeResource(context.resources, it)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSavedPatternBrushStroke(
    stroke: BrushStroke,
    bitmap: android.graphics.Bitmap,
    patternStyle: BrushStyleSpec.Pattern,
    scale: Float
) {
    val path = android.graphics.Path().apply {
        moveTo(stroke.points.first().x * scale, stroke.points.first().y * scale)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scale, point.y * scale)
        }
    }
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return
    val iconScale = (stroke.strokeWidth / bitmap.width.coerceAtLeast(1)) *
        patternStyle.scale.coerceAtLeast(0.1f) *
        scale
    val step = (bitmap.width * iconScale * patternStyle.spacingFactor.coerceAtLeast(0.35f))
        .coerceAtLeast(4f)
    val position = FloatArray(2)
    val tangent = FloatArray(2)
    val matrix = Matrix()
    var distance = step / 2f
    drawIntoCanvas { composeCanvas ->
        val nativeCanvas = composeCanvas.nativeCanvas
        while (distance < length && measure.getPosTan(distance, position, tangent)) {
            matrix.reset()
            matrix.setScale(iconScale, iconScale)
            matrix.postTranslate(
                -bitmap.width * iconScale / 2f,
                -bitmap.height * iconScale / 2f
            )
            if (patternStyle.followPath) {
                val angle = Math.toDegrees(kotlin.math.atan2(tangent[1], tangent[0]).toDouble()).toFloat()
                matrix.postRotate(angle)
            }
            matrix.postTranslate(position[0], position[1])
            nativeCanvas.drawBitmap(bitmap, matrix, null)
            distance += step
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSavedGlowBrushStroke(
    stroke: BrushStroke,
    scale: Float,
    color: Color,
    glowColor: Color
) {
    val nativePath = android.graphics.Path().apply {
        moveTo(stroke.points.first().x * scale, stroke.points.first().y * scale)
        stroke.points.drop(1).forEach { point ->
            lineTo(point.x * scale, point.y * scale)
        }
    }
    val width = stroke.strokeWidth * scale
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = glowColor.copy(alpha = 0.45f).toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeWidth = width * 1.9f
        maskFilter = BlurMaskFilter(width * 0.9f, BlurMaskFilter.Blur.NORMAL)
    }
    nativeCanvas.drawPath(nativePath, glowPaint)
    val corePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        strokeWidth = width
    }
    nativeCanvas.drawPath(nativePath, corePaint)
}

@Composable
private fun SavedDrawLayer(
    layer: DrawLayer,
    viewport: DesignViewportTransform,
    eraseColor: Color
) {
    val context = LocalContext.current
    val patternBrushBitmaps = rememberPatternBrushBitmaps()
    val bounds = remember(context, layer.drawData) {
        androidDrawLayerRenderBounds(
            context = context,
            drawData = layer.drawData,
            fontResIdFor = ::editorFontResId
        )
    } ?: layer.drawData.renderBounds() ?: return
    val textTrailTypeface = remember(layer.drawData) {
        val textTrail = layer.drawData as? DrawLayerData.TextTrail
        textTrail?.let {
            ResourcesCompat.getFont(context, editorFontResId(it.textStyle.fontFamilyId))
        }
    }
    val textTrailSpec = remember(context, layer.drawData) {
        val textTrail = layer.drawData as? DrawLayerData.TextTrail
        textTrail?.let {
            androidTextRenderSpec(
                context = context,
                text = it.text,
                style = it.textStyle,
                fontResId = editorFontResId(it.textStyle.fontFamilyId)
            )
        }
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = layer.transform.alpha)
    ) {
        withSavedDrawExporterTransform(
            bounds = bounds,
            transform = layer.transform,
            viewport = viewport
        ) {
            when (val data = layer.drawData) {
                is DrawLayerData.FreeStroke -> drawPreviewStrokeLocal(
                    stroke = data.stroke,
                    originX = 0f,
                    originY = 0f,
                    scale = viewport.scale,
                    color = parsePreviewColor(data.stroke.colorHex ?: "#1D1726"),
                    patternBrushBitmaps = patternBrushBitmaps
                )

                is DrawLayerData.EraseStroke -> drawPreviewStrokeLocal(
                    stroke = data.stroke,
                    originX = 0f,
                    originY = 0f,
                    scale = viewport.scale,
                    color = eraseColor,
                    patternBrushBitmaps = patternBrushBitmaps
                )

                is DrawLayerData.BrushStack -> Unit

                is DrawLayerData.TextTrail -> drawPreviewTextTrail(
                    data = data,
                    originX = 0f,
                    originY = 0f,
                    scale = viewport.scale,
                    typeface = textTrailTypeface,
                    textSpec = textTrailSpec
                )

                is DrawLayerData.StickerTrail -> Unit
            }
        }
    }
    val stickerTrail = layer.drawData as? DrawLayerData.StickerTrail
    if (stickerTrail != null) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = viewport.modelXToTarget(layer.transform.offsetX + bounds.minX).roundToInt(),
                        y = viewport.modelYToTarget(layer.transform.offsetY + bounds.minY).roundToInt()
                    )
                }
                .size(
                    width = with(LocalDensity.current) { (bounds.width * viewport.scale).toDp() },
                    height = with(LocalDensity.current) { (bounds.height * viewport.scale).toDp() }
                )
                .graphicsLayer(
                    rotationZ = layer.transform.rotation,
                    scaleX = layer.transform.scale,
                    scaleY = layer.transform.scale,
                    alpha = layer.transform.alpha
                )
        ) {
            DrawPreviewStickerTrail(
                data = stickerTrail,
                originX = bounds.minX,
                originY = bounds.minY,
                viewportScale = viewport.scale
            )
        }
    }
}

@Composable
private fun DrawPreviewStickerTrail(
    data: DrawLayerData.StickerTrail,
    originX: Float,
    originY: Float,
    viewportScale: Float
) {
    if (data.points.isEmpty()) return
    val density = LocalDensity.current
    data.points.forEachIndexed { index, point ->
        if (index % data.spacing.coerceAtLeast(1f).toInt().coerceAtLeast(1) == 0) {
            val rotation = if (data.rotationMode == StickerTrailRotationMode.FOLLOW_PATH && index > 0) {
                val previous = data.points[index - 1]
                kotlin.math.atan2(point.y - previous.y, point.x - previous.x) * 180f / Math.PI.toFloat()
            } else {
                0f
            }
            AsyncImage(
                model = data.stickerAssetPathOrUrl,
                contentDescription = null,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = ((point.x - originX) * viewportScale).roundToInt(),
                            y = ((point.y - originY) * viewportScale).roundToInt()
                        )
                    }
                    .size(with(density) { (data.stampSize * viewportScale).toDp() })
                    .graphicsLayer(rotationZ = rotation),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun SavedLayerBox(
    transform: LayerTransform,
    width: Float,
    height: Float,
    viewport: DesignViewportTransform,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = viewport.modelXToTarget(transform.offsetX).roundToInt(),
                    y = viewport.modelYToTarget(transform.offsetY).roundToInt()
                )
            }
            .size(
                width = with(density) { (width * viewport.scale).toDp() },
                height = with(density) { (height * viewport.scale).toDp() }
            )
            .graphicsLayer(
                rotationZ = transform.rotation,
                scaleX = transform.scale,
                scaleY = transform.scale,
                alpha = transform.alpha
            )
    ) {
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewStrokeLocal(
    stroke: BrushStroke,
    originX: Float,
    originY: Float,
    scale: Float,
    color: Color,
    patternBrushBitmaps: Map<String, android.graphics.Bitmap?>
) {
    if (stroke.points.size < 2) return
    val patternStyle = stroke.brushStyle as? BrushStyleSpec.Pattern
    val patternBitmap = patternStyle?.let { patternBrushBitmaps[it.drawableName] }
    if (patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
        drawSavedPatternBrushStrokeLocal(
            stroke = stroke,
            bitmap = patternBitmap,
            patternStyle = patternStyle,
            originX = originX,
            originY = originY,
            scale = scale
        )
        return
    }
    drawContext.canvas.nativeCanvas.apply {
        val path = android.graphics.Path()
        stroke.points.forEachIndexed { index, point ->
            val x = (point.x - originX) * scale
            val y = (point.y - originY) * scale
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        when (val style = stroke.brushStyle) {
            is BrushStyleSpec.Outline -> {
                val outlinePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = parsePreviewColor(style.strokeColorHex).toArgb()
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeWidth = stroke.strokeWidth * scale * 1.55f
                }
                drawPath(path, outlinePaint)
                val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = parsePreviewColor(style.fillColorHex).toArgb()
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeWidth = stroke.strokeWidth * scale
                }
                drawPath(path, fillPaint)
                return
            }

            is BrushStyleSpec.Glow -> {
                val width = stroke.strokeWidth * scale
                val glowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = parsePreviewColor(style.glowColorHex).copy(alpha = 0.45f).toArgb()
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeWidth = width * 1.9f
                    maskFilter = BlurMaskFilter(width * 0.9f, BlurMaskFilter.Blur.NORMAL)
                }
                drawPath(path, glowPaint)
                val corePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = parsePreviewColor(style.colorHex).toArgb()
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeWidth = width
                }
                drawPath(path, corePaint)
                return
            }

            else -> Unit
        }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = when (val style = stroke.brushStyle) {
                is BrushStyleSpec.Dashed -> parsePreviewColor(style.colorHex).toArgb()
                is BrushStyleSpec.Solid -> parsePreviewColor(style.colorHex).toArgb()
                else -> color.toArgb()
            }
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            strokeWidth = stroke.strokeWidth * scale
            if (stroke.brushStyle is BrushStyleSpec.Dashed) {
                pathEffect = android.graphics.DashPathEffect(
                    floatArrayOf(strokeWidth * 1.7f, strokeWidth * 1.25f),
                    0f
                )
            }
        }
        drawPath(path, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSavedPatternBrushStrokeLocal(
    stroke: BrushStroke,
    bitmap: android.graphics.Bitmap,
    patternStyle: BrushStyleSpec.Pattern,
    originX: Float,
    originY: Float,
    scale: Float
) {
    val path = android.graphics.Path().apply {
        moveTo((stroke.points.first().x - originX) * scale, (stroke.points.first().y - originY) * scale)
        stroke.points.drop(1).forEach { point ->
            lineTo((point.x - originX) * scale, (point.y - originY) * scale)
        }
    }
    val measure = PathMeasure(path, false)
    val length = measure.length
    if (length <= 0f) return
    val iconScale = (stroke.strokeWidth / bitmap.width.coerceAtLeast(1)) *
        patternStyle.scale.coerceAtLeast(0.1f) *
        scale
    val step = (bitmap.width * iconScale * patternStyle.spacingFactor.coerceAtLeast(0.35f))
        .coerceAtLeast(4f)
    val position = FloatArray(2)
    val tangent = FloatArray(2)
    val matrix = Matrix()
    var distance = step / 2f
    val nativeCanvas = drawContext.canvas.nativeCanvas
    while (distance < length && measure.getPosTan(distance, position, tangent)) {
        matrix.reset()
        matrix.setScale(iconScale, iconScale)
        matrix.postTranslate(
            -bitmap.width * iconScale / 2f,
            -bitmap.height * iconScale / 2f
        )
        if (patternStyle.followPath) {
            val angle = Math.toDegrees(kotlin.math.atan2(tangent[1], tangent[0]).toDouble()).toFloat()
            matrix.postRotate(angle)
        }
        matrix.postTranslate(position[0], position[1])
        nativeCanvas.drawBitmap(bitmap, matrix, null)
        distance += step
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPreviewTextTrail(
    data: DrawLayerData.TextTrail,
    originX: Float,
    originY: Float,
    scale: Float,
    typeface: Typeface?,
    textSpec: AndroidTextLayerRenderSpec?
) {
    if (data.points.isEmpty() || data.text.isBlank()) return
    val nativeCanvas = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = resolvePreviewTextColor(data.textStyle.textBrush, data.textStyle.textColorHex).toArgb()
        textSize = data.textStyle.fontSizeSp * density * scale
        isFakeBoldText = true
        this.typeface = typeface
    }
    androidTextTrailStampPoints(data).forEach { point ->
        nativeCanvas.drawText(
            data.text,
            (point.x + (textSpec?.drawX ?: 0f) - originX) * scale,
            (point.y + (textSpec?.drawBaselineY ?: 0f) - originY) * scale,
            paint
        )
    }
}

private fun resolvePreviewTextColor(
    brush: TextBrushStyle?,
    fallbackHex: String?
): Color {
    return when (brush) {
        is TextBrushStyle.Solid -> parsePreviewColor(brush.colorHex)
        is TextBrushStyle.Gradient -> parsePreviewColor(brush.colors.firstOrNull() ?: fallbackHex ?: "#1D1726")
        null -> parsePreviewColor(fallbackHex ?: "#1D1726")
    }
}

private fun resolvePreviewEraseColor(background: EditorBackground): Color {
    return when (background) {
        is EditorBackground.SolidColor -> parsePreviewColor(background.colorHex)
        else -> Color.White
    }
}

private fun parsePreviewColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Color.White)
}

private fun DrawLayerData.isBrushStackRenderable(): Boolean {
    return this is DrawLayerData.FreeStroke ||
        this is DrawLayerData.EraseStroke ||
        this is DrawLayerData.BrushStack
}

private val LOTTIE_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

private inline fun DrawLayerData.forEachBrushStackItem(block: (BrushStackItem) -> Unit) {
    when (this) {
        is DrawLayerData.FreeStroke -> block(BrushStackItem.Draw(stroke))
        is DrawLayerData.EraseStroke -> block(BrushStackItem.Erase(stroke))
        is DrawLayerData.BrushStack -> items.forEach(block)
        else -> Unit
    }
}

@Composable
private fun pxToDp(px: Float): Dp {
    return with(LocalDensity.current) { px.toDp() }
}
