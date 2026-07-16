package com.example.diywallpaper.domain.home

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.HomeFeedCategory
import com.example.diywallpaper.domain.model.HomeFeedItem
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import com.example.diywallpaper.domain.usecase.home.GetHomeFeedUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetHomeFeedUseCaseTest {

    @Test
    fun `invoke includes diy category on home and sorts categories by rank`() = runTest {
        val wallpaperRepository = FakeWallpaperRepository(
            categories = listOf(
                sampleWallpaperCategory(id = "Nature", rank = 3),
                sampleWallpaperCategory(id = "Cars", rank = 5)
            )
        )
        val diyRepository = FakeDiyRepository(
            templates = listOf(
                sampleDiyTemplate(id = "71", rank = 2),
                sampleDiyTemplate(id = "49", rank = 1, type = DiyTemplateType.DIY_LIVE)
            )
        )
        val useCase = GetHomeFeedUseCase(wallpaperRepository, diyRepository)

        val categories = useCase().firstValue()

        assertEquals(listOf("DIY", "Nature", "Cars"), categories.map { it.id })
        val diyCategory = categories.first()
        assertEquals(2, diyCategory.items.size)
        assertTrue(diyCategory.items.all { it is HomeFeedItem.DiyEntry })
        assertEquals(listOf("49", "71"), diyCategory.items.map { it.id })
    }

    @Test
    fun `invoke omits diy category when repository has no templates`() = runTest {
        val wallpaperRepository = FakeWallpaperRepository(
            categories = listOf(sampleWallpaperCategory(id = "Nature", rank = 3))
        )
        val diyRepository = FakeDiyRepository(templates = emptyList())
        val useCase = GetHomeFeedUseCase(wallpaperRepository, diyRepository)

        val categories = useCase().firstValue()

        assertEquals(listOf("Nature"), categories.map { it.id })
    }
}

private class FakeWallpaperRepository(
    categories: List<WallpaperCategory>
) : WallpaperRepository {
    private val state = MutableStateFlow(categories)

    override fun observeWallpaperCategories(): Flow<List<WallpaperCategory>> = state

    override suspend fun refreshWallpaperCategories(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun toggleFavorite(itemId: String): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeDiyRepository(
    templates: List<DiyTemplate>
) : DiyRepository {
    private val state = MutableStateFlow(templates)

    override fun observeDiyTemplates(): Flow<List<DiyTemplate>> = state

    override suspend fun refreshDiyTemplates(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun toggleFavorite(templateId: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String?
    ): AppResult<DiyTemplateData> = throw UnsupportedOperationException()

    override suspend fun getDiyAnimationRaw(
        templateId: String,
        animationUrl: String
    ): AppResult<DiyAnimationRaw> = throw UnsupportedOperationException()
}

private suspend fun Flow<List<HomeFeedCategory>>.firstValue(): List<HomeFeedCategory> = first()

private fun sampleWallpaperCategory(id: String, rank: Int): WallpaperCategory {
    return WallpaperCategory(
        id = id,
        name = id,
        iconUrl = null,
        rank = rank,
        items = listOf(
            WallpaperItem(
                id = "wallpaper_${id}_1",
                categoryId = id,
                type = WallpaperType.STATIC_2D,
                rank = 1,
                thumbUrl = "https://cdn/thumb.webp",
                imageUrl = "https://cdn/image.webp",
                videoUrl = null,
                isFavorite = false
            )
        )
    )
}

private fun sampleDiyTemplate(
    id: String,
    rank: Int,
    type: DiyTemplateType = DiyTemplateType.DIY_STATIC
): DiyTemplate {
    return DiyTemplate(
        id = id,
        type = type,
        rank = rank,
        thumbUrl = "https://cdn/diy_$id.webp",
        diyDataUrl = "https://cdn/diy/$id/data.json",
        diyAnimationUrl = if (type == DiyTemplateType.DIY_LIVE) {
            "https://cdn/diy/$id/animation.json"
        } else {
            null
        },
        isFavorite = false
    )
}
