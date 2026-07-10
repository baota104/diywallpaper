package com.example.diywallpaper.domain.wallpaper

import android.content.Intent
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
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
}

private class FakeLiveWallpaperSourceStore : LiveWallpaperSourceStore {
    var savedPath: String? = null

    override suspend fun saveVideoPath(path: String): AppResult<Unit> {
        savedPath = path
        return AppResult.Success(Unit)
    }

    override fun getVideoPath(): String? = savedPath
}
