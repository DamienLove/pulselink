package com.pulselink.feature

import com.pulselink.BuildConfig

enum class MonetizationPlan { FREE, ADS, ONE_TIME, SUBSCRIPTION, INCLUDED, UNKNOWN }

data class FeatureGate(
    val enabled: Boolean,
    val plan: MonetizationPlan
) {
    val isFree: Boolean get() = enabled && (plan == MonetizationPlan.FREE || plan == MonetizationPlan.ADS || plan == MonetizationPlan.INCLUDED)
}

object FeatureGates {
    val callerId = FeatureGate(
        enabled = true,
        plan = BuildConfig.FEATURE_CALLER_ID_PLAN.toPlan()
    )
    val actionSendTo = FeatureGate(
        enabled = true,
        plan = BuildConfig.FEATURE_ACTION_SENDTO_PLAN.toPlan()
    )
    val escalation = FeatureGate(
        enabled = true,
        plan = BuildConfig.FEATURE_ESCALATION_PLAN.toPlan()
    )
    val aiSummary = FeatureGate(
        enabled = true,
        plan = BuildConfig.FEATURE_AI_SUMMARY_PLAN.toPlan()
    )
    val emergencyWidget = FeatureGate(
        enabled = true,
        plan = BuildConfig.FEATURE_EMERGENCY_WIDGET_PLAN.toPlan()
    )
}

private fun String.toPlan(): MonetizationPlan = when (lowercase()) {
    "free" -> MonetizationPlan.FREE
    "ads" -> MonetizationPlan.ADS
    "one_time", "one-time" -> MonetizationPlan.ONE_TIME
    "subscription", "sub" -> MonetizationPlan.SUBSCRIPTION
    "included" -> MonetizationPlan.INCLUDED
    else -> MonetizationPlan.UNKNOWN
}
