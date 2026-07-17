package com.example.diywallpaper.data.local.files

import android.content.Context
import android.os.Build
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLUtils
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.text.TextPaint
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.example.diywallpaper.core.render.androidDrawLayerRenderBounds
import com.example.diywallpaper.core.render.androidTextLayerRenderSpec
import com.example.diywallpaper.core.render.androidTextRenderSpec
import com.example.diywallpaper.core.render.androidTextTrailStampPoints
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.BrushStackItem
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.DiyTemplateElementSnapshot
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.PhotoPlaceholderLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.DesignViewportScaleMode
import com.example.diywallpaper.domain.model.design.designViewportTransform
import com.example.diywallpaper.domain.model.design.photoRenderSize
import com.example.diywallpaper.domain.model.design.proceduralAnimatedTransform
import com.example.diywallpaper.domain.model.design.renderBounds
import com.example.diywallpaper.domain.model.design.renderSize
import com.example.diywallpaper.domain.model.design.stickerRenderSize
import com.example.diywallpaper.domain.repository.DesignVideoExporter
import com.example.diywallpaper.ui.feature.editor.editorFontResId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.roundToInt

@Singleton
class AndroidDesignVideoExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : DesignVideoExporter {
    private val patternBrushBitmapCache = mutableMapOf<String, Bitmap?>()

    override suspend fun export(project: EditorProject): AppResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val outputFile = File(context.cacheDir, "live_design/${project.id}_${project.updatedAt}.mp4")
                .apply {
                    parentFile?.mkdirs()
                    if (exists()) delete()
                }

            val renderAssets = preloadAssets(project)
            encodeProjectVideo(
                project = project,
                outputFile = outputFile,
                assets = renderAssets
            )
            outputFile.absolutePath
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.ExportError(it.message)) }
        )
    }

    private fun encodeProjectVideo(
        project: EditorProject,
        outputFile: File,
        assets: RenderAssets
    ) {
        val outputSize = resolveVideoOutputSize(project)
        val width = outputSize.width
        val height = outputSize.height
        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
        }
        val codec = MediaCodec.createEncoderByType(MIME_TYPE)
        var muxer: MediaMuxer? = null
        var inputSurface: CodecInputSurface? = null

        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = CodecInputSurface(codec.createInputSurface())
            codec.start()
            inputSurface.makeCurrent()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val drainState = DrainState()
            val totalFrames = FRAME_RATE * VIDEO_DURATION_SECONDS
            val textureRenderer = BitmapTextureRenderer().apply { initialize() }
            val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            var firstFrameRenderedLayers: Int? = null

            repeat(totalFrames) { frameIndex ->
                drainEncoder(codec, muxer, drainState, endOfStream = false)
                frameBitmap.eraseColor(Color.TRANSPARENT)
                val renderedLayers = renderFrame(
                    canvas = frameCanvas,
                    project = project,
                    assets = assets,
                    frameIndex = frameIndex,
                    outputWidth = width,
                    outputHeight = height
                )
                if (frameIndex == 0) {
                    firstFrameRenderedLayers = renderedLayers
                    check(project.layers.isEmpty() || renderedLayers > 0) {
                        "Unable to render any design layer into live wallpaper video."
                    }
                }
                textureRenderer.draw(frameBitmap)
                inputSurface.setPresentationTime(frameIndex.presentationTimeNs())
                inputSurface.swapBuffers()
            }

            codec.signalEndOfInputStream()
            drainEncoder(codec, muxer, drainState, endOfStream = true)
        } finally {
            runCatching { inputSurface?.release() }
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
        }
    }

    private fun drainEncoder(
        codec: MediaCodec,
        muxer: MediaMuxer,
        state: DrainState,
        endOfStream: Boolean
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, ENCODER_TIMEOUT_US)
            when {
                outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                }

                outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!state.muxerStarted) { "Output format changed after muxer started." }
                    state.trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    state.muxerStarted = true
                }

                outputBufferId >= 0 -> {
                    val encodedData = codec.getOutputBuffer(outputBufferId)
                    if (encodedData != null && bufferInfo.size > 0 && state.muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(state.trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private fun renderFrame(
        canvas: Canvas,
        project: EditorProject,
        assets: RenderAssets,
        frameIndex: Int,
        outputWidth: Int,
        outputHeight: Int
    ): Int {
        val designWidth = project.canvas.width.toFloat().coerceAtLeast(1f)
        val designHeight = project.canvas.height.toFloat().coerceAtLeast(1f)
        val viewport = designViewportTransform(
            designWidth = designWidth,
            designHeight = designHeight,
            targetWidth = outputWidth.toFloat(),
            targetHeight = outputHeight.toFloat(),
            scaleMode = DesignViewportScaleMode.Cover
        )
        var renderedLayers = 0
        canvas.save()
        canvas.translate(viewport.offsetX, viewport.offsetY)
        canvas.scale(viewport.scale, viewport.scale)
        try {
            drawBackground(canvas, project.background, assets, designWidth, designHeight)
            val diySource = project.source as? EditorProjectSource.Diy
            if (diySource != null) {
                if (assets.lottieComposition != null) {
                    drawDiyLottieTemplate(
                        canvas = canvas,
                        source = diySource,
                        assets = assets,
                        frameIndex = frameIndex
                    )
                    drawDiyTemplateContent(
                        canvas = canvas,
                        project = project,
                        source = diySource,
                        assets = assets,
                        frameTimeMs = frameIndex.frameTimeMs(),
                        isAnimating = false,
                        shouldDrawElement = { element -> element.shouldDrawOverLottie(assets.lottieDefaultBitmaps.keys) }
                    )
                } else {
                    drawDiyTemplateContent(
                        canvas = canvas,
                        project = project,
                        source = diySource,
                        assets = assets,
                        frameTimeMs = frameIndex.frameTimeMs(),
                        isAnimating = false
                    )
                }
            }
            project.layers
                .filterNot(EditorLayer::isHidden)
                .sortedBy(EditorLayer::zIndex)
                .forEach { layer ->
                    val rendered = when (layer) {
                        is StickerLayer -> drawSticker(canvas, layer, assets, frameIndex, scaleX = 1f, scaleY = 1f)
                        is PhotoLayer -> drawPhoto(
                            canvas = canvas,
                            layer = layer,
                            assets = assets,
                            scaleX = 1f,
                            scaleY = 1f
                        )
                        is TextLayer -> drawText(canvas, layer, scaleX = 1f, scaleY = 1f)
                        is DrawLayer -> {
                            if (layer.drawData.isBrushStackRenderable()) {
                                drawBrushStrokeStack(
                                    canvas = canvas,
                                    layers = listOf(layer),
                                    width = designWidth,
                                    height = designHeight
                                )
                                true
                            } else {
                                drawDrawLayer(canvas, layer, assets, frameIndex, scaleX = 1f, scaleY = 1f)
                            }
                        }
                    }
                    if (rendered) renderedLayers += 1
                }
        } finally {
            canvas.restore()
        }
        return renderedLayers
    }

    private fun drawDiyTemplateContent(
        canvas: Canvas,
        project: EditorProject,
        source: EditorProjectSource.Diy,
        assets: RenderAssets,
        frameTimeMs: Long,
        isAnimating: Boolean,
        shouldDrawElement: (DiyTemplateElementSnapshot) -> Boolean = { true }
    ) {
        source.templateSnapshot.elements
            .sortedBy { it.zIndex }
            .filter(shouldDrawElement)
            .forEach { element ->
                when {
                    element.isTextElement() -> {
                        drawDiyTemplateText(canvas, source.templateId, element, isAnimating, frameTimeMs)
                    }

                    element.isAssetElement() -> {
                        drawDiyTemplatePicture(canvas, source.templateId, element, isAnimating, frameTimeMs, assets)
                    }

                    element.isImageElement() -> drawDiyImageElement(canvas, source.templateId, element, isAnimating, frameTimeMs, assets)
                }
            }
    }

    private fun drawDiyLottieTemplate(
        canvas: Canvas,
        source: EditorProjectSource.Diy,
        assets: RenderAssets,
        frameIndex: Int
    ) {
        val composition = assets.lottieComposition ?: return
        val drawable = LottieDrawable().apply {
            setComposition(composition)
            setImageAssetDelegate(
                DiyLottieAssetDelegate(
                    replacementBitmaps = buildLottieReplacementBitmapMap(source, assets),
                    defaultBitmaps = buildLottieDefaultBitmapMap(assets)
                )
            )
            bounds = Rect(0, 0, source.templateSnapshot.width, source.templateSnapshot.height)
            progress = (frameIndex.toFloat() / (FRAME_RATE * VIDEO_DURATION_SECONDS).coerceAtLeast(1))
                .coerceIn(0f, 1f)
        }
        drawable.draw(canvas)
    }

    private fun buildLottieReplacementBitmapMap(
        source: EditorProjectSource.Diy,
        assets: RenderAssets
    ): Map<String, DiyLottieReplacementImage> {
        return source.templateSnapshot.elements
            .filter { it.isImageElement() }
            .map { element -> element to element.lottieAssetKeys() }
            .filter { (element, keys) -> !element.localImagePath.isNullOrBlank() && keys.isNotEmpty() }
            .flatMap { (element, keys) ->
                val localImagePath = element.localImagePath ?: return@flatMap emptyList()
                val bitmap = assets.bitmaps[localImagePath] ?: return@flatMap emptyList()
                val replacement = DiyLottieReplacementImage(
                    bitmap = renderDiyLottieReplacementBitmap(element, bitmap)
                )
                keys.map { key -> key to replacement }
            }
            .toMap()
    }

    private fun buildLottieDefaultBitmapMap(
        assets: RenderAssets
    ): Map<String, Bitmap?> {
        return assets.lottieDefaultBitmaps
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

    private fun EditorProject.diyLottieCompositionCacheKey(animationSource: String): String {
        val diySource = source as? EditorProjectSource.Diy ?: return animationSource
        val replacementFingerprint = diySource.templateSnapshot.elements
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
        return "diy-lottie:${id}:${updatedAt}:${diySource.templateId}:$animationSource:$replacementFingerprint"
    }

    private fun drawDiyTemplateText(
        canvas: Canvas,
        templateId: String,
        element: DiyTemplateElementSnapshot,
        isAnimating: Boolean,
        frameTimeMs: Long
    ) {
        val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
        val rect = centerScaledLayerRect(
            offsetX = transform.offsetX,
            offsetY = transform.offsetY,
            width = element.width,
            height = element.height,
            scale = transform.scale,
            scaleX = 1f,
            scaleY = 1f
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

    private fun drawDiyTemplatePicture(
        canvas: Canvas,
        templateId: String,
        element: DiyTemplateElementSnapshot,
        isAnimating: Boolean,
        frameTimeMs: Long,
        assets: RenderAssets
    ) {
        val bitmap = assets.bitmaps[element.assetUrl] ?: return
        val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
        val rect = centerScaledLayerRect(
            offsetX = transform.offsetX,
            offsetY = transform.offsetY,
            width = element.width,
            height = element.height,
            scale = transform.scale,
            scaleX = 1f,
            scaleY = 1f
        )
        canvas.save()
        canvas.rotate(transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapFit(canvas, bitmap, rect, alpha = 1f)
        canvas.restore()
    }

    private fun drawDiyImageElement(
        canvas: Canvas,
        templateId: String,
        element: DiyTemplateElementSnapshot,
        isAnimating: Boolean,
        frameTimeMs: Long,
        assets: RenderAssets
    ) {
        val previewSource = element.localImagePath?.takeUnless { it.isBlank() }
            ?: element.previewPathOrUrl
        val previewBitmap = assets.bitmaps[previewSource] ?: return
        val placeholderRect = RectF(
            element.x,
            element.y,
            element.x + element.width,
            element.y + element.height
        )
        val maskBitmap = assets.bitmaps[element.maskPathOrUrl]
        val checkpoint = maskBitmap?.let {
            canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)
        }
        canvas.save()
        canvas.rotate(element.rotation, placeholderRect.centerX(), placeholderRect.centerY())
        canvas.clipRect(placeholderRect)
        if (element.localImagePath.isNullOrBlank()) {
            drawBitmapCover(canvas, previewBitmap, placeholderRect)
        } else {
            val transform = element.proceduralAnimatedTransform(templateId, frameTimeMs, isAnimating)
            val baseWidth = element.contentBaseWidth ?: element.width
            val baseHeight = element.contentBaseHeight ?: element.height
            val contentRect = centerScaledLayerRect(
                offsetX = transform.offsetX,
                offsetY = transform.offsetY,
                width = baseWidth,
                height = baseHeight,
                scale = transform.scale,
                scaleX = 1f,
                scaleY = 1f
            )
            canvas.rotate(transform.rotation - element.rotation, contentRect.centerX(), contentRect.centerY())
            drawBitmapCrop(canvas, previewBitmap, element.crop, contentRect, alpha = 1f)
        }
        canvas.restore()

        if (maskBitmap != null && checkpoint != null) {
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
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

    private fun drawBackground(
        canvas: Canvas,
        background: EditorBackground,
        assets: RenderAssets,
        width: Float,
        height: Float
    ) {
        when (background) {
            is EditorBackground.SolidColor -> canvas.drawColor(parseColor(background.colorHex, Color.WHITE))
            is EditorBackground.Gradient -> {
                val colors = background.colors.ifEmpty { listOf("#FFFFFF", "#FDF0F7") }
                    .map { parseColor(it, Color.WHITE) }
                    .toIntArray()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(0f, 0f, width, height, colors, null, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, width, height, paint)
            }

            is EditorBackground.ApiImage -> {
                val bitmap = assets.bitmaps[background.imageUrl]
                if (bitmap != null) drawBitmapCover(canvas, bitmap, RectF(0f, 0f, width, height)) else canvas.drawColor(Color.WHITE)
            }

            is EditorBackground.LocalImage -> {
                val bitmap = assets.bitmaps[background.localPath]
                if (bitmap != null) drawBitmapCover(canvas, bitmap, RectF(0f, 0f, width, height)) else canvas.drawColor(Color.WHITE)
            }
        }
    }

    private fun drawSticker(
        canvas: Canvas,
        layer: StickerLayer,
        assets: RenderAssets,
        frameIndex: Int,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        val source = layer.animatedAssetPathOrUrl ?: layer.assetPathOrUrl
        val size = layer.renderSize()
        val rect = centerScaledLayerRect(
            offsetX = layer.transform.offsetX,
            offsetY = layer.transform.offsetY,
            width = size.width,
            height = size.height,
            scale = layer.transform.scale,
            scaleX = scaleX,
            scaleY = scaleY
        )
        var rendered = false
        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        val movie = assets.movies[source]
        if (movie != null) {
            val durationMs = movie.duration().takeIf { it > 0 } ?: DEFAULT_GIF_DURATION_MS
            movie.setTime(((frameIndex * 1000f / FRAME_RATE).roundToInt()) % durationMs)
            canvas.save()
            canvas.translate(rect.left, rect.top)
            canvas.scale(rect.width() / movie.width().coerceAtLeast(1), rect.height() / movie.height().coerceAtLeast(1))
            movie.draw(canvas, 0f, 0f)
            canvas.restore()
            rendered = true
        } else {
            assets.bitmaps[source]?.let {
                drawBitmapFit(canvas, it, rect, layer.transform.alpha)
                rendered = true
            }
        }
        canvas.restore()
        return rendered
    }

    private fun drawPhoto(
        canvas: Canvas,
        layer: PhotoLayer,
        assets: RenderAssets,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        val bitmap = assets.bitmaps[layer.localPath] ?: return false
        val size = photoRenderSize(layer.crop?.ratio)
        val rect = centerScaledLayerRect(
            offsetX = layer.transform.offsetX,
            offsetY = layer.transform.offsetY,
            width = size.width,
            height = size.height,
            scale = layer.transform.scale,
            scaleX = scaleX,
            scaleY = scaleY
        )
        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapCrop(canvas, bitmap, layer.crop, rect, layer.transform.alpha)
        canvas.restore()
        return true
    }

    private fun drawText(
        canvas: Canvas,
        layer: TextLayer,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        if (layer.text.isBlank()) return false
        val spec = androidTextLayerRenderSpec(
            context = context,
            layer = layer,
            fontResId = editorFontResId(layer.style.fontFamilyId)
        )
        val bounds = spec.bounds
        val rect = RectF(
            (layer.transform.offsetX + bounds.minX) * scaleX,
            (layer.transform.offsetY + bounds.minY) * scaleY,
            (layer.transform.offsetX + bounds.maxX) * scaleX,
            (layer.transform.offsetY + bounds.maxY) * scaleY
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextColor(layer)
            textSize = layer.style.fontSizeSp *
                context.resources.displayMetrics.scaledDensity *
                ((scaleX + scaleY) / 2f)
            alpha = (layer.transform.alpha * 255).toInt().coerceIn(0, 255)
            isFakeBoldText = true
            typeface = ResourcesCompat.getFont(context, editorFontResId(layer.style.fontFamilyId))
        }
        canvas.save()
        canvas.translate(rect.centerX(), rect.centerY())
        canvas.rotate(layer.transform.rotation)
        canvas.scale(layer.transform.scale, layer.transform.scale)
        canvas.translate(-rect.centerX(), -rect.centerY())
        canvas.drawText(
            layer.text,
            (layer.transform.offsetX + spec.drawX) * scaleX,
            (layer.transform.offsetY + spec.drawBaselineY) * scaleY,
            paint
        )
        canvas.restore()
        return true
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
                        scaleX = 1f,
                        scaleY = 1f,
                        color = parseColor(item.stroke.colorHex, Color.rgb(29, 23, 38)),
                        clear = false
                    )

                    is BrushStackItem.Erase -> drawStroke(
                        canvas = canvas,
                        stroke = item.stroke,
                        scaleX = 1f,
                        scaleY = 1f,
                        color = Color.TRANSPARENT,
                        clear = true
                    )
                }
            }
            canvas.restore()
            canvas.restoreToCount(checkpoint)
        }
    }

    private fun drawDrawLayer(
        canvas: Canvas,
        layer: DrawLayer,
        assets: RenderAssets,
        frameIndex: Int,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        val bounds = androidDrawLayerRenderBounds(
            context = context,
            drawData = layer.drawData,
            fontResIdFor = ::editorFontResId
        ) ?: layer.drawData.renderBounds()
        canvas.save()
        if (bounds != null) {
            val rect = RectF(
                (layer.transform.offsetX + bounds.minX) * scaleX,
                (layer.transform.offsetY + bounds.minY) * scaleY,
                (layer.transform.offsetX + bounds.maxX) * scaleX,
                (layer.transform.offsetY + bounds.maxY) * scaleY
            )
            canvas.translate(rect.centerX(), rect.centerY())
            canvas.rotate(layer.transform.rotation)
            canvas.scale(layer.transform.scale, layer.transform.scale)
            canvas.translate(-rect.width() / 2f, -rect.height() / 2f)
            canvas.translate(-bounds.minX * scaleX, -bounds.minY * scaleY)
        } else {
            canvas.translate(layer.transform.offsetX * scaleX, layer.transform.offsetY * scaleY)
            canvas.rotate(layer.transform.rotation)
            canvas.scale(layer.transform.scale, layer.transform.scale)
        }
        val rendered = when (val data = layer.drawData) {
            is DrawLayerData.FreeStroke -> {
                drawStroke(canvas, data.stroke, scaleX, scaleY, parseColor(data.stroke.colorHex, Color.rgb(29, 23, 38)), clear = false)
                data.stroke.points.size >= 2
            }

            is DrawLayerData.EraseStroke -> {
                drawStroke(canvas, data.stroke, scaleX, scaleY, Color.WHITE, clear = false)
                data.stroke.points.size >= 2
            }

            is DrawLayerData.BrushStack -> false
            is DrawLayerData.TextTrail -> drawTextTrail(canvas, data, scaleX, scaleY)
            is DrawLayerData.StickerTrail -> drawStickerTrail(canvas, data, assets, frameIndex, scaleX, scaleY)
        }
        canvas.restore()
        return rendered
    }

    private fun drawStroke(
        canvas: Canvas,
        stroke: BrushStroke,
        scaleX: Float,
        scaleY: Float,
        color: Int,
        clear: Boolean
    ) {
        if (stroke.points.size < 2) return
        val patternStyle = stroke.brushStyle as? BrushStyleSpec.Pattern
        val patternBitmap = patternStyle?.let { style -> loadPatternBrushBitmap(style.drawableName) }
        if (!clear && patternStyle != null && patternBitmap != null && !patternBitmap.isRecycled) {
            drawPatternStroke(canvas, stroke, patternBitmap, patternStyle, scaleX, scaleY)
            return
        }
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = point.x * scaleX
            val y = point.y * scaleY
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val width = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
        if (!clear) {
            when (val style = stroke.brushStyle) {
                is BrushStyleSpec.Outline -> {
                    drawNativeStroke(
                        canvas = canvas,
                        path = path,
                        color = parseColor(style.strokeColorHex, Color.BLACK),
                        strokeWidth = width * 1.55f
                    )
                    drawNativeStroke(
                        canvas = canvas,
                        path = path,
                        color = parseColor(style.fillColorHex, Color.BLACK),
                        strokeWidth = width
                    )
                    return
                }

                is BrushStyleSpec.Glow -> {
                    drawGlowStroke(
                        canvas = canvas,
                        path = path,
                        strokeWidth = width,
                        color = parseColor(style.colorHex, Color.WHITE),
                        glowColor = parseColor(style.glowColorHex, Color.WHITE)
                    )
                    return
                }

                else -> Unit
            }
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = when (val style = stroke.brushStyle) {
                is BrushStyleSpec.Dashed -> parseColor(style.colorHex, color)
                is BrushStyleSpec.Solid -> parseColor(style.colorHex, color)
                else -> color
            }
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = width
            if (clear) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else if (stroke.brushStyle is BrushStyleSpec.Dashed) {
                pathEffect = DashPathEffect(
                    floatArrayOf(width * 1.7f, width * 1.25f),
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
        patternStyle: BrushStyleSpec.Pattern,
        scaleX: Float,
        scaleY: Float
    ) {
        val path = Path().apply {
            moveTo(stroke.points.first().x * scaleX, stroke.points.first().y * scaleY)
            stroke.points.drop(1).forEach { point ->
                lineTo(point.x * scaleX, point.y * scaleY)
            }
        }
        val measure = PathMeasure(path, false)
        val length = measure.length
        if (length <= 0f) return
        val averageScale = ((scaleX + scaleY) / 2f).coerceAtLeast(0.0001f)
        val iconScale = (stroke.strokeWidth / bitmap.width.coerceAtLeast(1)) *
            patternStyle.scale.coerceAtLeast(0.1f) *
            averageScale
        val step = (bitmap.width * iconScale * patternStyle.spacingFactor.coerceAtLeast(0.35f))
            .coerceAtLeast(4f)
        val position = FloatArray(2)
        val tangent = FloatArray(2)
        val matrix = android.graphics.Matrix()
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

    private fun drawTextTrail(
        canvas: Canvas,
        data: DrawLayerData.TextTrail,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        if (data.points.isEmpty() || data.text.isBlank()) return false
        val textSpec = androidTextRenderSpec(
            context = context,
            text = data.text,
            style = data.textStyle,
            fontResId = editorFontResId(data.textStyle.fontFamilyId)
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextBrushColor(data.textStyle.textBrush, data.textStyle.textColorHex)
            textSize = data.textStyle.fontSizeSp * context.resources.displayMetrics.scaledDensity * ((scaleX + scaleY) / 2f)
            isFakeBoldText = true
            typeface = ResourcesCompat.getFont(context, editorFontResId(data.textStyle.fontFamilyId))
        }
        androidTextTrailStampPoints(data).forEach { point ->
            canvas.drawText(
                data.text,
                (point.x + textSpec.drawX) * scaleX,
                (point.y + textSpec.drawBaselineY) * scaleY,
                paint
            )
        }
        return true
    }

    private fun drawStickerTrail(
        canvas: Canvas,
        data: DrawLayerData.StickerTrail,
        assets: RenderAssets,
        frameIndex: Int,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        if (data.points.isEmpty()) return false
        val movie = assets.movies[data.stickerAssetPathOrUrl]
        val bitmap = assets.bitmaps[data.stickerAssetPathOrUrl]
        if (movie == null && bitmap == null) return false
        data.points.forEachIndexed { index, point ->
            if (index % data.spacing.coerceAtLeast(1f).toInt().coerceAtLeast(1) == 0) {
                val size = data.stampSize * ((scaleX + scaleY) / 2f)
                val rect = RectF(
                    point.x * scaleX - size / 2f,
                    point.y * scaleY - size / 2f,
                    point.x * scaleX + size / 2f,
                    point.y * scaleY + size / 2f
                )
                canvas.save()
                if (data.rotationMode == StickerTrailRotationMode.FOLLOW_PATH && index > 0) {
                    val previous = data.points[index - 1]
                    val angle = kotlin.math.atan2(point.y - previous.y, point.x - previous.x) * 180f / Math.PI.toFloat()
                    canvas.rotate(angle, rect.centerX(), rect.centerY())
                }
                if (movie != null) {
                    val durationMs = movie.duration().takeIf { it > 0 } ?: DEFAULT_GIF_DURATION_MS
                    movie.setTime(((frameIndex * 1000f / FRAME_RATE).roundToInt()) % durationMs)
                    canvas.translate(rect.left, rect.top)
                    canvas.scale(rect.width() / movie.width().coerceAtLeast(1), rect.height() / movie.height().coerceAtLeast(1))
                    movie.draw(canvas, 0f, 0f)
                } else if (bitmap != null) {
                    drawBitmapFit(canvas, bitmap, rect, 1f)
                }
                canvas.restore()
            }
        }
        return true
    }

    private fun preloadAssets(project: EditorProject): RenderAssets {
        val bitmaps = mutableMapOf<String, Bitmap?>()
        val movies = mutableMapOf<String, Movie?>()
        fun load(source: String, preferMovie: Boolean = false) {
            if (source.isBlank() || bitmaps.containsKey(source) || movies.containsKey(source)) return
            val bytes = readAssetBytes(source)
            if (preferMovie && bytes != null) {
                @Suppress("DEPRECATION")
                val movie = Movie.decodeByteArray(bytes, 0, bytes.size)
                if (movie != null) {
                    movies[source] = movie
                    return
                }
            }
            bitmaps[source] = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                ?: BitmapFactory.decodeFile(source)
        }

        when (val background = project.background) {
            is EditorBackground.ApiImage -> load(background.imageUrl)
            is EditorBackground.LocalImage -> load(background.localPath)
            else -> Unit
        }
        (project.source as? EditorProjectSource.Diy)
            ?.templateSnapshot
            ?.elements
            ?.forEach { element ->
                if (element.isAssetElement()) {
                    element.assetUrl?.let { load(it) }
                }
                if (element.isImageElement()) {
                    element.localImagePath?.let { load(it) }
                    element.previewPathOrUrl?.let { load(it) }
                    element.maskPathOrUrl?.let { load(it) }
                }
            }
        project.layers.forEach { layer ->
            when (layer) {
                is StickerLayer -> load(layer.animatedAssetPathOrUrl ?: layer.assetPathOrUrl, preferMovie = layer.isAnimated)
                is PhotoLayer -> load(layer.localPath)
                is DrawLayer -> {
                    val drawData = layer.drawData
                    if (drawData is DrawLayerData.StickerTrail) {
                        load(drawData.stickerAssetPathOrUrl, preferMovie = true)
                    }
                }
                else -> Unit
            }
        }
        project.placeholders.forEach { placeholder ->
            placeholder.maskPathOrUrl?.let { load(it) }
            placeholder.previewPathOrUrl?.let { load(it) }
        }
        val lottieAnimationSource = (project.source as? EditorProjectSource.Diy)
            ?.takeIf { it.isLive }
            ?.let(::resolveDiyLottieAnimationSource)
        val lottieDefaultBitmaps = lottieAnimationSource
            ?.let(::loadLottieDefaultImageAssets)
            .orEmpty()
        val lottieComposition = lottieAnimationSource
            ?.let { source -> loadLottieComposition(source, project.diyLottieCompositionCacheKey(source)) }
        return RenderAssets(
            bitmaps = bitmaps,
            movies = movies,
            lottieDefaultBitmaps = lottieDefaultBitmaps,
            lottieComposition = lottieComposition
        )
    }

    private fun loadLottieComposition(source: String, cacheKey: String): LottieComposition? {
        val json = readAssetBytes(source)?.toString(Charsets.UTF_8)
            ?: File(source).takeIf(File::exists)?.readText()
            ?: return null
        return LottieCompositionFactory.fromJsonStringSync(json, cacheKey).value
    }

    private fun resolveDiyLottieAnimationSource(source: EditorProjectSource.Diy): String? {
        return source.diyAnimationPathOrUrl
            ?.takeIf { it.isNotBlank() && (it.startsWith("http") || File(it).exists()) }
            ?: findCachedDiyAnimationJson(source.templateId)?.absolutePath
            ?: source.diyAnimationUrl?.takeIf { it.isNotBlank() }
    }

    private fun findCachedDiyAnimationJson(templateId: String): File? {
        val assetDirectory = File(context.filesDir, "diy_templates/$templateId/assets")
            .takeIf { it.exists() && it.isDirectory }
            ?: return null
        val preferred = File(assetDirectory, "animation/data.json")
            .takeIf { it.exists() && it.isFile }
        if (preferred != null) return preferred
        return File(assetDirectory, "animation")
            .takeIf { it.exists() && it.isDirectory }
            ?.walkTopDown()
            ?.firstOrNull { it.isFile && it.extension.equals("json", ignoreCase = true) }
    }

    private fun loadLottieDefaultImageAssets(animationSource: String): Map<String, Bitmap?> {
        val animationFile = File(animationSource).takeIf(File::exists) ?: return emptyMap()
        val animationDirectory = animationFile.parentFile ?: return emptyMap()
        val candidates = sequenceOf(
            File(animationDirectory, "images"),
            animationDirectory
        ).filter { it.exists() && it.isDirectory }

        return candidates
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

    private fun readAssetBytes(source: String): ByteArray? {
        return runCatching {
            if (source.startsWith("http://") || source.startsWith("https://")) {
                val request = Request.Builder().url(source).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@runCatching null
                    response.body?.bytes()
                }
            } else {
                File(source).takeIf(File::exists)?.readBytes()
            }
        }.getOrNull()
    }

    private fun centerScaledLayerRect(
        offsetX: Float,
        offsetY: Float,
        width: Float,
        height: Float,
        scale: Float,
        scaleX: Float,
        scaleY: Float
    ): RectF {
        val left = offsetX * scaleX
        val top = offsetY * scaleY
        val baseWidth = width * scaleX
        val baseHeight = height * scaleY
        val centerX = left + baseWidth / 2f
        val centerY = top + baseHeight / 2f
        val scaledWidth = baseWidth * scale
        val scaledHeight = baseHeight * scale
        return RectF(
            centerX - scaledWidth / 2f,
            centerY - scaledHeight / 2f,
            centerX + scaledWidth / 2f,
            centerY + scaledHeight / 2f
        )
    }

    private fun drawBitmapCover(canvas: Canvas, bitmap: Bitmap, rect: RectF, alpha: Float = 1f) {
        val scale = maxOf(rect.width() / bitmap.width, rect.height() / bitmap.height)
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val target = RectF(
            rect.left + (rect.width() - scaledWidth) / 2f,
            rect.top + (rect.height() - scaledHeight) / 2f,
            rect.left + (rect.width() + scaledWidth) / 2f,
            rect.top + (rect.height() + scaledHeight) / 2f
        )
        canvas.save()
        canvas.clipRect(rect)
        drawBitmapFit(canvas, bitmap, target, alpha)
        canvas.restore()
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
        crop: com.example.diywallpaper.domain.model.design.CropSpec?,
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

    private fun resolveTextColor(layer: TextLayer): Int {
        return resolveTextBrushColor(layer.style.textBrush, layer.style.textColorHex)
    }

    private fun resolveTextBrushColor(brush: TextBrushStyle?, fallbackHex: String?): Int {
        return when (brush) {
            is TextBrushStyle.Solid -> parseColor(brush.colorHex, Color.rgb(29, 23, 38))
            is TextBrushStyle.Gradient -> parseColor(brush.colors.firstOrNull(), Color.rgb(29, 23, 38))
            null -> parseColor(fallbackHex, Color.rgb(29, 23, 38))
        }
    }

    private fun parseColor(hex: String?, fallback: Int): Int {
        return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
    }

    private fun Int.withAlpha(alpha: Float): Int {
        return (this and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)
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

    private fun resolveVideoOutputSize(project: EditorProject): VideoOutputSize {
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
        var height = minOf(displayHeight, MAX_VIDEO_HEIGHT).coerceAtLeast(2)
        var width = (height * aspectRatio).roundToInt().coerceAtLeast(2)
        if (width > MAX_VIDEO_WIDTH) {
            width = MAX_VIDEO_WIDTH
            height = (width / aspectRatio).roundToInt().coerceAtLeast(2)
        }
        return VideoOutputSize(
            width = width.even().coerceAtLeast(2),
            height = height.even().coerceAtLeast(2)
        )
    }

    private fun Int.presentationTimeNs(): Long = this * 1_000_000_000L / FRAME_RATE

    private fun Int.frameTimeMs(): Long = this * 1_000L / FRAME_RATE

    private data class VideoOutputSize(
        val width: Int,
        val height: Int
    )

    private data class RenderAssets(
        val bitmaps: Map<String, Bitmap?>,
        val movies: Map<String, Movie?>,
        val lottieDefaultBitmaps: Map<String, Bitmap?> = emptyMap(),
        val lottieComposition: LottieComposition? = null
    )

    private data class DrainState(
        var trackIndex: Int = -1,
        var muxerStarted: Boolean = false
    )

    private class CodecInputSurface(
        private val surface: Surface
    ) {
        private val eglDisplay: EGLDisplay
        private val eglContext: EGLContext
        private val eglSurface: EGLSurface

        init {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display." }
            val version = IntArray(2)
            check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "Unable to initialize EGL." }

            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)) {
                "Unable to choose EGL config."
            }
            val config = configs[0] ?: error("Missing EGL config.")
            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                config,
                EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0
            )
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context." }

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, config, surface, surfaceAttribs, 0)
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window surface." }
        }

        fun makeCurrent() {
            check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                "Unable to make EGL context current."
            }
        }

        fun setPresentationTime(nsecs: Long) {
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs)
        }

        fun swapBuffers() {
            check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "Unable to swap EGL buffers." }
        }

        fun release() {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
            surface.release()
        }
    }

    private class BitmapTextureRenderer {
        private var program = 0
        private var textureId = 0
        private var positionHandle = 0
        private var texCoordHandle = 0
        private var textureHandle = 0
        private val vertices = java.nio.ByteBuffer
            .allocateDirect(FULL_RECTANGLE_COORDS.size * java.lang.Float.BYTES)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(FULL_RECTANGLE_COORDS)
                position(0)
            }
        private val texCoords = java.nio.ByteBuffer
            .allocateDirect(FULL_RECTANGLE_TEX_COORDS.size * java.lang.Float.BYTES)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(FULL_RECTANGLE_TEX_COORDS)
                position(0)
            }

        fun initialize() {
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }

        fun draw(bitmap: Bitmap) {
            GLES20.glViewport(0, 0, bitmap.width, bitmap.height)
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES20.glUniform1i(textureHandle, 0)

            vertices.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertices)

            texCoords.position(0)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoords)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val localProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(localProgram, vertexShader)
            GLES20.glAttachShader(localProgram, fragmentShader)
            GLES20.glLinkProgram(localProgram)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(localProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
            check(linkStatus[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(localProgram) }
            return localProgram
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            check(compiled[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(shader) }
            return shader
        }

        private companion object {
            val FULL_RECTANGLE_COORDS = floatArrayOf(
                -1f, -1f,
                1f, -1f,
                -1f, 1f,
                1f, 1f
            )
            val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(
                0f, 1f,
                1f, 1f,
                0f, 0f,
                1f, 0f
            )
            const val VERTEX_SHADER = """
                attribute vec4 aPosition;
                attribute vec2 aTexCoord;
                varying vec2 vTexCoord;
                void main() {
                    gl_Position = aPosition;
                    vTexCoord = aTexCoord;
                }
            """
            const val FRAGMENT_SHADER = """
                precision mediump float;
                uniform sampler2D uTexture;
                varying vec2 vTexCoord;
                void main() {
                    gl_FragColor = texture2D(uTexture, vTexCoord);
                }
            """
        }
    }

    private companion object {
        const val MIME_TYPE = "video/avc"
        const val FRAME_RATE = 30
        const val VIDEO_DURATION_SECONDS = 4
        const val BIT_RATE = 6_000_000
        const val I_FRAME_INTERVAL_SECONDS = 1
        const val ENCODER_TIMEOUT_US = 10_000L
        const val MAX_VIDEO_WIDTH = 1080
        const val MAX_VIDEO_HEIGHT = 1920
        const val DEFAULT_GIF_DURATION_MS = 1000
        const val EGL_RECORDABLE_ANDROID = 0x3142
        val LOTTIE_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")
    }
}
