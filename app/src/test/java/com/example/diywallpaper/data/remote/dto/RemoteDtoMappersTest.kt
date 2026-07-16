package com.example.diywallpaper.data.remote.dto

import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDtoMappersTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `parse data_url_full into categories`() {
        val payload = """
            [
              {
                "category": "Nature",
                "rank": 1,
                "icon": "https://cdn/icon.webp",
                "items": [
                  {
                    "id": 11,
                    "type": "2d",
                    "rank": 2,
                    "thumb": "https://cdn/thumb.webp",
                    "data": "https://cdn/image.webp"
                  }
                ]
              }
            ]
        """.trimIndent()

        val result = json.decodeFromString(ListSerializer(RemoteCategoryDto.serializer()), payload)

        assertEquals(1, result.size)
        assertEquals("Nature", result.first().category)
        assertEquals(1, result.first().items.size)
    }

    @Test
    fun `2d item maps to static wallpaper`() {
        val result = RemoteItemDto(
            id = 1,
            type = "2d",
            rank = 3,
            thumb = "thumb",
            data = "image"
        ).toWallpaperDomainOrNull("Nature")

        assertEquals(WallpaperType.STATIC_2D, result?.type)
        assertEquals("image", result?.imageUrl)
        assertNull(result?.videoUrl)
    }

    @Test
    fun `live item maps content to video url only`() {
        val result = RemoteItemDto(
            id = 2,
            type = "live",
            rank = 1,
            thumb = "thumb",
            content = "video.mp4"
        ).toWallpaperDomainOrNull("Nature")

        assertEquals(WallpaperType.LIVE_VIDEO, result?.type)
        assertEquals("video.mp4", result?.videoUrl)
        assertNull(result?.imageUrl)
    }

    @Test
    fun `live item without content still maps with null video`() {
        val result = RemoteItemDto(
            id = 3,
            type = "live",
            thumb = "thumb"
        ).toWallpaperDomainOrNull("Nature")

        assertEquals("thumb", result?.thumbUrl)
        assertNull(result?.videoUrl)
    }

    @Test
    fun `2d item without data falls back to thumb`() {
        val result = RemoteItemDto(
            id = 4,
            type = "2d",
            thumb = "thumb"
        ).toWallpaperDomainOrNull("Nature")

        assertEquals("thumb", result?.imageUrl)
    }

    @Test
    fun `category DIY is separated from wallpaper list`() {
        val category = RemoteCategoryDto(
            category = "DIY",
            items = listOf(
                RemoteItemDto(
                    id = 1,
                    type = "diy-live",
                    dataZip = "https://cdn/1.zip",
                    diyData = "https://cdn/data.json",
                    diyAnimation = "https://cdn/animation.json"
                )
            )
        )

        assertNull(category.toWallpaperCategoryOrNull())
        val diyTemplates = category.toDiyTemplates()
        assertEquals(1, diyTemplates.size)
        assertEquals(DiyTemplateType.DIY_LIVE, diyTemplates.first().type)
        assertEquals("https://cdn/1.zip", diyTemplates.first().dataZipUrl)
    }

    @Test
    fun `invalid wallpaper item is skipped`() {
        val result = RemoteItemDto(
            id = 5,
            type = "live",
            thumb = null,
            content = "video.mp4"
        ).toWallpaperDomainOrNull("Nature")

        assertNull(result)
    }

    @Test
    fun `background create maps rank from category rank`() {
        val result = BackgroundCreateDto(
            id = 9,
            categoryRank = 7,
            name = "Summer",
            data = "https://cdn/bg.webp"
        ).toDomainOrNull()

        assertEquals("9", result?.id)
        assertEquals(7, result?.rank)
        assertEquals("https://cdn/bg.webp", result?.thumbnailUrl)
    }

    @Test
    fun `sticker gif detection works`() {
        val result = StickerDto(
            id = 8,
            rank = 1,
            stickers = "https://cdn/sticker.gif"
        ).toDomainOrNull()

        assertTrue(result?.isAnimated == true)
    }
}
