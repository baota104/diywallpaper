package com.example.diywallpaper.data.repository

import app.cash.turbine.test
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.data.local.dao.UserDesignDao
import com.example.diywallpaper.data.local.entity.UserDesignEntity
import com.example.diywallpaper.data.local.files.JsonDesignFileStore
import com.example.diywallpaper.domain.model.design.BrushStroke
import com.example.diywallpaper.domain.model.design.DesignSourceType
import com.example.diywallpaper.domain.model.design.DrawLayer
import com.example.diywallpaper.domain.model.design.DrawLayerData
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.design.LayerTransform
import com.example.diywallpaper.domain.model.design.StrokePoint
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.example.diywallpaper.ui.feature.preview.MainDispatcherRule

class UserDesignRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    }

    @Test
    fun `createDraft writes metadata and project file`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        val project = sampleProject(id = "design_1")

        val result = repository.createDraft(project, title = "My first design")

        assertTrue(result is AppResult.Success)
        val entity = dao.getDesignById("design_1")
        requireNotNull(entity)
        assertEquals("My first design", entity.title)
        assertEquals(DesignSourceType.SCRATCH.name, entity.sourceType)
        assertTrue(File(entity.projectFilePath).exists())
    }

    @Test
    fun `observeDesigns emits active designs only`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        repository.createDraft(sampleProject(id = "active"), title = "Active")
        repository.createDraft(sampleProject(id = "deleted"), title = "Deleted")
        repository.deleteDesign("deleted")

        repository.observeDesigns().test {
            val items = awaitItem()
            assertEquals(1, items.size)
            assertEquals("active", items.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getProject returns saved project content`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        val project = sampleProject(id = "design_load")
        repository.createDraft(project, title = "Load me")

        val result = repository.getProject("design_load")

        require(result is AppResult.Success)
        assertEquals(project.id, result.data.id)
        assertEquals(project.layers.size, result.data.layers.size)
    }

    @Test
    fun `saveProject updates persisted project and metadata`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        repository.createDraft(sampleProject(id = "design_save"), title = "Before")
        val updatedProject = sampleProject(id = "design_save").copy(
            canvas = EditorCanvasSpec(width = 1440, height = 2560)
        )

        val result = repository.saveProject(updatedProject, title = "After")

        assertTrue(result is AppResult.Success)
        val entity = dao.getDesignById("design_save")
        requireNotNull(entity)
        assertEquals("After", entity.title)
        assertEquals(1440, entity.canvasWidth)
        val loaded = repository.getProject("design_save")
        require(loaded is AppResult.Success)
        assertEquals(1440, loaded.data.canvas.width)
    }

    @Test
    fun `renameDesign updates title metadata`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        repository.createDraft(sampleProject(id = "design_rename"), title = "Before")

        val result = repository.renameDesign("design_rename", "After")

        assertTrue(result is AppResult.Success)
        assertEquals("After", dao.getDesignById("design_rename")?.title)
    }

    @Test
    fun `updateAssets stores preview thumbnail and export paths`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        repository.createDraft(sampleProject(id = "design_assets"), title = "Assets")

        val result = repository.updateAssets(
            designId = "design_assets",
            thumbnailPath = store.thumbnailFilePath("design_assets"),
            previewPath = store.previewFilePath("design_assets"),
            exportedImagePath = store.exportedImageFilePath("design_assets")
        )

        assertTrue(result is AppResult.Success)
        val entity = dao.getDesignById("design_assets")
        requireNotNull(entity)
        assertEquals(store.thumbnailFilePath("design_assets"), entity.thumbnailPath)
        assertEquals(store.previewFilePath("design_assets"), entity.previewPath)
        assertEquals(store.exportedImageFilePath("design_assets"), entity.exportedImagePath)
    }

    @Test
    fun `deleteDesign marks metadata deleted and removes project files`() = runTest {
        val dao = FakeUserDesignDao()
        val store = JsonDesignFileStore(createTempDir(), json)
        val repository = UserDesignRepositoryImpl(dao, store)
        repository.createDraft(sampleProject(id = "design_delete"), title = "Delete")
        val projectPath = store.projectFilePath("design_delete")

        val result = repository.deleteDesign("design_delete")

        assertTrue(result is AppResult.Success)
        assertTrue(dao.getDesignById("design_delete")?.isDeleted == true)
        assertTrue(File(projectPath).exists().not())
    }

    private fun sampleProject(id: String): EditorProject {
        return EditorProject(
            id = id,
            source = EditorProjectSource.Scratch,
            canvas = EditorCanvasSpec(width = 1080, height = 1920),
            background = EditorBackground.SolidColor("#FFFFFF"),
            layers = listOf(
                DrawLayer(
                    id = "draw_1",
                    drawData = DrawLayerData.FreeStroke(
                        BrushStroke(
                            points = listOf(
                                StrokePoint(0f, 0f),
                                StrokePoint(10f, 20f)
                            ),
                            colorHex = "#000000",
                            strokeWidth = 8f
                        )
                    ),
                    zIndex = 1,
                    transform = LayerTransform(0f, 0f, 1f, 0f),
                    isLocked = false,
                    isHidden = false
                )
            ),
            placeholders = emptyList(),
            selectedLayerId = null,
            createdAt = 100L,
            updatedAt = 200L,
            schemaVersion = 1
        )
    }

    private fun createTempDir(): File {
        return kotlin.io.path.createTempDirectory("design-store-test").toFile()
    }
}

private class FakeUserDesignDao : UserDesignDao {
    private val entities = linkedMapOf<String, UserDesignEntity>()
    private val state = MutableStateFlow<List<UserDesignEntity>>(emptyList())

    override fun observeActiveDesigns(): Flow<List<UserDesignEntity>> = state

    override suspend fun getDesignById(designId: String): UserDesignEntity? = entities[designId]

    override suspend fun upsertDesign(entity: UserDesignEntity): Long {
        entities[entity.id] = entity
        emit()
        return 1L
    }

    override suspend fun renameDesign(designId: String, title: String, updatedAt: Long): Int {
        val entity = entities[designId] ?: return 0
        entities[designId] = entity.copy(
            title = title,
            updatedAt = updatedAt
        )
        emit()
        return 1
    }

    override suspend fun updateDesignSnapshot(
        designId: String,
        title: String?,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?,
        updatedAt: Long,
        lastOpenedAt: Long,
        canvasWidth: Int,
        canvasHeight: Int,
        schemaVersion: Int
    ): Int {
        val entity = entities[designId] ?: return 0
        entities[designId] = entity.copy(
            title = title,
            thumbnailPath = thumbnailPath,
            previewPath = previewPath,
            exportedImagePath = exportedImagePath,
            updatedAt = updatedAt,
            lastOpenedAt = lastOpenedAt,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            schemaVersion = schemaVersion
        )
        emit()
        return 1
    }

    override suspend fun updateAssets(
        designId: String,
        thumbnailPath: String?,
        previewPath: String?,
        exportedImagePath: String?,
        updatedAt: Long
    ): Int {
        val entity = entities[designId] ?: return 0
        entities[designId] = entity.copy(
            thumbnailPath = thumbnailPath,
            previewPath = previewPath,
            exportedImagePath = exportedImagePath,
            updatedAt = updatedAt
        )
        emit()
        return 1
    }

    override suspend fun markDeleted(designId: String, updatedAt: Long): Int {
        val entity = entities[designId] ?: return 0
        entities[designId] = entity.copy(
            isDeleted = true,
            updatedAt = updatedAt
        )
        emit()
        return 1
    }

    private fun emit() {
        state.value = entities.values
            .filterNot { it.isDeleted }
            .sortedByDescending { it.updatedAt }
    }
}
