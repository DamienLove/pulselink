package com.pulselink.ui.state

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulselink.BuildConfig
import com.pulselink.R
import com.pulselink.auth.AuthState
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.data.alert.AlertDispatcher.AlertResult
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor
import com.pulselink.data.assistant.VoiceCommandResult
import com.pulselink.data.alert.SoundCatalog
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.RemotePresence
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.domain.model.TimeFormat
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.BetaAgreementRepository
import com.pulselink.domain.repository.BlockedContactRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.service.AlertRouter
import com.pulselink.ui.screens.BugReportData
import com.pulselink.ui.state.DndStatusMessage
import com.pulselink.util.AudioOverrideManager
import com.pulselink.widget.WidgetStateManager
import com.pulselink.util.BeaconIconManager
import com.pulselink.util.normalizeSmsAddress
import com.pulselink.util.stripSmsDisplayName
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseUser
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    private val alertRepository: AlertRepository,
    private val settingsRepository: SettingsRepository,
    private val alertRouter: AlertRouter,
    private val soundCatalog: SoundCatalog,
    private val linkManager: ContactLinkManager,
    private val messageRepository: MessageRepository,
    private val blockedContactRepository: BlockedContactRepository,
    private val betaAgreementRepository: BetaAgreementRepository,
    private val naturalLanguageCommandProcessor: NaturalLanguageCommandProcessor,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val firestore: FirebaseFirestore,
    private val functions: com.google.firebase.functions.FirebaseFunctions,
    private val widgetStateManager: WidgetStateManager
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _deleteAccountState = MutableStateFlow<DeleteAccountState>(DeleteAccountState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountState> = _deleteAccountState

    private val dispatching = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow<String?>(null)
    private val dndStatus = MutableStateFlow<DndStatusMessage?>(null)
    private val emergencyActive = MutableStateFlow(false)
    private val emergencySounds = soundCatalog.emergencyOptions()
    private val checkInSounds = soundCatalog.checkInOptions()
    private val callSounds = soundCatalog.callOptions()
    private val profileUpdate = MutableStateFlow(ProfileUpdateUiState())
    private var lastKnownPhone: String? = null
    private var lastKnownEmail: String? = null
    private var remoteSettingsListener: ListenerRegistration? = null
    private var remoteSettingsUserId: String? = null
    private var pushedThemeFromDevice = false

    private val _uiState = MutableStateFlow(PulseLinkUiState())
    val uiState: StateFlow<PulseLinkUiState> = _uiState
    val authState: StateFlow<AuthState> = firebaseAuthManager.authState

    init {
        viewModelScope.launch {
            lastKnownPhone = settingsRepository.getLastKnownPhone()
            lastKnownEmail = settingsRepository.getLastKnownEmail()
        }
        viewModelScope.launch {
            pruneLocalUnreachable()
        }
        viewModelScope.launch {
            val baseState = combine(
                settingsRepository.settings,
                contactRepository.observeContacts(),
                alertRepository.observeRecent(10),
                alertRepository.observeUnreadCount(),
                dispatching
            ) { settings, contacts, events, unreadCount, isDispatching ->
                val normalizedSettings = ensureSoundDefaults(settings)
                val adsAvailable = BuildConfig.ADS_ENABLED
                val showAds = adsAvailable && !normalizedSettings.proUnlocked && !normalizedSettings.premiumUnlocked
                val isProUser = normalizedSettings.proUnlocked || normalizedSettings.premiumUnlocked || !adsAvailable

                PulseLinkUiState(
                    settings = normalizedSettings,
                    contacts = contacts,
                    recentEvents = events,
                    unreadAlertCount = unreadCount,
                    isDispatching = isDispatching,
                    lastMessagePreview = null,
                    emergencySoundOptions = emergencySounds,
                    checkInSoundOptions = checkInSounds,
                    callSoundOptions = callSounds,
                    showAds = showAds,
                    isProUser = isProUser,
                    isPremiumUser = normalizedSettings.premiumUnlocked,
                    adsAvailable = adsAvailable,
                    onboardingComplete = normalizedSettings.onboardingComplete,
                    autoUpdateContactInfo = normalizedSettings.autoUpdateContactInfo
                )
            }
            val withLastMessage = combine(baseState, lastMessage) { state, message ->
                state.copy(lastMessagePreview = message)
            }
            val withProfile = combine(withLastMessage, profileUpdate) { state, profile ->
                state.copy(profileUpdate = profile)
            }
            combine(withProfile, dndStatus, emergencyActive) { state, dndStatusMessage, isEmergencyActive ->
                state.copy(dndStatus = dndStatusMessage, isEmergencyActive = isEmergencyActive)
            }.collect { state ->
                _uiState.value = state
            }
        }

        // When a real account signs in, sync profile + contacts from cloud
        viewModelScope.launch {
            firebaseAuthManager.authState.collect { state ->
                val user = (state as? AuthState.Authenticated)?.user
                if (user != null && !user.isAnonymous) {
                    syncProfileFromCloud(user)
                    syncContactsFromCloud(user)
                    linkManager.syncLinksOnLogin()
                    startRemoteSettingsListener(user)
                    val currentPhone = user.phoneNumber
                    val currentEmail = user.email
                    if (currentPhone != lastKnownPhone || currentEmail != lastKnownEmail) {
                        linkManager.broadcastProfileUpdate()
                        lastKnownPhone = currentPhone
                        lastKnownEmail = currentEmail
                        settingsRepository.setLastKnownPhone(currentPhone)
                        settingsRepository.setLastKnownEmail(currentEmail)
                    }
                } else {
                    remoteSettingsListener?.remove()
                    remoteSettingsListener = null
                    remoteSettingsUserId = null
                    pushedThemeFromDevice = false
                }
            }
        }
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch {
            val isNewContact = contact.id == 0L
            val nextOrder = if (contact.id == 0L) {
                (_uiState.value.contacts.maxOfOrNull { it.contactOrder } ?: -1) + 1
            } else {
                contact.contactOrder
            }
            val withUid = contact.ensureRemoteUid()
            val storedContact = if (isNewContact) {
                withUid.copy(contactOrder = nextOrder)
            } else {
                withUid
            }
            contactRepository.upsert(storedContact)

            if (!storedContact.email.isNullOrBlank()) {
                val resolvedId = when {
                    storedContact.id != 0L -> storedContact.id
                    else -> contactRepository.getByEmail(storedContact.email)?.id
                        ?: contactRepository.getByPhone(storedContact.phoneNumber)?.id
                }
                resolvedId?.let { linkManager.hydrateContactFromEmail(it) }
            }

            // Mirror to cloud for authenticated users
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                upsertContactInCloud(user, storedContact)
            }

            if (isNewContact) {
                val phone = storedContact.primaryPhone()
                val email = storedContact.primaryEmail()
                if (!phone.isNullOrBlank() || !email.isNullOrBlank()) {
                    runCatching {
                        val persisted = when {
                            !phone.isNullOrBlank() -> contactRepository.getByPhone(phone)
                            !email.isNullOrBlank() -> contactRepository.getByEmail(email)
                            else -> null
                        }
                        if (persisted != null) {
                            linkManager.sendLinkRequest(persisted.id)
                        }
                    }.onFailure { error ->
                        Log.w(TAG, "Unable to auto-send link request for ${contact.displayName}", error)
                    }
                }
            }
        }

    fun markAlertsAsRead(alertIds: List<Long>) {
        viewModelScope.launch {
            alertRepository.markAsRead(alertIds)
        }
    }
    }

    fun markAlertsAsRead(alertIds: List<Long>) {
        viewModelScope.launch {
            alertRepository.markAsRead(alertIds)
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(id)
            contact?.let {
                (firebaseAuthManager.currentUser()?.takeIf { user -> !user.isAnonymous })?.let { user ->
                    deleteContactInCloud(user, it)
                }
                blockedContactRepository.block(
                    phoneNumber = it.phoneNumber,
                    linkCode = it.linkCode,
                    remoteDeviceId = it.remoteDeviceId,
                    displayName = it.displayName
                )
            }
            contactRepository.delete(id)
            messageRepository.clear(id)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _deleteAccountState.value = DeleteAccountState.Loading
            try {
                functions.getHttpsCallable("deleteAccount").call().await()
                _deleteAccountState.value = DeleteAccountState.Success
                firebaseAuthManager.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Account deletion failed", e)
                _deleteAccountState.value = DeleteAccountState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = DeleteAccountState.Idle
    }

    fun triggerEmergency() {
        dispatch(EscalationTier.EMERGENCY, "PulseLink emergency alert")
    }

    fun sendCheckIn() {
        dispatch(EscalationTier.CHECK_IN, "PulseLink check-in")
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuthManager.signOut()
        }
    }

    fun syncContactsNow() {
        viewModelScope.launch {
            val user = firebaseAuthManager.currentUser()
            if (user == null || user.isAnonymous) {
                Log.i(TAG, "Manual sync skipped: no signed-in user")
                return@launch
            }
            syncContactsFromCloud(user, forcePushLocal = true)
            linkManager.syncLinksOnLogin()
        }
        // Also trigger SMS sync if applicable
        if (BuildConfig.PREMIUM_FEATURES) {
            val request = OneTimeWorkRequest.Builder(com.pulselink.data.sms.SmsSyncWorker::class.java).build()
            workManager.enqueueUniqueWork("SmsSyncManual", ExistingWorkPolicy.KEEP, request)
        }
    }

    fun reorderContacts(contactIds: List<Long>) {
        viewModelScope.launch {
            contactRepository.updateOrder(contactIds)
        }
    }

    fun setIncludeLocation(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(includeLocation = enabled)
            }
        }
    }

    fun setOwnerName(name: String) {
        viewModelScope.launch {
            settingsRepository.setOwnerName(name)
            (firebaseAuthManager.currentUser()?.takeIf { user -> !user.isAnonymous })?.let { user ->
                pushProfileToCloud(user, name)
            }
        }
    }

    fun setOwnerAvatarUrl(url: String?) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(ownerAvatarUrl = url) }
            (firebaseAuthManager.currentUser()?.takeIf { user -> !user.isAnonymous })?.let { user ->
                val currentName = settingsRepository.settings.first().ownerName
                pushProfileToCloud(user, currentName)
            }
        }
    }

    fun setAutoAllowRemoteSoundChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAllowRemoteSoundChange(enabled)
        }
    }

    fun setAutoUpdateContactInfo(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoUpdateContactInfo(enabled)
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                pushSettingsToCloud(user, mapOf("autoUpdateContactInfo" to enabled))
            }
        }
    }

    fun acceptBetaAgreement(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val name = runCatching { settingsRepository.settings.first().ownerName }
                .getOrDefault("")
                .ifBlank { "PulseLink Tester" }

            val localSuccess = runCatching {
                settingsRepository.setBetaAgreementAcceptance(BETA_AGREEMENT_VERSION)
                settingsRepository.setBetaTesterStatus(true)
            }.onFailure { error ->
                Log.e(TAG, "Unable to persist local beta agreement acceptance", error)
            }.isSuccess

            if (!localSuccess) {
                onResult(false)
                return@launch
            }

            onResult(true)

            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    withTimeout(REMOTE_BETA_AGREEMENT_TIMEOUT_MS) {
                        betaAgreementRepository.recordAgreement(name, BETA_AGREEMENT_VERSION)
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Unable to upload beta agreement acceptance", error)
                }
            }
        }
    }

    fun dismissAssistantHint() {
        viewModelScope.launch {
            settingsRepository.setAssistantShortcutsDismissed(true)
        }
    }

    fun updateEmergencySound(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    emergencyProfile = settings.emergencyProfile.copy(soundKey = key)
                )
            }
        }
    }

    fun updateCheckInSound(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    checkInProfile = settings.checkInProfile.copy(soundKey = key)
                )
            }
        }
    }

    fun updateCallSound(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(callSoundKey = key)
            }
        }
    }

    fun updateMessageNotificationSound(uri: String?) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(messageNotificationSoundUri = uri)
            }
        }
    }

    fun updateMessageNotificationVibrationPattern(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(messageNotificationVibrationPattern = key)
            }
        }
    }

    fun updateMessageNotificationVibrate(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(messageNotificationVibrate = enabled)
            }
        }
    }

    fun updateMessageNotificationVibrationOverride(address: String, key: String?) {
        val normalized = normalizeSmsAddress(address)
        if (normalized.isBlank()) return
        viewModelScope.launch {
            settingsRepository.update { settings ->
                val overrides = settings.messageNotificationVibrationOverrides.toMutableMap()
                if (key.isNullOrBlank()) {
                    overrides.remove(normalized)
                } else {
                    overrides[normalized] = key
                }
                settings.copy(messageNotificationVibrationOverrides = overrides)
            }
        }
    }

    fun updateMessageNotificationOverride(address: String, uri: String?) {
        val normalized = normalizeSmsAddress(address)
        if (normalized.isBlank()) return
        viewModelScope.launch {
            settingsRepository.update { settings ->
                val overrides = settings.messageNotificationSoundOverrides.toMutableMap()
                if (uri.isNullOrBlank()) {
                    overrides.remove(normalized)
                } else {
                    overrides[normalized] = uri
                }
                settings.copy(messageNotificationSoundOverrides = overrides)
            }
        }
    }

    fun updateEmergencyVibrationPattern(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    emergencyProfile = settings.emergencyProfile.copy(vibrationPatternKey = key)
                )
            }
        }
    }

    fun updateCheckInVibrationPattern(key: String) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                settings.copy(
                    checkInProfile = settings.checkInProfile.copy(vibrationPatternKey = key)
                )
            }
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.upsert(contact)
        }
    }

    fun updateContactTheme(contactId: Long, theme: com.pulselink.domain.model.ThemePreferences?) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(contactId)
            if (contact != null) {
                contactRepository.upsert(contact.copy(themeOverride = theme))
            }
        }
    }

    fun updateContactSounds(contactId: Long, emergencyKey: String?, checkInKey: String?) {
        viewModelScope.launch {
            val contact = uiState.value.contacts.firstOrNull { it.id == contactId } ?: return@launch
            val emergencyChanged = emergencyKey != null && emergencyKey != contact.emergencySoundKey
            val checkInChanged = checkInKey != null && checkInKey != contact.checkInSoundKey
            contactRepository.upsert(
                contact.copy(
                    emergencySoundKey = emergencyKey ?: contact.emergencySoundKey,
                    checkInSoundKey = checkInKey ?: contact.checkInSoundKey
                )
            )
            if (emergencyChanged) {
                runCatching {
                    linkManager.sendSoundOverride(contactId, EscalationTier.EMERGENCY, emergencyKey)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to sync emergency sound override for ${contact.displayName}", error)
                }
            }
            if (checkInChanged) {
                runCatching {
                    linkManager.sendSoundOverride(contactId, EscalationTier.CHECK_IN, checkInKey)
                }.onFailure { error ->
                    Log.w(TAG, "Failed to sync check-in sound override for ${contact.displayName}", error)
                }
            }
        }
    }

    fun sendLinkRequest(contactId: Long) {
        viewModelScope.launch { linkManager.sendLinkRequest(contactId) }
    }

    fun approveLink(contactId: Long) {
        viewModelScope.launch { linkManager.approveLink(contactId) }
    }

    suspend fun sendPing(contactId: Long): Boolean = linkManager.sendPing(contactId)

    fun setRemoteSoundPermission(contactId: Long, allow: Boolean) {
        viewModelScope.launch { linkManager.updateRemoteSoundPermission(contactId, allow) }
    }

    fun setRemoteOverridePermission(contactId: Long, allow: Boolean) {
        viewModelScope.launch { linkManager.updateRemoteOverridePermission(contactId, allow) }
    }

    fun setRemotePin(contactId: Long, pin: String?) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(contactId)
            if (contact != null) {
                contactRepository.upsert(contact.copy(remotePin = pin))
            }
        }
    }

    suspend fun sendManualMessage(
        contactId: Long,
        message: String,
        urgency: com.pulselink.domain.model.MessageUrgency = com.pulselink.domain.model.MessageUrgency.STANDARD,
        volumeHint: com.pulselink.domain.model.VolumeHint? = null
    ): ManualMessageResult =
        linkManager.sendManualMessage(contactId, message, urgency, volumeHint)

    fun messagesForContact(contactId: Long): Flow<List<ContactMessage>> =
        messageRepository.observeForContact(contactId)

    fun setProUnlocked(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.settings.first().proUnlocked
            if (!enabled && current) {
                Log.w("MainViewModel", "Pro downgrade attempt ignored")
                return@launch
            }
            if (enabled == current) return@launch
            settingsRepository.setProUnlocked(enabled)
            if (enabled) {
                firebaseAuthManager.refreshClaims()
            }
        }
    }

    fun setBetaTesterStatus(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBetaTesterStatus(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun initiateCall(contactId: Long, phoneNumber: String): CallInitiationResult {
        return when (linkManager.prepareRemoteCall(contactId)) {
            ContactLinkManager.CallPreparationResult.READY -> CallInitiationResult.Ready
            ContactLinkManager.CallPreparationResult.TIMEOUT -> CallInitiationResult.Timeout
            ContactLinkManager.CallPreparationResult.FAILED -> CallInitiationResult.Failure
        }
    }

    fun notifyCallEnded(contactId: Long, callDuration: Long) {
        viewModelScope.launch {
            linkManager.sendCallEndedNotification(contactId, callDuration)
        }
    }

    fun broadcastProfileToContacts() {
        viewModelScope.launch {
            profileUpdate.value = ProfileUpdateUiState(inProgress = true)
            linkManager.broadcastProfileUpdate()
                .onSuccess { count ->
                    profileUpdate.value = ProfileUpdateUiState(
                        inProgress = false,
                        resultCount = count,
                        error = null
                    )
                }
                .onFailure { error ->
                    Log.w(TAG, "Profile broadcast failed", error)
                    profileUpdate.value = ProfileUpdateUiState(
                        inProgress = false,
                        resultCount = null,
                        error = error.localizedMessage ?: "Profile update failed"
                    )
                }
        }
    }

    fun needsBetaAgreement(settings: com.pulselink.domain.model.PulseLinkSettings): Boolean =
        settings.betaAgreementVersion != BETA_AGREEMENT_VERSION

    suspend fun processVoiceCommand(query: String): VoiceCommandResult =
        naturalLanguageCommandProcessor.handleCommand(query)

    private suspend fun syncProfileFromCloud(user: FirebaseUser) {
        runCatching {
            val localSettings = settingsRepository.settings.first()
            val deviceId = settingsRepository.ensureDeviceId()
            val profileRef = firestore.collection("users").document(user.uid)
            val snapshot = profileRef.get().await()
            val remoteName = snapshot.getString("ownerName")
                ?: user.displayName
            val remoteAvatar = snapshot.getString("avatarUrl")
            val remoteEmail = snapshot.getString("email")
            val remoteDeviceId = snapshot.getString("deviceId")

            if (!remoteAvatar.isNullOrBlank() && localSettings.ownerAvatarUrl != remoteAvatar) {
                settingsRepository.update { it.copy(ownerAvatarUrl = remoteAvatar) }
            }

            when {
                !remoteName.isNullOrBlank() && localSettings.ownerName.isBlank() -> {
                    settingsRepository.setOwnerName(remoteName)
                }
                remoteName.isNullOrBlank() && localSettings.ownerName.isNotBlank() -> {
                    profileRef.set(mapOf("ownerName" to localSettings.ownerName), SetOptions.merge()).await()
                }
                remoteName.isNullOrBlank() && localSettings.ownerName.isBlank() -> {
                    // nothing to sync yet
                }
                else -> Unit
            }
            // Sync email for Firebase-authenticated users
            when {
                !remoteEmail.isNullOrBlank() -> {
                    settingsRepository.setLastKnownEmail(remoteEmail)
                    if (snapshot.getString("emailLowercase").isNullOrBlank()) {
                        profileRef.set(mapOf("emailLowercase" to remoteEmail.lowercase()), SetOptions.merge()).await()
                    }
                }
                !user.email.isNullOrBlank() -> {
                    val email = user.email!!
                    settingsRepository.setLastKnownEmail(email)
                    profileRef.set(
                        mapOf(
                            "email" to email,
                            "emailLowercase" to email.lowercase()
                        ),
                        SetOptions.merge()
                    ).await()
                }
            }
            if (remoteDeviceId.isNullOrBlank() || remoteDeviceId != deviceId) {
                profileRef.set(mapOf("deviceId" to deviceId), SetOptions.merge()).await()
            }
            upsertUserByPhoneMapping(user, remoteName, remoteAvatar ?: localSettings.ownerAvatarUrl)
        }.onFailure { error ->
            Log.w(TAG, "Unable to sync profile", error)
        }
    }

    private fun startRemoteSettingsListener(user: FirebaseUser) {
        if (remoteSettingsUserId == user.uid && remoteSettingsListener != null) return
        remoteSettingsListener?.remove()
        remoteSettingsUserId = user.uid
        remoteSettingsListener = firestore.collection(COLLECTION_USERS).document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    return@addSnapshotListener
                }
                val themeMap = snapshot.get("themePreferences") as? Map<*, *>
                val remoteWebAccess = snapshot.getBoolean("remoteWebAccessEnabled")
                val autoUpdate = snapshot.getBoolean("autoUpdateContactInfo")
                val timeFormatRaw = snapshot.getString("timeFormat")
                viewModelScope.launch {
                    val current = settingsRepository.settings.first()
                    if (themeMap != null) {
                        val remoteTheme = themeFromMap(themeMap)
                        if (remoteTheme != current.themePreferences) {
                            settingsRepository.setThemePreferences(remoteTheme)
                            if (current.beaconLauncherEnabled && current.themePreferences.inboxIconVariant != remoteTheme.inboxIconVariant) {
                                applyInboxIconVariant(remoteTheme.inboxIconVariant, enabled = true)
                            }
                        }
                    } else if (!pushedThemeFromDevice) {
                        pushThemeToCloud(user, current.themePreferences)
                        pushedThemeFromDevice = true
                    }
                    remoteWebAccess?.let {
                        if (it != current.remoteWebAccessEnabled) {
                            settingsRepository.setRemoteWebAccessEnabled(it)
                        }
                    }
                    autoUpdate?.let {
                        if (it != current.autoUpdateContactInfo) {
                            settingsRepository.setAutoUpdateContactInfo(it)
                        }
                    }
                    timeFormatRaw?.let { raw ->
                        runCatching { TimeFormat.valueOf(raw) }.getOrNull()?.let { format ->
                            if (format != current.timeFormat) {
                                settingsRepository.setTimeFormat(format)
                            }
                        }
                    }
                }
            }
    }

    private suspend fun pushProfileToCloud(user: FirebaseUser, ownerName: String) {
        runCatching {
            val settings = settingsRepository.settings.first()
            val deviceId = settingsRepository.ensureDeviceId()
            val payload = mutableMapOf<String, Any>(
                "ownerName" to ownerName,
                "deviceId" to deviceId
            )
            settings.ownerAvatarUrl?.let { payload["avatarUrl"] = it }
            user.email?.let { email ->
                payload["email"] = email
                payload["emailLowercase"] = email.lowercase()
            }
            user.phoneNumber?.let { phone ->
                val normalized = normalizePhone(phone)
                if (normalized.isNotBlank()) {
                    payload["phoneNumber"] = phone
                    payload["phoneNumberNormalized"] = normalized
                }
            }
            firestore.collection("users").document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
            upsertUserByPhoneMapping(user, ownerName, settings.ownerAvatarUrl)
        }.onFailure { error ->
            Log.w(TAG, "Unable to push profile name", error)
        }
    }

    private suspend fun upsertUserByPhoneMapping(
        user: FirebaseUser,
        ownerName: String?,
        avatarUrl: String?
    ) {
        val rawPhone = user.phoneNumber?.trim().orEmpty()
        val normalized = normalizePhone(rawPhone)
        if (normalized.isBlank()) return
        val displayName = ownerName?.trim().takeIf { !it.isNullOrBlank() } ?: rawPhone
        val payload = mutableMapOf<String, Any>(
            "uid" to user.uid,
            "displayName" to displayName,
            "phoneNumber" to rawPhone,
            "phoneNumberNormalized" to normalized,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        avatarUrl?.takeIf { it.isNotBlank() }?.let { payload["avatarUrl"] = it }
        user.email?.takeIf { it.isNotBlank() }?.let { email ->
            payload["email"] = email
            payload["emailLowercase"] = email.lowercase()
        }
        runCatching {
            firestore.collection("users_by_phone")
                .document(normalized)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to update users_by_phone mapping", error)
        }
    }

    private suspend fun pushThemeToCloud(user: FirebaseUser, theme: ThemePreferences) {
        runCatching {
            val payload = mapOf(
                "themePreferences" to themeToMap(theme),
                "themeUpdatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_USERS).document(user.uid)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to push theme to cloud", error)
        }
    }

    private suspend fun pushSettingsToCloud(user: FirebaseUser, payload: Map<String, Any>) {
        runCatching {
            firestore.collection(COLLECTION_USERS).document(user.uid)
                .set(payload + mapOf("settingsUpdatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to push settings to cloud", error)
        }
    }

    private suspend fun syncContactsFromCloud(user: FirebaseUser, forcePushLocal: Boolean = false) {
        runCatching {
            val localContacts = contactRepository.getAll()

            val primarySnapshot = firestore.collection(COLLECTION_USERS).document(user.uid)
                .collection(COLLECTION_TRUSTED_CONTACTS)
                .get()
                .await()
            val primaryContacts = primarySnapshot.documents.mapNotNull { it.toContact() }

            val legacyContacts = fetchLegacyTrustedContacts(user)
            val remoteContacts = (primaryContacts + legacyContacts)
                .distinctBy { contactSyncKey(it) }

            val enrichedRemote = remoteContacts.map { contact ->
                if (contact.linkCode.isNullOrBlank()) {
                    contact
                } else {
                    resolveLinkFromDoc(contact, contact.linkCode, user.uid)
                }
            }

            if (enrichedRemote.isEmpty() && localContacts.isEmpty()) {
                return@runCatching
            }

            val merged = if (enrichedRemote.isEmpty()) {
                localContacts
            } else {
                mergeContacts(localContacts, enrichedRemote)
            }
            val reachableMerged = merged.filterNot { isUnreachable(it) }

            val remoteKeys = enrichedRemote.map { contactSyncKey(it) }.toSet()
            val localOnlyKeys = localContacts
                .filterNot { remoteKeys.contains(contactSyncKey(it)) }
                .map { contactSyncKey(it) }
                .toSet()

            contactRepository.clear()
            reachableMerged.sortedBy { it.contactOrder }
                .forEachIndexed { index, contact ->
                    val normalized = contact.copy(contactOrder = index)
                    contactRepository.upsert(normalized)
                    if (forcePushLocal || enrichedRemote.isEmpty() || localOnlyKeys.contains(contactSyncKey(contact))) {
                        upsertContactInCloud(user, normalized)
                    }
                }
            pruneLocalUnreachable()
        }.onFailure { error ->
            Log.w(TAG, "Unable to sync contacts from cloud", error)
        }
    }

    private suspend fun fetchLegacyTrustedContacts(user: FirebaseUser): List<Contact> {
        return runCatching {
            // Pull any legacy/alternate trusted contact storage in case the main subcollection is empty
            val ownerScoped = firestore.collectionGroup(COLLECTION_TRUSTED_CONTACTS)
                .whereEqualTo("ownerUid", user.uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.toContact() }

            if (ownerScoped.isNotEmpty()) return@runCatching ownerScoped

            val groupSnapshot = firestore.collectionGroup(COLLECTION_TRUSTED_CONTACTS)
                .get()
                .await()
            groupSnapshot.documents
                .filter { snap ->
                    val path = snap.reference.path
                    path.contains("/${user.uid}/") ||
                        snap.getString("ownerUid") == user.uid ||
                        snap.getString("userId") == user.uid
                }
                .mapNotNull { it.toContact() }
        }.getOrElse {
            Log.w(TAG, "Legacy trusted contacts lookup failed", it)
            emptyList()
        }
    }

    private fun DocumentSnapshot.toContact(): Contact? {
        val name = getString("displayName") ?: return null
        val phone = getString("phoneNumber") ?: ""
        val email = getString("email")
        val additionalPhones = (get("additionalPhones") as? List<*>)?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() } ?: emptyList()
        val additionalEmails = (get("additionalEmails") as? List<*>)?.mapNotNull { it as? String }
            ?.filter { it.isNotBlank() } ?: emptyList()
        val tier = getString("escalationTier")?.let { EscalationTier.valueOf(it) }
            ?: EscalationTier.EMERGENCY

        return Contact(
            id = 0,
            displayName = name,
            phoneNumber = phone,
            email = email,
            additionalPhones = additionalPhones,
            additionalEmails = additionalEmails,
            escalationTier = tier,
            includeLocation = getBoolean("includeLocation") ?: true,
            autoCall = getBoolean("autoCall") ?: false,
            emergencySoundKey = getString("emergencySoundKey"),
            checkInSoundKey = getString("checkInSoundKey"),
            contactOrder = (getLong("contactOrder") ?: getLong("order") ?: 0L).toInt(),
            allowRemoteSoundChange = getBoolean("allowRemoteSoundChange") ?: false,
            allowRemoteOverride = getBoolean("allowRemoteOverride") ?: true,
            linkStatus = getString("linkStatus")?.let { LinkStatus.valueOf(it) } ?: LinkStatus.NONE,
            linkCode = getString("linkCode"),
            remoteDeviceId = getString("remoteDeviceId"),
            pendingApproval = getBoolean("pendingApproval") ?: false,
            remoteUid = getString("remoteUid")
        )
    }

    private fun mergeContacts(local: List<Contact>, remote: List<Contact>): List<Contact> {
        val localByKey = local.associateBy { contactSyncKey(it) }
        val remoteByKey = remote.associateBy { contactSyncKey(it) }
        val merged = remote.map { remoteContact ->
            mergeContact(localByKey[contactSyncKey(remoteContact)], remoteContact)
        }.toMutableList()
        val localOnly = local.filterNot { remoteByKey.containsKey(contactSyncKey(it)) }
        merged.addAll(localOnly)
        return merged
    }

    private fun mergeContact(local: Contact?, remote: Contact): Contact {
        if (local == null) return remote
        val latestLastSeen = when {
            remote.remoteLastSeen != null && local.remoteLastSeen != null -> maxOf(remote.remoteLastSeen, local.remoteLastSeen)
            remote.remoteLastSeen != null -> remote.remoteLastSeen
            else -> local.remoteLastSeen
        }
        val presence = latestLastSeen?.let { presenceFrom(it) }
            ?: if (remote.remotePresence != RemotePresence.UNKNOWN) remote.remotePresence else local.remotePresence
        val resolvedLinkStatus = if (remote.linkStatus == LinkStatus.NONE && local.linkStatus != LinkStatus.NONE) {
            local.linkStatus
        } else {
            remote.linkStatus
        }
        return remote.copy(
            id = local.id,
            contactOrder = if (remote.contactOrder != 0 || local.contactOrder == 0) remote.contactOrder else local.contactOrder,
            remoteLastSeen = latestLastSeen,
            remotePresence = presence,
            remoteUid = remote.remoteUid ?: local.remoteUid,
            remoteDeviceId = remote.remoteDeviceId ?: local.remoteDeviceId,
            linkCode = remote.linkCode ?: local.linkCode,
            linkStatus = resolvedLinkStatus,
            emergencySoundKey = remote.emergencySoundKey ?: local.emergencySoundKey,
            checkInSoundKey = remote.checkInSoundKey ?: local.checkInSoundKey,
            allowRemoteOverride = remote.allowRemoteOverride || local.allowRemoteOverride,
            allowRemoteSoundChange = remote.allowRemoteSoundChange || local.allowRemoteSoundChange,
            pendingApproval = remote.pendingApproval || local.pendingApproval,
            includeLocation = remote.includeLocation,
            autoCall = remote.autoCall,
            displayName = remote.displayName.ifBlank { local.displayName },
            phoneNumber = remote.phoneNumber.ifBlank { local.phoneNumber },
            email = remote.email ?: local.email,
            additionalPhones = (remote.additionalPhones + local.additionalPhones).distinct().filter { it.isNotBlank() },
            additionalEmails = (remote.additionalEmails + local.additionalEmails).distinct().filter { it.isNotBlank() }
        )
    }

    private fun contactSyncKey(contact: Contact): String {
        val normalizedPhone = normalizePhone(contact.phoneNumber)
        val normalizedAltPhone = contact.additionalPhones.firstOrNull { normalizePhone(it).isNotBlank() }?.let { normalizePhone(it) } ?: ""
        val normalizedEmail = normalizeEmail(contact.email)
        val normalizedAltEmail = contact.additionalEmails.firstOrNull { normalizeEmail(it).isNotBlank() }?.let { normalizeEmail(it) } ?: ""
        return when {
            normalizedPhone.isNotBlank() -> "phone:$normalizedPhone"
            normalizedAltPhone.isNotBlank() -> "phone:$normalizedAltPhone"
            normalizedEmail.isNotBlank() -> "email:$normalizedEmail"
            normalizedAltEmail.isNotBlank() -> "email:$normalizedAltEmail"
            !contact.linkCode.isNullOrBlank() -> "link:${contact.linkCode}"
            !contact.remoteUid.isNullOrBlank() -> "uid:${contact.remoteUid}"
            else -> "name:${contact.displayName.lowercase().trim()}"
        }
    }

    private fun contactDocId(contact: Contact): String {
        val phoneRaw = contact.primaryPhone().orEmpty().trim()
        val normalizedEmail = normalizeEmail(contact.email ?: contact.additionalEmails.firstOrNull())
        return when {
            phoneRaw.isNotBlank() -> phoneRaw
            normalizedEmail.isNotBlank() -> "email_$normalizedEmail"
            !contact.remoteUid.isNullOrBlank() -> "uid_${contact.remoteUid}"
            !contact.linkCode.isNullOrBlank() -> "link_${contact.linkCode}"
            else -> contact.displayName.lowercase().replace("\\s+".toRegex(), "_")
                .ifBlank { contact.displayName.hashCode().toString() }
        }
    }

    suspend fun lookupPublicProfileByPhone(phone: String): PublicProfile? = withContext(Dispatchers.IO) {
        val normalized = normalizePhone(phone)
        if (normalized.isBlank()) return@withContext null
        val snapshot = runCatching {
            firestore.collection("users_by_phone")
                .document(normalized)
                .get()
                .await()
        }.getOrNull() ?: return@withContext null
        if (!snapshot.exists()) return@withContext null
        PublicProfile(
            displayName = snapshot.getString("displayName"),
            avatarUrl = snapshot.getString("avatarUrl"),
            email = snapshot.getString("email")
        )
    }

    private fun normalizePhone(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.startsWith("+")) "+$digits" else digits
    }

    private fun normalizeEmail(input: String?): String =
        input?.trim()?.lowercase() ?: ""

    private fun Contact.primaryPhone(): String? =
        (listOf(phoneNumber) + additionalPhones).firstOrNull { it.isNotBlank() }

    private fun Contact.primaryEmail(): String? =
        (listOfNotNull(email) + additionalEmails).firstOrNull { it.isNotBlank() }

    private fun isUnreachable(contact: Contact): Boolean {
        val hasPhone = contact.primaryPhone().isNullOrBlank().not()
        val hasEmail = !contact.email.isNullOrBlank() || contact.additionalEmails.any { it.isNotBlank() }
        val hasLink = contact.linkStatus != LinkStatus.NONE || !contact.linkCode.isNullOrBlank()
        val hasRemote = !contact.remoteUid.isNullOrBlank() || !contact.remoteDeviceId.isNullOrBlank()
        return !hasPhone && !hasEmail && !hasLink && !hasRemote
    }

    private suspend fun pruneLocalUnreachable() {
        val all = contactRepository.getAll()
        val unreachable = all.filter { isUnreachable(it) }
        unreachable.forEach { contact ->
            contactRepository.delete(contact.id)
            messageRepository.clear(contact.id)
        }
    }

    private fun presenceFrom(lastSeen: Long?): RemotePresence {
        return lastSeen?.let {
            val age = System.currentTimeMillis() - it
            when {
                age < 3 * 60 * 1000L -> RemotePresence.ONLINE
                age < 60 * 60 * 1000L -> RemotePresence.RECENT
                age < 24 * 60 * 60 * 1000L -> RemotePresence.OFFLINE
                else -> RemotePresence.STALE
            }
        } ?: RemotePresence.STALE
    }

    private fun themeFromMap(map: Map<*, *>): ThemePreferences {
        val defaults = ThemePreferences()
        return ThemePreferences(
            primaryColor = map["primaryColor"] as? String ?: defaults.primaryColor,
            secondaryColor = map["secondaryColor"] as? String ?: defaults.secondaryColor,
            bubbleOutgoing = map["bubbleOutgoing"] as? String ?: defaults.bubbleOutgoing,
            bubbleIncoming = map["bubbleIncoming"] as? String ?: defaults.bubbleIncoming,
            backgroundColor = map["backgroundColor"] as? String ?: defaults.backgroundColor,
            iconSizeFactor = (map["iconSizeFactor"] as? Number)?.toFloat() ?: defaults.iconSizeFactor,
            fontStyle = map["fontStyle"] as? String ?: defaults.fontStyle,
            bubbleCornerRadius = (map["bubbleCornerRadius"] as? Number)?.toInt() ?: defaults.bubbleCornerRadius,
            inboxIconVariant = map["inboxIconVariant"] as? String ?: defaults.inboxIconVariant,
            onBubbleOutgoing = map["onBubbleOutgoing"] as? String ?: defaults.onBubbleOutgoing,
            onBubbleIncoming = map["onBubbleIncoming"] as? String ?: defaults.onBubbleIncoming,
            onBackground = map["onBackground"] as? String ?: defaults.onBackground,
            topBarColor = map["topBarColor"] as? String ?: defaults.topBarColor,
            onTopBarColor = map["onTopBarColor"] as? String ?: defaults.onTopBarColor,
            bubbleCornerRadiusTopStart = (map["bubbleCornerRadiusTopStart"] as? Number)?.toInt(),
            bubbleCornerRadiusTopEnd = (map["bubbleCornerRadiusTopEnd"] as? Number)?.toInt(),
            bubbleCornerRadiusBottomStart = (map["bubbleCornerRadiusBottomStart"] as? Number)?.toInt(),
            bubbleCornerRadiusBottomEnd = (map["bubbleCornerRadiusBottomEnd"] as? Number)?.toInt(),
            timestampColor = map["timestampColor"] as? String,
            dividerColor = map["dividerColor"] as? String,
            appBackgroundGradientStart = map["appBackgroundGradientStart"] as? String,
            appBackgroundGradientEnd = map["appBackgroundGradientEnd"] as? String,
            fontScale = (map["fontScale"] as? Number)?.toFloat() ?: defaults.fontScale
        )
    }

    private fun themeToMap(theme: ThemePreferences): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "primaryColor" to theme.primaryColor,
            "secondaryColor" to theme.secondaryColor,
            "bubbleOutgoing" to theme.bubbleOutgoing,
            "bubbleIncoming" to theme.bubbleIncoming,
            "backgroundColor" to theme.backgroundColor,
            "iconSizeFactor" to theme.iconSizeFactor,
            "fontStyle" to theme.fontStyle,
            "bubbleCornerRadius" to theme.bubbleCornerRadius,
            "inboxIconVariant" to theme.inboxIconVariant,
            "onBubbleOutgoing" to theme.onBubbleOutgoing,
            "onBubbleIncoming" to theme.onBubbleIncoming,
            "onBackground" to theme.onBackground,
            "topBarColor" to theme.topBarColor,
            "onTopBarColor" to theme.onTopBarColor,
            "fontScale" to theme.fontScale
        )
        theme.bubbleCornerRadiusTopStart?.let { payload["bubbleCornerRadiusTopStart"] = it }
        theme.bubbleCornerRadiusTopEnd?.let { payload["bubbleCornerRadiusTopEnd"] = it }
        theme.bubbleCornerRadiusBottomStart?.let { payload["bubbleCornerRadiusBottomStart"] = it }
        theme.bubbleCornerRadiusBottomEnd?.let { payload["bubbleCornerRadiusBottomEnd"] = it }
        theme.timestampColor?.let { payload["timestampColor"] = it }
        theme.dividerColor?.let { payload["dividerColor"] = it }
        theme.appBackgroundGradientStart?.let { payload["appBackgroundGradientStart"] = it }
        theme.appBackgroundGradientEnd?.let { payload["appBackgroundGradientEnd"] = it }
        return payload
    }

    private suspend fun resolveLinkFromDoc(contact: Contact, code: String, uid: String): Contact {
        return runCatching {
            val doc = firestore.collection(ContactLinkManager.COLLECTION_LINKS).document(code).get().await()
            val uids = (doc.get("uids") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
            val remoteUid = uids.firstOrNull { it != uid }
            if (remoteUid == null) return contact
            val lastSeenMap = doc.get("lastSeen") as? Map<*, *>
            val remoteLastSeen = (lastSeenMap?.get(remoteUid) as? com.google.firebase.Timestamp)
                ?.toDate()
                ?.time
            val phones = doc.get("phones") as? Map<*, *>
            val remotePhone = phones?.get(remoteUid) as? String
            contact.copy(
                remoteUid = remoteUid,
                remoteLastSeen = remoteLastSeen,
                remotePresence = presenceFrom(remoteLastSeen),
                linkStatus = LinkStatus.LINKED,
                phoneNumber = if (!remotePhone.isNullOrBlank()) remotePhone else contact.phoneNumber,
                linkCode = contact.linkCode ?: code
            )
        }.getOrElse {
            Log.w(TAG, "Unable to resolve link doc for $code", it)
            contact
        }
    }

    private fun Contact.ensureRemoteUid(): Contact {
        // If we ever add remote UID to handshake, prefer stored value; this is a placeholder for future hook.
        return this
    }

    private suspend fun upsertContactInCloud(user: FirebaseUser, contact: Contact) {
        val docId = contactDocId(contact)
        val payload = mapOf(
            "displayName" to contact.displayName,
            "phoneNumber" to contact.phoneNumber,
            "email" to contact.email,
            "ownerUid" to user.uid,
            "userId" to user.uid,
            "additionalPhones" to contact.additionalPhones,
            "additionalEmails" to contact.additionalEmails,
            "escalationTier" to contact.escalationTier.name,
            "includeLocation" to contact.includeLocation,
            "autoCall" to contact.autoCall,
            "emergencySoundKey" to contact.emergencySoundKey,
            "checkInSoundKey" to contact.checkInSoundKey,
            "contactOrder" to contact.contactOrder,
            "allowRemoteSoundChange" to contact.allowRemoteSoundChange,
            "allowRemoteOverride" to contact.allowRemoteOverride,
            "linkStatus" to contact.linkStatus.name,
            "linkCode" to contact.linkCode,
            "remoteDeviceId" to contact.remoteDeviceId,
            "pendingApproval" to contact.pendingApproval,
            "remoteUid" to contact.remoteUid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        runCatching {
            firestore.collection(COLLECTION_USERS).document(user.uid)
                .collection(COLLECTION_TRUSTED_CONTACTS)
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to upsert contact in cloud", error)
        }
    }

    private suspend fun deleteContactInCloud(user: FirebaseUser, contact: Contact) {
        val docId = contactDocId(contact)
        runCatching {
            firestore.collection(COLLECTION_USERS).document(user.uid)
                .collection(COLLECTION_TRUSTED_CONTACTS)
                .document(docId)
                .delete()
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to delete contact in cloud", error)
        }
    }

    fun cancelEmergency(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val cancelled = linkManager.cancelActiveEmergency()
            if (cancelled) {
                emergencyActive.value = false
                settingsRepository.setEmergencyActive(false)
                widgetStateManager.requestWidgetUpdate()
            }
            onComplete(cancelled)
        }
    }

    private fun dispatch(tier: EscalationTier, phrase: String) {
        viewModelScope.launch {
            dispatching.value = true
            lastMessage.value = phrase
            val result = alertRouter.dispatchManual(tier, phrase)
            if (tier == EscalationTier.EMERGENCY && result != null) {
                emergencyActive.value = true
                settingsRepository.setEmergencyActive(true)
                widgetStateManager.requestWidgetUpdate()
            }
            emitDndStatus(result)
            dispatching.value = false
        }
    }

    fun buildBugReportUri(context: Context, bugReportData: BugReportData): Uri {
        val packageManager = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = packageInfo.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val osVersion = Build.VERSION.RELEASE ?: "unknown"
        val apiLevel = Build.VERSION.SDK_INT

        val formattedBody = buildString {
            appendLine("Summary: ${bugReportData.summary}")
            appendLine()
            appendLine("Steps to Reproduce:")
            appendLine(bugReportData.stepsToReproduce.ifBlank { "N/A" })
            appendLine()
            appendLine("Expected Behavior:")
            appendLine(bugReportData.expectedBehavior.ifBlank { "N/A" })
            appendLine()
            appendLine("Actual Behavior:")
            appendLine(bugReportData.actualBehavior)
            appendLine()
            appendLine("Frequency: ${bugReportData.frequency}")
            appendLine("Severity: ${bugReportData.severity}")
            if (bugReportData.userEmail.isNotBlank()) {
                appendLine("Reporter Email: ${bugReportData.userEmail}")
            }
            appendLine()
            appendLine("App Version: $versionName ($versionCode)")
            appendLine("Device: $manufacturer $model")
            appendLine("OS: Android $osVersion (API $apiLevel)")
        }

        val subjectSuffix = bugReportData.summary.ifBlank { "General issue" }

        return Uri.parse(BUG_REPORT_PAGE_URL).buildUpon()
            .appendQueryParameter("summary", bugReportData.summary)
            .appendQueryParameter("steps", bugReportData.stepsToReproduce)
            .appendQueryParameter("expected", bugReportData.expectedBehavior)
            .appendQueryParameter("actual", bugReportData.actualBehavior)
            .appendQueryParameter("frequency", bugReportData.frequency)
            .appendQueryParameter("severity", bugReportData.severity)
            .appendQueryParameter("reporter", bugReportData.userEmail)
            .appendQueryParameter("version_name", versionName)
            .appendQueryParameter("version_code", versionCode.toString())
            .appendQueryParameter(
                "build_flavor",
                when {
                    BuildConfig.PREMIUM_FEATURES -> "premium"
                    BuildConfig.ADS_ENABLED -> "free"
                    else -> "pro"
                }
            )
            .appendQueryParameter("device", "$manufacturer $model")
            .appendQueryParameter("os_version", "Android $osVersion (API $apiLevel)")
            .appendQueryParameter("summary_suffix", subjectSuffix)
            .appendQueryParameter("body", formattedBody)
            .build()
    }

    private fun ensureSoundDefaults(settings: com.pulselink.domain.model.PulseLinkSettings): com.pulselink.domain.model.PulseLinkSettings {
        var updatedSettings = settings
        if (settings.emergencyProfile.soundKey == null) {
            soundCatalog.defaultKeyFor(SoundCategory.SIREN)?.let { defaultKey ->
                viewModelScope.launch {
                    settingsRepository.update { current ->
                        current.copy(
                            emergencyProfile = current.emergencyProfile.copy(soundKey = defaultKey)
                        )
                    }
                }
                updatedSettings = updatedSettings.copy(
                    emergencyProfile = updatedSettings.emergencyProfile.copy(soundKey = defaultKey)
                )
            }
        }
        if (settings.checkInProfile.soundKey == null) {
            soundCatalog.defaultKeyFor(SoundCategory.CHIME)?.let { defaultKey ->
                viewModelScope.launch {
                    settingsRepository.update { current ->
                        current.copy(
                            checkInProfile = current.checkInProfile.copy(soundKey = defaultKey)
                        )
                    }
                }
                updatedSettings = updatedSettings.copy(
                    checkInProfile = updatedSettings.checkInProfile.copy(soundKey = defaultKey)
                )
            }
        }
        if (settings.callSoundKey == null) {
            soundCatalog.defaultKeyFor(SoundCategory.CALL)?.let { defaultKey ->
                viewModelScope.launch {
                    settingsRepository.update { current ->
                        current.copy(callSoundKey = defaultKey)
                    }
                }
                updatedSettings = updatedSettings.copy(callSoundKey = defaultKey)
            }
        }
        return updatedSettings
    }

    sealed class CallInitiationResult {
        object Ready : CallInitiationResult()
        object Timeout : CallInitiationResult()
        object Failure : CallInitiationResult()
    }

    sealed interface DeleteAccountState {
        object Idle : DeleteAccountState
        object Loading : DeleteAccountState
        object Success : DeleteAccountState
        data class Error(val message: String) : DeleteAccountState
    }

    fun clearDndStatusMessage() {
        dndStatus.value = null
    }

    fun setTimeFormat(format: com.pulselink.domain.model.TimeFormat) {
        viewModelScope.launch {
            settingsRepository.setTimeFormat(format)
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                pushSettingsToCloud(user, mapOf("timeFormat" to format.name))
            }
        }
    }

    fun setThemePreferences(theme: com.pulselink.domain.model.ThemePreferences) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.settings.first()
            val oldTheme = currentSettings.themePreferences
            settingsRepository.setThemePreferences(theme)
            if (currentSettings.beaconLauncherEnabled && oldTheme.inboxIconVariant != theme.inboxIconVariant) {
                applyInboxIconVariant(theme.inboxIconVariant, enabled = true)
            }
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                pushThemeToCloud(user, theme)
            }
        }
    }

    private fun applyInboxIconVariant(variant: String, enabled: Boolean) {
        try {
            BeaconIconManager.apply(context, variant, enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update app icon", e)
        }
    }

    fun setRemoteWebAccess(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemoteWebAccessEnabled(enabled)
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                pushSettingsToCloud(user, mapOf("remoteWebAccessEnabled" to enabled))
            }
        }
    }

    fun setOtpCleanupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOtpCleanupEnabled(enabled)
        }
    }

    fun setOtpCleanupDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setOtpCleanupDays(days)
        }
    }

    fun setPrivatePinHash(hash: String?) {
        viewModelScope.launch {
            settingsRepository.setPrivatePinHash(hash)
        }
    }

    fun setThreadPrivacy(threadId: Long, address: String, makePrivate: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { current ->
                val currentSet = current.privateThreadIds.toMutableSet()
                if (makePrivate) currentSet.add(threadId) else currentSet.remove(threadId)
                current.copy(privateThreadIds = currentSet.toList())
            }

            // Also mark the contact as private if found
            val phone = stripSmsDisplayName(address)
            val contact = contactRepository.getByPhone(phone)
                ?: contactRepository.getByPhone(normalizePhone(phone))

            if (contact != null) {
                saveContact(contact.copy(isPrivate = makePrivate))
            }
        }
    }

    fun setBeaconLauncherEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBeaconLauncherEnabled(enabled)
            val variant = settingsRepository.settings.first().themePreferences.inboxIconVariant
            applyInboxIconVariant(variant, enabled)
        }
    }

    fun setBeaconHintDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setBeaconHintDismissed(dismissed)
        }
    }

    fun setWebAccessHintDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWebAccessHintDismissed(dismissed)
        }
    }

    fun setFirebaseMessagingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFirebaseMessagingEnabled(enabled)
        }
    }

    fun setEmailFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEmailFallbackEnabled(enabled)
        }
    }

    fun setThirdPartyExtensionsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThirdPartyExtensionsEnabled(enabled)
        }
    }

    fun setCrashDetectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setCrashDetectionEnabled(enabled)
        }
    }

    fun setAiSummariesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiSummariesEnabled(enabled)
        }
    }

    fun setAiComposeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiComposeEnabled(enabled)
        }
    }

    fun setAiUrgencyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiUrgencyEnabled(enabled)
        }
    }

    fun setAiUrgencyBypassDnd(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiUrgencyBypassDnd(enabled)
        }
    }

    fun setAiUrgencyIncludeUnknown(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAiUrgencyIncludeUnknown(enabled)
        }
    }

    private fun emitDndStatus(result: AlertResult?) {
        val overrideResult = result?.overrideResult ?: run {
            dndStatus.value = null
            return
        }
        val messageRes = when {
            overrideResult.reason == AudioOverrideManager.OverrideResult.FailureReason.POLICY_PERMISSION_MISSING ->
                R.string.audio_override_permission_missing
            overrideResult.state == AudioOverrideManager.OverrideResult.State.PARTIAL ->
                R.string.audio_override_partial
            overrideResult.state == AudioOverrideManager.OverrideResult.State.FAILURE ->
                R.string.audio_override_failed
            else -> null
        }
        dndStatus.value = messageRes?.let { DndStatusMessage(it) }
    }

    companion object {
        const val TAG = "MainViewModel"
        const val BETA_AGREEMENT_VERSION = "2025.12.19"
        private const val REMOTE_BETA_AGREEMENT_TIMEOUT_MS = 10_000L
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TRUSTED_CONTACTS = "trustedContacts"
        const val BUG_REPORT_PAGE_URL = "https://pulselink.app/bug-report/"
    }
}

data class PublicProfile(
    val displayName: String?,
    val avatarUrl: String?,
    val email: String?
)
