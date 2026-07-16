package com.example.diywallpaper.core.render

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.diywallpaper.domain.model.design.DesignRawBounds
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.TextShadowSpec
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import kotlin.math.max

data class AndroidTextLayerRenderSpec(
    val bounds: DesignRawBounds,
    val drawX: Float,
    val drawBaselineY: Float
)

fun androidTextLayerRenderSpec(
    context: Context,
    layer: TextLayer,
    fontResId: Int
): AndroidTextLayerRenderSpec {
    return androidTextRenderSpec(
        context = context,
        text = layer.text,
        style = layer.style,
        fontResId = fontResId
    )
}

fun androidTextRenderSpec(
    context: Context,
    text: String,
    style: TextStyleSpec,
    fontResId: Int
): AndroidTextLayerRenderSpec {
    val safeText = text.ifBlank { " " }
    val fontSize = style.fontSizeSp.coerceAtLeast(1f) *
        context.resources.displayMetrics.scaledDensity
    val paint = buildAndroidTextPaint(
        context = context,
        style = style,
        fontResId = fontResId,
        color = Color.BLACK,
        alpha = 1f,
        textScale = 1f
    ).apply {
        textSize = fontSize
        shader = null
        clearShadowLayer()
    }
    val baselineY = fontSize
    val path = Path()
    val glyphBounds = RectF()
    paint.getTextPath(safeText, 0, safeText.length, 0f, baselineY, path)
    path.computeBounds(glyphBounds, true)
    if (glyphBounds.isEmpty) {
        val metrics = paint.fontMetrics
        glyphBounds.set(
            0f,
            baselineY + metrics.ascent,
            paint.measureText(safeText).coerceAtLeast(fontSize),
            baselineY + metrics.descent
        )
    }
    val horizontalPadding = max(fontSize * 0.22f, 10f)
    val verticalPadding = max(fontSize * 0.22f, 10f)
    return AndroidTextLayerRenderSpec(
        bounds = DesignRawBounds(
            minX = glyphBounds.left - horizontalPadding,
            minY = glyphBounds.top - verticalPadding,
            maxX = glyphBounds.right + horizontalPadding,
            maxY = glyphBounds.bottom + verticalPadding
        ),
        drawX = 0f,
        drawBaselineY = baselineY
    )
}

fun buildAndroidTextPaint(
    context: Context,
    style: TextStyleSpec,
    fontResId: Int,
    color: Int,
    alpha: Float,
    textScale: Float
): TextPaint {
    val scaledTextSize = style.fontSizeSp.coerceAtLeast(1f) *
        context.resources.displayMetrics.scaledDensity *
        textScale
    return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = scaledTextSize
        letterSpacing = style.letterSpacing / style.fontSizeSp.coerceAtLeast(1f)
        typeface = ResourcesCompat.getFont(context, fontResId)
        isFakeBoldText = true
        this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        applyAndroidTextShadow(style.shadow, textScale)
        if (style.textBrush is TextBrushStyle.Gradient) {
            shader = LinearGradient(
                0f,
                0f,
                measureText("MMMM").coerceAtLeast(1f),
                textSize,
                style.textBrush.colors.map { parseAndroidRenderColor(it, color) }.toIntArray(),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }
}

private fun TextPaint.applyAndroidTextShadow(shadow: TextShadowSpec?, scale: Float) {
    if (shadow == null) return
    setShadowLayer(
        shadow.blurRadius * scale,
        shadow.offsetX * scale,
        shadow.offsetY * scale,
        parseAndroidRenderColor(shadow.colorHex, Color.TRANSPARENT)
    )
}

private fun parseAndroidRenderColor(hex: String, fallback: Int): Int {
    return runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
}
