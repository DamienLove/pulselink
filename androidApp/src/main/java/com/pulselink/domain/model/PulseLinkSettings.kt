package com.pulselink.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertProfile(
    @SerialName("sound")
    val soundKey: String? = null,
    val breakThroughDnd: Boolean = true,
    val vibrate: Boolean = true,
    val vibrationPatternKey: String = "alert_default"
)

@Serializable
data class PulseLinkSettings(
    val primaryPhrase: String = "help me pulselink",
    val secondaryPhrase: String = "check in pulselink",
    val includeLocation: Boolean = true,
    val autoAllowRemoteSoundChange: Boolean = true,
    val assistantShortcutsDismissed: Boolean = false,
    val emergencyProfile: AlertProfile = AlertProfile(),
    val checkInProfile: AlertProfile = AlertProfile(
        breakThroughDnd = false,
        vibrate = true
    ),
    val callSoundKey: String? = null,
    val messageNotificationSoundUri: String? = null,
    val messageNotificationVibrate: Boolean = true,
    val messageNotificationSoundOverrides: Map<String, String> = emptyMap(),
    val messageNotificationVibrationPattern: String = "message_default",
    val messageNotificationVibrationOverrides: Map<String, String> = emptyMap(),
    val customVibrationPatternName: String? = null,
    val customVibrationPattern: List<Long>? = null,
    val betaAgreementAccepted: Boolean = false,
    val betaAgreementVersion: String? = null,
    val autoCallAfterAlert: Boolean = false,
    val proUnlocked: Boolean = false,
    val premiumUnlocked: Boolean = false,
    val premiumPurchaseToken: String? = null,
    val tierBeforePremium: String? = null,
    val onboardingComplete: Boolean = false,
    val deviceId: String = "",
    val isBetaTester: Boolean = false,
    val ownerName: String = "",
    val ownerAvatarUrl: String? = null,
    val autoUpdateContactInfo: Boolean = true,
    val timeFormat: TimeFormat = TimeFormat.AUTO,
    val themePreferences: ThemePreferences = ThemePreferences(),
    val remoteWebAccessEnabled: Boolean = false,
    val otpCleanupEnabled: Boolean = false,
    val otpCleanupDays: Int = 1,
    val privatePinHash: String? = null,
    val privateThreadIds: List<Long> = emptyList(),
    val beaconLauncherEnabled: Boolean = false,
    val beaconHintDismissed: Boolean = false,
    val webAccessHintDismissed: Boolean = false,
    val firebaseMessagingEnabled: Boolean = true,
    val emailFallbackEnabled: Boolean = false,
    val thirdPartyExtensionsEnabled: Boolean = false,
    val mergedExperienceEnabled: Boolean = false,
    val unifiedDisplayName: String? = null,
    val messagingChannelPriority: List<MessageChannel> = listOf(MessageChannel.FIREBASE, MessageChannel.SMS, MessageChannel.EMAIL),
    val crashDetectionEnabled: Boolean = false,
    val aiSummariesEnabled: Boolean = false,
    val aiComposeEnabled: Boolean = true,
    val aiUrgencyEnabled: Boolean = true,
    val aiUrgencyBypassDnd: Boolean = false,
    val aiUrgencyIncludeUnknown: Boolean = true,
    val lineInboxMode: LineInboxMode = LineInboxMode.COMBINED,
    val lineInboxModeChosen: Boolean = false,
    val activeLineId: String? = null,
    val defaultSendLineId: String? = null,
    val lineSendPreference: LineSendPreference = LineSendPreference.LAST_USED,
    val threadLineOverrides: Map<String, String> = emptyMap(),
    val devicePhoneNumber: String? = null,
    // Extensions
    val privateSafeEnabled: Boolean = false,
    val smartRepliesEnabled: Boolean = false
) {
    fun phrases(): List<String> = listOf(primaryPhrase, secondaryPhrase)
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
}

enum class TimeFormat {
    AUTO, TWELVE_HOUR, TWENTY_FOUR_HOUR
}

@kotlinx.serialization.Serializable
data class ThemePreferences(
    val primaryColor: String = "#6750A4",
    val secondaryColor: String = "#625B71",
    val bubbleOutgoing: String = "#D0BCFF",
    val bubbleIncoming: String = "#E8DEF8",
    val backgroundColor: String = "#FFFFFF",
    val iconSizeFactor: Float = 1.0f,
    val fontStyle: String = "Default",
    val bubbleCornerRadius: Int = 12,
    val inboxIconVariant: String = "Default",
    val onBubbleOutgoing: String = "#000000",
    val onBubbleIncoming: String = "#000000",
    val onBackground: String = "#000000",
    val topBarColor: String = "#FFFFFF",
    val onTopBarColor: String = "#000000",
    val bubbleCornerRadiusTopStart: Int? = null,
    val bubbleCornerRadiusTopEnd: Int? = null,
    val bubbleCornerRadiusBottomStart: Int? = null,
    val bubbleCornerRadiusBottomEnd: Int? = null,
    val timestampColor: String? = null,
    val dividerColor: String? = null,
    val appBackgroundGradientStart: String? = null,
    val appBackgroundGradientEnd: String? = null,
    val fontScale: Float = 1.0f,
    val backgroundImageUrl: String? = null,
    val iconOverrides: Map<String, String> = emptyMap()
)
