package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.domain.model.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeaconSettingsScreen(
    settings: PulseLinkSettings,
    onBack: () -> Unit,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onOpenVisualSettings: () -> Unit,
    onOpenProfileSettings: () -> Unit,
    onOpenExtensionsStore: () -> Unit,
    messageSoundLabel: String,
    messageVibrate: Boolean,
    onEditMessageSound: () -> Unit,
    messageVibrationLabel: String,
    onEditMessageVibration: () -> Unit,
    onToggleMessageVibrate: (Boolean) -> Unit,
    isDefaultSmsApp: Boolean,
    defaultSmsSupported: Boolean,
    onRequestDefaultSms: () -> Unit,
    remoteWebAccessEnabled: Boolean,
    isPremiumActive: Boolean,
    onToggleRemoteWebAccess: (Boolean) -> Unit,
    otpCleanupEnabled: Boolean,
    otpCleanupDays: Int,
    onToggleOtpCleanup: (Boolean) -> Unit,
    onChangeOtpCleanupDays: (Int) -> Unit,
    onSetPrivatePin: () -> Unit,
    onPurchasePremium: () -> Unit,
    beaconLauncherEnabled: Boolean,
    onToggleBeaconLauncher: (Boolean) -> Unit,
    aiSummariesEnabled: Boolean,
    aiComposeEnabled: Boolean,
    aiUrgencyEnabled: Boolean,
    aiUrgencyBypassDnd: Boolean,
    aiUrgencyIncludeUnknown: Boolean,
    onToggleAiSummaries: (Boolean) -> Unit,
    onToggleAiCompose: (Boolean) -> Unit,
    onToggleAiUrgency: (Boolean) -> Unit,
    onToggleAiUrgencyBypass: (Boolean) -> Unit,
    onToggleAiUrgencyIncludeUnknown: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Beacon Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Features entry (separate screen)
            BeaconSettingsActionRow(
                title = stringResource(R.string.settings_extensions_store_title),
                subtitle = stringResource(R.string.settings_extensions_store_subtitle),
                actionLabel = stringResource(R.string.settings_extensions_store_action),
                onAction = onOpenExtensionsStore,
                leadingIcon = Icons.Filled.AddCircle
            )

            val defaultSmsSubtitle = when {
                isDefaultSmsApp -> "PulseLink is set as the default SMS app."
                defaultSmsSupported -> "Required for Beacon messaging and Play compliance."
                else -> "Default SMS role unavailable on this device."
            }
            val defaultSmsActionLabel = if (isDefaultSmsApp) "Change" else "Make default"

            BeaconCollapsibleSection(title = "Profile") {
                BeaconSettingsActionRow(
                    title = "Public Profile",
                    subtitle = "Name and avatar seen by others.",
                    actionLabel = "Edit",
                    onAction = onOpenProfileSettings,
                    leadingIcon = Icons.Filled.Person
                )
            }

            BeaconCollapsibleSection(title = "Themes & appearance") {
                BeaconSettingsActionRow(
                    title = "Visual customization",
                    subtitle = "Colors, layout, icons, and chat appearance.",
                    actionLabel = "Open",
                    onAction = onOpenVisualSettings,
                    leadingIcon = Icons.Filled.Palette
                )
            }

            BeaconCollapsibleSection(title = "Messaging") {
                BeaconSettingsActionRow(
                    title = "Default SMS app",
                    subtitle = defaultSmsSubtitle,
                    actionLabel = defaultSmsActionLabel,
                    onAction = onRequestDefaultSms,
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsActionRow(
                    title = "Time format",
                    subtitle = when (settings.timeFormat) {
                        TimeFormat.AUTO -> "Follow device"
                        TimeFormat.TWELVE_HOUR -> "12-hour"
                        TimeFormat.TWENTY_FOUR_HOUR -> "24-hour"
                    },
                    actionLabel = "Change",
                    onAction = {
                        val next = when (settings.timeFormat) {
                            TimeFormat.AUTO -> TimeFormat.TWELVE_HOUR
                            TimeFormat.TWELVE_HOUR -> TimeFormat.TWENTY_FOUR_HOUR
                            TimeFormat.TWENTY_FOUR_HOUR -> TimeFormat.AUTO
                        }
                        onTimeFormatChange(next)
                    },
                    leadingIcon = Icons.Filled.AccessTime
                )
                BeaconSettingsActionRow(
                    title = "Message notification sound",
                    subtitle = messageSoundLabel,
                    actionLabel = "Edit",
                    onAction = onEditMessageSound,
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsToggleRow(
                    title = "Message vibration",
                    subtitle = "Vibrate when new texts arrive.",
                    checked = messageVibrate,
                    onCheckedChange = onToggleMessageVibrate,
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsActionRow(
                    title = "Message vibration pattern",
                    subtitle = messageVibrationLabel,
                    actionLabel = "Edit",
                    onAction = onEditMessageVibration,
                    leadingIcon = Icons.Filled.Message
                )
            }

            BeaconCollapsibleSection(title = "Security & cleanup") {
                val retentionLabel = when (otpCleanupDays) {
                    1 -> "1 day"
                    3 -> "3 days"
                    7 -> "7 days"
                    30 -> "30 days"
                    else -> "$otpCleanupDays days"
                }
                BeaconSettingsToggleRow(
                    title = "2-step code cleanup",
                    subtitle = if (otpCleanupEnabled) {
                        "Auto-delete after $retentionLabel"
                    } else {
                        "Off (2-step messages stay forever)"
                    },
                    checked = otpCleanupEnabled,
                    onCheckedChange = onToggleOtpCleanup,
                    leadingIcon = Icons.Filled.VpnKey
                )
                BeaconSettingsActionRow(
                    title = "2-step cleanup window",
                    subtitle = "Delete 2-step messages after $retentionLabel",
                    actionLabel = "Change",
                    onAction = {
                        val next = when (otpCleanupDays) {
                            1 -> 3
                            3 -> 7
                            7 -> 30
                            30 -> 1
                            else -> 1
                        }
                        onChangeOtpCleanupDays(next)
                    },
                    leadingIcon = Icons.Filled.AccessTime
                )
                BeaconSettingsActionRow(
                    title = "Private chats PIN",
                    subtitle = if (settings.privatePinHash != null) "PIN set - tap to change/clear" else "Protect private contacts and chats",
                    actionLabel = "Set",
                    onAction = onSetPrivatePin,
                    leadingIcon = Icons.Filled.Lock
                )
            }

            BeaconCollapsibleSection(title = "Remote access") {
                BeaconSettingsToggleRow(
                    title = "Web access to messages",
                    subtitle = if (remoteWebAccessEnabled) {
                        "Enabled" + if (isPremiumActive) " (Premium)" else ""
                    } else {
                        "Premium-only: securely access SMS from the web"
                    },
                    checked = remoteWebAccessEnabled,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleRemoteWebAccess(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Language
                )
            }

            BeaconCollapsibleSection(title = "AI features") {
                BeaconSettingsToggleRow(
                    title = "AI summaries",
                    subtitle = "Summarize conversations in Beacon inbox.",
                    checked = aiSummariesEnabled,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleAiSummaries(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsToggleRow(
                    title = "AI writing",
                    subtitle = "Rewrite, shorten, or draft replies.",
                    checked = aiComposeEnabled,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleAiCompose(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Edit
                )
                BeaconSettingsToggleRow(
                    title = "AI urgency detection",
                    subtitle = "Flag urgent or emergency incoming messages.",
                    checked = aiUrgencyEnabled,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleAiUrgency(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsToggleRow(
                    title = "AI urgency: bypass DND",
                    subtitle = "Let urgent AI alerts bypass silent/DND.",
                    checked = aiUrgencyBypassDnd,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleAiUrgencyBypass(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Message
                )
                BeaconSettingsToggleRow(
                    title = "AI urgency: include unknown senders",
                    subtitle = "Also classify messages from non-trusted numbers.",
                    checked = aiUrgencyIncludeUnknown,
                    onCheckedChange = {
                        if (isPremiumActive) {
                            onToggleAiUrgencyIncludeUnknown(it)
                        } else {
                            onPurchasePremium()
                        }
                    },
                    leadingIcon = Icons.Filled.Message
                )
            }

            BeaconCollapsibleSection(title = "Beacon launcher") {
                BeaconSettingsToggleRow(
                    title = "Beacon inbox icon",
                    subtitle = "Shows launcher shortcut",
                    checked = beaconLauncherEnabled,
                    onCheckedChange = onToggleBeaconLauncher,
                    leadingIcon = Icons.Filled.Message
                )
            }
        }
    }
}

@Composable
private fun BeaconCollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "beaconSectionRotation")

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BeaconSettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onAction) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun BeaconSettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
