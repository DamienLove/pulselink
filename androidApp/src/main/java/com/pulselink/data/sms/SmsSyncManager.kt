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
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

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
    }

    companion object {
        private const val TAG = "SmsSyncManager"
    }
}
