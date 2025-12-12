package com.pulselink.domain.repository

import com.pulselink.domain.model.PulseLinkSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PulseLinkSettings>
    suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings)
    suspend fun setProUnlocked(enabled: Boolean)
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
    suspend fun setRealtimeEnabled(enabled: Boolean)
    suspend fun setFcmToken(token: String)
    suspend fun getFcmToken(): String?
}
