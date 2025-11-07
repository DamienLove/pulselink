package com.pulselink.ui.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.pulselink.R
import com.pulselink.data.ads.AdConfig

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    if (!enabled || !AdConfig.isAdsEnabled || AdConfig.nativeAdvancedUnitId.isBlank()) {
        return
    }

    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    val adLoader = remember {
        AdLoader.Builder(context, AdConfig.nativeAdvancedUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeAd?.destroy()
                    nativeAd = null
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestCustomMuteThisAd(true)
                    .build()
            )
            .build()
    }

    LaunchedEffect(enabled, nativeAd) {
        if (enabled && nativeAd == null) {
            adLoader.loadAd(AdRequest.Builder().build())
        }
        if (!enabled && nativeAd != null) {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    DisposableEffect(nativeAd) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return

    Card(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                NativeAdView(ctx).apply {
                    LayoutInflater.from(ctx).inflate(R.layout.native_ad_view, this, true)
                    bindNativeAd(this, ad)
                }
            },
            update = { adView: NativeAdView ->
                bindNativeAd(adView, ad)
            }
        )
    }
}

private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    val headline = adView.findViewById<TextView>(R.id.ad_headline)
    val body = adView.findViewById<TextView>(R.id.ad_body)
    val callToAction = adView.findViewById<Button>(R.id.ad_call_to_action)
    val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val mediaView = adView.findViewById<MediaView>(R.id.ad_media)
    val ratingBar = adView.findViewById<RatingBar>(R.id.ad_stars)

    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = callToAction
    adView.iconView = icon
    adView.mediaView = mediaView
    adView.starRatingView = ratingBar

    headline.text = nativeAd.headline

    val bodyText = nativeAd.body
    if (bodyText.isNullOrBlank()) {
        body.visibility = View.GONE
    } else {
        body.visibility = View.VISIBLE
        body.text = bodyText
    }

    if (nativeAd.callToAction.isNullOrBlank()) {
        callToAction.visibility = View.INVISIBLE
    } else {
        callToAction.visibility = View.VISIBLE
        callToAction.text = nativeAd.callToAction
    }

    nativeAd.icon?.let { iconDrawable ->
        icon.visibility = View.VISIBLE
        icon.setImageDrawable(iconDrawable.drawable)
    } ?: run {
        icon.visibility = View.GONE
    }

    mediaView.setMediaContent(nativeAd.mediaContent)

    val starRating = nativeAd.starRating
    if (starRating != null && starRating > 0) {
        ratingBar.visibility = View.VISIBLE
        ratingBar.rating = starRating.toFloat()
    } else {
        ratingBar.visibility = View.GONE
    }

    adView.setNativeAd(nativeAd)
}
