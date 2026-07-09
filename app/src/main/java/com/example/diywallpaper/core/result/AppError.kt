package com.example.diywallpaper.core.result

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object Timeout : AppError
    data object EmptyResponse : AppError

    data class HttpError(val code: Int, val message: String?) : AppError
    data class JsonParseError(val source: String, val reason: String?) : AppError
    data class InvalidDataContract(
        val source: String,
        val field: String?,
        val reason: String
    ) : AppError
    data class AssetLoadError(val url: String, val reason: String?) : AppError
    data class VideoLoadError(val url: String, val reason: String?) : AppError
    data class ExportError(val reason: String?) : AppError
    data class StorageError(val reason: String?) : AppError
    data class Unknown(val throwable: Throwable?) : AppError
}
