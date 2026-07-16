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
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.model.preview.PreviewPlayableSource
import com.example.diywallpaper.domain.model.preview.PreviewPrimaryAction
import com.example.diywallpaper.domain.model.preview.PreviewSourceType
import com.example.diywallpaper.domain.model.preview.WallpaperTarget
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DesignVideoExporter
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import com.example.diywallpaper.domain.usecase.design.GetDesignProjectUseCase
import com.example.diywallpaper.domain.usecase.design.GetUserDesignUseCase
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperLauncher
import com.example.diywallpaper.domain.wallpaper.LiveWallpaperSourceStore
import com.example.diywallpaper.domain.usecase.preview.GetPreviewCarouselItemsUseCase
import com.example.diywallpaper.domain.usecase.preview.PreviewCarouselPlaybackPolicy
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveWallpaperUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveDesignWallpaperUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetStaticWallpaperUseCase
import com.example.diywallpaper.ui.feature.preview.carousel.PreviewCarouselViewModel
import com.example.diywallpaper.ui.feature.preview.device.DevicePreviewViewModel
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.wallpaper.SystemWallpaperManager
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
            getUserDesignUseCase = buildUserDesignUseCase(),
            getDesignProjectUseCase = buildDesignProjectUseCase(),
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveDesignWallpaperUseCase = buildLiveDesignWallpaperUseCase(),
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
            getUserDesignUseCase = buildUserDesignUseCase(),
            getDesignProjectUseCase = buildDesignProjectUseCase(),
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveDesignWallpaperUseCase = buildLiveDesignWallpaperUseCase(),
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
        assertTrue(viewModel.uiState.value.showTargetDialog)
        viewModel.applyStaticWallpaper(WallpaperTarget.HOME)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isApplyingWallpaper)
        assertEquals(null, state.errorMessage)
        assertEquals("Wallpaper applied successfully.", state.successMessage)
    }

    @Test
    fun `device preview view model returns launch intent for live wallpaper apply`() = runTest {
        val viewModel = DevicePreviewViewModel(
            getUserDesignUseCase = buildUserDesignUseCase(),
            getDesignProjectUseCase = buildDesignProjectUseCase(),
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveDesignWallpaperUseCase = buildLiveDesignWallpaperUseCase(),
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
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isApplyingWallpaper)
        assertEquals("live.intent", state.launchIntent?.action)
        assertEquals("Live wallpaper is ready. Choose where to apply it.", state.successMessage)
    }

    @Test
    fun `device preview view model binds saved design from collection`() = runTest {
        val viewModel = DevicePreviewViewModel(
            getUserDesignUseCase = buildUserDesignUseCase(),
            getDesignProjectUseCase = buildDesignProjectUseCase(),
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveDesignWallpaperUseCase = buildLiveDesignWallpaperUseCase(),
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "collection",
                initialItemId = "design_1",
                sourceType = PreviewSourceType.CREATE_FROM_SCRATCH
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(PreviewPrimaryAction.SET_WALLPAPER, state.primaryAction)
        assertEquals("/tmp/design_1/exported.png", state.designExportPath)
        assertEquals("Saved Design", state.currentItem?.title)
    }

    @Test
    fun `device preview applies animated saved design without requiring video url`() = runTest {
        val repository = buildUserDesignRepository(project = animatedSavedProject("design_live"))
        val viewModel = DevicePreviewViewModel(
            getUserDesignUseCase = GetUserDesignUseCase(repository),
            getDesignProjectUseCase = GetDesignProjectUseCase(repository),
            getPreviewCarouselItemsUseCase = buildUseCase(),
            setStaticWallpaperUseCase = buildStaticWallpaperUseCase(),
            setLiveDesignWallpaperUseCase = buildLiveDesignWallpaperUseCase(),
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase()
        )

        viewModel.bind(
            PreviewArgs(
                categoryId = "collection",
                initialItemId = "design_live",
                sourceType = PreviewSourceType.CREATE_FROM_SCRATCH
            )
        )
        advanceUntilIdle()
        assertEquals(PreviewPrimaryAction.SET_LIVE_WALLPAPER, viewModel.uiState.value.primaryAction)

        viewModel.onApplyClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("live.design.intent", state.launchIntent?.action)
        assertEquals(null, state.errorMessage)
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

    private fun buildLiveWallpaperUseCase(intentAction: String = "live.intent"): SetLiveWallpaperUseCase {
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
                    every { intent.action } returns intentAction
                    return AppResult.Success(intent)
                }
            }
        )
    }

    private fun buildLiveDesignWallpaperUseCase(): SetLiveDesignWallpaperUseCase {
        return SetLiveDesignWallpaperUseCase(
            designVideoExporter = object : DesignVideoExporter {
                override suspend fun export(project: EditorProject): AppResult<String> {
                    return AppResult.Success("/tmp/${project.id}.mp4")
                }
            },
            setLiveWallpaperUseCase = buildLiveWallpaperUseCase("live.design.intent")
        )
    }

    private fun buildUserDesignUseCase(): GetUserDesignUseCase {
        return GetUserDesignUseCase(buildUserDesignRepository())
    }

    private fun buildDesignProjectUseCase(): GetDesignProjectUseCase {
        return GetDesignProjectUseCase(buildUserDesignRepository())
    }

    private fun buildUserDesignRepository(project: EditorProject? = null): UserDesignRepository {
        return object : UserDesignRepository {
            override fun observeDesigns(): Flow<List<UserDesign>> = flowOf(emptyList())

            override suspend fun getDesign(designId: String): AppResult<UserDesign> {
                return AppResult.Success(
                    UserDesign(
                        id = designId,
                        sourceType = DesignSourceType.SCRATCH,
                        title = "Saved Design",
                        thumbnailPath = "/tmp/$designId/thumb.webp",
                        previewPath = "/tmp/$designId/preview.webp",
                        templateId = null,
                        projectFilePath = "/tmp/$designId/project.json",
                        canvasWidth = 1080,
                        canvasHeight = 1920,
                        exportedImagePath = "/tmp/$designId/exported.png",
                        createdAt = 1L,
                        updatedAt = 2L,
                        lastOpenedAt = 3L,
                        isDeleted = false,
                        schemaVersion = 1
                    )
                )
            }

            override suspend fun createDraft(project: EditorProject, title: String?) = AppResult.Success(project.id)

            override suspend fun getProject(designId: String): AppResult<EditorProject> {
                return project?.let { AppResult.Success(it) } ?: AppResult.Error(AppError.EmptyResponse)
            }

            override suspend fun saveProject(project: EditorProject, title: String?) = AppResult.Success(Unit)

            override suspend fun renameDesign(designId: String, title: String) = AppResult.Success(Unit)

            override suspend fun updateAssets(
                designId: String,
                thumbnailPath: String?,
                previewPath: String?,
                exportedImagePath: String?
            ) = AppResult.Success(Unit)

            override suspend fun deleteDesign(designId: String) = AppResult.Success(Unit)
        }
    }

    private fun animatedSavedProject(designId: String): EditorProject {
        return EditorProject(
            id = designId,
            source = EditorProjectSource.Scratch,
            canvas = EditorCanvasSpec(1080, 1920),
            background = EditorBackground.SolidColor("#FFFFFF"),
            layers = listOf(
                StickerLayer(
                    id = "sticker_live",
                    stickerId = "sticker_live",
                    assetPathOrUrl = "https://cdn/sticker-preview.webp",
                    animatedAssetPathOrUrl = "https://cdn/sticker.gif",
                    isAnimated = true,
                    zIndex = 1,
                    transform = LayerTransform(10f, 20f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                )
            ),
            placeholders = emptyList(),
            selectedLayerId = "sticker_live",
            createdAt = 1L,
            updatedAt = 2L,
            schemaVersion = 1
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
    override suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String?
    ): AppResult<DiyTemplateData> {
        return AppResult.Error(AppError.EmptyResponse)
    }
    override suspend fun getDiyAnimationRaw(templateId: String, animationUrl: String): AppResult<DiyAnimationRaw> {
        return AppResult.Error(AppError.EmptyResponse)
    }
}
