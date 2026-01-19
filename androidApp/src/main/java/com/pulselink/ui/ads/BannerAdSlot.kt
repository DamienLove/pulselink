package com.pulselink.ui.ads

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.pulselink.data.ads.AdConfig
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BannerAdSlot(
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    if (!enabled || !AdConfig.isAdsEnabled || AdConfig.bannerUnitId.isBlank()) {
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adUnitId = AdConfig.bannerUnitId

    val adView = remember(adUnitId) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
        }
    }

    val requestGate = remember(adView) { AtomicBoolean(false) }

    fun requestAdIfNeeded() {
        if (!adView.isAttachedToWindow) return
        if (requestGate.compareAndSet(false, true)) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                requestAdIfNeeded()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (adView.isAttachedToWindow) {
                    adView.resume()
                }
                requestAdIfNeeded()
            }

            override fun onPause(owner: LifecycleOwner) {
                adView.pause()
            }
        }
        val listener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                requestGate.set(false)
            }
        }
        adView.adListener = listener
        adView.addOnAttachStateChangeListener(attachListener)
        lifecycleOwner.lifecycle.addObserver(observer)
        requestAdIfNeeded()
        onDispose {
            adView.removeOnAttachStateChangeListener(attachListener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.adListener = object : AdListener() {}
            adView.pause()
            adView.removeAllViews()
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )
}
