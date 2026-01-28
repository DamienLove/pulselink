package com.pulselink.data.sms

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.pulselink.R
import com.pulselink.data.alert.NotificationRegistrar
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsCloudSynchronizer: SmsCloudSynchronizer,
    private val notificationRegistrar: NotificationRegistrar
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notificationRegistrar.ensureChannels()
        val notification = NotificationCompat.Builder(applicationContext, NotificationRegistrar.CHANNEL_BACKGROUND)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText("Syncing messages...")
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .build()
        return ForegroundInfo(1001, notification)
    }

    override suspend fun doWork(): Result {
        val targetedThreadId = inputData.getLong("threadId", -1L)
        val result = smsCloudSynchronizer.sync(targetedThreadId)
        return if (result.isSuccess) Result.success() else Result.retry()
    }
}
