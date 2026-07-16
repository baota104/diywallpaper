package com.example.diywallpaper.domain.preview

import app.cash.turbine.test
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.domain.model.preview.DiyAnimationState
import com.example.diywallpaper.domain.model.preview.PreviewItemKind
import com.example.diywallpaper.domain.model.preview.PreviewPlayableSource
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.domain.model.preview.primaryAction
import com.example.diywallpaper.domain.model.preview.toPlayableSource
import com.example.diywallpaper.domain.model.preview.toPreviewCatalogItem
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import com.example.diywallpaper.domain.usecase.diy.GetDiyAnimationPayloadUseCase
import com.example.diywallpaper.domain.usecase.preview.GetPreviewCarouselItemsUseCase
import com.example.diywallpaper.domain.usecase.preview.PreviewCarouselPlaybackPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPhaseOneTest {
    @Test
    fun `wallpaper live maps to preview catalog and playable source`() {
        val item = WallpaperItem(
            id = "wall_1",
            categoryId = "nature",
            type = WallpaperType.LIVE_VIDEO,
            rank = 1,
            thumbUrl = "thumb.webp",
            imageUrl = null,
            videoUrl = "video.mp4",
            isFavorite = true
        )

        val previewItem = item.toPreviewCatalogItem(categoryTitle = "Nature")

        assertEquals(PreviewItemKind.WALLPAPER_LIVE, previewItem.kind)
        assertEquals(PreviewPrimaryAction.SET_LIVE_WALLPAPER, previewItem.primaryAction())
        assertEquals(
            PreviewPlayableSource.LiveWallpaper(
                itemId = "wall_1",
                videoUrl = "video.mp4",
                thumbUrl = "thumb.webp"
            ),
            previewItem.toPlayableSource()
        )
    }

    @Test
    fun `diy live maps to animation preview source`() {
        val template = DiyTemplate(
            id = "diy_1",
            type = DiyTemplateType.DIY_LIVE,
            rank = 2,
            thumbUrl = "thumb.webp",
            diyDataUrl = "data.json",
            dataZipUrl = null,
            diyAnimationUrl = "animation.json",
            isFavorite = false
        )

        val previewItem = template.toPreviewCatalogItem()

        assertEquals(PreviewItemKind.DIY_LIVE, previewItem.kind)
        assertEquals(PreviewPrimaryAction.EDIT_DIY, previewItem.primaryAction())
        assertEquals(
            PreviewPlayableSource.DiyLive(
                itemId = "diy_1",
                diyDataUrl = "data.json",
                diyAnimationUrl = "animation.json",
                thumbUrl = "thumb.webp"
            ),
            previewItem.toPlayableSource()
        )
    }

    @Test
    fun `create from scratch maps to internal preview item`() {
        val background = BackgroundCreateItem(
            id = "bg_1",
            rank = 3,
            name = "Dream",
            imageUrl = "image.webp",
            thumbnailUrl = "thumb.webp"
        )

        val previewItem = background.toPreviewCatalogItem()

        assertEquals(PreviewItemKind.CREATE_FROM_SCRATCH, previewItem.kind)
        assertEquals(PreviewPrimaryAction.CREATE_FROM_SCRATCH, previewItem.primaryAction())
        assertEquals(
            PreviewPlayableSource.Scratch(
                itemId = "bg_1",
                templateBackgroundUrl = "image.webp"
            ),
            previewItem.toPlayableSource()
        )
    }

    @Test
    fun `playback policy keeps current and neighbor items active`() {
        val items = listOf(
            buildPreviewWallpaper(id = "1", rank = 1),
            buildPreviewWallpaper(id = "2", rank = 2),
            buildPreviewWallpaper(id = "3", rank = 3),
            buildPreviewWallpaper(id = "4", rank = 4)
        )

        val result = PreviewCarouselPlaybackPolicy().resolve(items = items, currentIndex = 2)

        assertEquals("3", result.primaryItemId)
        assertEquals(setOf("2", "3", "4"), result.activeItemIds)
        assertEquals(setOf("2", "4"), result.neighborItemIds)
    }

    @Test
    fun `carousel use case returns sorted wallpaper items and initial index`() = runTest {
        val useCase = GetPreviewCarouselItemsUseCase(
            wallpaperRepository = FakeWallpaperRepository(
                categories = listOf(
                    WallpaperCategory(
                        id = "nature",
                        name = "Nature",
                        iconUrl = null,
                        rank = 1,
                        items = listOf(
                            WallpaperItem("wall_2", "nature", WallpaperType.STATIC_2D, 2, "thumb2", "image2", null, false),
                            WallpaperItem("wall_1", "nature", WallpaperType.LIVE_VIDEO, 1, "thumb1", null, "video1", true)
                        )
                    )
                )
            ),
            diyRepository = FakeDiyRepository(emptyList()),
            backgroundCreateRepository = FakeBackgroundRepository(emptyList())
        )

        useCase(categoryId = "nature", initialItemId = "wall_2", sourceType = PreviewSourceType.WALLPAPER).test {
            val item = awaitItem()
            assertEquals(listOf("wall_1", "wall_2"), item.items.map { it.id })
            assertEquals(1, item.initialIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `diy animation use case maps raw json to ready state`() = runTest {
        val useCase = GetDiyAnimationPayloadUseCase(
            diyRepository = FakeDiyRepository(
                templates = emptyList(),
                animationResult = AppResult.Success(
                    DiyAnimationRaw(
                        templateId = "diy_1",
                        animationUrl = "animation.json",
                        rawJson = """{"fps":30}"""
                    )
                )
            )
        )

        val result = useCase(templateId = "diy_1", animationUrl = "animation.json")

        assertTrue(result is AppResult.Success)
        val payload = (result as AppResult.Success).data
        assertEquals(DiyAnimationState.READY, payload.state)
        assertEquals("animation.json", payload.animationUrl)
    }

    @Test
    fun `diy animation use case maps blank raw json to unsupported state`() = runTest {
        val useCase = GetDiyAnimationPayloadUseCase(
            diyRepository = FakeDiyRepository(
                templates = emptyList(),
                animationResult = AppResult.Success(
                    DiyAnimationRaw(
                        templateId = "diy_2",
                        animationUrl = "animation.json",
                        rawJson = ""
                    )
                )
            )
        )

        val result = useCase(templateId = "diy_2", animationUrl = "animation.json")

        assertTrue(result is AppResult.Success)
        val payload = (result as AppResult.Success).data
        assertEquals(DiyAnimationState.UNSUPPORTED, payload.state)
    }

    private fun buildPreviewWallpaper(id: String, rank: Int) =
        WallpaperItem(
            id = id,
            categoryId = "nature",
            type = WallpaperType.LIVE_VIDEO,
            rank = rank,
            thumbUrl = "thumb_$id",
            imageUrl = null,
            videoUrl = "video_$id",
            isFavorite = false
        ).toPreviewCatalogItem(categoryTitle = "Nature")
}

private class FakeWallpaperRepository(
    private val categories: List<WallpaperCategory>
) : WallpaperRepository {
    override fun observeWallpaperCategories(): Flow<List<WallpaperCategory>> = flowOf(categories)

    override suspend fun refreshWallpaperCategories(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun toggleFavorite(itemId: String): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeBackgroundRepository(
    private val items: List<BackgroundCreateItem>
) : BackgroundCreateRepository {
    override fun observeBackgrounds(): Flow<List<BackgroundCreateItem>> = flowOf(items)

    override suspend fun refreshBackgrounds(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeDiyRepository(
    private val templates: List<DiyTemplate>,
    private val animationResult: AppResult<DiyAnimationRaw> = AppResult.Error(AppError.EmptyResponse)
) : DiyRepository {
    override fun observeDiyTemplates(): Flow<List<DiyTemplate>> = flowOf(templates)

    override suspend fun refreshDiyTemplates(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun toggleFavorite(templateId: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String?
    ): AppResult<DiyTemplateData> {
        return AppResult.Success(
            DiyTemplateData(
                width = 1080,
                height = 1920,
                background = "#FFFFFF",
                elements = emptyList(),
                placeholders = emptyList()
            )
        )
    }

    override suspend fun getDiyAnimationRaw(
        templateId: String,
        animationUrl: String
    ): AppResult<DiyAnimationRaw> = animationResult
}
