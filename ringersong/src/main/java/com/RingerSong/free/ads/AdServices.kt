package com.RingerSong.free.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import java.util.concurrent.TimeUnit

object AdServices {
    @Volatile
    private var initialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var appOpenAdManager: AppOpenAdManager? = null

    fun initialize(app: Application) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(app)
        appOpenAdManager = AppOpenAdManager(app).also { manager ->
            ProcessLifecycleOwner.get().lifecycle.addObserver(manager)
            manager.loadAd()
        }
        preloadInterstitial(app)
    }

    fun preloadInterstitial(context: Context) {
        if (isInterstitialLoading || interstitialAd != null) return
        isInterstitialLoading = true
        InterstitialAd.load(
            context,
            AdsConfig.INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: (() -> Unit)? = null) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity)
            onDismissed?.invoke()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preloadInterstitial(activity)
                onDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                preloadInterstitial(activity)
                onDismissed?.invoke()
            }
        }
        ad.show(activity)
    }

    private class AppOpenAdManager(private val app: Application) : DefaultLifecycleObserver {
        private var appOpenAd: AppOpenAd? = null
        private var isLoading = false
        private var isShowing = false
        private var loadTime = 0L

        override fun onStart(owner: LifecycleOwner) {
            showAdIfAvailable()
        }

        fun loadAd() {
            if (isLoading || isAdAvailable()) return
            isLoading = true
            AppOpenAd.load(
                app,
                AdsConfig.APP_OPEN_ID,
                AdRequest.Builder().build(),
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoading = false
                        loadTime = System.currentTimeMillis()
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        appOpenAd = null
                        isLoading = false
                    }
                }
            )
        }

        private fun showAdIfAvailable() {
            if (isShowing) return
            if (!isAdAvailable()) {
                loadAd()
                return
            }
            val activity = app.currentActivity() ?: return
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowing = false
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowing = false
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowing = true
                }
            }
            appOpenAd?.show(activity)
        }

        private fun isAdAvailable(): Boolean {
            val age = System.currentTimeMillis() - loadTime
            val fresh = age < TimeUnit.HOURS.toMillis(4)
            return appOpenAd != null && fresh
        }
    }
}

private fun Application.currentActivity(): Activity? {
    return CurrentActivityHolder.activity
}

internal object CurrentActivityHolder {
    @Volatile
    var activity: Activity? = null
}
