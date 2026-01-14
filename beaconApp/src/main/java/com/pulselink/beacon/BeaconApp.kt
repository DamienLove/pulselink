package com.pulselink.beacon

import android.app.Application
import com.google.android.gms.ads.MobileAds
import androidx.lifecycle.ProcessLifecycleOwner
import com.pulselink.beacon.data.SmsSyncManager

class BeaconApp : Application() {
    private lateinit var appOpenManager: AppOpenAdManager
    lateinit var smsSyncManager: SmsSyncManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        appOpenManager = AppOpenAdManager(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenManager)

        smsSyncManager = SmsSyncManager.getInstance(this)
        smsSyncManager.startMonitoring()
    }
}
