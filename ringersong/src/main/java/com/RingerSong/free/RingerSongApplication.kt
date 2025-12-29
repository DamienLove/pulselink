package com.RingerSong.free

import android.app.Application
import android.util.Log
import com.RingerSong.free.ads.AdServices
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class RingerSongApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdServices.initialize(this)
        try {
            YoutubeDL.getInstance().init(this)
            Thread {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel.STABLE)
                } catch (e: Exception) {
                    Log.e("RingerSong", "failed to update youtubedl-android", e)
                }
            }.start()
        } catch (e: YoutubeDLException) {
            Log.e("RingerSong", "failed to initialize youtubedl-android", e)
        }
    }
}
