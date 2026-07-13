package com.example.diywallpaper.ui.feature.collection

import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.usecase.design.ObserveUserDesignsUseCase
import com.example.diywallpaper.ui.feature.dashboard.collection.CollectionFilter
import com.example.diywallpaper.ui.feature.dashboard.collection.DashboardCollectionViewModel
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
        val viewModel = DashboardCollectionViewModel(
            observeUserDesignsUseCase = ObserveUserDesignsUseCase(repository),
            renameDesignUseCase = RenameDesignUseCase(repository),
            deleteDesignUseCase = DeleteDesignUseCase(repository)
        )

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
        val viewModel = DashboardCollectionViewModel(
            observeUserDesignsUseCase = ObserveUserDesignsUseCase(FakeUserDesignRepository()),
            renameDesignUseCase = RenameDesignUseCase(FakeUserDesignRepository()),
            deleteDesignUseCase = DeleteDesignUseCase(FakeUserDesignRepository())
        )

        viewModel.onFilterSelected(CollectionFilter.FAVORITES)

        assertEquals(CollectionFilter.FAVORITES, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `view model emits and clears pending open design`() = runTest {
        val repository = FakeUserDesignRepository()
        val viewModel = DashboardCollectionViewModel(
            observeUserDesignsUseCase = ObserveUserDesignsUseCase(repository),
            renameDesignUseCase = RenameDesignUseCase(repository),
            deleteDesignUseCase = DeleteDesignUseCase(repository)
        )

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
        val viewModel = DashboardCollectionViewModel(
            observeUserDesignsUseCase = ObserveUserDesignsUseCase(repository),
            renameDesignUseCase = RenameDesignUseCase(repository),
            deleteDesignUseCase = DeleteDesignUseCase(repository)
        )

        viewModel.renameDesign("design_1", "Renamed")
        advanceUntilIdle()
        assertEquals("Renamed", repository.state.value.first().title)

        viewModel.onDesignSelected("design_1")
        viewModel.deleteDesign("design_1")
        advanceUntilIdle()
        assertEquals(0, repository.state.value.size)
        assertEquals(null, viewModel.uiState.value.pendingOpenDesignId)
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
