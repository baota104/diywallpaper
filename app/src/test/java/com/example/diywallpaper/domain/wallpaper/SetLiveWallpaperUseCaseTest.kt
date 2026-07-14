package com.example.diywallpaper.domain.wallpaper

import android.content.Intent
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.design.EditorBackground
import com.example.diywallpaper.domain.model.design.EditorCanvasSpec
import com.example.diywallpaper.domain.model.design.EditorProject
import com.example.diywallpaper.domain.model.design.EditorProjectSource
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.repository.DesignVideoExporter
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveDesignWallpaperUseCase
import com.example.diywallpaper.domain.usecase.wallpaper.SetLiveWallpaperUseCase
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetLiveWallpaperUseCaseTest {
    @Test
    fun `set live wallpaper resolves video stores source and returns launch intent`() = runTest {
        val file = File("live.mp4")
        val store = FakeLiveWallpaperSourceStore()
        val launcherIntent = mockk<Intent>(relaxed = true).also {
            every { it.action } returns "live.intent"
        }
        val useCase = SetLiveWallpaperUseCase(
            wallpaperAssetResolver = object : WallpaperAssetResolver {
                override suspend fun resolveStaticImage(source: WallpaperApplySource.StaticImage) =
                    AppResult.Error(AppError.EmptyResponse)

                override suspend fun resolveLiveVideo(source: WallpaperApplySource.LiveVideo) =
                    AppResult.Success(file)
            },
            liveWallpaperSourceStore = store,
            liveWallpaperLauncher = object : LiveWallpaperLauncher {
                override fun createLaunchIntent(): AppResult<Intent> = AppResult.Success(launcherIntent)
            }
        )

        val result = useCase(
            WallpaperApplySource.LiveVideo(
                itemId = "live_1",
                videoUrl = "https://cdn/live.mp4"
            )
        )

        assertTrue(result is AppResult.Success)
        assertEquals(file.absolutePath, store.savedPath)
        assertEquals("live.intent", (result as AppResult.Success).data.action)
    }

    @Test
    fun `set live design wallpaper exports video then reuses video live wallpaper flow`() = runTest {
        val store = FakeLiveWallpaperSourceStore()
        val launcherIntent = mockk<Intent>(relaxed = true).also {
            every { it.action } returns "live.design.intent"
        }
        val setLiveWallpaperUseCase = SetLiveWallpaperUseCase(
            wallpaperAssetResolver = object : WallpaperAssetResolver {
                override suspend fun resolveStaticImage(source: WallpaperApplySource.StaticImage) =
                    AppResult.Error(AppError.EmptyResponse)

                override suspend fun resolveLiveVideo(source: WallpaperApplySource.LiveVideo) =
                    AppResult.Success(File(source.localPath.orEmpty()))
            },
            liveWallpaperSourceStore = store,
            liveWallpaperLauncher = object : LiveWallpaperLauncher {
                override fun createLaunchIntent(): AppResult<Intent> = AppResult.Success(launcherIntent)
            }
        )
        val useCase = SetLiveDesignWallpaperUseCase(
            designVideoExporter = object : DesignVideoExporter {
                override suspend fun export(project: EditorProject): AppResult<String> {
                    return AppResult.Success("/tmp/${project.id}.mp4")
                }
            },
            setLiveWallpaperUseCase = setLiveWallpaperUseCase
        )

        val result = useCase(sampleProject("design_animated"))

        assertTrue(result is AppResult.Success)
        assertEquals(File("/tmp/design_animated.mp4").absolutePath, store.savedPath)
        assertEquals("live.design.intent", (result as AppResult.Success).data.action)
    }

    private fun sampleProject(id: String): EditorProject {
        return EditorProject(
            id = id,
            source = EditorProjectSource.Scratch,
            canvas = EditorCanvasSpec(1080, 1920),
            background = EditorBackground.SolidColor("#FFFFFF"),
            layers = emptyList(),
            placeholders = emptyList(),
            selectedLayerId = null,
            createdAt = 1L,
            updatedAt = 2L,
            schemaVersion = 1
        )
    }
}

private class FakeLiveWallpaperSourceStore : LiveWallpaperSourceStore {
    var savedPath: String? = null

    override suspend fun saveVideoPath(path: String): AppResult<Unit> {
        savedPath = path
        return AppResult.Success(Unit)
    }

    override fun getVideoPath(): String? = savedPath
}
