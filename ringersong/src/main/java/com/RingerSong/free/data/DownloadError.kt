package com.RingerSong.free.data

/**
 * Structured error types for download operations.
 */
sealed class DownloadError {
    data class NoApiKey(val message: String = "RapidAPI key not configured") : DownloadError()
    object NetworkUnavailable : DownloadError()
    data class ApiError(val statusCode: Int, val message: String? = null) : DownloadError()
    data class RateLimitExceeded(val message: String? = null) : DownloadError()
    data class NoDownloadUrl(val message: String? = null) : DownloadError()
    data class AudioDownloadFailed(val statusCode: Int? = null, val message: String? = null) : DownloadError()
    data class UnknownError(val message: String? = null) : DownloadError()
}
