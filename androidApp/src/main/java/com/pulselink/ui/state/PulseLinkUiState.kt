package com.pulselink.ui.state

import androidx.annotation.StringRes
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.model.SoundOption

data class PulseLinkUiState(
    val settings: PulseLinkSettings = PulseLinkSettings(),
    val contacts: List<Contact> = emptyList(),
    val recentEvents: List<AlertEvent> = emptyList(),
    val isDispatching: Boolean = false,
    val lastMessagePreview: String? = null,
    val emergencySoundOptions: List<SoundOption> = emptyList(),
    val checkInSoundOptions: List<SoundOption> = emptyList(),
    val showAds: Boolean = false,
    val isProUser: Boolean = true,
    val adsAvailable: Boolean = false,
    val onboardingComplete: Boolean = false,
    val dndStatus: DndStatusMessage? = null
)

data class DndStatusMessage(
    @StringRes val messageResId: Int
)
