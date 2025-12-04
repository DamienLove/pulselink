package com.pulselink.data.assistant

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.pulselink.BuildConfig
import com.pulselink.R
import com.pulselink.auth.FirebaseAuthManager
import com.pulselink.data.link.ContactLinkManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.repository.ContactRepository
import com.pulselink.service.AlertRouter
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NaturalLanguageCommandProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    private val alertRouter: AlertRouter,
    private val contactRepository: ContactRepository,
    private val linkManager: ContactLinkManager,
    private val firebaseAuthManager: FirebaseAuthManager
) {

    suspend fun handleCommand(query: String): VoiceCommandResult = withContext(Dispatchers.IO) {
        firebaseAuthManager.ensureSignedIn()
        if (!ensureProAccess()) {
            return@withContext VoiceCommandResult.UpgradeRequired
        }
        val callable = functions.getHttpsCallable("naturalLanguageQuery")
        val response = runCatching { callable.call(query).await() }
            .getOrElse {
                return@withContext VoiceCommandResult.Error(
                    context.getString(R.string.voice_command_error_network)
                )
            }
        val data = response.data as? Map<*, *> ?: return@withContext VoiceCommandResult.Error(
            context.getString(R.string.voice_command_error_unknown)
        )
        val intent = (data["intent"] as? String)?.lowercase()?.trim().orEmpty()
        val entitiesRaw = data["entities"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        val entities = entitiesRaw.filterKeys { it is String }.mapValues { it.value?.toString().orEmpty() }

        when (intent) {
            "send_emergency_alert" -> {
                alertRouter.dispatchManual(
                    EscalationTier.EMERGENCY,
                    context.getString(R.string.voice_command_trigger_reason, query)
                )
                VoiceCommandResult.Success(context.getString(R.string.voice_command_emergency_sent))
            }

            "message_contact" -> {
                val contact = resolveContact(entities["contactName"])
                    ?: return@withContext VoiceCommandResult.Error(
                        context.getString(R.string.voice_command_contact_missing)
                    )
                val body = entities["messageBody"].takeUnless { it.isNullOrBlank() }
                    ?: context.getString(R.string.voice_command_default_message)
                val result = linkManager.sendManualMessage(contact.id, body)
                return@withContext if (result is ManualMessageResult.Success) {
                    VoiceCommandResult.Success(
                        context.getString(R.string.voice_command_message_sent, contact.displayName)
                    )
                } else {
                    VoiceCommandResult.Error(context.getString(R.string.voice_command_message_failed))
                }
            }

            "activate_contact_siren" -> {
                val contact = resolveContact(entities["contactName"])
                    ?: return@withContext VoiceCommandResult.Error(
                        context.getString(R.string.voice_command_contact_missing)
                    )
                val success = linkManager.sendPing(contact.id)
                if (success) {
                    VoiceCommandResult.Success(
                        context.getString(R.string.voice_command_siren_triggered, contact.displayName)
                    )
                } else {
                    VoiceCommandResult.Error(context.getString(R.string.voice_command_siren_failed))
                }
            }

            "cancel_emergency" -> {
                val cancelled = linkManager.cancelActiveEmergency()
                if (cancelled) {
                    VoiceCommandResult.Success(context.getString(R.string.voice_command_emergency_cancelled))
                } else {
                    VoiceCommandResult.Error(context.getString(R.string.voice_command_cancel_failed))
                }
            }

            else -> VoiceCommandResult.Error(context.getString(R.string.voice_command_error_unknown))
        }
    }

    private suspend fun ensureProAccess(): Boolean {
        if (BuildConfig.PRO_FEATURES) return true
        val user = firebaseAuthManager.currentUser() ?: return false
        val claims = runCatching { user.getIdToken(false).await().claims }.getOrNull() ?: return false
        return claims["pro"] == true
    }

    private suspend fun resolveContact(name: String?): Contact? {
        if (name.isNullOrBlank()) return null
        val normalized = name.lowercase()
        val contacts = contactRepository.observeContacts().first()
        return contacts.firstOrNull { it.displayName.lowercase() == normalized }
            ?: contacts.firstOrNull { it.displayName.lowercase().contains(normalized) }
    }
}

sealed interface VoiceCommandResult {
    data class Success(val message: String) : VoiceCommandResult
    data class Error(val message: String) : VoiceCommandResult
    object UpgradeRequired : VoiceCommandResult
}
