package com.pulselink.ui.branding

import com.pulselink.R

/**
 * Returns the appropriate logo based on whether premium/pro branding should be shown.
 * Gold logo is used for Pro builds and for users with an active premium or pro unlock.
 */
fun brandLogoRes(usePremiumBranding: Boolean): Int =
    if (usePremiumBranding) R.drawable.ic_pulselink_pro else R.drawable.ic_logo

fun pulseBrandName(isPremium: Boolean, isPro: Boolean): String = when {
    isPremium -> "PulseLink Premium"
    isPro -> "PulseLink Pro"
    else -> "PulseLink"
}

fun beaconBrandName(isPremium: Boolean, isPro: Boolean): String = when {
    isPremium -> "Beacon Premium"
    isPro -> "Beacon Pro"
    else -> "Beacon"
}
