package com.example.diywallpaper.ui.feature.editor

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.StickerItem
import com.example.diywallpaper.domain.model.design.BrushStyleSpec
import com.example.diywallpaper.domain.model.design.CropPresetRatio
import com.example.diywallpaper.domain.model.design.CropSpec
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.PhotoLayer
import com.example.diywallpaper.domain.model.design.StickerTrailRotationMode
import com.example.diywallpaper.domain.model.design.StrokePoint
import com.example.diywallpaper.domain.model.design.StickerLayer
import com.example.diywallpaper.domain.model.design.TextBrushStyle
import com.example.diywallpaper.domain.model.design.TextLayer
import com.example.diywallpaper.domain.model.design.TextStyleSpec
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.model.design.GeneratedDesignAssets
import com.example.diywallpaper.domain.repository.BackgroundCreateRepository
import com.example.diywallpaper.domain.repository.DesignAssetExporter
import com.example.diywallpaper.domain.repository.StickerRepository
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.usecase.design.CreateDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.GenerateDesignAssetsUseCase
import com.example.diywallpaper.domain.usecase.design.GetDesignProjectUseCase
import com.example.diywallpaper.domain.usecase.design.GetEditorTextLibraryUseCase
import com.example.diywallpaper.domain.usecase.design.GetUserDesignUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import com.example.diywallpaper.domain.usecase.design.SaveDesignProjectUseCase
import com.example.diywallpaper.domain.usecase.design.UpdateDesignAssetsUseCase
import com.example.diywallpaper.domain.usecase.sticker.GetStickersUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.GetBackgroundCreateItemsUseCase
import com.example.diywallpaper.ui.feature.preview.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    @Test
    fun `bindProject populates editor state`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        val project = sampleProject()

        viewModel.bindProject(project)

        val state = viewModel.uiState.value
        assertEquals(project.id, state.projectId)
        assertEquals(DesignSourceType.SCRATCH, state.sourceType)
        assertEquals(project.layers.size, state.layers.size)
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
    }

    @Test
    fun `startNewProject keeps design in runtime until next`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)

        viewModel.startNewProject(sampleProject(), title = "Draft")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPersisted)
        assertEquals(0, repository.createdDraftCount)
    }

    @Test
    fun `updateBackground pushes history and autosaves`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.updateBackground(EditorBackground.SolidColor("#000000"))
        assertTrue(viewModel.uiState.value.canUndo)
        assertTrue(viewModel.uiState.value.isSaving)

        advanceTimeBy(751)
        advanceUntilIdle()

        assertEquals(1, repository.savedProjects.size)
        assertEquals(
            EditorBackground.SolidColor("#000000"),
            repository.savedProjects.last().background
        )
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `undo and redo restore project states`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        val newLayer = StickerLayer(
            id = "layer_2",
            assetPathOrUrl = "asset://sticker",
            zIndex = 2,
            transform = LayerTransform(10f, 20f, 1f, 0f),
            isLocked = false,
            isHidden = false
        )

        viewModel.upsertLayer(newLayer)
        assertEquals(2, viewModel.uiState.value.layers.size)

        viewModel.undo()
        assertEquals(1, viewModel.uiState.value.layers.size)
        assertTrue(viewModel.uiState.value.canRedo)

        viewModel.redo()
        assertEquals(2, viewModel.uiState.value.layers.size)
    }

    @Test
    fun `loadDesign hydrates state from repository`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        repository.projectToLoad = sampleProject().copy(id = "loaded_design")
        repository.designToLoad = sampleDesign("loaded_design").copy(
            title = "Loaded design",
            thumbnailPath = "/tmp/loaded_thumb.webp",
            previewPath = "/tmp/loaded_preview.webp",
            exportedImagePath = "/tmp/loaded_export.png"
        )
        val viewModel = createViewModel(repository)

        viewModel.loadDesign("loaded_design")
        advanceUntilIdle()

        assertEquals("loaded_design", viewModel.uiState.value.projectId)
        assertEquals("Loaded design", viewModel.uiState.value.title)
        assertEquals("/tmp/loaded_export.png", viewModel.uiState.value.exportedImagePath)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `phase 6 background operations update real project state`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val backgroundRepository = FakeBackgroundCreateRepository().apply {
            emit(
                listOf(
                    BackgroundCreateItem(
                        id = "bg_1",
                        rank = 1,
                        name = "Aurora",
                        imageUrl = "https://cdn/bg.webp",
                        thumbnailUrl = "https://cdn/bg-thumb.webp"
                    )
                )
            )
        }
        val viewModel = createViewModel(
            repository = repository,
            backgroundRepository = backgroundRepository
        )
        advanceUntilIdle()
        viewModel.bindProject(sampleProject())

        viewModel.applyApiBackground(
            BackgroundCreateItem(
                id = "bg_1",
                rank = 1,
                name = "Aurora",
                imageUrl = "https://cdn/bg.webp",
                thumbnailUrl = "https://cdn/bg-thumb.webp"
            )
        )
        assertEquals(
            EditorBackground.ApiImage("bg_1", "https://cdn/bg.webp"),
            viewModel.uiState.value.background
        )
        assertEquals(1, viewModel.uiState.value.availableBackgrounds.size)
        assertEquals("bg_1", viewModel.uiState.value.availableBackgrounds.first().id)

        viewModel.applyLocalImageBackground(
            localPath = "content://image/1",
            crop = CropSpec(0f, 0f, 1f, 1f, CropPresetRatio.RATIO_9_16)
        )
        assertEquals(
            EditorBackground.LocalImage(
                localPath = "content://image/1",
                crop = CropSpec(0f, 0f, 1f, 1f, CropPresetRatio.RATIO_9_16)
            ),
            viewModel.uiState.value.background
        )
    }

    @Test
    fun `phase 6 text and sticker tools mutate project state`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        val textStyle = sampleTextStyle()

        viewModel.addTextLayer(text = "Hello", style = textStyle)
        val textLayer = viewModel.uiState.value.layers.last() as TextLayer
        assertEquals("Hello", textLayer.text)

        val updatedStyle = textStyle.copy(
            fontFamilyId = "script_font",
            textColorHex = "#FF00AA"
        )
        viewModel.updateTextLayer(
            layerId = textLayer.id,
            text = "Updated",
            style = updatedStyle
        )
        val updatedLayer = viewModel.uiState.value.layers
            .first { it.id == textLayer.id } as TextLayer
        assertEquals("Updated", updatedLayer.text)
        assertEquals("script_font", updatedLayer.style.fontFamilyId)
        assertEquals("#FF00AA", updatedLayer.style.textColorHex)

        viewModel.addStickerLayer(
            sticker = StickerItem(
                id = "sticker_1",
                rank = 1,
                stickerUrl = "https://cdn/sticker.webp",
                thumbnailUrl = "https://cdn/sticker-thumb.webp",
                isAnimated = false
            )
        )
        val stickerLayer = viewModel.uiState.value.layers.last() as StickerLayer
        assertEquals("sticker_1", stickerLayer.stickerId)
    }

    @Test
    fun `phase 6 preview mode toggles independently`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.enterPreviewMode()
        assertTrue(viewModel.uiState.value.isPreviewMode)
        assertEquals(EditorTool.PREVIEW, viewModel.uiState.value.activeTool)

        viewModel.exitPreviewMode()
        assertFalse(viewModel.uiState.value.isPreviewMode)
    }

    @Test
    fun `phase 7 advanced creative tools create draw layers`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        val points = listOf(StrokePoint(0f, 0f), StrokePoint(16f, 20f))

        viewModel.addBrushStroke(
            points = points,
            brushStyle = BrushStyleSpec.Gradient(listOf("#111111", "#222222")),
            strokeWidth = 12f
        )
        assertTrue((viewModel.uiState.value.layers.last() as DrawLayer).drawData is DrawLayerData.FreeStroke)

        viewModel.addEraseStroke(points = points, strokeWidth = 20f)
        assertTrue((viewModel.uiState.value.layers.last() as DrawLayer).drawData is DrawLayerData.EraseStroke)

        viewModel.addStickerTrail(
            sticker = StickerItem(
                id = "sticker_trail",
                rank = 1,
                stickerUrl = "https://cdn/stamp.webp",
                thumbnailUrl = "https://cdn/stamp-thumb.webp",
                isAnimated = false
            ),
            points = points,
            spacing = 24f,
            stampSize = 32f,
            rotationMode = StickerTrailRotationMode.FOLLOW_PATH
        )
        assertTrue((viewModel.uiState.value.layers.last() as DrawLayer).drawData is DrawLayerData.StickerTrail)

        viewModel.addTextTrail(
            text = "Trail",
            style = sampleTextStyle().copy(
                textBrush = TextBrushStyle.Gradient(listOf("#AA00FF", "#00BBFF"))
            ),
            points = points,
            spacing = 18f
        )
        assertTrue((viewModel.uiState.value.layers.last() as DrawLayer).drawData is DrawLayerData.TextTrail)
    }

    @Test
    fun `configureBrushTool then commitCanvasStroke creates draw layer from real points`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        val points = listOf(StrokePoint(10f, 10f), StrokePoint(30f, 40f))

        viewModel.configureBrushTool(
            erase = false,
            colorHex = "#FF3548",
            brushSize = 22f
        )
        viewModel.commitCanvasStroke(points)

        val layer = viewModel.uiState.value.layers.last() as DrawLayer
        val drawData = layer.drawData as DrawLayerData.FreeStroke
        assertEquals(points, drawData.stroke.points)
        assertEquals("#FF3548", drawData.stroke.colorHex)
        assertEquals(22f, drawData.stroke.strokeWidth)
    }

    @Test
    fun `configureTextBrushTool then commitCanvasStroke creates text trail`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        val points = listOf(StrokePoint(10f, 10f), StrokePoint(30f, 40f))

        viewModel.configureTextBrushTool(
            text = "Hello",
            style = sampleTextStyle(),
            spacing = 18f
        )
        viewModel.commitCanvasStroke(points)

        val layer = viewModel.uiState.value.layers.last() as DrawLayer
        val drawData = layer.drawData as DrawLayerData.TextTrail
        assertEquals("Hello", drawData.text)
        assertEquals(points, drawData.points)
        assertEquals(18f, drawData.spacing)
    }

    @Test
    fun `phase 7 transform updates selected layer position scale and rotation`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.updateLayerTransform(
            layerId = "layer_1",
            offsetXDelta = 40f,
            offsetYDelta = 60f,
            scaleMultiplier = 1.5f,
            rotationDelta = 18f
        )

        val layer = viewModel.uiState.value.layers.first { it.id == "layer_1" } as StickerLayer
        assertEquals(40f, layer.transform.offsetX)
        assertEquals(60f, layer.transform.offsetY)
        assertEquals(1.5f, layer.transform.scale)
        assertEquals(18f, layer.transform.rotation)
        assertEquals("layer_1", viewModel.uiState.value.selectedLayerId)
    }

    @Test
    fun `reorder actions update content layer z order without touching background`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProjectWithMultipleLayers())

        viewModel.selectLayer("layer_2")
        viewModel.bringSelectedLayerForward()
        assertEquals(
            listOf("layer_1", "layer_3", "layer_2"),
            viewModel.uiState.value.layers.sortedBy { it.zIndex }.map { it.id }
        )

        viewModel.sendSelectedLayerToBack()
        assertEquals(
            listOf("layer_2", "layer_1", "layer_3"),
            viewModel.uiState.value.layers.sortedBy { it.zIndex }.map { it.id }
        )

        viewModel.bringSelectedLayerToFront()
        assertEquals(
            listOf("layer_1", "layer_3", "layer_2"),
            viewModel.uiState.value.layers.sortedBy { it.zIndex }.map { it.id }
        )

        viewModel.sendSelectedLayerBackward()
        assertEquals(
            listOf("layer_1", "layer_2", "layer_3"),
            viewModel.uiState.value.layers.sortedBy { it.zIndex }.map { it.id }
        )

        assertEquals(EditorBackground.SolidColor("#FFFFFF"), viewModel.uiState.value.background)
    }

    @Test
    fun `moveLayer reorders by explicit target index`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProjectWithMultipleLayers())

        viewModel.moveLayer(layerId = "layer_3", targetIndex = 0)

        assertEquals(
            listOf("layer_3", "layer_1", "layer_2"),
            viewModel.uiState.value.layers.sortedBy { it.zIndex }.map { it.id }
        )
        assertEquals("layer_3", viewModel.uiState.value.selectedLayerId)
        assertEquals(EditorBackground.SolidColor("#FFFFFF"), viewModel.uiState.value.background)
    }

    @Test
    fun `phase 7 updateTextLayer edits selected text content and style`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())
        viewModel.addTextLayer(text = "Old", style = sampleTextStyle())
        val layerId = viewModel.uiState.value.layers.last().id

        viewModel.updateTextLayer(
            layerId = layerId,
            text = "New copy",
            style = sampleTextStyle().copy(
                fontFamilyId = "plus_jakarta_sans",
                textColorHex = "#43B5F5"
            )
        )

        val layer = viewModel.uiState.value.layers.first { it.id == layerId } as TextLayer
        assertEquals("New copy", layer.text)
        assertEquals("plus_jakarta_sans", layer.style.fontFamilyId)
        assertEquals("#43B5F5", layer.style.textColorHex)
        assertEquals(layerId, viewModel.uiState.value.selectedLayerId)
    }

    @Test
    fun `phase 8 rename updates title metadata`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        repository.designToLoad = sampleDesign("design_runtime")
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.renameDesign("Renamed Design")
        advanceUntilIdle()

        assertEquals("Renamed Design", repository.designToLoad?.title)
        assertEquals("Renamed Design", viewModel.uiState.value.title)
    }

    @Test
    fun `phase 8 asset update stores output paths in state`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.updateGeneratedAssets(
            thumbnailPath = "/tmp/thumb.webp",
            previewPath = "/tmp/preview.webp",
            exportedImagePath = "/tmp/export.png"
        )
        advanceUntilIdle()

        assertEquals("/tmp/thumb.webp", viewModel.uiState.value.thumbnailPath)
        assertEquals("/tmp/export.png", viewModel.uiState.value.exportedImagePath)
    }

    @Test
    fun `phase 8 delete marks state deleted and clears runtime project`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val viewModel = createViewModel(repository)
        viewModel.bindProject(sampleProject())

        viewModel.deleteCurrentDesign()
        advanceUntilIdle()

        assertTrue(repository.deletedDesignIds.contains("design_runtime"))
        assertTrue(viewModel.uiState.value.isDeleted)
        assertEquals("design_runtime", viewModel.uiState.value.projectId)
    }

    @Test
    fun `phase 8 preparePreviewNavigation updates state with exported paths and pending navigation`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val exporter = FakeDesignAssetExporter()
        val viewModel = createViewModel(
            repository = repository,
            designAssetExporter = exporter
        )
        viewModel.startNewProject(sampleProject(), title = "Draft")

        viewModel.preparePreviewNavigation()
        advanceUntilIdle()

        assertEquals("/tmp/design_runtime/thumb.webp", viewModel.uiState.value.thumbnailPath)
        assertEquals("/tmp/design_runtime/preview.webp", viewModel.uiState.value.previewPath)
        assertEquals("/tmp/design_runtime/exported.png", viewModel.uiState.value.exportedImagePath)
        assertEquals("design_runtime", viewModel.uiState.value.pendingPreviewDesignId)
        assertTrue(viewModel.uiState.value.isPersisted)
        assertEquals(1, repository.createdDraftCount)
        assertEquals("design_runtime", exporter.lastProjectId)
    }

    @Test
    fun `preparePreviewNavigation skips blocking asset export for animated sticker design`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val exporter = FakeDesignAssetExporter()
        val viewModel = createViewModel(
            repository = repository,
            designAssetExporter = exporter
        )
        viewModel.startNewProject(sampleAnimatedStickerProject(), title = "Animated Draft")

        viewModel.preparePreviewNavigation()
        advanceUntilIdle()

        assertEquals("design_runtime", viewModel.uiState.value.pendingPreviewDesignId)
        assertTrue(viewModel.uiState.value.isPersisted)
        assertEquals(1, repository.createdDraftCount)
        assertEquals(null, exporter.lastProjectId)
        assertEquals(null, viewModel.uiState.value.previewPath)
        assertEquals(null, viewModel.uiState.value.exportedImagePath)
    }

    @Test
    fun `phase 5 observes real background and sticker catalogs`() = runTest(dispatcher) {
        val repository = FakeEditorDesignRepository()
        val backgroundRepository = FakeBackgroundCreateRepository().apply {
            emit(
                listOf(
                    BackgroundCreateItem(
                        id = "bg_1",
                        rank = 1,
                        name = "Aurora",
                        imageUrl = "https://cdn/bg.webp",
                        thumbnailUrl = "https://cdn/bg-thumb.webp"
                    )
                )
            )
        }
        val stickerRepository = FakeStickerRepository().apply {
            emit(
                listOf(
                    StickerItem(
                        id = "sticker_1",
                        rank = 1,
                        stickerUrl = "https://cdn/sticker.webp",
                        thumbnailUrl = "https://cdn/sticker-thumb.webp",
                        isAnimated = false
                    )
                )
            )
        }

        val viewModel = createViewModel(
            repository = repository,
            backgroundRepository = backgroundRepository,
            stickerRepository = stickerRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.availableBackgrounds.size)
        assertEquals("bg_1", state.availableBackgrounds.first().id)
        assertEquals(1, state.availableStickers.size)
        assertEquals("sticker_1", state.availableStickers.first().id)
        assertTrue(backgroundRepository.refreshCalled)
        assertTrue(stickerRepository.refreshCalled)
        assertFalse(state.isLoadingBackgroundCatalog)
        assertFalse(state.isLoadingStickerCatalog)
    }

    @Test
    fun `phase 6 exposes editor fonts and text presets`() = runTest(dispatcher) {
        val viewModel = createViewModel(FakeEditorDesignRepository())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.availableFonts.size > 2)
        assertEquals(GetEditorTextLibraryUseCase.FONT_INTER, state.availableFonts.first().id)
        assertTrue(state.availableFonts.any { it.id == "allura" })
        assertTrue(state.textPresets.isNotEmpty())
    }

    private fun sampleProject(): EditorProject {
        return EditorProject(
            id = "design_runtime",
            source = EditorProjectSource.Scratch,
            canvas = EditorCanvasSpec(1080, 1920),
            background = EditorBackground.SolidColor("#FFFFFF"),
            layers = listOf(
                StickerLayer(
                    id = "layer_1",
                    assetPathOrUrl = "asset://base",
                    zIndex = 1,
                    transform = LayerTransform(0f, 0f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                )
            ),
            placeholders = emptyList(),
            selectedLayerId = "layer_1",
            createdAt = 10L,
            updatedAt = 20L,
            schemaVersion = 1
        )
    }

    private fun sampleProjectWithMultipleLayers(): EditorProject {
        return EditorProject(
            id = "design_runtime",
            source = EditorProjectSource.Scratch,
            canvas = EditorCanvasSpec(1080, 1920),
            background = EditorBackground.SolidColor("#FFFFFF"),
            layers = listOf(
                StickerLayer(
                    id = "layer_1",
                    assetPathOrUrl = "asset://base",
                    zIndex = 1,
                    transform = LayerTransform(0f, 0f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                ),
                TextLayer(
                    id = "layer_2",
                    text = "Hello",
                    style = sampleTextStyle(),
                    zIndex = 2,
                    transform = LayerTransform(24f, 12f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                ),
                PhotoLayer(
                    id = "layer_3",
                    localPath = "content://image/3",
                    zIndex = 3,
                    transform = LayerTransform(32f, 18f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                )
            ),
            placeholders = emptyList(),
            selectedLayerId = "layer_2",
            createdAt = 10L,
            updatedAt = 20L,
            schemaVersion = 1
        )
    }

    private fun sampleAnimatedStickerProject(): EditorProject {
        return sampleProject().copy(
            layers = listOf(
                StickerLayer(
                    id = "animated_sticker",
                    stickerId = "sticker_animated",
                    assetPathOrUrl = "https://cdn/sticker-preview.webp",
                    animatedAssetPathOrUrl = "https://cdn/sticker.gif",
                    isAnimated = true,
                    zIndex = 1,
                    transform = LayerTransform(0f, 0f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                )
            ),
            selectedLayerId = "animated_sticker"
        )
    }

    private fun sampleTextStyle(): TextStyleSpec {
        return TextStyleSpec(
            fontFamilyId = "inter",
            fontDisplayName = "Inter",
            fontSizeSp = 24f,
            textColorHex = "#222222"
        )
    }
}

private fun createViewModel(
    repository: FakeEditorDesignRepository,
    backgroundRepository: FakeBackgroundCreateRepository = FakeBackgroundCreateRepository(),
    stickerRepository: FakeStickerRepository = FakeStickerRepository(),
    designAssetExporter: FakeDesignAssetExporter = FakeDesignAssetExporter()
): EditorViewModel {
    return EditorViewModel(
        createDesignDraftUseCase = CreateDesignDraftUseCase(repository),
        getUserDesignUseCase = GetUserDesignUseCase(repository),
        getDesignProjectUseCase = GetDesignProjectUseCase(repository),
        saveDesignProjectUseCase = SaveDesignProjectUseCase(repository),
        generateDesignAssetsUseCase = GenerateDesignAssetsUseCase(designAssetExporter),
        renameDesignUseCase = RenameDesignUseCase(repository),
        updateDesignAssetsUseCase = UpdateDesignAssetsUseCase(repository),
        deleteDesignUseCase = DeleteDesignUseCase(repository),
        getEditorTextLibraryUseCase = GetEditorTextLibraryUseCase(),
        getBackgroundCreateItemsUseCase = GetBackgroundCreateItemsUseCase(backgroundRepository),
        getStickersUseCase = GetStickersUseCase(stickerRepository)
    )
}

private class FakeEditorDesignRepository : UserDesignRepository {
    val savedProjects = mutableListOf<EditorProject>()
    val deletedDesignIds = mutableListOf<String>()
    var createdDraftCount: Int = 0
    var projectToLoad: EditorProject? = null
    var designToLoad: UserDesign? = null

    override fun observeDesigns(): Flow<List<UserDesign>> =
        emptyFlow()

    override suspend fun getDesign(designId: String): AppResult<UserDesign> {
        return AppResult.Success(designToLoad ?: sampleDesign(designId))
    }

    override suspend fun createDraft(project: EditorProject, title: String?): AppResult<String> {
        createdDraftCount += 1
        return AppResult.Success(project.id)
    }

    override suspend fun getProject(designId: String): AppResult<EditorProject> {
        return AppResult.Success(projectToLoad ?: error("Missing project"))
    }

    override suspend fun saveProject(project: EditorProject, title: String?): AppResult<Unit> {
        savedProjects += project
        return AppResult.Success(Unit)
    }

    override suspend fun renameDesign(designId: String, title: String): AppResult<Unit> {
        designToLoad = (designToLoad ?: sampleDesign(designId)).copy(title = title)
        return AppResult.Success(Unit)
    }

    override suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?
    ): AppResult<Unit> {
        designToLoad = (designToLoad ?: sampleDesign(designId)).copy(
            thumbnailPath = thumbnailPath,
            previewPath = previewPath,
            exportedImagePath = exportedImagePath
        )
        return AppResult.Success(Unit)
    }

    override suspend fun deleteDesign(designId: String): AppResult<Unit> {
        deletedDesignIds += designId
        return AppResult.Success(Unit)
    }
}

private class FakeDesignAssetExporter : DesignAssetExporter {
    var lastProjectId: String? = null

    override suspend fun export(project: EditorProject): AppResult<GeneratedDesignAssets> {
        lastProjectId = project.id
        return AppResult.Success(
            GeneratedDesignAssets(
                thumbnailPath = "/tmp/${project.id}/thumb.webp",
                previewPath = "/tmp/${project.id}/preview.webp",
                exportedImagePath = "/tmp/${project.id}/exported.png"
            )
        )
    }
}

private class FakeBackgroundCreateRepository : BackgroundCreateRepository {
    private val state = MutableStateFlow<List<BackgroundCreateItem>>(emptyList())
    var refreshCalled: Boolean = false

    fun emit(items: List<BackgroundCreateItem>) {
        state.value = items
    }

    override fun observeBackgrounds(): Flow<List<BackgroundCreateItem>> = state

    override suspend fun refreshBackgrounds(): AppResult<Unit> {
        refreshCalled = true
        return AppResult.Success(Unit)
    }
}

private class FakeStickerRepository : StickerRepository {
    private val state = MutableStateFlow<List<StickerItem>>(emptyList())
    var refreshCalled: Boolean = false

    fun emit(items: List<StickerItem>) {
        state.value = items
    }

    override fun observeStickers(): Flow<List<StickerItem>> = state

    override suspend fun refreshStickers(): AppResult<Unit> {
        refreshCalled = true
        return AppResult.Success(Unit)
    }
}

private fun sampleDesign(id: String): UserDesign {
    return UserDesign(
        id = id,
        sourceType = DesignSourceType.SCRATCH,
        title = "Design $id",
        thumbnailPath = null,
        previewPath = null,
        templateId = null,
        projectFilePath = "files/$id/project.json",
        canvasWidth = 1080,
        canvasHeight = 1920,
        exportedImagePath = null,
        createdAt = 10L,
        updatedAt = 20L,
        lastOpenedAt = 20L,
        isDeleted = false,
        schemaVersion = 1
    )
}
