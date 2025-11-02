package com.pulselink.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.pulselink.data.ads.AdConfig

@Composable
fun BannerAdSlot(
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    if (!enabled || !AdConfig.isAdsEnabled || AdConfig.bannerUnitId.isBlank()) {
        return
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdConfig.bannerUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
