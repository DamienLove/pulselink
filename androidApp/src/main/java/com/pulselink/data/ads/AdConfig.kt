package com.pulselink.data.ads

import com.pulselink.BuildConfig

/**
 * Centralizes AdMob configuration derived from build variants.
 */
object AdConfig {
    val isAdsEnabled: Boolean
        get() = BuildConfig.ADS_ENABLED && BuildConfig.AD_APP_ID.isNotBlank()

    val appId: String
        get() = BuildConfig.AD_APP_ID

    val bannerUnitId: String
        get() = BuildConfig.AD_UNIT_BANNER

    val interstitialUnitId: String
        get() = BuildConfig.AD_UNIT_INTERSTITIAL

    val rewardedInterstitialUnitId: String
        get() = BuildConfig.AD_UNIT_REWARDED_INTERSTITIAL

    val nativeAdvancedUnitId: String
        get() = BuildConfig.AD_UNIT_NATIVE_ADVANCED

    val appOpenUnitId: String
        get() = BuildConfig.AD_UNIT_APP_OPEN
}
