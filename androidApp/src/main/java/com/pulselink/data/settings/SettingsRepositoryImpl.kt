package com.pulselink.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pulselink.domain.model.AlertProfile
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

private const val DATA_STORE_NAME = "pulselink_settings"

private val PRIMARY_PHRASE = stringPreferencesKey("primary_phrase")
private val SECONDARY_PHRASE = stringPreferencesKey("secondary_phrase")
private val LISTENING_ENABLED = booleanPreferencesKey("listening_enabled")
private val INCLUDE_LOCATION = booleanPreferencesKey("include_location")
private val EMERGENCY_PROFILE = stringPreferencesKey("emergency_profile")
private val CHECKIN_PROFILE = stringPreferencesKey("checkin_profile")
private val AUTO_CALL = booleanPreferencesKey("auto_call")
private val PRO_UNLOCKED = booleanPreferencesKey("pro_unlocked")
private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
private val DEVICE_ID = stringPreferencesKey("device_id")

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<PulseLinkSettings> = dataStore.data.map { prefs ->
        PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            listeningEnabled = prefs[LISTENING_ENABLED] ?: PulseLinkSettings().listeningEnabled,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            emergencyProfile = prefs[EMERGENCY_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = prefs[CHECKIN_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().checkInProfile,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId
        )
    }

    override suspend fun update(transform: (PulseLinkSettings) -> PulseLinkSettings) {
        dataStore.edit { prefs ->
            val current = settingsValue(prefs)
            val updated = transform(current)
            prefs[PRIMARY_PHRASE] = updated.primaryPhrase
            prefs[SECONDARY_PHRASE] = updated.secondaryPhrase
            prefs[LISTENING_ENABLED] = updated.listeningEnabled
            prefs[INCLUDE_LOCATION] = updated.includeLocation
            prefs[EMERGENCY_PROFILE] = json.encodeToString(AlertProfile.serializer(), updated.emergencyProfile)
            prefs[CHECKIN_PROFILE] = json.encodeToString(AlertProfile.serializer(), updated.checkInProfile)
            prefs[AUTO_CALL] = updated.autoCallAfterAlert
            prefs[PRO_UNLOCKED] = updated.proUnlocked
            prefs[ONBOARDING_COMPLETE] = updated.onboardingComplete
            prefs[DEVICE_ID] = updated.deviceId
        }
    }

    override suspend fun setListening(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[LISTENING_ENABLED] = enabled
        }
    }

    override suspend fun setProUnlocked(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PRO_UNLOCKED] = enabled
        }
    }

    override suspend fun setOnboardingComplete() {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETE] = true
        }
    }

    override suspend fun ensureDeviceId(): String {
        val existing = dataStore.data.map { it[DEVICE_ID] }.first()
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            prefs[DEVICE_ID] = generated
        }
        return generated
    }

    private fun settingsValue(prefs: Preferences): PulseLinkSettings {
        return PulseLinkSettings(
            primaryPhrase = prefs[PRIMARY_PHRASE] ?: PulseLinkSettings().primaryPhrase,
            secondaryPhrase = prefs[SECONDARY_PHRASE] ?: PulseLinkSettings().secondaryPhrase,
            listeningEnabled = prefs[LISTENING_ENABLED] ?: PulseLinkSettings().listeningEnabled,
            includeLocation = prefs[INCLUDE_LOCATION] ?: PulseLinkSettings().includeLocation,
            emergencyProfile = prefs[EMERGENCY_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().emergencyProfile,
            checkInProfile = prefs[CHECKIN_PROFILE]?.let { json.decodeFromString(AlertProfile.serializer(), it) }
                ?: PulseLinkSettings().checkInProfile,
            autoCallAfterAlert = prefs[AUTO_CALL] ?: PulseLinkSettings().autoCallAfterAlert,
            proUnlocked = prefs[PRO_UNLOCKED] ?: PulseLinkSettings().proUnlocked,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: PulseLinkSettings().onboardingComplete,
            deviceId = prefs[DEVICE_ID] ?: PulseLinkSettings().deviceId
        )
    }
}

fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
    androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) }
    )
