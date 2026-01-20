package com.pulselink.data.sms

import android.util.Log
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages event-driven SMS synchronization.
 *
 * POLICY: SMS sync is strictly EVENT-DRIVEN.
 * - Triggers immediately when Premium/Pro status unlocks.
 * - Triggers immediately when "Remote Web Access" is enabled.
 * - Triggers immediately when a new message is received or sent (via SmsRepository/SmsStore).
 * - NO periodic/interval synchronization is allowed for SMS.
 */
@Singleton
class SmsSyncManager @Inject constructor(
    private val smsRepository: SmsRepository,
    private val smsSyncTrigger: SmsSyncTrigger,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isStarted = AtomicBoolean(false)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        // 1. Observe SMS DB changes
        // This catches changes from default SMS app if it's not PulseLink, or generic updates.
        // SmsStore also explicitly triggers sync for immediate reaction.
        smsRepository.changes()
            .debounce(500L) // Debounce for 0.5 seconds to batch rapid changes
            .onEach {
                try {
                    val settings = settingsRepository.settings.first()
                    if (settings.remoteWebAccessEnabled) {
                        Log.d(TAG, "Repository changed, triggering web sync")
                        smsSyncTrigger.triggerSync()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking settings for sync", e)
                }
            }
            .launchIn(scope)

        // 2. Observe Settings changes to immediately sync when Premium/Web Access changes
        // This ensures that "As soon as they get premium" requirement is met.
        settingsRepository.settings
            .distinctUntilChanged { old, new ->
                old.remoteWebAccessEnabled == new.remoteWebAccessEnabled &&
                    old.premiumUnlocked == new.premiumUnlocked &&
                    old.proUnlocked == new.proUnlocked
            }
            .onEach {
                Log.d(TAG, "Settings changed (premium/web), triggering sync worker")
                smsSyncTrigger.triggerSync()
            }
            .launchIn(scope)
    }

    companion object {
        private const val TAG = "SmsSyncManager"
    }
}
