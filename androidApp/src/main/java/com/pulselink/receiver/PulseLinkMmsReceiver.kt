package com.pulselink.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.pulselink.service.AlertRouter

@AndroidEntryPoint
class PulseLinkMmsReceiver : BroadcastReceiver() {

    @Inject lateinit var alertRouter: AlertRouter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.WAP_PUSH_DELIVER") return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // MMS payload parsing is heavy; route to alert handler as placeholder.
                alertRouter.onInboundMessage("[MMS received]")
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to handle inbound MMS", t)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PulseLinkMmsReceiver"
    }
}
