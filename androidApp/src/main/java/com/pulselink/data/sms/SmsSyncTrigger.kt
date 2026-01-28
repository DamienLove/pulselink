package com.pulselink.data.sms

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SmsSyncTrigger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsCloudSynchronizer: SmsCloudSynchronizer,
    @Named("ApplicationScope") private val applicationScope: CoroutineScope
) {

    fun triggerSync(threadId: Long? = null) {
        // Immediate execution for premium users (handled by internal checks in synchronizer)
        // We launch it in application scope to ensure it survives activity/receiver lifecycle
        applicationScope.launch {
            smsCloudSynchronizer.sync(threadId ?: -1L)
        }

        // Also schedule WorkManager as a reliability fallback
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val builder = OneTimeWorkRequestBuilder<SmsSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)

        val workName: String
        if (threadId != null) {
            val data = Data.Builder()
                .putLong("threadId", threadId)
                .build()
            builder.setInputData(data)
            // Use a unique work name per thread to allow parallel syncing of different threads
            workName = "SmsSyncTargeted_$threadId"
        } else {
            workName = "SmsSyncFull"
        }

        val syncRequest = builder.build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }
}
