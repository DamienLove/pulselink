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
import com.pulselink.data.alert.AlertDispatcher.AlertResult
import com.pulselink.data.alert.SoundCatalog
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.SoundCategory
import com.pulselink.domain.repository.AlertRepository
import com.pulselink.domain.repository.BlockedContactRepository
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.domain.repository.MessageRepository
import com.pulselink.domain.repository.SettingsRepository
import com.pulselink.service.AlertRouter
import com.pulselink.ui.screens.BugReportData
import com.pulselink.ui.state.DndStatusMessage
import com.pulselink.util.AudioOverrideManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val blockedContactRepository: BlockedContactRepository
) : ViewModel() {

    private val dispatching = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow<String?>(null)
    private val dndStatus = MutableStateFlow<DndStatusMessage?>(null)
    private val emergencyActive = MutableStateFlow(false)
    private val emergencySounds = soundCatalog.emergencyOptions()
    private val checkInSounds = soundCatalog.checkInOptions()

    private val _uiState = MutableStateFlow(PulseLinkUiState())
    val uiState: StateFlow<PulseLinkUiState> = _uiState

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
    }

    fun saveContact(contact: Contact) {
        viewModelScope.launch {
            val nextOrder = if (contact.id == 0L) {
                (_uiState.value.contacts.maxOfOrNull { it.contactOrder } ?: -1) + 1
            } else {
                contact.contactOrder
            }
            contactRepository.upsert(
                if (contact.id == 0L) contact.copy(contactOrder = nextOrder) else contact
            )
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            val contact = contactRepository.getContact(id)
            contact?.let {
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
        }
    }

    fun setAutoAllowRemoteSoundChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAllowRemoteSoundChange(enabled)
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

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            contactRepository.upsert(contact)
        }
    }

    fun updateContactSounds(contactId: Long, emergencyKey: String?, checkInKey: String?) {
        viewModelScope.launch {
            val contact = uiState.value.contacts.firstOrNull { it.id == contactId } ?: return@launch
            contactRepository.upsert(
                contact.copy(
                    emergencySoundKey = emergencyKey ?: contact.emergencySoundKey,
                    checkInSoundKey = checkInKey ?: contact.checkInSoundKey
                )
            )
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
        private const val BUG_REPORT_PAGE_URL = "https://DamienLove.github.io/PulseLink/bug-report/"
    }
}
