package com.pulselink.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var hasRequestedAd by remember(adView) { mutableStateOf(false) }

    LaunchedEffect(adView, hasRequestedAd) {
        if (!hasRequestedAd) {
            adView.loadAd(AdRequest.Builder().build())
            hasRequestedAd = true
        }
    }

    DisposableEffect(lifecycleOwner, adView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                adView.resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                adView.pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                adView.destroy()
            }
        }
        val listener = object : AdListener() {
            override fun onAdFailedToLoad(error: LoadAdError) {
                hasRequestedAd = false
            }
        }
        adView.adListener = listener
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            adView.adListener = object : AdListener() {}
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { adView }
    )
}
