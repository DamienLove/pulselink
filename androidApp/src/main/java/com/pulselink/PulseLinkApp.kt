package com.pulselink

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.firebase.FirebaseApp
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.data.ads.AdConfig
import com.pulselink.data.ads.AppOpenAdController
import com.pulselink.assistant.AssistantShortcuts
import com.pulselink.data.remoteconfig.RemoteConfigService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class PulseLinkApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var appOpenAdController: AppOpenAdController
    @Inject lateinit var firebaseAuthManager: FirebaseAuthManager
    @Inject lateinit var remoteConfigService: RemoteConfigService
    @Inject lateinit var smsRelayService: com.pulselink.data.sms.SmsRelayService

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Touch auth early so listeners register before any UI state checks
        firebaseAuthManager.currentUser()
        smsRelayService.start()
        CoroutineScope(Dispatchers.IO).launch {
            remoteConfigService.fetchAndActivate()
        }
        AssistantShortcuts.publish(this)
        if (AdConfig.isAdsEnabled) {
            Thread {
                MobileAds.initialize(this) { initializationStatus ->
                    logAdapterStatus(initializationStatus)
                }
            }.start()
            appOpenAdController.updateAvailability(false)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequest.Builder(
            com.pulselink.data.sms.SmsSyncWorker::class.java,
            15,
            java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SmsSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        val otpCleanupRequest = PeriodicWorkRequest.Builder(
            com.pulselink.data.sms.OtpCleanupWorker::class.java,
            24,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OtpCleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            otpCleanupRequest
        )
    }

    private fun logAdapterStatus(initializationStatus: InitializationStatus) {
        val statusMap = initializationStatus.adapterStatusMap
        for ((adapterClass, status) in statusMap) {
            Log.d(
                "AdMob",
                "Adapter: $adapterClass, Status: ${status.description}, Latency: ${status.latency}"
            )
        }
    }
}
