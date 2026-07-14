package com.example.diywallpaper.data.local.files

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
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
import android.view.Surface
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorLayer
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.repository.DesignVideoExporter
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
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : DesignVideoExporter {
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
        val width = project.canvas.width.coerceAtLeast(2).coerceAtMost(MAX_VIDEO_WIDTH).even()
        val height = project.canvas.height.coerceAtLeast(2).coerceAtMost(MAX_VIDEO_HEIGHT).even()
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
        val scaleX = outputWidth.toFloat() / project.canvas.width.toFloat().coerceAtLeast(1f)
        val scaleY = outputHeight.toFloat() / project.canvas.height.toFloat().coerceAtLeast(1f)
        drawBackground(canvas, project.background, assets, outputWidth.toFloat(), outputHeight.toFloat())
        var renderedLayers = 0
        project.layers
            .filterNot(EditorLayer::isHidden)
            .sortedBy(EditorLayer::zIndex)
            .forEach { layer ->
                val rendered = when (layer) {
                    is StickerLayer -> drawSticker(canvas, layer, assets, frameIndex, scaleX, scaleY)
                    is PhotoLayer -> drawPhoto(canvas, layer, assets, scaleX, scaleY)
                    is TextLayer -> drawText(canvas, layer, scaleX, scaleY)
                    is DrawLayer -> drawDrawLayer(canvas, layer, assets, frameIndex, scaleX, scaleY)
                }
                if (rendered) renderedLayers += 1
            }
        return renderedLayers
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
        val rect = layerRect(layer.transform.offsetX, layer.transform.offsetY, DEFAULT_LAYER_SIZE, DEFAULT_LAYER_SIZE, layer.transform.scale, scaleX, scaleY)
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
        val rect = layerRect(layer.transform.offsetX, layer.transform.offsetY, DEFAULT_LAYER_SIZE, DEFAULT_LAYER_SIZE, layer.transform.scale, scaleX, scaleY)
        canvas.save()
        canvas.rotate(layer.transform.rotation, rect.centerX(), rect.centerY())
        drawBitmapCover(canvas, bitmap, rect, layer.transform.alpha)
        canvas.restore()
        return true
    }

    private fun drawText(
        canvas: Canvas,
        layer: TextLayer,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextColor(layer)
            textSize = layer.style.fontSizeSp * context.resources.displayMetrics.scaledDensity * ((scaleX + scaleY) / 2f)
            alpha = (layer.transform.alpha * 255).toInt().coerceIn(0, 255)
            isFakeBoldText = true
        }
        val x = layer.transform.offsetX * scaleX
        val y = layer.transform.offsetY * scaleY + paint.textSize
        canvas.save()
        canvas.rotate(layer.transform.rotation, x, y)
        canvas.scale(layer.transform.scale, layer.transform.scale, x, y)
        canvas.drawText(layer.text.ifBlank { " " }, x, y, paint)
        canvas.restore()
        return layer.text.isNotBlank()
    }

    private fun drawDrawLayer(
        canvas: Canvas,
        layer: DrawLayer,
        assets: RenderAssets,
        frameIndex: Int,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        canvas.save()
        canvas.translate(layer.transform.offsetX * scaleX, layer.transform.offsetY * scaleY)
        canvas.scale(layer.transform.scale, layer.transform.scale)
        canvas.rotate(layer.transform.rotation)
        val rendered = when (val data = layer.drawData) {
            is DrawLayerData.FreeStroke -> {
                drawStroke(canvas, data.stroke, scaleX, scaleY, parseColor(data.stroke.colorHex, Color.rgb(29, 23, 38)))
                data.stroke.points.size >= 2
            }

            is DrawLayerData.EraseStroke -> {
                drawStroke(canvas, data.stroke, scaleX, scaleY, Color.WHITE)
                data.stroke.points.size >= 2
            }

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
        color: Int
    ) {
        if (stroke.points.size < 2) return
        val path = Path()
        stroke.points.forEachIndexed { index, point ->
            val x = point.x * scaleX
            val y = point.y * scaleY
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.strokeWidth * ((scaleX + scaleY) / 2f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawTextTrail(
        canvas: Canvas,
        data: DrawLayerData.TextTrail,
        scaleX: Float,
        scaleY: Float
    ): Boolean {
        if (data.points.isEmpty() || data.text.isBlank()) return false
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveTextBrushColor(data.textStyle.textBrush, data.textStyle.textColorHex)
            textSize = data.textStyle.fontSizeSp * context.resources.displayMetrics.scaledDensity * ((scaleX + scaleY) / 2f)
            isFakeBoldText = true
        }
        data.points.forEachIndexed { index, point ->
            if (index % 2 == 0) {
                canvas.drawText(data.text, point.x * scaleX, point.y * scaleY, paint)
            }
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
        return RenderAssets(bitmaps = bitmaps, movies = movies)
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

    private fun layerRect(
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
        val right = left + width * scaleX * scale
        val bottom = top + height * scaleY * scale
        return RectF(left, top, right, bottom)
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

    private fun Int.even(): Int = if (this % 2 == 0) this else this - 1

    private fun Int.presentationTimeNs(): Long = this * 1_000_000_000L / FRAME_RATE

    private data class RenderAssets(
        val bitmaps: Map<String, Bitmap?>,
        val movies: Map<String, Movie?>
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
        const val DEFAULT_LAYER_SIZE = 220f
        const val DEFAULT_GIF_DURATION_MS = 1000
        const val EGL_RECORDABLE_ANDROID = 0x3142
    }
}
