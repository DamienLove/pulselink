package com.pulselink.data.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SmsSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val smsCloudSync: SmsCloudSync
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val success = smsCloudSync.sync()
        return if (success) Result.success() else Result.retry()
    }
}
