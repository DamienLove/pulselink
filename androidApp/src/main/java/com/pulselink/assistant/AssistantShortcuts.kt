package com.pulselink.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.pulselink.R
import com.pulselink.ui.AssistantShortcutActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AssistantShortcuts {

    private val shortcutScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun publish(context: Context) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val emergency = ShortcutInfo.Builder(context, SHORTCUT_EMERGENCY)
            .setShortLabel(context.getString(R.string.assistant_shortcut_emergency_short))
            .setLongLabel(context.getString(R.string.assistant_shortcut_emergency_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_logo))
            .setIntent(
                Intent(context, AssistantShortcutActivity::class.java).apply {
                    action = AssistantShortcutActivity.ACTION_ASSISTANT_EMERGENCY
                }
            )
            .build()

        val checkIn = ShortcutInfo.Builder(context, SHORTCUT_CHECK_IN)
            .setShortLabel(context.getString(R.string.assistant_shortcut_check_in_short))
            .setLongLabel(context.getString(R.string.assistant_shortcut_check_in_long))
            .setIcon(Icon.createWithResource(context, R.drawable.ic_logo))
            .setIntent(
                Intent(context, AssistantShortcutActivity::class.java).apply {
                    action = AssistantShortcutActivity.ACTION_ASSISTANT_CHECK_IN
                }
            )
            .build()

        runCatching {
            manager.addDynamicShortcuts(listOf(emergency, checkIn))
        }.onFailure { error ->
            Log.w(TAG, "Failed to publish Assistant shortcuts", error)
        }
    }

    fun launchShortcutSettings(context: Context) {
        shortcutScope.launch {
            val packageManager = context.packageManager
            val assistantPackage = resolveAssistantPackage(context)
            val intents = buildList {
                resolveAssistantEntryIntent(context, assistantPackage)?.let { add(it) }
                add(Intent("com.google.android.googlequicksearchbox.intent.action.ASSISTANT_SHORTCUTS"))
                add(Intent("com.google.android.googlequicksearchbox.intent.action.ASSISTANT_SETTINGS"))
                add(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                add(Intent(Intent.ACTION_VIEW, Uri.parse("https://assistant.google.com/shortcuts")))
            }.map { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent
            }
            val launched = intents.firstOrNull { intent ->
                intent.resolveActivity(packageManager) != null
            }?.let { intent ->
                runCatching { context.startActivity(intent) }.isSuccess
            } ?: false
            if (launched) {
                val toastMessage = when (assistantPackage) {
                    PACKAGE_GEMINI -> R.string.assistant_shortcut_help_gemini
                    PACKAGE_GOOGLE_APP, PACKAGE_GOOGLE_ASSISTANT -> R.string.assistant_shortcut_help_google
                    else -> R.string.assistant_shortcut_help_generic
                }
                Toast.makeText(context, context.getString(toastMessage), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.assistant_shortcut_missing),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private const val SHORTCUT_EMERGENCY = "assistant_emergency"
    private const val SHORTCUT_CHECK_IN = "assistant_check_in"
    private const val TAG = "AssistantShortcuts"
    private const val PACKAGE_GEMINI = "com.google.android.apps.bard"
    private const val PACKAGE_GOOGLE_APP = "com.google.android.googlequicksearchbox"
    private const val PACKAGE_GOOGLE_ASSISTANT = "com.google.android.apps.googleassistant"

    private fun resolveAssistantEntryIntent(context: Context, assistantPackage: String?): Intent? {
        assistantPackage ?: return null
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage(assistantPackage)
    }

    private fun resolveAssistantPackage(context: Context): String? {
        val explicit = Settings.Secure.getString(context.contentResolver, "assistant")
            ?.let { ComponentName.unflattenFromString(it)?.packageName }
        if (!explicit.isNullOrBlank()) return explicit
        val pm = context.packageManager
        val resolveInfo = pm.resolveActivity(Intent(Intent.ACTION_ASSIST), 0)
        val resolved = resolveInfo?.activityInfo?.packageName
        if (!resolved.isNullOrBlank()) return resolved
        return when {
            isInstalled(context, PACKAGE_GEMINI) -> PACKAGE_GEMINI
            isInstalled(context, PACKAGE_GOOGLE_APP) -> PACKAGE_GOOGLE_APP
            isInstalled(context, PACKAGE_GOOGLE_ASSISTANT) -> PACKAGE_GOOGLE_ASSISTANT
            else -> null
        }
    }

    private fun isInstalled(context: Context, packageName: String): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        }.getOrElse { false }
    }
}
