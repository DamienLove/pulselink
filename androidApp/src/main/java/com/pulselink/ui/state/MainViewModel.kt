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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
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
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val dispatching = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow<String?>(null)
    private val dndStatus = MutableStateFlow<DndStatusMessage?>(null)
    private val emergencyActive = MutableStateFlow(false)
    private val emergencySounds = soundCatalog.emergencyOptions()
    private val checkInSounds = soundCatalog.checkInOptions()
    private val callSounds = soundCatalog.callOptions()

    private val _uiState = MutableStateFlow(PulseLinkUiState())
    val uiState: StateFlow<PulseLinkUiState> = _uiState
    val authState: StateFlow<AuthState> = firebaseAuthManager.authState

    init {
        viewModelScope.launch {
            val baseState = combine(
                settingsRepository.settings,
                contactRepository.observeContacts(),
                alertRepository.observeRecent(10),
                dispatching,
                lastMessage
            ) { settings, contacts, events, isDispatching, message ->
                val normalizedSettings = ensureSoundDefaults(settings)
                val adsAvailable = BuildConfig.ADS_ENABLED
                val showAds = adsAvailable && !normalizedSettings.proUnlocked
                val isProUser = normalizedSettings.proUnlocked || !adsAvailable

                PulseLinkUiState(
                    settings = normalizedSettings,
                    contacts = contacts,
                    recentEvents = events,
                    isDispatching = isDispatching,
                    lastMessagePreview = message,
                    emergencySoundOptions = emergencySounds,
                    checkInSoundOptions = checkInSounds,
                    callSoundOptions = callSounds,
                    showAds = showAds,
                    isProUser = isProUser,
                    adsAvailable = adsAvailable,
                    onboardingComplete = normalizedSettings.onboardingComplete
                )
            }
            combine(baseState, dndStatus, emergencyActive) { state, dndStatusMessage, isEmergencyActive ->
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

            // Mirror to cloud for authenticated users
            (firebaseAuthManager.currentUser()?.takeIf { !it.isAnonymous })?.let { user ->
                upsertContactInCloud(user, storedContact)
            }

            if (isNewContact && contact.phoneNumber.isNotBlank()) {
                runCatching {
                    val persisted = contactRepository.getByPhone(contact.phoneNumber)
                    if (persisted != null) {
                        linkManager.sendLinkRequest(persisted.id)
                    }
                }.onFailure { error ->
                    Log.w(TAG, "Unable to auto-send link request for ${contact.displayName}", error)
                }
            }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(id)
            contact?.let {
                (firebaseAuthManager.currentUser()?.takeIf { user -> !user.isAnonymous })?.let { user ->
                    deleteContactInCloud(user, it.phoneNumber)
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

    fun setAutoAllowRemoteSoundChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAllowRemoteSoundChange(enabled)
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

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.upsert(contact)
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

    suspend fun sendManualMessage(contactId: Long, message: String): ManualMessageResult =
        linkManager.sendManualMessage(contactId, message)

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

    fun needsBetaAgreement(settings: com.pulselink.domain.model.PulseLinkSettings): Boolean =
        settings.betaAgreementVersion != BETA_AGREEMENT_VERSION

    suspend fun processVoiceCommand(query: String): VoiceCommandResult =
        naturalLanguageCommandProcessor.handleCommand(query)

    private suspend fun syncProfileFromCloud(user: FirebaseUser) {
        runCatching {
            val localSettings = settingsRepository.settings.first()
            val profileRef = firestore.collection("users").document(user.uid)
            val snapshot = profileRef.get().await()
            val remoteName = snapshot.getString("ownerName")
                ?: user.displayName
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
        }.onFailure { error ->
            Log.w(TAG, "Unable to sync profile", error)
        }
    }

    private suspend fun pushProfileToCloud(user: FirebaseUser, ownerName: String) {
        runCatching {
            firestore.collection("users").document(user.uid)
                .set(mapOf("ownerName" to ownerName), SetOptions.merge())
                .await()
        }.onFailure { error ->
            Log.w(TAG, "Unable to push profile name", error)
        }
    }

    private suspend fun syncContactsFromCloud(user: FirebaseUser, forcePushLocal: Boolean = false) {
        runCatching {
            val localContacts = contactRepository.getAll()
            val snapshot = firestore.collection(COLLECTION_USERS).document(user.uid)
                .collection(COLLECTION_TRUSTED_CONTACTS)
                .get()
                .await()
            val remoteContacts = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("displayName") ?: return@mapNotNull null
                val phone = doc.getString("phoneNumber") ?: ""
                val tier = doc.getString("escalationTier")?.let { EscalationTier.valueOf(it) }
                    ?: EscalationTier.EMERGENCY
                Contact(
                    id = 0,
                    displayName = name,
                    phoneNumber = phone,
                    escalationTier = tier,
                    includeLocation = doc.getBoolean("includeLocation") ?: true,
                    autoCall = doc.getBoolean("autoCall") ?: false,
                    emergencySoundKey = doc.getString("emergencySoundKey"),
                    checkInSoundKey = doc.getString("checkInSoundKey"),
                    contactOrder = (doc.getLong("contactOrder") ?: 0L).toInt(),
                    allowRemoteSoundChange = doc.getBoolean("allowRemoteSoundChange") ?: false,
                    allowRemoteOverride = doc.getBoolean("allowRemoteOverride") ?: false,
                    linkStatus = doc.getString("linkStatus")?.let { LinkStatus.valueOf(it) }
                        ?: LinkStatus.NONE,
                    linkCode = doc.getString("linkCode"),
                    remoteDeviceId = doc.getString("remoteDeviceId"),
                    pendingApproval = doc.getBoolean("pendingApproval") ?: false,
                    remoteUid = doc.getString("remoteUid")
                )
            }
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

            val remoteKeys = enrichedRemote.map { contactSyncKey(it) }.toSet()
            val localOnlyKeys = localContacts
                .filterNot { remoteKeys.contains(contactSyncKey(it)) }
                .map { contactSyncKey(it) }
                .toSet()

            contactRepository.clear()
            merged.sortedBy { it.contactOrder }
                .forEachIndexed { index, contact ->
                    val normalized = contact.copy(contactOrder = index)
                    contactRepository.upsert(normalized)
                    if (forcePushLocal || enrichedRemote.isEmpty() || localOnlyKeys.contains(contactSyncKey(contact))) {
                        upsertContactInCloud(user, normalized)
                    }
                }
        }.onFailure { error ->
            Log.w(TAG, "Unable to sync contacts from cloud", error)
        }
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
            phoneNumber = remote.phoneNumber.ifBlank { local.phoneNumber }
        )
    }

    private fun contactSyncKey(contact: Contact): String {
        val normalizedPhone = normalizePhone(contact.phoneNumber)
        return when {
            normalizedPhone.isNotBlank() -> "phone:$normalizedPhone"
            !contact.linkCode.isNullOrBlank() -> "link:${contact.linkCode}"
            !contact.remoteUid.isNullOrBlank() -> "uid:${contact.remoteUid}"
            else -> "name:${contact.displayName.lowercase().trim()}"
        }
    }

    private fun normalizePhone(input: String): String =
        input.filter { it.isDigit() || it == '+' }

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
        val docId = contact.phoneNumber.ifBlank {
            contact.displayName.lowercase().replace("\\s+".toRegex(), "_")
                .ifBlank { contact.displayName.hashCode().toString() }
        }
        val payload = mapOf(
            "displayName" to contact.displayName,
            "phoneNumber" to contact.phoneNumber,
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

    private suspend fun deleteContactInCloud(user: FirebaseUser, phoneNumber: String) {
        val docId = phoneNumber.ifBlank { return }
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
            .appendQueryParameter("build_flavor", if (BuildConfig.ADS_ENABLED) "free" else "pro")
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

    fun clearDndStatusMessage() {
        dndStatus.value = null
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
        private const val TAG = "MainViewModel"
        // Public bug portal (no GitHub login required)
        const val BUG_REPORT_PAGE_URL = "https://damiennichols.com/report-bug/"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TRUSTED_CONTACTS = "trustedContacts"
        const val BETA_AGREEMENT_VERSION = "2025-11-13"
        private const val REMOTE_BETA_AGREEMENT_TIMEOUT_MS = 10_000L
    }
}
