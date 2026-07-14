package com.example.diywallpaper.data.local.files

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorTextAlign
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
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
import com.example.diywallpaper.domain.model.design.stickerRenderSize
import com.example.diywallpaper.domain.model.design.textRenderSize
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
    @ApplicationContext private val context: Context,
    private val designFileStore: DesignFileStore,
    private val okHttpClient: OkHttpClient
) : DesignAssetExporter {

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
            project.layers
                .filterNot(EditorLayer::isHidden)
                .sortedBy(EditorLayer::zIndex)
                .forEach { layer ->
                    when (layer) {
                        is TextLayer -> drawTextLayer(canvas, layer)
                        is StickerLayer -> drawStickerLayer(canvas, layer, assetCache)
                        is PhotoLayer -> drawPhotoLayer(canvas, layer, assetCache)
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
        val size = stickerRenderSize()
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
        val size = textRenderSize()
        val rect = centerScaledLayerRect(
            offsetX = layer.transform.offsetX,
            offsetY = layer.transform.offsetY,
            width = size.width,
            height = size.height,
            scale = layer.transform.scale
        )
        val textPaint = buildTextPaint(layer.style, layer.transform.alpha, layer.transform.scale)

        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        canvas.drawText(layer.text, rect.left, rect.top + textPaint.textSize, textPaint)
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
            val bounds = layer.drawData.renderBounds() ?: return@forEach
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
        val bounds = layer.drawData.renderBounds()
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
                    val textPaint = buildTextPaint(drawData.textStyle, alpha = 1f, transformScale = 1f)
                    drawData.points.forEachIndexed { index, point ->
                        if (index % 2 == 0) {
                            canvas.drawText(drawData.text, point.x, point.y, textPaint)
                        }
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

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
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
            }
        }
        val path = Path().apply {
            moveTo(stroke.points.first().x, stroke.points.first().y)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x, point.y)
            }
        }
        canvas.drawPath(path, paint)
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
            null -> parseColor(stroke.colorHex, Color.BLACK)
        }
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
