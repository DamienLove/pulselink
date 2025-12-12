package com.pulselink.beacon

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class BeaconApp : Application() {
    override fun onCreate() {
        super.onCreate()
        maybeInitFirebase()
    }

    private fun maybeInitFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val appId = BuildConfig.FIREBASE_APP_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val senderId = BuildConfig.FIREBASE_SENDER_ID
        if (apiKey.isBlank() || appId.isBlank() || projectId.isBlank() || senderId.isBlank()) {
            Log.w(TAG, "Firebase not initialized: missing config (set via Gradle/env)")
            return
        }
        val options = FirebaseOptions.Builder()
            .setApiKey(apiKey)
            .setApplicationId(appId)
            .setProjectId(projectId)
            .setGcmSenderId(senderId)
            .build()
        FirebaseApp.initializeApp(this, options)
        Log.i(TAG, "Firebase initialized for Beacon")
    }

    companion object {
        private const val TAG = "BeaconApp"
    }
}
