package com.pulselink.data.sms

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSyncTrigger @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun triggerSync(threadId: Long? = null) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val builder = OneTimeWorkRequestBuilder<SmsSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)

        if (threadId != null) {
            builder.setInputData(workDataOf("threadId" to threadId))
        }

        // Use a unique name that depends on threadId if specific, or general if full sync?
        // Actually, if we use APPEND_OR_REPLACE with "SmsSyncOnDemand", a full sync might be replaced by a targeted sync or vice-versa.
        // If we want immediate targeted sync, maybe use a different name or just APPEND.
        // If we use APPEND, they run sequentially.
        // If we use REPLACE, the current one is cancelled.
        // If a Full Sync is running, and we trigger a Targeted Sync, we probably want both?
        // Or if Targeted Sync is faster, we want it to run.

        // Let's use a unique name for targeted syncs to avoid clashing with full syncs.
        val workName = if (threadId != null) "SmsSyncTargeted_$threadId" else "SmsSyncOnDemand"

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            builder.build()
        )
    }
}
