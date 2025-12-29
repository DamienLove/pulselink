package com.pulselink.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSmsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isDefaultSms(): Boolean {
        val packageName = context.packageName
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            runCatching { roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true }.getOrDefault(false)
        } else {
            false
        }
        val defaultPackage = runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()
        val telephonyMatch = defaultPackage == packageName
        val isDefault = roleHeld || telephonyMatch
        if (!isDefault) {
            Log.d(
                TAG,
                "isDefaultSms=false package=$packageName roleHeld=$roleHeld telephonyDefault=$defaultPackage"
            )
        }
        return isDefault
    }

    fun buildRoleRequestIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            } else {
                null
            }
        } else {
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
        }
    }

    suspend fun checkDefaultSmsWithRetry(
        maxAttempts: Int = 7,
        initialDelayMs: Long = 300
    ): Boolean {
        var delayMs = initialDelayMs
        var latest = false
        repeat(maxAttempts) { attempt ->
            latest = isDefaultSms()
            Log.d(TAG, "checkDefaultSmsWithRetry attempt ${attempt + 1}/$maxAttempts -> $latest")
            if (latest) return true
            if (attempt < maxAttempts - 1) {
                delay(delayMs)
                delayMs *= 2
            }
        }
        return latest
    }

    /**
     * Fire a best-effort request to make this app the default SMS handler.
     * Calls [onResult] with the latest default status after launching the system intent.
     */
    fun requestDefaultSms(onResult: (Boolean) -> Unit) {
        val intent = buildRoleRequestIntent()
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        onResult(isDefaultSms())
    }

    private companion object {
        private const val TAG = "DefaultSmsHelper"
    }
}
