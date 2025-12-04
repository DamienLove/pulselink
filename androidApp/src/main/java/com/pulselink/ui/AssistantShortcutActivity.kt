package com.pulselink.ui

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.pulselink.R
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor
import com.pulselink.data.assistant.VoiceCommandResult
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.EscalationTier
import com.pulselink.service.AlertRouter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AssistantShortcutActivity : ComponentActivity() {

    @Inject lateinit var alertRouter: AlertRouter
    @Inject lateinit var naturalLanguageCommandProcessor: NaturalLanguageCommandProcessor
    @Inject lateinit var contactLinkManager: ContactLinkManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val feature = extractDeepLinkFeature(intent)
        if (!feature.isNullOrBlank()) {
            lifecycleScope.launch { handleShortcutFeature(feature) }
            return
        }
        val voiceQuery = extractVoiceQuery(intent)
        if (!voiceQuery.isNullOrBlank()) {
            lifecycleScope.launch { handleVoiceCommand(voiceQuery) }
            return
        }
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

    private suspend fun handleVoiceCommand(query: String) {
        val result = naturalLanguageCommandProcessor.handleCommand(query)
        val message = when (result) {
            is VoiceCommandResult.Success -> result.message
            is VoiceCommandResult.Error -> result.message
            VoiceCommandResult.UpgradeRequired -> getString(R.string.voice_command_upgrade_required)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private suspend fun handleShortcutFeature(feature: String) {
        when (feature.lowercase(Locale.US)) {
            FEATURE_EMERGENCY -> triggerAlert(EscalationTier.EMERGENCY)
            FEATURE_CANCEL -> handleCancelEmergency()
            else -> finishSilently()
        }
    }

    private fun extractVoiceQuery(intent: Intent?): String? {
        if (intent == null) return null
        val extras = listOf(
            intent.getStringExtra(EXTRA_VOICE_QUERY),
            intent.getStringExtra(Intent.EXTRA_TEXT),
            intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT),
            intent.getStringExtra(SearchManager.QUERY)
        ).firstOrNull { !it.isNullOrBlank() }
        return extras?.trim()
    }

    private fun extractDeepLinkFeature(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (intent.action != Intent.ACTION_VIEW) return null
        if (data.scheme != "pulselink" || data.host != "assistant") return null
        return data.lastPathSegment
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

    private suspend fun handleCancelEmergency() {
        val cancelled = contactLinkManager.cancelActiveEmergency()
        val message = if (cancelled) {
            getString(R.string.voice_command_emergency_cancelled)
        } else {
            getString(R.string.voice_command_cancel_failed)
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun finishSilently() {
        finish()
    }

    companion object {
        const val ACTION_ASSISTANT_EMERGENCY = "com.pulselink.intent.ASSISTANT_EMERGENCY"
        const val ACTION_ASSISTANT_CHECK_IN = "com.pulselink.intent.ASSISTANT_CHECK_IN"
        const val ACTION_ASSISTANT_VOICE = "com.pulselink.intent.ASSISTANT_VOICE"
        const val EXTRA_VOICE_QUERY = "com.pulselink.extra.VOICE_QUERY"
        private const val FEATURE_EMERGENCY = "emergency"
        private const val FEATURE_CANCEL = "cancel"
    }
}
