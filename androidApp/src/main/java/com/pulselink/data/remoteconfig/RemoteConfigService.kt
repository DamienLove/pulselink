package com.pulselink.data.remoteconfig

import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.pulselink.R
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigService @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed", e)
        }
    }

    fun isExampleFeatureEnabled(): Boolean {
        return remoteConfig.getBoolean("example_feature_enabled")
    }

    companion object {
        private const val TAG = "RemoteConfigService"
    }
}
