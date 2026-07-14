package com.example.diywallpaper.domain.design

import com.example.diywallpaper.domain.model.design.DesignViewportScaleMode
import com.example.diywallpaper.domain.model.design.CropPresetRatio
import com.example.diywallpaper.domain.model.design.designViewportTransform
import com.example.diywallpaper.domain.model.design.photoRenderSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignRenderSpecTest {
    @Test
    fun `photo render size matches editor base sizing for portrait crop`() {
        val size = photoRenderSize(CropPresetRatio.RATIO_9_16)

        assertEquals(123.75f, size.width, 0.0001f)
        assertEquals(220f, size.height, 0.0001f)
    }

    @Test
    fun `cover viewport fills target and center crops evenly`() {
        val viewport = designViewportTransform(
            designWidth = 1080f,
            designHeight = 1920f,
            targetWidth = 1080f,
            targetHeight = 2400f,
            scaleMode = DesignViewportScaleMode.Cover
        )

        assertEquals(1.25f, viewport.scale, 0.0001f)
        assertEquals(-135f, viewport.offsetX, 0.0001f)
        assertEquals(0f, viewport.offsetY, 0.0001f)
        assertTrue(viewport.scaledWidth >= 1080f)
        assertTrue(viewport.scaledHeight >= 2400f)
    }

    @Test
    fun `contain viewport keeps full design visible and centers it`() {
        val viewport = designViewportTransform(
            designWidth = 1080f,
            designHeight = 1920f,
            targetWidth = 1080f,
            targetHeight = 2400f,
            scaleMode = DesignViewportScaleMode.Contain
        )

        assertEquals(1f, viewport.scale, 0.0001f)
        assertEquals(0f, viewport.offsetX, 0.0001f)
        assertEquals(240f, viewport.offsetY, 0.0001f)
        assertTrue(viewport.scaledWidth <= 1080f)
        assertTrue(viewport.scaledHeight <= 2400f)
    }
}
