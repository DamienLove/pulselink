package com.pulselink.ui.branding

import com.pulselink.R

/**
 * Returns the appropriate logo based on whether premium/pro branding should be shown.
 * Gold logo is used for Pro builds and for users with an active premium unlock.
 */
fun brandLogoRes(usePremiumBranding: Boolean): Int =
    if (usePremiumBranding) R.drawable.ic_pulselink_pro else R.drawable.ic_logo
