package com.pulselink.beacon.ui

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pulselink.beacon.util.BeaconIconManager
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulselink.beacon.data.InboxIconVariant
import com.pulselink.beacon.data.ThemeFont
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemePreferencesRepository
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.data.ThemeTarget
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ThemePreferencesRepository(app.applicationContext)
    private val syncManager = com.pulselink.beacon.data.ThemeSyncManager(repo)
    private var lastLauncherVariant: InboxIconVariant? = null

    var themeState by mutableStateOf(ThemeState())
        private set

    init {
        syncManager.startSync(viewModelScope)
        viewModelScope.launch {
            repo.flow.collect { state ->
                themeState = state
                if (lastLauncherVariant != state.global.iconVariant) {
                    applyLauncherIconVariant(state.global.iconVariant)
                    lastLauncherVariant = state.global.iconVariant
                }
            }
        }
    }

    fun applyColor(target: ThemeTarget, color: Color, address: String?) {
        val base = themeState.forAddress(address)
        val updated = base.withTarget(target, color)
        persist(address, updated)
    }

    fun applyFont(font: ThemeFont, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(font = font))
    }

    fun applyRadius(radius: Float, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(bubbleRadius = radius.coerceIn(4f, 28f)))
    }

    fun applyUiStyle(style: String, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(uiStyle = style))
    }

    fun applyIconStyle(style: String, address: String?) {
        val base = themeState.forAddress(address)
        persist(address, base.copy(iconStyle = style))
    }

    fun applyPreset(preset: ThemePalette, address: String?) {
        persist(address, preset)
    }

    fun resetContact(address: String) {
        viewModelScope.launch { repo.saveContact(address, null) }
    }

    fun updateIconVariant(variant: InboxIconVariant) {
        val updated = themeState.global.copy(iconVariant = variant)
        persist(null, updated)
    }

    private fun persist(address: String?, updated: ThemePalette) {
        viewModelScope.launch {
            if (address.isNullOrBlank()) {
                repo.saveGlobal(updated)
                if (lastLauncherVariant != updated.iconVariant) {
                    applyLauncherIconVariant(updated.iconVariant)
                    lastLauncherVariant = updated.iconVariant
                }
            } else {
                repo.saveContact(address, updated)
            }
        }
    }

    private fun applyLauncherIconVariant(variant: InboxIconVariant) {
        try {
            val pm = getApplication<Application>().packageManager
            val pkg = getApplication<Application>().packageName
            val beaconComp = ComponentName(pkg, "$pkg.LauncherBeacon")
            val bubbleComp = ComponentName(pkg, "$pkg.LauncherBubble")
            val shieldComp = ComponentName(pkg, "$pkg.LauncherShield")
            val minimalComp = ComponentName(pkg, "$pkg.LauncherMinimal")

            val (toEnable, toDisable) = when (variant) {
                InboxIconVariant.Beacon -> listOf(beaconComp) to listOf(bubbleComp, shieldComp, minimalComp)
                InboxIconVariant.Bubble -> listOf(bubbleComp) to listOf(beaconComp, shieldComp, minimalComp)
                InboxIconVariant.Shield -> listOf(shieldComp) to listOf(beaconComp, bubbleComp, minimalComp)
                InboxIconVariant.Minimal -> listOf(minimalComp) to listOf(beaconComp, bubbleComp, shieldComp)
            }

            toEnable.forEach {
                pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            }
            toDisable.forEach {
                pm.setComponentEnabledSetting(it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e("ThemeViewModel", "Failed to update launcher icon", e)
        }
    }

    companion object {
        fun factory(app: Application) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ThemeViewModel(app) as T
            }
        }
    }
}
