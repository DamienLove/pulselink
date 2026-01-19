package com.pulselink.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.delay

object UnifiedLauncherManager {
    private const val TAG = "UnifiedLauncherManager"

    /**
     * Apply unified navigation mode settings.
     * @return true if operation succeeded, false if it failed
     */
    suspend fun apply(
        context: Context,
        unifiedEnabled: Boolean,
        preferredLabel: String?
    ): Boolean {
        return try {
            Log.d(TAG, "apply() called - unifiedEnabled=$unifiedEnabled, preferredLabel=$preferredLabel")

            val pm = context.packageManager
            val pkg = context.packageName
            val pulseLinkLauncher = ComponentName(pkg, "com.pulselink.PulseLinkLauncher")
            val main = ComponentName(pkg, "com.pulselink.ui.MainActivity")
            val inbox = ComponentName(pkg, "com.pulselink.ui.InboxLauncherActivity")
            val inboxLogo = ComponentName(pkg, "com.pulselink.BeaconInboxLogo")
            val inboxPro = ComponentName(pkg, "com.pulselink.BeaconInboxPro")
            val beaconVariants = listOf(
                inboxLogo,
                inboxPro,
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeDefaultLight"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeMidnightOled"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeOceanDeep"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeRosePetal"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeSunsetFade"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeCitrusPop"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeForestTrail"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeLavenderHaze"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeSlateMono"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeDesertClay"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeNordFrost"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeNeonNoir"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemePaperback"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeMintBreeze"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeAmethystNight"),
                ComponentName(pkg, "com.pulselink.BeaconInboxThemeAurora")
            )

            val unifiedMessages = ComponentName(pkg, "com.pulselink.UnifiedLauncherMessages")
            val unifiedPulseLink = ComponentName(pkg, "com.pulselink.UnifiedLauncherPulseLinkPremium")
            val unifiedBeacon = ComponentName(pkg, "com.pulselink.UnifiedLauncherBeaconPremium")

            val unifiedTargets = mapOf(
                "messages" to unifiedMessages,
                "pulselink premium" to unifiedPulseLink,
                "beacon premium" to unifiedBeacon
            )

            val desired = preferredLabel?.trim()?.lowercase()
            val targetUnified = unifiedTargets[desired] ?: unifiedMessages

            Log.d(TAG, "Target unified component: ${targetUnified.className}")

            if (unifiedEnabled) {
                Log.d(TAG, "Enabling unified mode")
                // Enable unified launcher; keep MainActivity enabled so the alias can open
                // We use DONT_KILL_APP (killApp=false) to prevent crash loops and bad UX.
                // The launcher icon update might be delayed, but it's safer.
                if (!isComponentEnabled(pm, targetUnified)) {
                    enable(pm, targetUnified, "unified target", killApp = false)
                }

                // Ensure MainActivity is enabled
                if (!isComponentEnabled(pm, main)) {
                    enable(pm, main, "MainActivity")
                }

                // Verify the target unified component was enabled before hiding other icons.
                delay(500) // Delay to allow launcher to process
                if (!isComponentEnabled(pm, targetUnified)) {
                    Log.w(TAG, "Target unified component not enabled after first attempt. Retrying...")
                    enable(pm, targetUnified, "unified target (retry)", killApp = false)
                    delay(500)
                }
                if (!isComponentEnabled(pm, targetUnified)) {
                    Log.e(TAG, "Failed to enable unified component after retry.")
                    // Fallback: ensure at least the PulseLink launcher icon stays visible
                    enable(pm, pulseLinkLauncher, "PulseLinkLauncher (fallback)")
                    enable(pm, inbox, "InboxLauncherActivity (fallback)")
                    enable(pm, main, "MainActivity (fallback)")
                    return false
                }

                // Now that unified is confirmed, hide the other launchers.
                disable(pm, pulseLinkLauncher, "PulseLinkLauncher")
                disable(pm, inbox, "InboxLauncherActivity")
                beaconVariants.forEach { disable(pm, it, "beacon variant") }
                unifiedTargets.values.filterNot { it == targetUnified }.forEach {
                    disable(pm, it, "other unified target")
                }
                Log.d(TAG, "Unified mode enabled successfully")
            } else {
                Log.d(TAG, "Disabling unified mode")
                // Restore default PulseLink launcher, keep unified aliases hidden
                enable(pm, main, "MainActivity")
                enable(pm, pulseLinkLauncher, "PulseLinkLauncher")
                enable(pm, inbox, "InboxLauncherActivity")
                // Beacon variants are managed by BeaconIconManager based on user's theme selection
                unifiedTargets.values.forEach { disable(pm, it, "unified target") }
                // Inbox launcher follows its existing runtime flag elsewhere; leave it untouched
                Log.d(TAG, "Unified mode disabled successfully")
            }

            // Give launcher time to refresh
            delay(500)
            Log.d(TAG, "Launcher refresh delay complete")
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while applying unified mode", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "IllegalArgumentException while applying unified mode", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception while applying unified mode", e)
            false
        }
    }

    private fun enable(
        pm: PackageManager,
        component: ComponentName,
        label: String,
        killApp: Boolean = false
    ) {
        Log.d(TAG, "Enabling $label: ${component.className} (killApp=$killApp)")
        val flags = if (killApp) 0 else PackageManager.DONT_KILL_APP
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            flags
        )
    }

    private fun disable(pm: PackageManager, component: ComponentName, label: String) {
        Log.d(TAG, "Disabling $label: ${component.className}")
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun isComponentEnabled(pm: PackageManager, component: ComponentName): Boolean =
        try {
            val state = pm.getComponentEnabledSetting(component)
            // COMPONENT_ENABLED_STATE_DEFAULT means use manifest value, check that too
            if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                pm.getActivityInfo(component, 0).enabled
            } else {
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read component enabled state for ${component.className}", e)
            false
        }
}
