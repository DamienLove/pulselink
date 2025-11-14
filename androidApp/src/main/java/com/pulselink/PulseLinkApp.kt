package com.pulselink

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.data.ads.AdConfig
import com.pulselink.data.ads.AppOpenAdController
import com.pulselink.assistant.AssistantShortcuts
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PulseLinkApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appOpenAdController: AppOpenAdController
    @Inject lateinit var firebaseAuthManager: FirebaseAuthManager

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        firebaseAuthManager.ensureSignedIn()
        AssistantShortcuts.publish(this)
        if (AdConfig.isAdsEnabled) {
            MobileAds.initialize(this)
            appOpenAdController.updateAvailability(false)
        }
    }
}
