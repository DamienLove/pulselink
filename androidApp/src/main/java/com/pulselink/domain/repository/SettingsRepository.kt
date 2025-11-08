package com.pulselink.domain.repository

import com.pulselink.domain.model.PulseLinkSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<PulseLinkSettings>
    suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings)
    suspend fun setListening(enabled: Boolean)
    suspend fun setProUnlocked(enabled: Boolean)
    suspend fun setOnboardingComplete()
    suspend fun ensureDeviceId(): String
    suspend fun setOwnerName(name: String)
    suspend fun setAutoAllowRemoteSoundChange(enabled: Boolean)
    suspend fun setBetaTesterStatus(enabled: Boolean)
}
