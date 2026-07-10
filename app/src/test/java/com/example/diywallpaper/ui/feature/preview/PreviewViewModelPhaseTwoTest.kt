package com.example.diywallpaper.ui.feature.preview

import android.content.Intent
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.model.preview.PreviewPlayableSource
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperLauncher
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperSourceStore
import com.example.diywallpaper.domain.usecase.preview.GetPreviewCarouselItemsUseCase
import com.example.diywallpaper.domain.usecase.preview.PreviewCarouselPlaybackPolicy
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveWallpaperUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetStaticWallpaperUseCase
import com.example.diywallpaper.ui.feature.preview.carousel.PreviewCarouselViewModel
import com.example.diywallpaper.ui.feature.preview.device.DevicePreviewViewModel
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import com.example.diywallpaper.domain.wallpaper.SystemWallpaperManager
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PreviewViewModelPhaseTwoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `carousel view model loads initial state and playback window`() = runTest {
        val viewModel = PreviewCarouselViewModel(
            getPreviewCarouselItemsUseCase = buildUseCase(),
            playbackPolicy = PreviewCarouselPlaybackPolicy()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "nature",
                initialItemId = "wall_2",
                sourceType = PreviewSourceType.WALLPAPER
            )
        )

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.currentIndex)
        assertEquals(setOf("wall_1", "wall_2", "wall_live"), state.activePlaybackIds)
        assertEquals("wall_2", state.primaryItemId)
        assertEquals(PreviewPrimaryAction.SET_WALLPAPER, state.currentAction)
    }

    @Test
    fun `carousel view model updates page settled state`() = runTest {
        val viewModel = PreviewCarouselViewModel(
            getPreviewCarouselItemsUseCase = buildUseCase(),
            playbackPolicy = PreviewCarouselPlaybackPolicy()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "nature",
                initialItemId = "wall_1",
                sourceType = PreviewSourceType.WALLPAPER
            )
        )
        viewModel.onPageSettled(1)

        val state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertEquals("wall_2", state.primaryItemId)
        assertEquals(PreviewPrimaryAction.SET_WALLPAPER, state.currentAction)
    }

    @Test
    fun `device preview view model resolves playable source and chrome state`() = runTest {
        val viewModel = DevicePreviewViewModel(
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "nature",
                initialItemId = "wall_live",
                sourceType = PreviewSourceType.WALLPAPER
            )
        )

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(PreviewPrimaryAction.SET_LIVE_WALLPAPER, state.primaryAction)
        assertTrue(state.playableSource is PreviewPlayableSource.LiveWallpaper)
        assertTrue(state.isChromeVisible)

        viewModel.onToggleChrome()
        viewModel.onApplyStarted()
        state = viewModel.uiState.value
        assertFalse(state.isChromeVisible)
        assertTrue(state.isApplyingWallpaper)

        viewModel.onApplyFinished(errorMessage = "failed")
        state = viewModel.uiState.value
        assertFalse(state.isApplyingWallpaper)
        assertEquals("failed", state.errorMessage)
    }

    @Test
    fun `device preview view model applies static wallpaper successfully`() = runTest {
        val viewModel = DevicePreviewViewModel(
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "nature",
                initialItemId = "wall_1",
                sourceType = PreviewSourceType.WALLPAPER
            )
        )
        viewModel.onApplyClick()

        val state = viewModel.uiState.value
        assertFalse(state.isApplyingWallpaper)
        assertEquals(null, state.errorMessage)
        assertEquals("Wallpaper applied successfully.", state.successMessage)
    }

    @Test
    fun `device preview view model returns launch intent for live wallpaper apply`() = runTest {
        val viewModel = DevicePreviewViewModel(
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "nature",
                initialItemId = "wall_live",
                sourceType = PreviewSourceType.WALLPAPER
            )
        )
        viewModel.onApplyClick()

        val state = viewModel.uiState.value
        assertFalse(state.isApplyingWallpaper)
        assertEquals("live.intent", state.launchIntent?.action)
        assertEquals("Live wallpaper is ready. Choose where to apply it.", state.successMessage)
    }

    private fun buildUseCase(): GetPreviewCarouselItemsUseCase {
        return GetPreviewCarouselItemsUseCase(
            wallpaperRepository = FakeWallpaperRepository(
                categories = listOf(
                    WallpaperCategory(
                        id = "nature",
                        name = "Nature",
                        iconUrl = null,
                        rank = 1,
                        items = listOf(
                            WallpaperItem("wall_2", "nature", WallpaperType.STATIC_2D, 2, "thumb2", "image2", null, false),
                            WallpaperItem("wall_1", "nature", WallpaperType.STATIC_2D, 1, "thumb1", "image1", null, false),
                            WallpaperItem("wall_live", "nature", WallpaperType.LIVE_VIDEO, 3, "thumb3", null, "video3", true)
                        )
                    )
                )
            ),
            diyRepository = FakeDiyRepository(),
            backgroundCreateRepository = FakeBackgroundRepository()
        )
    }

    private fun buildStaticWallpaperUseCase(): SetStaticWallpaperUseCase {
        return SetStaticWallpaperUseCase(
            wallpaperAssetResolver = object : WallpaperAssetResolver {
                override suspend fun resolveStaticImage(source: WallpaperApplySource.StaticImage): AppResult<File> {
                    return AppResult.Success(File("test_wallpaper.jpg"))
                }

                override suspend fun resolveLiveVideo(source: WallpaperApplySource.LiveVideo): AppResult<File> {
                    return AppResult.Success(File("test_live.mp4"))
                }
            },
            systemWallpaperManager = object : SystemWallpaperManager {
                override suspend fun setStaticWallpaper(file: File, target: WallpaperTarget): AppResult<Unit> {
                    return AppResult.Success(Unit)
                }
            }
        )
    }

    private fun buildLiveWallpaperUseCase(): SetLiveWallpaperUseCase {
        return SetLiveWallpaperUseCase(
            wallpaperAssetResolver = object : WallpaperAssetResolver {
                override suspend fun resolveStaticImage(source: WallpaperApplySource.StaticImage): AppResult<File> {
                    return AppResult.Error(AppError.EmptyResponse)
                }

                override suspend fun resolveLiveVideo(source: WallpaperApplySource.LiveVideo): AppResult<File> {
                    return AppResult.Success(File("test_live.mp4"))
                }
            },
            liveWallpaperSourceStore = object : LiveWallpaperSourceStore {
                override suspend fun saveVideoPath(path: String): AppResult<Unit> = AppResult.Success(Unit)
                override fun getVideoPath(): String? = null
            },
            liveWallpaperLauncher = object : LiveWallpaperLauncher {
                override fun createLaunchIntent(): AppResult<Intent> {
                    val intent = mockk<Intent>(relaxed = true)
                    every { intent.action } returns "live.intent"
                    return AppResult.Success(intent)
                }
            }
        )
    }
}

private class FakeWallpaperRepository(
    private val categories: List<WallpaperCategory>
) : WallpaperRepository {
    override fun observeWallpaperCategories(): Flow<List<WallpaperCategory>> = flowOf(categories)
    override suspend fun refreshWallpaperCategories(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleFavorite(itemId: String): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeBackgroundRepository : BackgroundCreateRepository {
    override fun observeBackgrounds(): Flow<List<BackgroundCreateItem>> = flowOf(emptyList())
    override suspend fun refreshBackgrounds(): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeDiyRepository : DiyRepository {
    override fun observeDiyTemplates(): Flow<List<DiyTemplate>> = flowOf(emptyList())
    override suspend fun refreshDiyTemplates(): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun toggleFavorite(templateId: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun getDiyTemplateData(templateId: String, diyDataUrl: String): AppResult<DiyTemplateData> {
        return AppResult.Error(AppError.EmptyResponse)
    }
    override suspend fun getDiyAnimationRaw(templateId: String, animationUrl: String): AppResult<DiyAnimationRaw> {
        return AppResult.Error(AppError.EmptyResponse)
    }
}
