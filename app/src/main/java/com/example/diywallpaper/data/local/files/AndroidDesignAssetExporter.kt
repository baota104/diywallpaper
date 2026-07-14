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
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.diywallpaper.R
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.EditorTextAlign
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.TextShadowSpec
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.repository.DesignAssetExporter
import com.example.diywallpaper.ui.feature.editor.editorFontResId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.max
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
                val exportedImagePath = writeBitmapAsset(
                    project = project,
                    width = project.canvas.width.coerceAtLeast(1),
                    height = project.canvas.height.coerceAtLeast(1),
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
        val scaleX = width / project.canvas.width.coerceAtLeast(1)
        val scaleY = height / project.canvas.height.coerceAtLeast(1)

        drawBackground(
            canvas = canvas,
            background = project.background,
            canvasWidth = width,
            canvasHeight = height,
            assetCache = assetCache
        )

        val diyElementMap = (project.source as? EditorProjectSource.Diy)
            ?.templateSnapshot
            ?.elements
            ?.associateBy(DiyTemplateElementSnapshot::id)
            .orEmpty()
        val placeholderMap = project.placeholders.associateBy(PhotoPlaceholderLayer::id)

        project.placeholders.sortedBy { it.zIndex }.forEach { placeholder ->
            if (project.layers.none { it is PhotoLayer && it.placeholderId == placeholder.id }) {
                drawPlaceholder(canvas, placeholder, scaleX, scaleY)
            }
        }

        project.layers
            .filterNot { it.isHidden }
            .sortedBy { it.zIndex }
            .forEach { layer ->
                when (layer) {
                    is TextLayer -> drawTextLayer(canvas, layer, scaleX, scaleY)
                    is StickerLayer -> drawStickerLayer(canvas, layer, scaleX, scaleY, diyElementMap[layer.id], assetCache)
                    is PhotoLayer -> drawPhotoLayer(canvas, layer, scaleX, scaleY, placeholderMap[layer.placeholderId], assetCache)
                    is DrawLayer -> drawDrawLayer(canvas, layer, scaleX, scaleY, assetCache)
                }
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

    private fun drawPlaceholder(
        canvas: Canvas,
        placeholder: PhotoPlaceholderLayer,
        scaleX: Float,
        scaleY: Float
    ) {
        val rect = RectF(
            placeholder.x * scaleX,
            placeholder.y * scaleY,
            (placeholder.x + placeholder.width) * scaleX,
            (placeholder.y + placeholder.height) * scaleY
        )
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 214, 241, 255)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 139, 92, 246)
            style = Paint.Style.STROKE
            strokeWidth = max(2f, 4f * ((scaleX + scaleY) / 2f))
        }
        canvas.save()
        canvas.rotate(
            placeholder.rotation,
            rect.centerX(),
            rect.centerY()
        )
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)
        canvas.drawRoundRect(rect, 24f, 24f, strokePaint)
        canvas.restore()
    }

    private suspend fun drawStickerLayer(
        canvas: Canvas,
        layer: StickerLayer,
        scaleX: Float,
        scaleY: Float,
        snapshot: DiyTemplateElementSnapshot?,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val bitmap = loadBitmap(layer.assetPathOrUrl, assetCache, 1024, 1024) ?: return
        val targetWidth = ((snapshot?.width ?: DEFAULT_STICKER_SIZE) * scaleX * layer.transform.scale).coerceAtLeast(1f)
        val targetHeight = ((snapshot?.height ?: DEFAULT_STICKER_SIZE) * scaleY * layer.transform.scale).coerceAtLeast(1f)
        drawTransformedBitmap(
            canvas = canvas,
            bitmap = bitmap,
            transform = layer.transform,
            scaleX = scaleX,
            scaleY = scaleY,
            width = targetWidth,
            height = targetHeight
        )
    }

    private suspend fun drawPhotoLayer(
        canvas: Canvas,
        layer: PhotoLayer,
        scaleX: Float,
        scaleY: Float,
        placeholder: PhotoPlaceholderLayer?,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        val bitmap = loadBitmap(layer.localPath, assetCache, 1400, 1400) ?: return
        val targetWidth = ((placeholder?.width ?: bitmap.width.toFloat()) * scaleX * layer.transform.scale).coerceAtLeast(1f)
        val targetHeight = ((placeholder?.height ?: bitmap.height.toFloat()) * scaleY * layer.transform.scale).coerceAtLeast(1f)
        drawTransformedBitmap(
            canvas = canvas,
            bitmap = bitmap,
            transform = layer.transform,
            scaleX = scaleX,
            scaleY = scaleY,
            width = targetWidth,
            height = targetHeight,
            crop = layer.crop
        )
    }

    private fun drawTextLayer(
        canvas: Canvas,
        layer: TextLayer,
        scaleX: Float,
        scaleY: Float
    ) {
        val textPaint = buildTextPaint(layer.style, scaleX, scaleY, layer.transform.alpha)
        val lines = layer.text.ifBlank { " " }.split('\n')
        val desiredWidth = lines.maxOf { line -> textPaint.measureText(line).toInt().coerceAtLeast(1) }
        val textWidth = max(desiredWidth, 1)

        val layout = StaticLayout.Builder
            .obtain(layer.text.ifBlank { " " }, 0, layer.text.ifBlank { " " }.length, textPaint, textWidth)
            .setAlignment(layer.style.textAlign.toLayoutAlignment())
            .setIncludePad(false)
            .setLineSpacing(0f, resolveLineSpacingMultiplier(layer.style))
            .build()

        val pivotX = when (layer.style.textAlign) {
            EditorTextAlign.CENTER -> textWidth / 2f
            EditorTextAlign.END -> textWidth.toFloat()
            EditorTextAlign.START -> 0f
        }

        canvas.save()
        canvas.translate(layer.transform.offsetX * scaleX, layer.transform.offsetY * scaleY)
        canvas.rotate(layer.transform.rotation)
        canvas.scale(layer.transform.scale, layer.transform.scale)
        if (layer.style.textBrush is TextBrushStyle.Gradient) {
            textPaint.shader = LinearGradient(
                0f,
                0f,
                textWidth.toFloat(),
                layout.height.toFloat().coerceAtLeast(1f),
                layer.style.textBrush.colors.map { parseColor(it, Color.BLACK) }.toIntArray(),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.translate(-pivotX, 0f)
        layout.draw(canvas)
        canvas.restore()
    }

    private suspend fun drawDrawLayer(
        canvas: Canvas,
        layer: DrawLayer,
        scaleX: Float,
        scaleY: Float,
        assetCache: MutableMap<String, Bitmap?>
    ) {
        when (val drawData = layer.drawData) {
            is DrawLayerData.FreeStroke -> drawStroke(canvas, drawData.stroke, scaleX, scaleY, false)
            is DrawLayerData.EraseStroke -> drawStroke(canvas, drawData.stroke, scaleX, scaleY, true)
            is DrawLayerData.StickerTrail -> {
                val bitmap = loadBitmap(drawData.stickerAssetPathOrUrl, assetCache, 512, 512) ?: return
                drawData.points.forEachIndexed { index, point ->
                    if (index % 2 == 0) {
                        val stampSize = (drawData.stampSize * ((scaleX + scaleY) / 2f)).coerceAtLeast(1f)
                        val rect = RectF(
                            point.x * scaleX,
                            point.y * scaleY,
                            point.x * scaleX + stampSize,
                            point.y * scaleY + stampSize
                        )
                        drawBitmapCover(canvas, bitmap, rect, null)
                    }
                }
            }

            is DrawLayerData.TextTrail -> {
                val textPaint = buildTextPaint(drawData.textStyle, scaleX, scaleY, 1f)
                if (drawData.textStyle.textBrush is TextBrushStyle.Gradient) {
                    textPaint.shader = LinearGradient(
                        0f,
                        0f,
                        textPaint.measureText(drawData.text).coerceAtLeast(1f),
                        textPaint.textSize,
                        drawData.textStyle.textBrush.colors.map { parseColor(it, Color.BLACK) }.toIntArray(),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                drawData.points.forEachIndexed { index, point ->
                    if (index % 2 == 0) {
                        canvas.drawText(drawData.text, point.x * scaleX, point.y * scaleY, textPaint)
                    }
                }
            }
        }
    }

    private fun drawStroke(
        canvas: Canvas,
        stroke: BrushStroke,
        scaleX: Float,
        scaleY: Float,
        erase: Boolean
    ) {
        if (stroke.points.size < 2) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (erase) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = resolveStrokeColor(stroke)
                if (stroke.brushStyle is BrushStyleSpec.Gradient) {
                    shader = LinearGradient(
                        stroke.points.first().x * scaleX,
                        stroke.points.first().y * scaleY,
                        stroke.points.last().x * scaleX,
                        stroke.points.last().y * scaleY,
                        stroke.brushStyle.colors.map { parseColor(it, Color.BLACK) }.toIntArray(),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
            }
        }
        val path = Path().apply {
            moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x * scaleX, point.y * scaleY)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun buildTextPaint(
        style: TextStyleSpec,
        scaleX: Float,
        scaleY: Float,
        alpha: Float
    ): TextPaint {
        val averageScale = ((scaleX + scaleY) / 2f).coerceAtLeast(0.01f)
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextColor(style)
            textSize = style.fontSizeSp * averageScale
            letterSpacing = style.letterSpacing / style.fontSizeSp.coerceAtLeast(1f)
            typeface = ResourcesCompat.getFont(
                context,
                editorFontResId(style.fontFamilyId)
            )
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            applyShadow(style.shadow, averageScale)
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

    private suspend fun drawTransformedBitmap(
        canvas: Canvas,
        bitmap: Bitmap,
        transform: LayerTransform,
        scaleX: Float,
        scaleY: Float,
        width: Float,
        height: Float,
        crop: CropSpec? = null
    ) {
        val left = transform.offsetX * scaleX
        val top = transform.offsetY * scaleY
        val rect = RectF(left, top, left + width, top + height)
        canvas.save()
        canvas.rotate(transform.rotation, rect.centerX(), rect.centerY())
        val alphaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (transform.alpha * 255).toInt().coerceIn(0, 255)
        }
        drawBitmapCover(canvas, bitmap, rect, crop, alphaPaint)
        canvas.restore()
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

    @Suppress("DEPRECATION")
    private fun webpFormat(): Bitmap.CompressFormat {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
    }

    private companion object {
        const val DEFAULT_STICKER_SIZE = 220f
    }
}
