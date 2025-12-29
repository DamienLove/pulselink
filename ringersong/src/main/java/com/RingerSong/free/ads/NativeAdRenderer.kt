package com.RingerSong.free.ads

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.RingerSong.free.R
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    val headlineView = adView.findViewById<TextView>(R.id.ad_headline)
    val bodyView = adView.findViewById<TextView>(R.id.ad_body)
    val ctaView = adView.findViewById<Button>(R.id.ad_call_to_action)
    val iconView = adView.findViewById<ImageView>(R.id.ad_app_icon)

    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = ctaView
    adView.iconView = iconView

    headlineView.text = nativeAd.headline

    if (nativeAd.body.isNullOrBlank()) {
        bodyView.visibility = View.GONE
    } else {
        bodyView.visibility = View.VISIBLE
        bodyView.text = nativeAd.body
    }

    if (nativeAd.callToAction.isNullOrBlank()) {
        ctaView.visibility = View.INVISIBLE
    } else {
        ctaView.visibility = View.VISIBLE
        ctaView.text = nativeAd.callToAction
    }

    val icon = nativeAd.icon
    if (icon == null) {
        iconView.visibility = View.GONE
    } else {
        iconView.visibility = View.VISIBLE
        iconView.setImageDrawable(icon.drawable)
    }

    adView.setNativeAd(nativeAd)
}
