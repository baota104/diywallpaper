package com.example.diywallpaper.data.local.files

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.example.diywallpaper.core.render.androidDrawLayerRenderBounds
import com.example.diywallpaper.core.render.androidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidTextRenderSpec
import com.example.diywallpaper.core.render.androidTextTrailStampPoints
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.EditorTextAlign
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.TextShadowSpec
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.model.design.DesignViewportScaleMode
import com.example.diywallpaper.domain.model.design.designViewportTransform
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.model.design.renderBounds
import com.example.diywallpaper.domain.model.design.renderSize
import com.example.diywallpaper.domain.model.design.stickerRenderSize
import com.example.diywallpaper.domain.repository.DesignAssetExporter
import com.example.diywallpaper.ui.feature.editor.editorFontResId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class AndroidDesignAssetExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val designFileStore: DesignFileStore,
    private val okHttpClient: OkHttpClient
) : DesignAssetExporter {
    private val patternBrushBitmapCache = mutableMapOf<String, Bitmap?>()

    override suspend fun export(project: EditorProject): AppResult<GeneratedDesignAssets> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val assetCache = mutableMapOf<String, Bitmap?>()
                val thumbnailPath = writeBitmapAsset(
                    project = project,
                    width = 270,
                    height = 480,
                    filePath = designFileStore.thumbnailFilePath(project.id),
                    format = webpFormat(),
                    quality = 90,
                    assetCache = assetCache
                )
                val previewPath = writeBitmapAsset(
                    project = project,
                    width = 540,
                    height = 960,
                    filePath = designFileStore.previewFilePath(project.id),
                    format = webpFormat(),
                    quality = 92,
                    assetCache = assetCache
                )
                val exportSize = resolveStaticOutputSize(project)
                val exportedImagePath = writeBitmapAsset(
                    project = project,
                    width = exportSize.width,
                    height = exportSize.height,
                    filePath = designFileStore.exportedImageFilePath(project.id),
                    format = Bitmap.CompressFormat.PNG,
                    quality = 100,
                    assetCache = assetCache
                )
                assetCache.values.forEach { bitmap ->
                    bitmap?.takeUnless(Bitmap::isRecycled)?.recycle()
                }
                GeneratedDesignAssets(
                    thumbnailPath = thumbnailPath,
                    previewPath = previewPath,
                    exportedImagePath = exportedImagePath
                )
            }.fold(
                onSuccess = { AppResult.Success(it) },
                onFailure = { AppResult.Error(AppError.ExportError(it.message)) }
            )
        }
    }

    private suspend fun writeBitmapAsset(
        project: EditorProject,
        width: Int,
        height: Int,
        filePath: String,
        format: Bitmap.CompressFormat,
        quality: Int,
        assetCache: MutableMap<String, Bitmap?>
    ): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        renderProject(
            canvas = canvas,
            project = project,
            width = width.toFloat(),
            height = height.toFloat(),
            assetCache = assetCache
        )
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(format, quality, output)
        bitmap.recycle()
        return when (val result = designFileStore.writeBinaryAsset(filePath, output.toByteArray())) {
            is AppResult.Success -> result.data
            is AppResult.Error -> throw IllegalStateException(result.error.toString())
        }
    }

    private suspend fun renderProject(
        canvas: Canvas,
        project: EditorProject,
        width: Float,
        height: Float,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val designWidth = project.canvas.width.toFloat().coerceAtLeast(1f)
        val designHeight = project.canvas.height.toFloat().coerceAtLeast(1f)
        val viewport = designViewportTransform(
            designWidth = designWidth,
            designHeight = designHeight,
            targetWidth = width,
            targetHeight = height,
            scaleMode = DesignViewportScaleMode.Cover
        )

        canvas.save()
        canvas.translate(viewport.offsetX, viewport.offsetY)
        canvas.scale(viewport.scale, viewport.scale)
        try {
            drawBackground(
                canvas = canvas,
                background = project.background,
                canvasWidth = designWidth,
                canvasHeight = designHeight,
                assetCache = assetCache
            )

            val eraseColor = resolveEraseColor(project.background)
            val diySource = project.source as? EditorProjectSource.Diy
            if (diySource != null) {
                drawDiyTemplateContent(
                    canvas = canvas,
                    project = project,
                    source = diySource,
                    assetCache = assetCache
                )
            }
            project.layers
                .filterNot(EditorLayer::isHidden)
                .sortedBy(EditorLayer::zIndex)
                .forEach { layer ->
                    when (layer) {
                        is TextLayer -> drawTextLayer(canvas, layer)
                        is StickerLayer -> drawStickerLayer(canvas, layer, assetCache)
                        is PhotoLayer -> drawPhotoLayer(
                            canvas = canvas,
                            layer = layer,
                            assetCache = assetCache
                        )
                        is DrawLayer -> {
                            if (layer.drawData.isBrushStackRenderable()) {
                                drawBrushStrokeStack(
                                    canvas = canvas,
                                    layers = listOf(layer),
                                    width = designWidth,
                                    height = designHeight
                                )
                            } else {
                                drawDrawLayer(canvas, layer, eraseColor, assetCache)
                            }
                        }
                    }
                }
        } finally {
            canvas.restore()
        }
    }

    private suspend fun drawDiyTemplateContent(
        canvas: Canvas,
        project: EditorProject,
        source: EditorProjectSource.Diy,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        source.templateSnapshot.elements
            .sortedBy { it.zIndex }
            .forEach { element ->
                when {
                    element.isTextElement() -> {
                        drawDiyTemplateText(canvas, element)
                    }

                    element.isAssetElement() -> {
                        drawDiyTemplatePicture(canvas, element, assetCache)
                    }

                    element.isImageElement() -> drawDiyImageElement(canvas, element, assetCache)
                }
            }
    }

    private fun drawDiyTemplateText(
        canvas: Canvas,
        element: DiyTemplateElementSnapshot
    ) {
        val transform = element.editTransform()
        val rect = RectF(
            transform.offsetX,
            transform.offsetY,
            transform.offsetX + element.width * transform.scale,
            transform.offsetY + element.height * transform.scale
        )
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(element.fontColor, Color.BLACK)
            textSize = (element.fontSize * transform.scale).coerceAtLeast(1f)
            typeface = Typeface.DEFAULT
        }
        canvas.save()
        canvas.rotate(transform.rotation, rect.centerX(), rect.centerY())
        canvas.drawText(element.title, rect.left, rect.top + paint.textSize, paint)
        canvas.restore()
    }

    private suspend fun drawDiyTemplatePicture(
        canvas: Canvas,
        element: DiyTemplateElementSnapshot,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val bitmap = loadBitmap(element.assetUrl.orEmpty(), assetCache, element.width.roundToInt(), element.height.roundToInt())
            ?: return
        val transform = element.editTransform()
        val rect = RectF(
            transform.offsetX,
            transform.offsetY,
            transform.offsetX + element.width * transform.scale,
            transform.offsetY + element.height * transform.scale
        )
        canvas.save()
        canvas.rotate(transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapFit(canvas, bitmap, rect, alpha = 1f)
        canvas.restore()
    }

    private suspend fun drawDiyImageElement(
        canvas: Canvas,
        element: DiyTemplateElementSnapshot,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val previewSource = element.localImagePath?.takeUnless { it.isBlank() }
            ?: element.previewPathOrUrl?.takeUnless { it.isBlank() }
            ?: return
        val previewBitmap = loadBitmap(
            model = previewSource,
            assetCache = assetCache,
            width = element.width.roundToInt().coerceAtLeast(1),
            height = element.height.roundToInt().coerceAtLeast(1)
        ) ?: return
        val placeholderRect = RectF(
            element.x,
            element.y,
            element.x + element.width,
            element.y + element.height
        )
        val maskBitmap = element.maskPathOrUrl?.let {
            loadBitmap(
                model = it,
                assetCache = assetCache,
                width = element.width.roundToInt().coerceAtLeast(1),
                height = element.height.roundToInt().coerceAtLeast(1)
            )
        }
        val checkpoint = maskBitmap?.let {
            canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)
        }
        canvas.save()
        canvas.rotate(element.rotation, placeholderRect.centerX(), placeholderRect.centerY())
        canvas.clipRect(placeholderRect)
        if (element.localImagePath.isNullOrBlank()) {
            drawBitmapCover(canvas, previewBitmap, placeholderRect, null)
        } else {
            val transform = element.editTransform()
            val baseWidth = element.contentBaseWidth ?: element.width
            val baseHeight = element.contentBaseHeight ?: element.height
            val contentRect = RectF(
                transform.offsetX,
                transform.offsetY,
                transform.offsetX + baseWidth * transform.scale,
                transform.offsetY + baseHeight * transform.scale
            )
            canvas.rotate(transform.rotation - element.rotation, contentRect.centerX(), contentRect.centerY())
            drawBitmapCrop(canvas, previewBitmap, element.crop, contentRect, alpha = 1f)
        }
        canvas.restore()

        if (maskBitmap != null && checkpoint != null) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            canvas.save()
            canvas.rotate(element.rotation, placeholderRect.centerX(), placeholderRect.centerY())
            canvas.drawBitmap(maskBitmap, null, placeholderRect, maskPaint)
            canvas.restore()
            maskPaint.xfermode = null
            canvas.restoreToCount(checkpoint)
        }
    }

    private suspend fun drawBackground(
        canvas: Canvas,
        background: EditorBackground,
        canvasWidth: Float,
        canvasHeight: Float,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        when (background) {
            is EditorBackground.SolidColor -> {
                canvas.drawColor(parseColor(background.colorHex, Color.WHITE))
            }

            is EditorBackground.Gradient -> {
                val colors = background.colors.ifEmpty { listOf("#FFFFFF", "#F4E8FF") }
                    .map { parseColor(it, Color.WHITE) }
                    .toIntArray()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        canvasWidth,
                        canvasHeight,
                        colors,
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, canvasWidth, canvasHeight, paint)
            }

            is EditorBackground.ApiImage -> {
                val bitmap = loadBitmap(background.imageUrl, assetCache, canvasWidth.toInt(), canvasHeight.toInt())
                if (bitmap != null) {
                    drawBitmapCover(canvas, bitmap, RectF(0f, 0f, canvasWidth, canvasHeight), null)
                } else {
                    drawFallbackImageBackground(canvas, canvasWidth, canvasHeight)
                }
            }

            is EditorBackground.LocalImage -> {
                val bitmap = loadBitmap(background.localPath, assetCache, canvasWidth.toInt(), canvasHeight.toInt())
                if (bitmap != null) {
                    drawBitmapCover(canvas, bitmap, RectF(0f, 0f, canvasWidth, canvasHeight), background.crop)
                } else {
                    drawFallbackImageBackground(canvas, canvasWidth, canvasHeight)
                }
            }
        }
    }

    private suspend fun drawStickerLayer(
        canvas: Canvas,
        layer: StickerLayer,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val source = layer.animatedAssetPathOrUrl ?: layer.assetPathOrUrl
        val bitmap = loadBitmap(source, assetCache, 1024, 1024) ?: return
        val size = layer.renderSize()
        val rect = centerScaledLayerRect(
            offsetX = layer.transform.offsetX,
            offsetY = layer.transform.offsetY,
            width = size.width,
            height = size.height,
            scale = layer.transform.scale
        )
        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapFit(canvas, bitmap, rect, layer.transform.alpha)
        canvas.restore()
    }

    private suspend fun drawPhotoLayer(
        canvas: Canvas,
        layer: PhotoLayer,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val bitmap = loadBitmap(layer.localPath, assetCache, 1400, 1400) ?: return
        val size = photoRenderSize(layer.crop?.ratio)
        val rect = centerScaledLayerRect(
            offsetX = layer.transform.offsetX,
            offsetY = layer.transform.offsetY,
            width = size.width,
            height = size.height,
            scale = layer.transform.scale
        )
        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapCrop(canvas, bitmap, layer.crop, rect, layer.transform.alpha)
        canvas.restore()
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: TextLayer
    ) {
        if (layer.text.isBlank()) return
        val spec = androidTextLayerRenderSpec(
            context = context,
            layer = layer,
            fontResId = editorFontResId(layer.style.fontFamilyId)
        )
        val bounds = spec.bounds
        val rect = RectF(
            layer.transform.offsetX + bounds.minX,
            layer.transform.offsetY + bounds.minY,
            layer.transform.offsetX + bounds.maxX,
            layer.transform.offsetY + bounds.maxY
        )
        val textPaint = buildTextPaint(layer.style, layer.transform.alpha, transformScale = 1f)

        canvas.save()
        canvas.translate(rect.centerX(), rect.centerY())
        canvas.rotate(layer.transform.rotation)
        canvas.scale(layer.transform.scale, layer.transform.scale)
        canvas.translate(-rect.centerX(), -rect.centerY())
        canvas.drawText(
            layer.text,
            layer.transform.offsetX + spec.drawX,
            layer.transform.offsetY + spec.drawBaselineY,
            textPaint
        )
        canvas.restore()
    }

    private fun drawBrushStrokeStack(
        canvas: Canvas,
        layers: List<DrawLayer>,
        width: Float,
        height: Float
    ) {
        if (layers.isEmpty()) return
        layers.sortedBy { it.zIndex }.forEach { layer ->
            val bounds = androidDrawLayerRenderBounds(
                context = context,
                drawData = layer.drawData,
                fontResIdFor = ::editorFontResId
            ) ?: layer.drawData.renderBounds() ?: return@forEach
            val rect = RectF(
                layer.transform.offsetX + bounds.minX,
                layer.transform.offsetY + bounds.minY,
                layer.transform.offsetX + bounds.maxX,
                layer.transform.offsetY + bounds.maxY
            )
            val checkpoint = canvas.saveLayer(0f, 0f, width, height, null)
            canvas.save()
            canvas.translate(rect.centerX(), rect.centerY())
            canvas.rotate(layer.transform.rotation)
            canvas.scale(layer.transform.scale, layer.transform.scale)
            canvas.translate(-rect.width() / 2f, -rect.height() / 2f)
            canvas.translate(-bounds.minX, -bounds.minY)
            layer.drawData.forEachBrushStackItem { item ->
                when (item) {
                    is BrushStackItem.Draw -> drawStroke(
                        canvas = canvas,
                        stroke = item.stroke,
                        color = resolveStrokeColor(item.stroke),
                        clear = false
                    )

                    is BrushStackItem.Erase -> drawStroke(
                        canvas = canvas,
                        stroke = item.stroke,
                        color = Color.TRANSPARENT,
                        clear = true
                    )
                }
            }
            canvas.restore()
            canvas.restoreToCount(checkpoint)
        }
    }

    private suspend fun drawDrawLayer(
        canvas: Canvas,
        layer: DrawLayer,
        eraseColor: Int,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val bounds = androidDrawLayerRenderBounds(
            context = context,
            drawData = layer.drawData,
            fontResIdFor = ::editorFontResId
        ) ?: layer.drawData.renderBounds()
        canvas.save()
        if (bounds != null) {
            val rect = RectF(
                layer.transform.offsetX + bounds.minX,
                layer.transform.offsetY + bounds.minY,
                layer.transform.offsetX + bounds.maxX,
                layer.transform.offsetY + bounds.maxY
            )
            canvas.translate(rect.centerX(), rect.centerY())
            canvas.rotate(layer.transform.rotation)
            canvas.scale(layer.transform.scale, layer.transform.scale)
            canvas.translate(-rect.width() / 2f, -rect.height() / 2f)
            canvas.translate(-bounds.minX, -bounds.minY)
        } else {
            canvas.translate(layer.transform.offsetX, layer.transform.offsetY)
            canvas.scale(layer.transform.scale, layer.transform.scale)
            canvas.rotate(layer.transform.rotation)
        }
        try {
            when (val drawData = layer.drawData) {
                is DrawLayerData.FreeStroke -> drawStroke(canvas, drawData.stroke, resolveStrokeColor(drawData.stroke), clear = false)
                is DrawLayerData.EraseStroke -> drawStroke(canvas, drawData.stroke, eraseColor, clear = false)
                is DrawLayerData.BrushStack -> Unit
                is DrawLayerData.StickerTrail -> {
                    val bitmap = loadBitmap(drawData.stickerAssetPathOrUrl, assetCache, 512, 512) ?: return
                    drawData.points.forEachIndexed { index, point ->
                        if (index % drawData.spacing.coerceAtLeast(1f).toInt().coerceAtLeast(1) == 0) {
                            val stampSize = drawData.stampSize.coerceAtLeast(1f)
                            val rect = RectF(
                                point.x - stampSize / 2f,
                                point.y - stampSize / 2f,
                                point.x + stampSize / 2f,
                                point.y + stampSize / 2f
                            )
                            canvas.save()
                            if (drawData.rotationMode == StickerTrailRotationMode.FOLLOW_PATH && index > 0) {
                                val previous = drawData.points[index - 1]
                                val angle = kotlin.math.atan2(point.y - previous.y, point.x - previous.x) * 180f / Math.PI.toFloat()
                                canvas.rotate(angle, rect.centerX(), rect.centerY())
                            }
                            drawBitmapFit(canvas, bitmap, rect, 1f)
                            canvas.restore()
                        }
                    }
                }

                is DrawLayerData.TextTrail -> {
                    val textSpec = androidTextRenderSpec(
                        context = context,
                        text = drawData.text,
                        style = drawData.textStyle,
                        fontResId = editorFontResId(drawData.textStyle.fontFamilyId)
                    )
                    val textPaint = buildTextPaint(drawData.textStyle, alpha = 1f, transformScale = 1f)
                    androidTextTrailStampPoints(drawData).forEach { point ->
                        canvas.drawText(
                            drawData.text,
                            point.x + textSpec.drawX,
                            point.y + textSpec.drawBaselineY,
                            textPaint
                        )
                    }
                }
            }
        } finally {
            canvas.restore()
        }
    }

    private fun drawStroke(
        canvas: Canvas,
        stroke: BrushStroke,
        color: Int,
        clear: Boolean
    ) {
        if (stroke.points.size < 2) return
        val patternStyle = stroke.brushStyle as? BrushStyleSpec.Pattern
        val patternBitmap = patternStyle?.let { style -> loadPatternBrushBitmap(style.drawableName) }
        if (!clear && patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
            drawPatternStroke(canvas, stroke, patternBitmap, patternStyle)
            return
        }
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        if (!clear) {
            when (val style = stroke.brushStyle) {
                is BrushStyleSpec.Outline -> {
                    drawNativeStroke(
                        canvas = canvas,
                        path = path,
                        color = parseColor(style.strokeColorHex, Color.BLACK),
                        strokeWidth = stroke.strokeWidth * 1.55f
                    )
                    drawNativeStroke(
                        canvas = canvas,
                        path = path,
                        color = parseColor(style.fillColorHex, Color.BLACK),
                        strokeWidth = stroke.strokeWidth
                    )
                    return
                }

                is BrushStyleSpec.Glow -> {
                    drawGlowStroke(
                        canvas = canvas,
                        path = path,
                        strokeWidth = stroke.strokeWidth,
                        color = parseColor(style.colorHex, Color.WHITE),
                        glowColor = parseColor(style.glowColorHex, Color.WHITE)
                    )
                    return
                }

                else -> Unit
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = when (val style = stroke.brushStyle) {
                is BrushStyleSpec.Dashed -> parseColor(style.colorHex, color)
                is BrushStyleSpec.Solid -> parseColor(style.colorHex, color)
                else -> color
            }
            if (clear) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else if (stroke.brushStyle is BrushStyleSpec.Gradient) {
                shader = LinearGradient(
                    stroke.points.first().x,
                    stroke.points.first().y,
                    stroke.points.last().x,
                    stroke.points.last().y,
                    stroke.brushStyle.colors.map { parseColor(it, Color.BLACK) }.toIntArray(),
                    null,
                    Shader.TileMode.CLAMP
                )
            } else if (stroke.brushStyle is BrushStyleSpec.Dashed) {
                pathEffect = DashPathEffect(
                    floatArrayOf(strokeWidth * 1.7f, strokeWidth * 1.25f),
                    0f
                )
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawNativeStroke(
        canvas: Canvas,
        path: Path,
        color: Int,
        strokeWidth: Float
    ) {
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                this.strokeWidth = strokeWidth
            }
        )
    }

    private fun drawGlowStroke(
        canvas: Canvas,
        path: Path,
        strokeWidth: Float,
        color: Int,
        glowColor: Int
    ) {
        canvas.drawPath(
            path,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = glowColor.withAlpha(0.45f)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                this.strokeWidth = strokeWidth * 1.9f
                maskFilter = BlurMaskFilter(strokeWidth * 0.9f, BlurMaskFilter.Blur.NORMAL)
            }
        )
        drawNativeStroke(canvas, path, color, strokeWidth)
    }

    private fun drawPatternStroke(
        canvas: Canvas,
        stroke: BrushStroke,
        bitmap: Bitmap,
        patternStyle: BrushStyleSpec.Pattern
    ) {
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        val measure = PathMeasure(path, false)
        val length = measure.length
        if (length <= 0f) return
        val iconScale = (stroke.strokeWidth / bitmap.width.coerceAtLeast(1)) *
            patternStyle.scale.coerceAtLeast(0.1f)
        val step = (bitmap.width * iconScale * patternStyle.spacingFactor.coerceAtLeast(0.35f))
            .coerceAtLeast(4f)
        val position = FloatArray(2)
        val tangent = FloatArray(2)
        val matrix = Matrix()
        var distance = step / 2f
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
            canvas.drawBitmap(bitmap, matrix, null)
            distance += step
        }
    }

    private fun buildTextPaint(
        style: TextStyleSpec,
        alpha: Float,
        transformScale: Float
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextColor(style)
            textSize = style.fontSizeSp * context.resources.displayMetrics.scaledDensity * transformScale
            letterSpacing = style.letterSpacing / style.fontSizeSp.coerceAtLeast(1f)
            typeface = ResourcesCompat.getFont(
                context,
                editorFontResId(style.fontFamilyId)
            )
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            applyShadow(style.shadow, transformScale)
            if (style.textBrush is TextBrushStyle.Gradient) {
                shader = LinearGradient(
                    0f,
                    0f,
                    measureText("MMMM").coerceAtLeast(1f),
                    textSize,
                    style.textBrush.colors.map { parseColor(it, Color.BLACK) }.toIntArray(),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
        }
    }

    private fun TextPaint.applyShadow(shadow: TextShadowSpec?, scale: Float) {
        if (shadow == null) return
        setShadowLayer(
            shadow.blurRadius * scale,
            shadow.offsetX * scale,
            shadow.offsetY * scale,
            parseColor(shadow.colorHex, Color.TRANSPARENT)
        )
    }

    private fun centerScaledLayerRect(
        offsetX: Float,
        offsetY: Float,
        width: Float,
        height: Float,
        scale: Float
    ): RectF {
        val centerX = offsetX + width / 2f
        val centerY = offsetY + height / 2f
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        return RectF(
            centerX - scaledWidth / 2f,
            centerY - scaledHeight / 2f,
            centerX + scaledWidth / 2f,
            centerY + scaledHeight / 2f
        )
    }

    private fun drawBitmapFit(canvas: Canvas, bitmap: Bitmap, rect: RectF, alpha: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(bitmap, null, rect, paint)
    }

    private fun drawBitmapCrop(
        canvas: Canvas,
        bitmap: Bitmap,
        crop: CropSpec?,
        rect: RectF,
        alpha: Float
    ) {
        val left = ((crop?.normalizedLeft ?: 0f).coerceIn(0f, 1f) * bitmap.width)
            .roundToInt()
        val top = ((crop?.normalizedTop ?: 0f).coerceIn(0f, 1f) * bitmap.height)
            .roundToInt()
        val right = ((crop?.normalizedRight ?: 1f).coerceIn(0f, 1f) * bitmap.width)
            .roundToInt()
            .coerceAtLeast(left + 1)
        val bottom = ((crop?.normalizedBottom ?: 1f).coerceIn(0f, 1f) * bitmap.height)
            .roundToInt()
            .coerceAtLeast(top + 1)
        val src = Rect(
            left.coerceIn(0, bitmap.width - 1),
            top.coerceIn(0, bitmap.height - 1),
            right.coerceIn(1, bitmap.width),
            bottom.coerceIn(1, bitmap.height)
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(bitmap, src, rect, paint)
    }

    private fun drawBitmapCover(
        canvas: Canvas,
        bitmap: Bitmap,
        destination: RectF,
        crop: CropSpec?,
        paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    ) {
        val sourceRect = resolveSourceRect(bitmap, crop)
        val matrix = Matrix().apply {
            setRectToRect(
                sourceRect,
                destination,
                Matrix.ScaleToFit.CENTER
            )
            val values = FloatArray(9)
            getValues(values)
            val scale = max(values[Matrix.MSCALE_X], values[Matrix.MSCALE_Y])
            postScale(
                scale / values[Matrix.MSCALE_X].coerceAtLeast(0.0001f),
                scale / values[Matrix.MSCALE_Y].coerceAtLeast(0.0001f),
                destination.centerX(),
                destination.centerY()
            )
        }
        canvas.save()
        canvas.clipRect(destination)
        canvas.drawBitmap(bitmap, matrix, paint)
        canvas.restore()
    }

    private fun resolveSourceRect(bitmap: Bitmap, crop: CropSpec?): RectF {
        if (crop == null) {
            return RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        }
        val left = (crop.normalizedLeft.coerceIn(0f, 1f) * bitmap.width)
        val top = (crop.normalizedTop.coerceIn(0f, 1f) * bitmap.height)
        val right = (crop.normalizedRight.coerceIn(0f, 1f) * bitmap.width).coerceAtLeast(left + 1f)
        val bottom = (crop.normalizedBottom.coerceIn(0f, 1f) * bitmap.height).coerceAtLeast(top + 1f)
        return RectF(left, top, right, bottom)
    }

    private suspend fun loadBitmap(
        model: String,
        assetCache: MutableMap<String, Bitmap?>,
        width: Int,
        height: Int
    ): Bitmap? {
        assetCache[model]?.let { return it }
        val bitmap = runCatching {
            when {
                model.startsWith("http://") || model.startsWith("https://") -> {
                    val request = Request.Builder().url(model).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        response.body?.bytes()?.let { bytes ->
                            decodeBitmapBytes(bytes)
                        }
                    }
                }

                model.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(model))?.use(BitmapFactory::decodeStream)
                }

                model.startsWith("file://") -> {
                    BitmapFactory.decodeFile(model.removePrefix("file://"))
                }

                else -> {
                    BitmapFactory.decodeFile(model)
                }
            }
        }.getOrNull()
        assetCache[model] = bitmap
        return bitmap
    }

    private fun decodeBitmapBytes(bytes: ByteArray): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(ByteBuffer.wrap(bytes))
            ) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private fun drawFallbackImageBackground(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width,
                height,
                intArrayOf(
                    Color.parseColor("#FFF8FC"),
                    Color.parseColor("#E6F4FF"),
                    Color.parseColor("#F8EEFF")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, paint)
    }

    private fun resolveStrokeColor(stroke: BrushStroke): Int {
        return when (val style = stroke.brushStyle) {
            is BrushStyleSpec.Gradient -> parseColor(style.colors.firstOrNull(), Color.BLACK)
            is BrushStyleSpec.Solid -> parseColor(style.colorHex, Color.BLACK)
            is BrushStyleSpec.Dashed -> parseColor(style.colorHex, Color.BLACK)
            is BrushStyleSpec.Outline -> parseColor(style.fillColorHex, Color.BLACK)
            is BrushStyleSpec.Glow -> parseColor(style.colorHex, Color.WHITE)
            is BrushStyleSpec.Pattern -> parseColor(stroke.colorHex, Color.BLACK)
            null -> parseColor(stroke.colorHex, Color.BLACK)
        }
    }

    private fun loadPatternBrushBitmap(drawableName: String): Bitmap? {
        if (patternBrushBitmapCache.containsKey(drawableName)) {
            return patternBrushBitmapCache[drawableName]
        }
        val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
        val bitmap = resId.takeIf { it != 0 }?.let {
            BitmapFactory.decodeResource(context.resources, it)
        }
        patternBrushBitmapCache[drawableName] = bitmap
        return bitmap
    }

    private fun resolveTextColor(style: TextStyleSpec): Int {
        return when (val brush = style.textBrush) {
            is TextBrushStyle.Solid -> parseColor(brush.colorHex, Color.BLACK)
            is TextBrushStyle.Gradient -> parseColor(brush.colors.firstOrNull(), Color.BLACK)
            null -> parseColor(style.textColorHex, Color.BLACK)
        }
    }

    private fun resolveEraseColor(background: EditorBackground): Int {
        return when (background) {
            is EditorBackground.SolidColor -> parseColor(background.colorHex, Color.WHITE)
            else -> Color.WHITE
        }
    }

    private fun resolveLineSpacingMultiplier(style: TextStyleSpec): Float {
        val defaultLineHeight = style.fontSizeSp * 1.2f
        val targetLineHeight = style.lineHeight ?: defaultLineHeight
        return (targetLineHeight / style.fontSizeSp.coerceAtLeast(1f)).coerceAtLeast(1f)
    }

    private fun EditorTextAlign.toLayoutAlignment(): Layout.Alignment {
        return when (this) {
            EditorTextAlign.START -> Layout.Alignment.ALIGN_NORMAL
            EditorTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            EditorTextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
    }

    private fun parseColor(hex: String?, fallback: Int): Int {
        return runCatching { Color.parseColor(hex ?: "") }.getOrDefault(fallback)
    }

    private fun Int.withAlpha(alpha: Float): Int {
        return (this and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)
    }

    private fun resolveStaticOutputSize(project: EditorProject): StaticOutputSize {
        val bounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java)
                ?.maximumWindowMetrics
                ?.bounds
        } else {
            null
        }
        val metrics = context.resources.displayMetrics
        val displayWidth = bounds?.width()?.takeIf { it > 0 }
            ?: metrics.widthPixels.takeIf { it > 0 }
            ?: project.canvas.width
        val displayHeight = bounds?.height()?.takeIf { it > 0 }
            ?: metrics.heightPixels.takeIf { it > 0 }
            ?: project.canvas.height
        val aspectRatio = displayWidth.toFloat() / displayHeight.toFloat().coerceAtLeast(1f)
        var height = minOf(displayHeight, MAX_EXPORT_HEIGHT).coerceAtLeast(2)
        var width = (height * aspectRatio).roundToInt().coerceAtLeast(2)
        if (width > MAX_EXPORT_WIDTH) {
            width = MAX_EXPORT_WIDTH
            height = (width / aspectRatio).roundToInt().coerceAtLeast(2)
        }
        return StaticOutputSize(
            width = width.even().coerceAtLeast(2),
            height = height.even().coerceAtLeast(2)
        )
    }

    private fun Int.even(): Int = if (this % 2 == 0) this else this - 1

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

    private fun DrawLayerData.isBrushStackRenderable(): Boolean {
        return this is DrawLayerData.FreeStroke ||
            this is DrawLayerData.EraseStroke ||
            this is DrawLayerData.BrushStack
    }

    private inline fun DrawLayerData.forEachBrushStackItem(block: (BrushStackItem) -> Unit) {
        when (this) {
            is DrawLayerData.FreeStroke -> block(BrushStackItem.Draw(stroke))
            is DrawLayerData.EraseStroke -> block(BrushStackItem.Erase(stroke))
            is DrawLayerData.BrushStack -> items.forEach(block)
            else -> Unit
        }
    }

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
    }

    private data class StaticOutputSize(
        val width: Int,
        val height: Int
    )

    private companion object {
        const val MAX_EXPORT_WIDTH = 1080
        const val MAX_EXPORT_HEIGHT = 1920
    }
}
