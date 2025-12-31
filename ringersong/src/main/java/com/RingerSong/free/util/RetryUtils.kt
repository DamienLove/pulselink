package com.RingerSong.free.util

import com.RingerSong.free.data.DownloadError
import com.RingerSong.free.data.DownloadResult
import kotlinx.coroutines.delay

object RetryUtils {
    suspend fun retryDownload(
        attempts: Int = 2,
        delayMs: Long = 2000,
        block: suspend () -> DownloadResult
    ): DownloadResult {
        repeat(attempts) { attemptIndex ->
            val result = block()
            when (result) {
                is DownloadResult.Success -> return result
                is DownloadResult.Failure -> {
                    val shouldRetry = when (result.error) {
                        is DownloadError.ApiError,
                        is DownloadError.AudioDownloadFailed -> true
                        else -> false
                    }
                    if (!shouldRetry || attemptIndex == attempts - 1) {
                        return result
                    }
                    delay(delayMs)
                }
            }
        }
        // Fallback, though loop should always return.
        return block()
    }
}
