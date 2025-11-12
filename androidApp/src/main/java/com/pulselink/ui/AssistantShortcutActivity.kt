package com.pulselink.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pulselink.R
import com.pulselink.domain.model.EscalationTier
import com.pulselink.service.AlertRouter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssistantShortcutActivity : ComponentActivity() {

    @Inject lateinit var alertRouter: AlertRouter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action
        if (action == null) {
            finishSilently()
            return
        }
        lifecycleScope.launch {
            when (action) {
                ACTION_ASSISTANT_EMERGENCY -> triggerAlert(EscalationTier.EMERGENCY)
                ACTION_ASSISTANT_CHECK_IN -> triggerAlert(EscalationTier.CHECK_IN)
                else -> finishSilently()
            }
        }
    }

    private suspend fun triggerAlert(tier: EscalationTier) {
        val statusMessage = when (tier) {
            EscalationTier.EMERGENCY -> getString(R.string.assistant_trigger_emergency)
            EscalationTier.CHECK_IN -> getString(R.string.assistant_trigger_check_in)
        }
        alertRouter.dispatchManual(tier, statusMessage)
        Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun finishSilently() {
        finish()
    }

    companion object {
        const val ACTION_ASSISTANT_EMERGENCY = "com.pulselink.intent.ASSISTANT_EMERGENCY"
        const val ACTION_ASSISTANT_CHECK_IN = "com.pulselink.intent.ASSISTANT_CHECK_IN"
    }
}
