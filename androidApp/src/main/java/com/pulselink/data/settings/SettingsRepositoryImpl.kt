package com.pulselink.data.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.domain.model.MessageChannel
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID
import com.pulselink.util.BeaconIconManager

private const val DATA_STORE_NAME = "pulselink_settings"

private val PRIMARY_PHRASE = stringPreferencesKey("primary_phrase")
private val SECONDARY_PHRASE = stringPreferencesKey("secondary_phrase")
private val INCLUDE_LOCATION = booleanPreferencesKey("include_location")
private val AUTO_ALLOW_REMOTE_SOUND = booleanPreferencesKey("auto_allow_remote_sound")
private val ASSISTANT_HINT_DISMISSED = booleanPreferencesKey("assistant_hint_dismissed")
private val EMERGENCY_PROFILE = stringPreferencesKey("emergency_profile")
private val CHECKIN_PROFILE = stringPreferencesKey("checkin_profile")
private val CALL_SOUND = stringPreferencesKey("call_sound")
private val MESSAGE_NOTIFICATION_SOUND = stringPreferencesKey("message_notification_sound")
private val MESSAGE_NOTIFICATION_VIBRATE = booleanPreferencesKey("message_notification_vibrate")
private val MESSAGE_NOTIFICATION_SOUND_OVERRIDES = stringPreferencesKey("message_notification_sound_overrides")
private val MESSAGE_NOTIFICATION_VIBRATION_PATTERN = stringPreferencesKey("message_notification_vibration_pattern")
private val MESSAGE_NOTIFICATION_VIBRATION_OVERRIDES = stringPreferencesKey("message_notification_vibration_overrides")
private val BETA_AGREEMENT_ACCEPTED = booleanPreferencesKey("beta_agreement_accepted")
private val BETA_AGREEMENT_VERSION = stringPreferencesKey("beta_agreement_version")
private val AUTO_CALL = booleanPreferencesKey("auto_call")
private val PRO_UNLOCKED = booleanPreferencesKey("pro_unlocked")
private val PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
private val PREMIUM_PURCHASE_TOKEN = stringPreferencesKey("premium_purchase_token")
private val TIER_BEFORE_PREMIUM = stringPreferencesKey("tier_before_premium")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
private val DEVICE_ID = stringPreferencesKey("device_id")
private val OWNER_NAME = stringPreferencesKey("owner_name")
private val IS_BETA_TESTER = booleanPreferencesKey("is_beta_tester")
private val WIDGET_LAST_CHECK_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("widget_last_check_timestamp")
private val EMERGENCY_ACTIVE = booleanPreferencesKey("emergency_active")
private val LAST_KNOWN_PHONE = stringPreferencesKey("last_known_phone")
private val LAST_KNOWN_EMAIL = stringPreferencesKey("last_known_email")
private val AUTO_UPDATE_CONTACT_INFO = booleanPreferencesKey("auto_update_contact_info")
private val TIME_FORMAT = stringPreferencesKey("time_format")
private val THEME_PREFERENCES = stringPreferencesKey("theme_preferences")
private val REMOTE_WEB_ACCESS = booleanPreferencesKey("remote_web_access")
private val OTP_CLEANUP_ENABLED = booleanPreferencesKey("otp_cleanup_enabled")
private val OTP_CLEANUP_DAYS = intPreferencesKey("otp_cleanup_days")
private val PRIVATE_PIN_HASH = stringPreferencesKey("private_pin_hash")
private val PRIVATE_THREADS = stringPreferencesKey("private_threads")
private val BEACON_LAUNCHER_ENABLED = booleanPreferencesKey("beacon_launcher_enabled")
private val BEACON_HINT_DISMISSED = booleanPreferencesKey("beacon_hint_dismissed")
private val WEB_ACCESS_HINT_DISMISSED = booleanPreferencesKey("web_access_hint_dismissed")
private val FIREBASE_MESSAGING_ENABLED = booleanPreferencesKey("firebase_messaging_enabled")
private val EMAIL_FALLBACK_ENABLED = booleanPreferencesKey("email_fallback_enabled")
private val MESSAGING_CHANNEL_PRIORITY = stringPreferencesKey("messaging_channel_priority")
private val CRASH_DETECTION_ENABLED = booleanPreferencesKey("crash_detection_enabled")
private val AI_SUMMARIES_ENABLED = booleanPreferencesKey("ai_summaries_enabled")
private val AI_COMPOSE_ENABLED = booleanPreferencesKey("ai_compose_enabled")
private val AI_URGENCY_ENABLED = booleanPreferencesKey("ai_urgency_enabled")
private val AI_URGENCY_BYPASS_DND = booleanPreferencesKey("ai_urgency_bypass_dnd")
private val AI_URGENCY_INCLUDE_UNKNOWN = booleanPreferencesKey("ai_urgency_include_unknown")
private val LINE_INBOX_MODE = stringPreferencesKey("line_inbox_mode")
private val LINE_INBOX_MODE_CHOSEN = booleanPreferencesKey("line_inbox_mode_chosen")
private val ACTIVE_LINE_ID = stringPreferencesKey("active_line_id")
private val DEFAULT_SEND_LINE_ID = stringPreferencesKey("default_send_line_id")
private val LINE_SEND_PREFERENCE = stringPreferencesKey("line_send_preference")
private val THREAD_LINE_OVERRIDES = stringPreferencesKey("thread_line_overrides")
private val DEVICE_PHONE_NUMBER = stringPreferencesKey("device_phone_number")

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {
    private suspend fun editOnIo(block: suspend (MutablePreferences) -> Unit) {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs -> block(prefs) }
        }
    }

    private suspend fun <T> decodeJsonOrNull(raw: String?, decoder: (String) -> T): T? {
        if (raw.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching { decoder(raw) }.getOrNull()
        }
    }

    private suspend fun encodeJson(encoder: () -> String): String {
        return withContext(Dispatchers.IO) { encoder() }
    }

    override val settings: Flow<PulseLinkSettings> = dataStore.data.map { prefs ->
        PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            autoAllowRemoteSoundChange = prefs[AUTO_ALLOW_REMOTE_SOUND]
                ?: PulseLinkSettings().autoAllowRemoteSoundChange,
            assistantShortcutsDismissed = prefs[ASSISTANT_HINT_DISMISSED]
                ?: PulseLinkSettings().assistantShortcutsDismissed,
            emergencyProfile = decodeJsonOrNull(prefs[EMERGENCY_PROFILE]) {
                json.decodeFromString(AlertProfile.serializer(), it)
            }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = decodeJsonOrNull(prefs[CHECKIN_PROFILE]) {
                json.decodeFromString(AlertProfile.serializer(), it)
            }
                ?: PulseLinkSettings().checkInProfile,
            callSoundKey = prefs[CALL_SOUND] ?: PulseLinkSettings().callSoundKey,
            messageNotificationSoundUri = prefs[MESSAGE_NOTIFICATION_SOUND]
                ?: PulseLinkSettings().messageNotificationSoundUri,
            messageNotificationVibrate = prefs[MESSAGE_NOTIFICATION_VIBRATE]
                ?: PulseLinkSettings().messageNotificationVibrate,
            messageNotificationSoundOverrides = decodeJsonOrNull(
                prefs[MESSAGE_NOTIFICATION_SOUND_OVERRIDES]
            ) { json.decodeFromString<Map<String, String>>(it) }
                ?: PulseLinkSettings().messageNotificationSoundOverrides,
            messageNotificationVibrationPattern = prefs[MESSAGE_NOTIFICATION_VIBRATION_PATTERN]
                ?: PulseLinkSettings().messageNotificationVibrationPattern,
            messageNotificationVibrationOverrides = decodeJsonOrNull(
                prefs[MESSAGE_NOTIFICATION_VIBRATION_OVERRIDES]
            ) { json.decodeFromString<Map<String, String>>(it) }
                ?: PulseLinkSettings().messageNotificationVibrationOverrides,
            betaAgreementAccepted = prefs[BETA_AGREEMENT_ACCEPTED] ?: PulseLinkSettings().betaAgreementAccepted,
            betaAgreementVersion = prefs[BETA_AGREEMENT_VERSION] ?: PulseLinkSettings().betaAgreementVersion,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            premiumUnlocked = prefs[PREMIUM_UNLOCKED] ?: PulseLinkSettings().premiumUnlocked,
            premiumPurchaseToken = prefs[PREMIUM_PURCHASE_TOKEN],
            tierBeforePremium = prefs[TIER_BEFORE_PREMIUM],
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId,
            ownerName = prefs[OWNER_NAME] ?: PulseLinkSettings().ownerName,
            isBetaTester = prefs[IS_BETA_TESTER] ?: PulseLinkSettings().isBetaTester,
            autoUpdateContactInfo = prefs[AUTO_UPDATE_CONTACT_INFO] ?: PulseLinkSettings().autoUpdateContactInfo,
            timeFormat = prefs[TIME_FORMAT]?.let { runCatching { com.pulselink.domain.model.TimeFormat.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().timeFormat,
            themePreferences = decodeJsonOrNull(prefs[THEME_PREFERENCES]) {
                json.decodeFromString(com.pulselink.domain.model.ThemePreferences.serializer(), it)
            } ?: PulseLinkSettings().themePreferences,
            remoteWebAccessEnabled = prefs[REMOTE_WEB_ACCESS] ?: PulseLinkSettings().remoteWebAccessEnabled,
            otpCleanupEnabled = prefs[OTP_CLEANUP_ENABLED] ?: PulseLinkSettings().otpCleanupEnabled,
            otpCleanupDays = prefs[OTP_CLEANUP_DAYS] ?: PulseLinkSettings().otpCleanupDays,
            privatePinHash = prefs[PRIVATE_PIN_HASH],
            privateThreadIds = decodeJsonOrNull(prefs[PRIVATE_THREADS]) {
                json.decodeFromString<List<Long>>(it)
            } ?: PulseLinkSettings().privateThreadIds,
            beaconLauncherEnabled = prefs[BEACON_LAUNCHER_ENABLED] ?: PulseLinkSettings().beaconLauncherEnabled,
            beaconHintDismissed = prefs[BEACON_HINT_DISMISSED] ?: PulseLinkSettings().beaconHintDismissed,
            webAccessHintDismissed = prefs[WEB_ACCESS_HINT_DISMISSED] ?: PulseLinkSettings().webAccessHintDismissed,
            firebaseMessagingEnabled = prefs[FIREBASE_MESSAGING_ENABLED] ?: PulseLinkSettings().firebaseMessagingEnabled,
            emailFallbackEnabled = prefs[EMAIL_FALLBACK_ENABLED] ?: PulseLinkSettings().emailFallbackEnabled,
            messagingChannelPriority = decodeJsonOrNull(prefs[MESSAGING_CHANNEL_PRIORITY]) {
                json.decodeFromString<List<MessageChannel>>(it)
            } ?: PulseLinkSettings().messagingChannelPriority,
            crashDetectionEnabled = prefs[CRASH_DETECTION_ENABLED] ?: PulseLinkSettings().crashDetectionEnabled,
            aiSummariesEnabled = prefs[AI_SUMMARIES_ENABLED] ?: PulseLinkSettings().aiSummariesEnabled,
            aiComposeEnabled = prefs[AI_COMPOSE_ENABLED] ?: PulseLinkSettings().aiComposeEnabled,
            aiUrgencyEnabled = prefs[AI_URGENCY_ENABLED] ?: PulseLinkSettings().aiUrgencyEnabled,
            aiUrgencyBypassDnd = prefs[AI_URGENCY_BYPASS_DND] ?: PulseLinkSettings().aiUrgencyBypassDnd,
            aiUrgencyIncludeUnknown = prefs[AI_URGENCY_INCLUDE_UNKNOWN]
                ?: PulseLinkSettings().aiUrgencyIncludeUnknown,
            lineInboxMode = prefs[LINE_INBOX_MODE]?.let { runCatching { LineInboxMode.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().lineInboxMode,
            lineInboxModeChosen = prefs[LINE_INBOX_MODE_CHOSEN] ?: PulseLinkSettings().lineInboxModeChosen,
            activeLineId = prefs[ACTIVE_LINE_ID],
            defaultSendLineId = prefs[DEFAULT_SEND_LINE_ID],
            lineSendPreference = prefs[LINE_SEND_PREFERENCE]?.let {
                runCatching { LineSendPreference.valueOf(it) }.getOrNull()
            } ?: PulseLinkSettings().lineSendPreference,
            threadLineOverrides = decodeJsonOrNull(prefs[THREAD_LINE_OVERRIDES]) {
                json.decodeFromString<Map<String, String>>(it)
            } ?: PulseLinkSettings().threadLineOverrides,
            devicePhoneNumber = prefs[DEVICE_PHONE_NUMBER]
        )
    }

    override suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings) {
        editOnIo { prefs ->
            val current = settingsValue(prefs)
            val updated = transform(current)
            prefs[PRIMARY_PHRASE] = updated.primaryPhrase
            prefs[SECONDARY_PHRASE] = updated.secondaryPhrase
            prefs[INCLUDE_LOCATION] = updated.includeLocation
            prefs[AUTO_ALLOW_REMOTE_SOUND] = updated.autoAllowRemoteSoundChange
            prefs[ASSISTANT_HINT_DISMISSED] = updated.assistantShortcutsDismissed
            prefs[EMERGENCY_PROFILE] = encodeJson {
                json.encodeToString(AlertProfile.serializer(), updated.emergencyProfile)
            }
            prefs[CHECKIN_PROFILE] = encodeJson {
                json.encodeToString(AlertProfile.serializer(), updated.checkInProfile)
            }
            updated.callSoundKey?.let { prefs[CALL_SOUND] = it } ?: prefs.remove(CALL_SOUND)
            updated.messageNotificationSoundUri?.let { prefs[MESSAGE_NOTIFICATION_SOUND] = it }
                ?: prefs.remove(MESSAGE_NOTIFICATION_SOUND)
            prefs[MESSAGE_NOTIFICATION_VIBRATE] = updated.messageNotificationVibrate
            prefs[MESSAGE_NOTIFICATION_SOUND_OVERRIDES] = encodeJson {
                json.encodeToString(updated.messageNotificationSoundOverrides)
            }
            prefs[MESSAGE_NOTIFICATION_VIBRATION_PATTERN] =
                updated.messageNotificationVibrationPattern
            prefs[MESSAGE_NOTIFICATION_VIBRATION_OVERRIDES] = encodeJson {
                json.encodeToString(updated.messageNotificationVibrationOverrides)
            }
            prefs[BETA_AGREEMENT_ACCEPTED] = updated.betaAgreementAccepted
            updated.betaAgreementVersion?.let { prefs[BETA_AGREEMENT_VERSION] = it } ?: prefs.remove(BETA_AGREEMENT_VERSION)
            prefs[AUTO_CALL] = updated.autoCallAfterAlert
            prefs[PRO_UNLOCKED] = updated.proUnlocked
            prefs[PREMIUM_UNLOCKED] = updated.premiumUnlocked
            prefs[ONBOARDING_COMPLETE] = updated.onboardingComplete
            prefs[DEVICE_ID] = updated.deviceId
            prefs[OWNER_NAME] = updated.ownerName
            prefs[IS_BETA_TESTER] = updated.isBetaTester
            prefs[AUTO_UPDATE_CONTACT_INFO] = updated.autoUpdateContactInfo
            prefs[TIME_FORMAT] = updated.timeFormat.name
            prefs[THEME_PREFERENCES] = encodeJson {
                json.encodeToString(
                    com.pulselink.domain.model.ThemePreferences.serializer(),
                    updated.themePreferences
                )
            }
            prefs[REMOTE_WEB_ACCESS] = updated.remoteWebAccessEnabled
            prefs[OTP_CLEANUP_ENABLED] = updated.otpCleanupEnabled
            prefs[OTP_CLEANUP_DAYS] = updated.otpCleanupDays
            updated.privatePinHash?.let { prefs[PRIVATE_PIN_HASH] = it } ?: prefs.remove(PRIVATE_PIN_HASH)
            prefs[PRIVATE_THREADS] = encodeJson {
                json.encodeToString(updated.privateThreadIds)
            }
            prefs[BEACON_LAUNCHER_ENABLED] = updated.beaconLauncherEnabled
            prefs[BEACON_HINT_DISMISSED] = updated.beaconHintDismissed
            prefs[WEB_ACCESS_HINT_DISMISSED] = updated.webAccessHintDismissed
            prefs[FIREBASE_MESSAGING_ENABLED] = updated.firebaseMessagingEnabled
            prefs[EMAIL_FALLBACK_ENABLED] = updated.emailFallbackEnabled
            prefs[MESSAGING_CHANNEL_PRIORITY] = encodeJson {
                json.encodeToString(updated.messagingChannelPriority)
            }
            prefs[CRASH_DETECTION_ENABLED] = updated.crashDetectionEnabled
            prefs[AI_SUMMARIES_ENABLED] = updated.aiSummariesEnabled
            prefs[AI_COMPOSE_ENABLED] = updated.aiComposeEnabled
            prefs[AI_URGENCY_ENABLED] = updated.aiUrgencyEnabled
            prefs[AI_URGENCY_BYPASS_DND] = updated.aiUrgencyBypassDnd
            prefs[AI_URGENCY_INCLUDE_UNKNOWN] = updated.aiUrgencyIncludeUnknown
            prefs[LINE_INBOX_MODE] = updated.lineInboxMode.name
            prefs[LINE_INBOX_MODE_CHOSEN] = updated.lineInboxModeChosen
            updated.activeLineId?.let { prefs[ACTIVE_LINE_ID] = it } ?: prefs.remove(ACTIVE_LINE_ID)
            updated.defaultSendLineId?.let { prefs[DEFAULT_SEND_LINE_ID] = it } ?: prefs.remove(DEFAULT_SEND_LINE_ID)
            prefs[LINE_SEND_PREFERENCE] = updated.lineSendPreference.name
            prefs[THREAD_LINE_OVERRIDES] = encodeJson {
                json.encodeToString(updated.threadLineOverrides)
            }
            updated.devicePhoneNumber?.let { prefs[DEVICE_PHONE_NUMBER] = it } ?: prefs.remove(DEVICE_PHONE_NUMBER)
        }
    }

    override suspend fun setCrashDetectionEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[CRASH_DETECTION_ENABLED] = enabled
        }
    }

    override suspend fun setAiSummariesEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AI_SUMMARIES_ENABLED] = enabled
        }
    }

    override suspend fun setAiComposeEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AI_COMPOSE_ENABLED] = enabled
        }
    }

    override suspend fun setAiUrgencyEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AI_URGENCY_ENABLED] = enabled
        }
    }

    override suspend fun setAiUrgencyBypassDnd(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AI_URGENCY_BYPASS_DND] = enabled
        }
    }

    override suspend fun setAiUrgencyIncludeUnknown(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AI_URGENCY_INCLUDE_UNKNOWN] = enabled
        }
    }

    override suspend fun setLineInboxMode(mode: LineInboxMode, chosen: Boolean) {
        editOnIo { prefs ->
            prefs[LINE_INBOX_MODE] = mode.name
            prefs[LINE_INBOX_MODE_CHOSEN] = chosen
        }
    }

    override suspend fun setActiveLineId(lineId: String?) {
        editOnIo { prefs ->
            lineId?.let { prefs[ACTIVE_LINE_ID] = it } ?: prefs.remove(ACTIVE_LINE_ID)
        }
    }

    override suspend fun setDefaultSendLineId(lineId: String?) {
        editOnIo { prefs ->
            lineId?.let { prefs[DEFAULT_SEND_LINE_ID] = it } ?: prefs.remove(DEFAULT_SEND_LINE_ID)
        }
    }

    override suspend fun setLineSendPreference(preference: LineSendPreference) {
        editOnIo { prefs ->
            prefs[LINE_SEND_PREFERENCE] = preference.name
        }
    }

    override suspend fun setThreadLineOverrides(overrides: Map<String, String>) {
        editOnIo { prefs ->
            prefs[THREAD_LINE_OVERRIDES] = encodeJson { json.encodeToString(overrides) }
        }
    }

    override suspend fun setThreadLineOverride(threadKey: String, lineId: String?) {
        editOnIo { prefs ->
            val current = decodeJsonOrNull(prefs[THREAD_LINE_OVERRIDES]) {
                json.decodeFromString<Map<String, String>>(it)
            } ?: emptyMap()
            val updated = if (lineId == null) {
                current - threadKey
            } else {
                current + (threadKey to lineId)
            }
            prefs[THREAD_LINE_OVERRIDES] = encodeJson { json.encodeToString(updated) }
        }
    }

    override suspend fun setDevicePhoneNumber(phone: String?) {
        editOnIo { prefs ->
            phone?.let { prefs[DEVICE_PHONE_NUMBER] = it } ?: prefs.remove(DEVICE_PHONE_NUMBER)
        }
    }

    override suspend fun setProUnlocked(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[PRO_UNLOCKED] = enabled
        }
    }

    override suspend fun setPremiumUnlocked(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[PREMIUM_UNLOCKED] = enabled
        }
    }

    override suspend fun setPremiumPurchaseToken(token: String?) {
        editOnIo { prefs ->
            if (token.isNullOrBlank()) {
                prefs.remove(PREMIUM_PURCHASE_TOKEN)
            } else {
                prefs[PREMIUM_PURCHASE_TOKEN] = token
            }
        }
    }

    override suspend fun setTierBeforePremium(tier: String?) {
        editOnIo { prefs ->
            if (tier.isNullOrBlank()) {
                prefs.remove(TIER_BEFORE_PREMIUM)
            } else {
                prefs[TIER_BEFORE_PREMIUM] = tier
            }
        }
    }

    override suspend fun setOnboardingComplete() {
        editOnIo { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
        }
    }

    override suspend fun ensureDeviceId(): String {
        val existing = dataStore.data.map { it[DEVICE_ID] }.first()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        editOnIo { prefs ->
            prefs[DEVICE_ID] = generated
        }
        return generated
    }

    override suspend fun setOwnerName(name: String) {
        editOnIo { prefs ->
            prefs[OWNER_NAME] = name
        }
    }

    override suspend fun setAutoAllowRemoteSoundChange(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AUTO_ALLOW_REMOTE_SOUND] = enabled
        }
    }

    override suspend fun setBetaTesterStatus(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[IS_BETA_TESTER] = enabled
        }
    }

    override suspend fun setAssistantShortcutsDismissed(dismissed: Boolean) {
        editOnIo { prefs ->
            prefs[ASSISTANT_HINT_DISMISSED] = dismissed
        }
    }

    override suspend fun setBetaAgreementAcceptance(version: String) {
        editOnIo { prefs ->
            prefs[BETA_AGREEMENT_ACCEPTED] = true
            prefs[BETA_AGREEMENT_VERSION] = version
        }
    }

    override suspend fun getLastWidgetCheckTimestamp(): Long {
        return dataStore.data.map { it[WIDGET_LAST_CHECK_TIMESTAMP] ?: 0L }.first()
    }

    override suspend fun updateLastWidgetCheckTimestamp(timestamp: Long) {
        editOnIo { prefs ->
            prefs[WIDGET_LAST_CHECK_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setEmergencyActive(active: Boolean) {
        editOnIo { prefs ->
            prefs[EMERGENCY_ACTIVE] = active
        }
    }

    override suspend fun isEmergencyActive(): Boolean {
        return dataStore.data.map { it[EMERGENCY_ACTIVE] ?: false }.first()
    }

    override suspend fun setLastKnownPhone(phone: String?) {
        editOnIo { prefs ->
            phone?.let { prefs[LAST_KNOWN_PHONE] = it } ?: prefs.remove(LAST_KNOWN_PHONE)
        }
    }

    override suspend fun getLastKnownPhone(): String? {
        return dataStore.data.map { it[LAST_KNOWN_PHONE] }.first()
    }

    override suspend fun setLastKnownEmail(email: String?) {
        editOnIo { prefs ->
            email?.let { prefs[LAST_KNOWN_EMAIL] = it } ?: prefs.remove(LAST_KNOWN_EMAIL)
        }
    }

    override suspend fun getLastKnownEmail(): String? {
        return dataStore.data.map { it[LAST_KNOWN_EMAIL] }.first()
    }

    override suspend fun setAutoUpdateContactInfo(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[AUTO_UPDATE_CONTACT_INFO] = enabled
        }
    }

    override suspend fun setTimeFormat(format: com.pulselink.domain.model.TimeFormat) {
        editOnIo { prefs ->
            prefs[TIME_FORMAT] = format.name
        }
    }

    override suspend fun setThemePreferences(theme: com.pulselink.domain.model.ThemePreferences) {
        editOnIo { prefs ->
            prefs[THEME_PREFERENCES] = encodeJson {
                json.encodeToString(com.pulselink.domain.model.ThemePreferences.serializer(), theme)
            }
        }
    }

    override suspend fun setRemoteWebAccessEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[REMOTE_WEB_ACCESS] = enabled
        }
    }

    override suspend fun setOtpCleanupEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[OTP_CLEANUP_ENABLED] = enabled
        }
    }

    override suspend fun setOtpCleanupDays(days: Int) {
        editOnIo { prefs ->
            prefs[OTP_CLEANUP_DAYS] = days.coerceAtLeast(1)
        }
    }

    override suspend fun setPrivatePinHash(hash: String?) {
        editOnIo { prefs ->
            hash?.let { prefs[PRIVATE_PIN_HASH] = it } ?: prefs.remove(PRIVATE_PIN_HASH)
        }
    }

    override suspend fun setPrivateThreads(threadIds: List<Long>) {
        editOnIo { prefs ->
            prefs[PRIVATE_THREADS] = encodeJson { json.encodeToString(threadIds.distinct()) }
        }
    }

    override suspend fun setBeaconLauncherEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[BEACON_LAUNCHER_ENABLED] = enabled
        }
        runCatching {
            val variant = settings.first().themePreferences.inboxIconVariant
            BeaconIconManager.apply(context, variant, enabled)
        }.onFailure { error ->
            Log.w(TAG, "Unable to update Beacon launcher icon enabled=$enabled", error)
        }
    }

    override suspend fun setBeaconHintDismissed(dismissed: Boolean) {
        editOnIo { prefs ->
            prefs[BEACON_HINT_DISMISSED] = dismissed
        }
    }

    override suspend fun setWebAccessHintDismissed(dismissed: Boolean) {
        editOnIo { prefs ->
            prefs[WEB_ACCESS_HINT_DISMISSED] = dismissed
        }
    }

    override suspend fun setFirebaseMessagingEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[FIREBASE_MESSAGING_ENABLED] = enabled
        }
    }

    override suspend fun setEmailFallbackEnabled(enabled: Boolean) {
        editOnIo { prefs ->
            prefs[EMAIL_FALLBACK_ENABLED] = enabled
        }
    }

    override suspend fun setMessagingChannelPriority(priority: List<MessageChannel>) {
        editOnIo { prefs ->
            prefs[MESSAGING_CHANNEL_PRIORITY] = encodeJson { json.encodeToString(priority) }
        }
    }

    private suspend fun settingsValue(prefs: Preferences): PulseLinkSettings {
        return PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            autoAllowRemoteSoundChange = prefs[AUTO_ALLOW_REMOTE_SOUND]
                ?: PulseLinkSettings().autoAllowRemoteSoundChange,
            assistantShortcutsDismissed = prefs[ASSISTANT_HINT_DISMISSED]
                ?: PulseLinkSettings().assistantShortcutsDismissed,
            emergencyProfile = decodeJsonOrNull(prefs[EMERGENCY_PROFILE]) {
                json.decodeFromString(AlertProfile.serializer(), it)
            }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = decodeJsonOrNull(prefs[CHECKIN_PROFILE]) {
                json.decodeFromString(AlertProfile.serializer(), it)
            }
                ?: PulseLinkSettings().checkInProfile,
            callSoundKey = prefs[CALL_SOUND] ?: PulseLinkSettings().callSoundKey,
            messageNotificationSoundUri = prefs[MESSAGE_NOTIFICATION_SOUND]
                ?: PulseLinkSettings().messageNotificationSoundUri,
            messageNotificationVibrate = prefs[MESSAGE_NOTIFICATION_VIBRATE]
                ?: PulseLinkSettings().messageNotificationVibrate,
            messageNotificationSoundOverrides = decodeJsonOrNull(
                prefs[MESSAGE_NOTIFICATION_SOUND_OVERRIDES]
            ) { json.decodeFromString<Map<String, String>>(it) }
                ?: PulseLinkSettings().messageNotificationSoundOverrides,
            betaAgreementAccepted = prefs[BETA_AGREEMENT_ACCEPTED] ?: PulseLinkSettings().betaAgreementAccepted,
            betaAgreementVersion = prefs[BETA_AGREEMENT_VERSION] ?: PulseLinkSettings().betaAgreementVersion,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            premiumUnlocked = prefs[PREMIUM_UNLOCKED] ?: PulseLinkSettings().premiumUnlocked,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId,
            ownerName = prefs[OWNER_NAME] ?: PulseLinkSettings().ownerName,
            isBetaTester = prefs[IS_BETA_TESTER] ?: PulseLinkSettings().isBetaTester,
            autoUpdateContactInfo = prefs[AUTO_UPDATE_CONTACT_INFO] ?: PulseLinkSettings().autoUpdateContactInfo,
            timeFormat = prefs[TIME_FORMAT]?.let { runCatching { com.pulselink.domain.model.TimeFormat.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().timeFormat,
            themePreferences = decodeJsonOrNull(prefs[THEME_PREFERENCES]) {
                json.decodeFromString(com.pulselink.domain.model.ThemePreferences.serializer(), it)
            } ?: PulseLinkSettings().themePreferences,
            remoteWebAccessEnabled = prefs[REMOTE_WEB_ACCESS] ?: PulseLinkSettings().remoteWebAccessEnabled,
            otpCleanupEnabled = prefs[OTP_CLEANUP_ENABLED] ?: PulseLinkSettings().otpCleanupEnabled,
            otpCleanupDays = prefs[OTP_CLEANUP_DAYS] ?: PulseLinkSettings().otpCleanupDays,
            privatePinHash = prefs[PRIVATE_PIN_HASH],
            privateThreadIds = decodeJsonOrNull(prefs[PRIVATE_THREADS]) {
                json.decodeFromString<List<Long>>(it)
            } ?: PulseLinkSettings().privateThreadIds,
            beaconLauncherEnabled = prefs[BEACON_LAUNCHER_ENABLED] ?: PulseLinkSettings().beaconLauncherEnabled,
            beaconHintDismissed = prefs[BEACON_HINT_DISMISSED] ?: PulseLinkSettings().beaconHintDismissed,
            webAccessHintDismissed = prefs[WEB_ACCESS_HINT_DISMISSED] ?: PulseLinkSettings().webAccessHintDismissed,
            firebaseMessagingEnabled = prefs[FIREBASE_MESSAGING_ENABLED] ?: PulseLinkSettings().firebaseMessagingEnabled,
            emailFallbackEnabled = prefs[EMAIL_FALLBACK_ENABLED] ?: PulseLinkSettings().emailFallbackEnabled,
            messagingChannelPriority = decodeJsonOrNull(prefs[MESSAGING_CHANNEL_PRIORITY]) {
                json.decodeFromString<List<MessageChannel>>(it)
            } ?: PulseLinkSettings().messagingChannelPriority,
            lineInboxMode = prefs[LINE_INBOX_MODE]?.let { runCatching { LineInboxMode.valueOf(it) }.getOrNull() }
                ?: PulseLinkSettings().lineInboxMode,
            lineInboxModeChosen = prefs[LINE_INBOX_MODE_CHOSEN] ?: PulseLinkSettings().lineInboxModeChosen,
            activeLineId = prefs[ACTIVE_LINE_ID],
            defaultSendLineId = prefs[DEFAULT_SEND_LINE_ID],
            lineSendPreference = prefs[LINE_SEND_PREFERENCE]?.let {
                runCatching { LineSendPreference.valueOf(it) }.getOrNull()
            } ?: PulseLinkSettings().lineSendPreference,
            threadLineOverrides = decodeJsonOrNull(prefs[THREAD_LINE_OVERRIDES]) {
                json.decodeFromString<Map<String, String>>(it)
            } ?: PulseLinkSettings().threadLineOverrides,
            devicePhoneNumber = prefs[DEVICE_PHONE_NUMBER]
        )
    }

    companion object {
        private const val TAG = "SettingsRepositoryImpl"
    }
}

fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) }
    )
