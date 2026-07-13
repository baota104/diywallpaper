package com.example.diywallpaper.domain.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.BackgroundCreateItem
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.usecase.design.CreateDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.CreateScratchDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.ScratchDesignBackground
import com.example.diywallpaper.domain.usecase.design.ScratchDesignDraftRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateScratchDesignDraftUseCaseTest {

    @Test
    fun `creates scratch draft with default canvas and solid background`() = runTest {
        val repository = FakeScratchUserDesignRepository()
        val useCase = CreateScratchDesignDraftUseCase(
            createDesignDraftUseCase = CreateDesignDraftUseCase(repository)
        )

        val result = useCase()

        require(result is AppResult.Success)
        assertEquals("scratch_design_id", result.data)
        assertEquals(1, repository.createdProjects.size)
        val project = repository.createdProjects.first()
        assertTrue(project.source is EditorProjectSource.Scratch)
        assertEquals(EditorCanvasSpec(1080, 1920), project.canvas)
        assertEquals(EditorBackground.SolidColor("#FFFFFF"), project.background)
    }

    @Test
    fun `creates scratch draft with api background`() = runTest {
        val repository = FakeScratchUserDesignRepository()
        val useCase = CreateScratchDesignDraftUseCase(
            createDesignDraftUseCase = CreateDesignDraftUseCase(repository)
        )
        val request = ScratchDesignDraftRequest(
            background = ScratchDesignBackground.ApiBackground(
                item = BackgroundCreateItem(
                    id = "bg_1",
                    rank = 1,
                    name = "Aurora",
                    imageUrl = "https://cdn/bg.webp",
                    thumbnailUrl = "https://cdn/bg-thumb.webp"
                )
            )
        )

        useCase(request = request, title = "Aurora Draft")

        val project = repository.createdProjects.first()
        assertEquals(
            EditorBackground.ApiImage(
                backgroundId = "bg_1",
                imageUrl = "https://cdn/bg.webp"
            ),
            project.background
        )
        assertEquals("Aurora Draft", repository.createdTitles.first())
    }

    @Test
    fun `creates scratch draft with gradient background`() = runTest {
        val repository = FakeScratchUserDesignRepository()
        val useCase = CreateScratchDesignDraftUseCase(
            createDesignDraftUseCase = CreateDesignDraftUseCase(repository)
        )

        useCase(
            request = ScratchDesignDraftRequest(
                background = ScratchDesignBackground.Gradient(
                    listOf("#111111", "#222222")
                )
            )
        )

        val project = repository.createdProjects.first()
        assertEquals(
            EditorBackground.Gradient(listOf("#111111", "#222222")),
            project.background
        )
    }
}

private class FakeScratchUserDesignRepository : UserDesignRepository {
    val createdProjects = mutableListOf<EditorProject>()
    val createdTitles = mutableListOf<String?>()

    override fun observeDesigns(): Flow<List<com.example.diywallpaper.domain.model.design.UserDesign>> =
        emptyFlow()

    override suspend fun getDesign(designId: String): AppResult<UserDesign> =
        throw UnsupportedOperationException()

    override suspend fun createDraft(project: EditorProject, title: String?): AppResult<String> {
        createdProjects += project
        createdTitles += title
        return AppResult.Success("scratch_design_id")
    }

    override suspend fun getProject(designId: String) = throw UnsupportedOperationException()

    override suspend fun saveProject(project: EditorProject, title: String?) =
        throw UnsupportedOperationException()

    override suspend fun renameDesign(designId: String, title: String): AppResult<Unit> =
        throw UnsupportedOperationException()

    override suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?
    ) = throw UnsupportedOperationException()

    override suspend fun deleteDesign(designId: String) = throw UnsupportedOperationException()
}
