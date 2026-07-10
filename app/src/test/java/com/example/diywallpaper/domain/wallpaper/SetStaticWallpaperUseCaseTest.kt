package com.example.diywallpaper.domain.wallpaper

import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.usecase.wallpaper.SetStaticWallpaperUseCase
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetStaticWallpaperUseCaseTest {
    @Test
    fun `set static wallpaper resolves asset then applies wallpaper`() = runTest {
        val file = File("wallpaper.jpg")
        val resolver = FakeWallpaperAssetResolver(AppResult.Success(file))
        val manager = FakeSystemWallpaperManager(AppResult.Success(Unit))
        val useCase = SetStaticWallpaperUseCase(resolver, manager)

        val result = useCase(
            source = WallpaperApplySource.StaticImage(
                itemId = "wall_1",
                imageUrl = "https://cdn/image.webp"
            )
        )

        assertTrue(result is AppResult.Success)
        assertEquals(file, manager.receivedFile)
    }

    @Test
    fun `set static wallpaper returns resolver error without applying`() = runTest {
        val resolver = FakeWallpaperAssetResolver(
            AppResult.Error(AppError.AssetLoadError("url", "download failed"))
        )
        val manager = FakeSystemWallpaperManager(AppResult.Success(Unit))
        val useCase = SetStaticWallpaperUseCase(resolver, manager)

        val result = useCase(
            source = WallpaperApplySource.StaticImage(
                itemId = "wall_1",
                imageUrl = "https://cdn/image.webp"
            )
        )

        assertTrue(result is AppResult.Error)
        assertEquals(null, manager.receivedFile)
    }
}

private class FakeWallpaperAssetResolver(
    private val result: AppResult<File>
) : WallpaperAssetResolver {
    override suspend fun resolveStaticImage(
        source: WallpaperApplySource.StaticImage
    ): AppResult<File> = result

    override suspend fun resolveLiveVideo(
        source: WallpaperApplySource.LiveVideo
    ): AppResult<File> = result
}

private class FakeSystemWallpaperManager(
    private val result: AppResult<Unit>
) : SystemWallpaperManager {
    var receivedFile: File? = null

    override suspend fun setStaticWallpaper(
        file: File,
        target: com.example.diywallpaper.domain.model.preview.WallpaperTarget
    ): AppResult<Unit> {
        receivedFile = file
        return result
    }
}
