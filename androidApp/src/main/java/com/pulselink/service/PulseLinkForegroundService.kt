package com.pulselink.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.pulselink.R
import com.pulselink.data.alert.NotificationRegistrar
import com.pulselink.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PulseLinkForegroundService : Service() {

    @Inject lateinit var notificationRegistrar: NotificationRegistrar
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)

    override fun onCreate() {
        super.onCreate()
        notificationRegistrar.ensureChannels()
        startForeground(NOTIFICATION_ID, buildNotification(listening = true))
        observeListeningState()
        ContextCompat.startForegroundService(
            this,
            Intent(this, PulseLinkVoiceService::class.java)
        )
    }

    private fun observeListeningState() {
        serviceScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                val notification = buildNotification(settings.listeningEnabled)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildNotification(listening: Boolean): Notification {
        return NotificationCompat.Builder(this, NotificationRegistrar.CHANNEL_LISTENING)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(
                if (listening) getString(R.string.status_listening_on)
                else getString(R.string.status_listening_off)
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 42

        fun enqueue(context: Context) {
            val intent = Intent(context, PulseLinkForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PulseLinkForegroundService::class.java))
        }
    }
}
