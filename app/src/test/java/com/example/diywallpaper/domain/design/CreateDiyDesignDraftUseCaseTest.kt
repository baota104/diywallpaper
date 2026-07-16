package com.example.diywallpaper.domain.design

import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.DiyElement
import com.example.diywallpaper.domain.model.DiyElementType
import com.example.diywallpaper.domain.model.DiyTemplate
import com.example.diywallpaper.domain.model.DiyTemplateData
import com.example.diywallpaper.domain.model.DiyTemplateType
import com.example.diywallpaper.domain.model.PhotoPlaceholder
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.UserDesign
import com.example.diywallpaper.domain.repository.UserDesignRepository
import com.example.diywallpaper.domain.usecase.design.CreateDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.CreateDiyDesignDraftUseCase
import com.example.diywallpaper.domain.usecase.design.toEditorProject
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateDiyDesignDraftUseCaseTest {

    @Test
    fun `toEditorProject maps diy template data into fixed snapshot and placeholders`() {
        val template = sampleTemplate()
        val templateData = sampleTemplateData(background = "bg.webp")

        val project = template.toEditorProject(
            templateData = templateData,
            projectId = "design_diy_1",
            createdAt = 123L
        )

        assertEquals("design_diy_1", project.id)
        assertEquals(1080, project.canvas.width)
        assertTrue(project.background is EditorBackground.ApiImage)
        assertEquals(0, project.layers.size)
        assertEquals(1, project.placeholders.size)
        val source = project.source as EditorProjectSource.Diy
        val pictureElement = source.templateSnapshot.elements.first { it.type == "PICTURE" }
        assertEquals(300f, pictureElement.width)
        assertEquals(400f, pictureElement.height)
        assertEquals("https://cdn/frame.png", pictureElement.assetUrl)
        assertEquals("https://cdn/mask1.png", project.placeholders.first().maskPathOrUrl)
        assertEquals("https://cdn/slot.png", project.placeholders.first().previewPathOrUrl)
    }

    @Test
    fun `toEditorProject maps color background into solid color`() {
        val template = sampleTemplate()
        val templateData = sampleTemplateData(background = "#FFAA00")

        val project = template.toEditorProject(templateData = templateData)

        assertEquals(EditorBackground.SolidColor("#FFAA00"), project.background)
    }

    @Test
    fun `create diy draft use case creates design draft`() = runTest {
        val repository = FakeUserDesignRepository()
        val useCase = CreateDiyDesignDraftUseCase(
            createDesignDraftUseCase = CreateDesignDraftUseCase(repository)
        )

        val result = useCase(
            template = sampleTemplate(),
            templateData = sampleTemplateData(background = "https://cdn/bg.webp"),
            title = "My DIY"
        )

        require(result is AppResult.Success)
        assertEquals("created_design_id", result.data)
        assertEquals(1, repository.createdProjects.size)
        assertEquals("My DIY", repository.createdTitles.last())
    }

    private fun sampleTemplate(): DiyTemplate {
        return DiyTemplate(
            id = "template_1",
            type = DiyTemplateType.DIY_STATIC,
            rank = 1,
            thumbUrl = "thumb.webp",
            diyDataUrl = "https://cdn.leansoft-ai.com/diy/1/data.json",
            diyAnimationUrl = null,
            isFavorite = false
        )
    }

    private fun sampleTemplateData(background: String): DiyTemplateData {
        return DiyTemplateData(
            width = 1080,
            height = 1920,
            background = background,
            elements = listOf(
                DiyElement(
                    type = DiyElementType.PICTURE,
                    x = 12f,
                    y = 24f,
                    width = 300f,
                    height = 400f,
                    rotation = 5f,
                    zIndex = 2,
                    srcName = "frame.png",
                    assetUrl = "https://cdn/frame.png"
                ),
                DiyElement(
                    type = DiyElementType.IMAGE,
                    x = 40f,
                    y = 80f,
                    width = 200f,
                    height = 240f,
                    rotation = 0f,
                    zIndex = 3,
                    srcName = "slot.png",
                    assetUrl = null,
                    maskName = "mask1.png",
                    maskUrl = "https://cdn/mask1.png"
                )
            ),
            placeholders = listOf(
                PhotoPlaceholder(
                    id = "placeholder_1",
                    x = 40f,
                    y = 80f,
                    width = 200f,
                    height = 240f,
                    rotation = 0f,
                    zIndex = 3,
                    maskName = "mask1.png",
                    maskUrl = "https://cdn/mask1.png",
                    previewName = "slot.png",
                    previewUrl = "https://cdn/slot.png"
                )
            )
        )
    }
}

private class FakeUserDesignRepository : UserDesignRepository {
    val createdProjects = mutableListOf<EditorProject>()
    val createdTitles = mutableListOf<String?>()

    override fun observeDesigns(): Flow<List<com.example.diywallpaper.domain.model.design.UserDesign>> =
        emptyFlow()

    override suspend fun getDesign(designId: String): AppResult<UserDesign> =
        throw UnsupportedOperationException()

    override suspend fun createDraft(project: EditorProject, title: String?): AppResult<String> {
        createdProjects += project
        createdTitles += title
        return AppResult.Success("created_design_id")
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
