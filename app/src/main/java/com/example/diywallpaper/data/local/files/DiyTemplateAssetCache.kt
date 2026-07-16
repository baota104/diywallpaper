package com.example.diywallpaper.data.local.files

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class DiyTemplateAssetCacheResult(
    val dataJsonFile: File,
    val assetDirectory: File?
)

@Singleton
class DiyTemplateAssetCache @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    suspend fun preload(
        templateId: String,
        diyDataUrl: String,
        dataZipUrl: String?
    ): DiyTemplateAssetCacheResult = withContext(Dispatchers.IO) {
        val templateDirectory = File(context.filesDir, "diy_templates/$templateId")
            .apply { mkdirs() }
        val dataJsonFile = File(templateDirectory, "data.json")
        val dataUrlMarker = File(templateDirectory, ".data_url")

        if (!dataJsonFile.exists() || dataUrlMarker.readTextOrNull() != diyDataUrl) {
            dataJsonFile.writeText(downloadText(diyDataUrl))
            dataUrlMarker.writeText(diyDataUrl)
        }

        val assetDirectory = dataZipUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { zipUrl ->
                val directory = File(templateDirectory, "assets")
                val marker = File(templateDirectory, ".assets_zip_url")
                if (!directory.exists() || marker.readTextOrNull() != zipUrl) {
                    runCatching {
                        directory.deleteRecursively()
                        directory.mkdirs()
                        unzip(downloadBytes(zipUrl), directory)
                        marker.writeText(zipUrl)
                    }
                }
                directory.takeIf { it.exists() }
            }

        DiyTemplateAssetCacheResult(
            dataJsonFile = dataJsonFile,
            assetDirectory = assetDirectory
        )
    }

    private fun downloadText(url: String): String {
        return executeRequest(url).use { response ->
            response.body?.string() ?: error("Empty response body: $url")
        }
    }

    private fun downloadBytes(url: String): ByteArray {
        return executeRequest(url).use { response ->
            response.body?.bytes() ?: error("Empty response body: $url")
        }
    }

    private fun executeRequest(url: String): okhttp3.Response {
        val request = Request.Builder()
            .url(url)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            error("Request failed ${response.code}: $url")
        }
        return response
    }

    private fun unzip(zipBytes: ByteArray, targetDirectory: File) {
        val canonicalTarget = targetDirectory.canonicalFile
        ZipInputStream(zipBytes.inputStream()).use { zipStream ->
            generateSequence { zipStream.nextEntry }.forEach { entry ->
                val outputFile = File(targetDirectory, entry.name).canonicalFile
                if (!outputFile.path.startsWith(canonicalTarget.path + File.separator)) {
                    error("Unsafe zip entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    outputFile.parentFile?.mkdirs()
                    FileOutputStream(outputFile).use { output ->
                        zipStream.copyTo(output)
                    }
                }
                zipStream.closeEntry()
            }
        }
    }

    private fun File.readTextOrNull(): String? {
        return runCatching { takeIf { exists() }?.readText() }.getOrNull()
    }
}
