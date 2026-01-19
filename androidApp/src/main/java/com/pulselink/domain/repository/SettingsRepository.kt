package com.pulselink.domain.repository

import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.domain.model.MessageChannel
import com.pulselink.domain.model.PulseLinkSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PulseLinkSettings>
    suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings)
    suspend fun setProUnlocked(enabled: Boolean)
    suspend fun setPremiumUnlocked(enabled: Boolean)
    suspend fun setPremiumPurchaseToken(token: String?)
    suspend fun setTierBeforePremium(tier: String?)
    suspend fun setOnboardingComplete()
    suspend fun ensureDeviceId(): String
    suspend fun setOwnerName(name: String)
    suspend fun setAutoAllowRemoteSoundChange(enabled: Boolean)
    suspend fun setBetaTesterStatus(enabled: Boolean)
    suspend fun setAssistantShortcutsDismissed(dismissed: Boolean)
    suspend fun setBetaAgreementAcceptance(version: String)
    suspend fun getLastWidgetCheckTimestamp(): Long
    suspend fun updateLastWidgetCheckTimestamp(timestamp: Long)
    suspend fun setEmergencyActive(active: Boolean)
    suspend fun isEmergencyActive(): Boolean
    suspend fun setLastKnownPhone(phone: String?)
    suspend fun getLastKnownPhone(): String?
    suspend fun setLastKnownEmail(email: String?)
    suspend fun getLastKnownEmail(): String?
    suspend fun setAutoUpdateContactInfo(enabled: Boolean)
    suspend fun setTimeFormat(format: com.pulselink.domain.model.TimeFormat)
    suspend fun setThemePreferences(theme: com.pulselink.domain.model.ThemePreferences)
    suspend fun setRemoteWebAccessEnabled(enabled: Boolean)
    suspend fun setOtpCleanupEnabled(enabled: Boolean)
    suspend fun setOtpCleanupDays(days: Int)
    suspend fun setMergedExperienceEnabled(enabled: Boolean)
    suspend fun setThirdPartyExtensionsEnabled(enabled: Boolean)
    suspend fun setTruecallerEnabled(enabled: Boolean)
    suspend fun setPrivatePinHash(hash: String?)
    suspend fun setPrivateThreads(threadIds: List<Long>)
    suspend fun setBeaconLauncherEnabled(enabled: Boolean)
    suspend fun setBeaconHintDismissed(dismissed: Boolean)
    suspend fun setWebAccessHintDismissed(dismissed: Boolean)
    suspend fun setFirebaseMessagingEnabled(enabled: Boolean)
    suspend fun setEmailFallbackEnabled(enabled: Boolean)
    suspend fun setMessagingChannelPriority(priority: List<MessageChannel>)
    suspend fun setCrashDetectionEnabled(enabled: Boolean)
    suspend fun setAiSummariesEnabled(enabled: Boolean)
    suspend fun setAiComposeEnabled(enabled: Boolean)
    suspend fun setAiUrgencyEnabled(enabled: Boolean)
    suspend fun setAiUrgencyBypassDnd(enabled: Boolean)
    suspend fun setAiUrgencyIncludeUnknown(enabled: Boolean)
    suspend fun setLineInboxMode(mode: LineInboxMode, chosen: Boolean = true)
    suspend fun setActiveLineId(lineId: String?)
    suspend fun setDefaultSendLineId(lineId: String?)
    suspend fun setLineSendPreference(preference: LineSendPreference)
    suspend fun setThreadLineOverrides(overrides: Map<String, String>)
    suspend fun setThreadLineOverride(threadKey: String, lineId: String?)
    suspend fun setDevicePhoneNumber(phone: String?)
    suspend fun setCustomVibrationPattern(name: String, pattern: List<Long>)
    suspend fun clearCustomVibrationPattern()
}
