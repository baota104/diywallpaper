package com.example.diywallpaper.data.wallpaper

import android.content.Context
import com.example.diywallpaper.core.result.AppError
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.model.preview.WallpaperApplySource
import com.example.diywallpaper.domain.wallpaper.WallpaperAssetResolver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class OkHttpWallpaperAssetResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : WallpaperAssetResolver {
    override suspend fun resolveStaticImage(
        source: WallpaperApplySource.StaticImage
    ): AppResult<File> = withContext(Dispatchers.IO) {
        resolveToFile(
            itemId = source.itemId,
            remoteUrl = source.imageUrl,
            localPath = source.localPath,
            extension = "img"
        )
    }

    override suspend fun resolveLiveVideo(
        source: WallpaperApplySource.LiveVideo
    ): AppResult<File> = withContext(Dispatchers.IO) {
        resolveToFile(
            itemId = source.itemId,
            remoteUrl = source.videoUrl,
            localPath = source.localPath,
            extension = "mp4"
        )
    }

    private fun resolveToFile(
        itemId: String,
        remoteUrl: String,
        localPath: String?,
        extension: String
    ): AppResult<File> {
        return runCatching {
            localPath
                ?.takeIf { it.isNotBlank() }
                ?.let(::File)
                ?.takeIf(File::exists)
                ?.let { return@runCatching it }

            val cacheDir = File(context.cacheDir, "wallpaper_assets").apply { mkdirs() }
            val targetFile = File(cacheDir, "$itemId.$extension")
            if (targetFile.exists() && targetFile.length() > 0L) {
                return@runCatching targetFile
            }

            val request = Request.Builder()
                .url(remoteUrl)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP_${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("EMPTY_BODY")
                targetFile.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
            }

            targetFile
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = {
                AppResult.Error(
                    AppError.AssetLoadError(
                        url = remoteUrl,
                        reason = it.message
                    )
                )
            }
        )
    }
}
