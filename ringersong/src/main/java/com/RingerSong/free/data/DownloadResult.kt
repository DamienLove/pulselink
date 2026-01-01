package com.RingerSong.free.data

sealed class DownloadResult {
    data class Success(val filePath: String) : DownloadResult()
    data class Failure(val error: DownloadError) : DownloadResult()
}
