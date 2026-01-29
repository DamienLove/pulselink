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
import kotlinx.coroutines.launch
import com.pulselink.domain.model.PulseLinkSettings
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class SmsSyncManager @Inject constructor(
    private val smsRepository: SmsRepository,
    private val smsSyncTrigger: SmsSyncTrigger,
    private val settingsRepository: SettingsRepository,
    private val smsCloudSynchronizer: SmsCloudSynchronizer
) {
    @androidx.annotation.VisibleForTesting
    var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isStarted = AtomicBoolean(false)

    @OptIn(FlowPreview::class)
    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        // 1. Observe SMS DB changes
        smsRepository.changes()
            .debounce(500L) // Debounce for 0.5 seconds to batch rapid changes
            .onEach {
                try {
                    val settings = settingsRepository.settings.first()
                    if (settings.remoteWebAccessEnabled) {
                        if (settings.premiumUnlocked) {
                            Log.d(TAG, "Repository changed, Premium user: running immediate sync")
                            // Launch immediate sync in IO scope, bypassing WorkManager
                            scope.launch {
                                smsCloudSynchronizer.sync()
                            }
                        } else {
                            Log.d(TAG, "Repository changed, Free user: triggering WorkManager sync")
                            smsSyncTrigger.triggerSync()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking settings for sync", e)
                }
            }
            .launchIn(scope)

        // 2. Observe Settings changes to immediately sync when Premium/Web Access changes
        scope.launch {
            var previousSettings: PulseLinkSettings? = null
            settingsRepository.settings.collect { currentSettings ->
                try {
                    val prev = previousSettings
                    previousSettings = currentSettings

                    if (prev == null) {
                        // Initial load: trigger sync if enabled to ensure consistency
                        if (currentSettings.remoteWebAccessEnabled) {
                            if (currentSettings.premiumUnlocked) {
                                scope.launch { smsCloudSynchronizer.sync() }
                            } else {
                                smsSyncTrigger.triggerSync()
                            }
                        }
                        return@collect
                    }

                    // Automatically enable Remote Web Access when upgrading to Premium
                    if (!prev.premiumUnlocked && currentSettings.premiumUnlocked && !currentSettings.remoteWebAccessEnabled) {
                        Log.i(TAG, "Premium upgraded, automatically enabling remote web access and initiating sync chain")
                        settingsRepository.setRemoteWebAccessEnabled(true)
                        // The update will trigger a new emission, so we don't need to sync here
                        return@collect
                    }

                    val shouldSync = (prev.remoteWebAccessEnabled != currentSettings.remoteWebAccessEnabled) ||
                            (prev.premiumUnlocked != currentSettings.premiumUnlocked) ||
                            (prev.proUnlocked != currentSettings.proUnlocked)

                    if (shouldSync) {
                        Log.d(TAG, "Settings changed (premium/web), triggering immediate sync")
                        if (currentSettings.premiumUnlocked) {
                            scope.launch { smsCloudSynchronizer.sync() }
                        } else {
                            smsSyncTrigger.triggerSync()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling settings change in SmsSyncManager", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SmsSyncManager"
    }
}
