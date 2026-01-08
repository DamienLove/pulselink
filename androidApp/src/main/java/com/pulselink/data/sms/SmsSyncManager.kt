package com.pulselink.data.sms

import android.util.Log
import com.pulselink.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages the automatic synchronization of SMS messages to the cloud.
 * Listens for local repository changes and triggers a sync worker if enabled.
 */
@Singleton
class SmsSyncManager @Inject constructor(
    private val smsRepository: SmsRepository,
    private val smsSyncTrigger: SmsSyncTrigger,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isStarted = AtomicBoolean(false)
    private var syncJob: Job? = null

    /**
     * Starts listening for SMS repository changes.
     * This method is idempotent; calling it multiple times has no effect.
     */
    @OptIn(FlowPreview::class)
    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        // Initial check for sync on startup
        scope.launch {
             try {
                val settings = settingsRepository.settings.first()
                if (settings.remoteWebAccessEnabled) {
                    Log.d(TAG, "Initial startup sync trigger")
                    smsSyncTrigger.triggerSync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking settings for initial sync", e)
            }
        }

        syncJob = smsRepository.changes()
            .debounce(2000L) // Debounce for 2 seconds to batch rapid changes
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
    }

    /**
     * Stops listening for SMS repository changes.
     */
    fun stop() {
        if (isStarted.compareAndSet(true, false)) {
            syncJob?.cancel()
            syncJob = null
        }
    }

    /**
     * Cleans up resources. Should be called when the application is terminating
     * or the component is being destroyed.
     */
    fun destroy() {
        stop()
        scope.cancel()
    }

    companion object {
        private const val TAG = "SmsSyncManager"
    }
}
