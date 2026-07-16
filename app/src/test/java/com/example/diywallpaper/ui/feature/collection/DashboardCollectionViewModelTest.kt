package com.example.diywallpaper.ui.feature.collection

import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.model.DiyAnimationRaw
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.WallpaperCategory
import com.example.diywallpaper.domain.model.WallpaperItem
import com.example.diywallpaper.domain.model.WallpaperType
import com.example.diywallpaper.domain.repository.DiyRepository
import com.example.diywallpaper.domain.repository.WallpaperRepository
import com.example.diywallpaper.domain.usecase.collection.ObserveFavoriteFeedItemsUseCase
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.usecase.design.ObserveUserDesignsUseCase
import com.example.diywallpaper.domain.usecase.diy.ToggleDiyFavoriteUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.ToggleWallpaperFavoriteUseCase
import com.example.diywallpaper.ui.feature.dashboard.collection.CollectionFilter
import com.example.diywallpaper.ui.feature.dashboard.collection.DashboardCollectionViewModel
import com.example.diywallpaper.ui.preview.core.GridPreviewCoordinator
import com.example.diywallpaper.ui.feature.preview.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardCollectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `view model observes designs and updates count`() = runTest {
        val repository = FakeUserDesignRepository()
        val viewModel = createViewModel(repository)

        repository.emit(listOf(sampleDesign("design_1"), sampleDesign("design_2")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(2, state.designCount)
        assertEquals(2, state.designs.size)
        assertEquals(CollectionFilter.DESIGNS, state.selectedFilter)
    }

    @Test
    fun `view model switches selected filter`() = runTest {
        val viewModel = createViewModel(FakeUserDesignRepository())

        viewModel.onFilterSelected(CollectionFilter.FAVORITES)

        assertEquals(CollectionFilter.FAVORITES, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `view model emits and clears pending open design`() = runTest {
        val repository = FakeUserDesignRepository()
        val viewModel = createViewModel(repository)

        viewModel.onDesignSelected("design_1")
        assertEquals("design_1", viewModel.uiState.value.pendingOpenDesignId)

        viewModel.consumePendingOpenDesign()
        assertEquals(null, viewModel.uiState.value.pendingOpenDesignId)
    }

    @Test
    fun `view model rename and delete delegate to repository`() = runTest {
        val repository = FakeUserDesignRepository()
        val initial = sampleDesign("design_1")
        repository.emit(listOf(initial))
        val viewModel = createViewModel(repository)

        viewModel.renameDesign("design_1", "Renamed")
        advanceUntilIdle()
        assertEquals("Renamed", repository.state.value.first().title)

        viewModel.onDesignSelected("design_1")
        viewModel.deleteDesign("design_1")
        advanceUntilIdle()
        assertEquals(0, repository.state.value.size)
        assertEquals(null, viewModel.uiState.value.pendingOpenDesignId)
    }

    @Test
    fun `view model observes favorite feed items and updates count`() = runTest {
        val designRepository = FakeUserDesignRepository()
        val wallpaperRepository = FakeWallpaperRepository()
        val diyRepository = FakeDiyRepository()
        val viewModel = createViewModel(
            userDesignRepository = designRepository,
            wallpaperRepository = wallpaperRepository,
            diyRepository = diyRepository
        )

        wallpaperRepository.emit(
            listOf(
                WallpaperCategory(
                    id = "nature",
                    name = "Nature",
                    iconUrl = null,
                    rank = 1,
                    items = listOf(
                        sampleWallpaper("wallpaper_1", isFavorite = true),
                        sampleWallpaper("wallpaper_2", isFavorite = false)
                    )
                )
            )
        )
        diyRepository.emit(
            listOf(
                sampleDiy("diy_1", isFavorite = true),
                sampleDiy("diy_2", isFavorite = false)
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(2, state.favoriteCount)
        assertEquals(2, state.favorites.size)
    }

    private fun createViewModel(
        userDesignRepository: FakeUserDesignRepository,
        wallpaperRepository: FakeWallpaperRepository = FakeWallpaperRepository(),
        diyRepository: FakeDiyRepository = FakeDiyRepository()
    ): DashboardCollectionViewModel {
        return DashboardCollectionViewModel(
            observeUserDesignsUseCase = ObserveUserDesignsUseCase(userDesignRepository),
            observeFavoriteFeedItemsUseCase = ObserveFavoriteFeedItemsUseCase(
                wallpaperRepository = wallpaperRepository,
                diyRepository = diyRepository
            ),
            renameDesignUseCase = RenameDesignUseCase(userDesignRepository),
            deleteDesignUseCase = DeleteDesignUseCase(userDesignRepository),
            toggleWallpaperFavoriteUseCase = ToggleWallpaperFavoriteUseCase(wallpaperRepository),
            toggleDiyFavoriteUseCase = ToggleDiyFavoriteUseCase(diyRepository),
            previewCoordinator = GridPreviewCoordinator()
        )
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
            createdAt = 100L,
            updatedAt = 200L,
            lastOpenedAt = 200L,
            isDeleted = false,
            schemaVersion = 1
        )
    }

    private fun sampleWallpaper(id: String, isFavorite: Boolean): WallpaperItem {
        return WallpaperItem(
            id = id,
            categoryId = "nature",
            type = WallpaperType.STATIC_2D,
            rank = 1,
            thumbUrl = "https://example.com/$id.webp",
            imageUrl = "https://example.com/$id-full.webp",
            videoUrl = null,
            isFavorite = isFavorite
        )
    }

    private fun sampleDiy(id: String, isFavorite: Boolean): DiyTemplate {
        return DiyTemplate(
            id = id,
            type = DiyTemplateType.DIY_STATIC,
            rank = 1,
            thumbUrl = "https://example.com/$id.webp",
            diyDataUrl = "https://example.com/$id.json",
            diyAnimationUrl = null,
            isFavorite = isFavorite
        )
    }
}

private class FakeUserDesignRepository : UserDesignRepository {
    val state = MutableStateFlow<List<UserDesign>>(emptyList())

    fun emit(items: List<UserDesign>) {
        state.value = items
    }

    override fun observeDesigns(): Flow<List<UserDesign>> = state

    override suspend fun getDesign(designId: String) =
        state.value.firstOrNull { it.id == designId }?.let { com.example.diywallpaper.core.result.AppResult.Success(it) }
            ?: error("Missing design")

    override suspend fun createDraft(project: EditorProject, title: String?) =
        throw UnsupportedOperationException()

    override suspend fun getProject(designId: String) =
        throw UnsupportedOperationException()

    override suspend fun saveProject(project: EditorProject, title: String?) =
        throw UnsupportedOperationException()

    override suspend fun renameDesign(designId: String, title: String): com.example.diywallpaper.core.result.AppResult<Unit> {
        state.value = state.value.map { design ->
            if (design.id == designId) design.copy(title = title) else design
        }
        return com.example.diywallpaper.core.result.AppResult.Success(Unit)
    }

    override suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?
    ) = throw UnsupportedOperationException()

    override suspend fun deleteDesign(designId: String): com.example.diywallpaper.core.result.AppResult<Unit> {
        state.value = state.value.filterNot { it.id == designId }
        return com.example.diywallpaper.core.result.AppResult.Success(Unit)
    }
}

private class FakeWallpaperRepository : WallpaperRepository {
    private val state = MutableStateFlow<List<WallpaperCategory>>(emptyList())

    fun emit(items: List<WallpaperCategory>) {
        state.value = items
    }

    override fun observeWallpaperCategories(): Flow<List<WallpaperCategory>> = state

    override suspend fun refreshWallpaperCategories() =
        com.example.diywallpaper.core.result.AppResult.Success(Unit)

    override suspend fun toggleFavorite(itemId: String): com.example.diywallpaper.core.result.AppResult<Unit> {
        state.value = state.value.map { category ->
            category.copy(
                items = category.items.map { item ->
                    if (item.id == itemId) item.copy(isFavorite = !item.isFavorite) else item
                }
            )
        }
        return com.example.diywallpaper.core.result.AppResult.Success(Unit)
    }
}

private class FakeDiyRepository : DiyRepository {
    private val state = MutableStateFlow<List<DiyTemplate>>(emptyList())

    fun emit(items: List<DiyTemplate>) {
        state.value = items
    }

    override fun observeDiyTemplates(): Flow<List<DiyTemplate>> = state

    override suspend fun refreshDiyTemplates() =
        com.example.diywallpaper.core.result.AppResult.Success(Unit)

    override suspend fun toggleFavorite(templateId: String): com.example.diywallpaper.core.result.AppResult<Unit> {
        state.value = state.value.map { template ->
            if (template.id == templateId) template.copy(isFavorite = !template.isFavorite) else template
        }
        return com.example.diywallpaper.core.result.AppResult.Success(Unit)
    }

    override suspend fun getDiyTemplateData(
        templateId: String,
        diyDataUrl: String
    ): com.example.diywallpaper.core.result.AppResult<DiyTemplateData> {
        throw UnsupportedOperationException()
    }

    override suspend fun getDiyAnimationRaw(
        templateId: String,
        animationUrl: String
    ): com.example.diywallpaper.core.result.AppResult<DiyAnimationRaw> {
        throw UnsupportedOperationException()
    }
}
