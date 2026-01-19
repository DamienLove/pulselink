package com.pulselink.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.BuildConfig
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.ui.state.ProfileUpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PulseLinkSettings,
    hasDndAccess: Boolean,
    showAds: Boolean,
    isProUser: Boolean,
    isDefaultSmsApp: Boolean,
    defaultSmsSupported: Boolean,
    beaconLauncherEnabled: Boolean,
    onToggleIncludeLocation: (Boolean) -> Unit,
    onToggleCrashDetection: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestBatteryOpt: () -> Unit,
    onRequestUnusedApps: () -> Unit,
    onToggleAutoAllowRemoteSoundChange: (Boolean) -> Unit,
    onToggleAutoUpdateContactInfo: (Boolean) -> Unit,
    onToggleFirebaseMessaging: (Boolean) -> Unit,
    onToggleEmailFallback: (Boolean) -> Unit,
    onSetOtpCleanupDays: (Int) -> Unit,
    onChangePrivatePin: () -> Unit,
    onToggleSmartReplies: (Boolean) -> Unit,
    onToggleAiCompose: (Boolean) -> Unit,
    onToggleAiUrgency: (Boolean) -> Unit,
    onRequestDefaultSms: () -> Unit,
    onToggleBeaconLauncher: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    profileUpdateState: ProfileUpdateUiState,
    onBroadcastProfileUpdate: () -> Unit,
    onEditEmergencyTone: () -> Unit,
    onEditCheckInTone: () -> Unit,
    onEditCallTone: () -> Unit,
    messageSoundLabel: String,
    messageVibrate: Boolean,
    onEditMessageSound: () -> Unit,
    messageVibrationLabel: String,
    onEditMessageVibration: () -> Unit,
    emergencyVibrationLabel: String,
    onEditEmergencyVibration: () -> Unit,
    checkInVibrationLabel: String,
    onEditCheckInVibration: () -> Unit,
    onToggleMessageVibrate: (Boolean) -> Unit,
    onReportBug: () -> Unit,
    onBetaTesters: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenBeacon: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenThemes: () -> Unit,
    showAddLogin: Boolean,
    onAddLogin: () -> Unit,
    onSignOut: () -> Unit,
    onOpenExtensionsStore: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHelp) {
                        Icon(
                            imageVector = Icons.Filled.Help,
                            contentDescription = stringResource(id = R.string.settings_help_action)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Features entry (separate screen)
            SettingsActionRow(
                title = stringResource(R.string.settings_extensions_store_title),
                subtitle = stringResource(R.string.settings_extensions_store_subtitle),
                actionLabel = stringResource(R.string.settings_extensions_store_action),
                onAction = onOpenExtensionsStore,
                leadingIcon = Icons.Filled.AddCircle
            )

            // Profile
            CollapsibleSettingsSection(
                title = "Profile",
                initiallyExpanded = false
            ) {
                SettingsActionRow(
                    title = "My Profile",
                    subtitle = "Edit name and avatar seen by contacts",
                    actionLabel = "Edit",
                    onAction = onEditProfile,
                    leadingIcon = Icons.Filled.Person
                )
            }

            // Delivery & permissions
            CollapsibleSettingsSection(
                title = "Delivery & permissions",
                initiallyExpanded = false
            ) {
                SettingsActionRow(
                    title = stringResource(R.string.dnd_override_title),
                    subtitle = if (hasDndAccess) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            stringResource(R.string.dnd_override_android15_note)
                        } else {
                            stringResource(R.string.dnd_override_ready)
                        }
                    } else {
                        stringResource(R.string.dnd_override_permission_prompt)
                    },
                    actionLabel = if (hasDndAccess) {
                        stringResource(R.string.dnd_override_action_manage)
                    } else {
                        stringResource(R.string.dnd_override_action_allow)
                    },
                    onAction = onRequestDndAccess,
                    leadingIcon = Icons.Filled.NotificationsActive
                )
                SettingsActionRow(
                    title = "Battery optimizations",
                    subtitle = "Disable optimizations so alerts can always send.",
                    actionLabel = "Open",
                    onAction = onRequestBatteryOpt,
                    leadingIcon = Icons.Filled.BugReport
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    SettingsActionRow(
                        title = "Unused app reminders",
                        subtitle = "Make sure PulseLink isn’t auto-disabled by Android.",
                        actionLabel = "Open",
                        onAction = onRequestUnusedApps,
                        leadingIcon = Icons.Filled.Schedule
                    )
                }
                if (settings.firebaseMessagingEnabled) {
                    SettingsToggleRow(
                        title = stringResource(R.string.extension_relay_title),
                        subtitle = "Uses internet for instant delivery. Requires both devices to be online.",
                        checked = settings.firebaseMessagingEnabled,
                        onCheckedChange = onToggleFirebaseMessaging
                    )
                }
                if (settings.emailFallbackEnabled) {
                    SettingsToggleRow(
                        title = stringResource(R.string.extension_email_backup_title),
                        subtitle = "Send email if other channels fail.",
                        checked = settings.emailFallbackEnabled,
                        onCheckedChange = onToggleEmailFallback
                    )
                }
                SettingsToggleRow(
                    title = "Auto-allow remote sound change",
                    subtitle = null,
                    checked = settings.autoAllowRemoteSoundChange,
                    onCheckedChange = onToggleAutoAllowRemoteSoundChange
                )
            }

            // Permissions & System
            CollapsibleSettingsSection(
                title = "Permissions & System",
                initiallyExpanded = false
            ) {
                SettingsToggleRow(
                    title = "Share location in alerts",
                    subtitle = null,
                    checked = settings.includeLocation,
                    onCheckedChange = onToggleIncludeLocation
                )
                if (settings.crashDetectionEnabled) {
                    SettingsToggleRow(
                        title = "Crash Detection",
                        subtitle = if (BuildConfig.CRASH_DETECTION_ENABLED) {
                            "Alert trusted contacts if a vehicle crash is detected."
                        } else {
                            "Temporarily disabled while we finish review materials."
                        },
                        checked = settings.crashDetectionEnabled && BuildConfig.CRASH_DETECTION_ENABLED,
                        enabled = BuildConfig.CRASH_DETECTION_ENABLED,
                        onCheckedChange = onToggleCrashDetection
                    )
                }
                SettingsActionRow(
                    title = stringResource(R.string.permission_battery_opt_title),
                    subtitle = null,
                    actionLabel = stringResource(R.string.permission_battery_opt_action),
                    onAction = onRequestBatteryOpt,
                    leadingIcon = Icons.Filled.PowerSettingsNew
                )
                SettingsActionRow(
                    title = stringResource(R.string.permission_unused_apps_title),
                    subtitle = null,
                    actionLabel = stringResource(R.string.permission_unused_apps_action),
                    onAction = onRequestUnusedApps,
                    leadingIcon = Icons.Filled.Schedule
                )
                val defaultSmsSubtitle = when {
                    isDefaultSmsApp -> stringResource(id = R.string.settings_default_sms_ready)
                    defaultSmsSupported -> stringResource(id = R.string.settings_default_sms_required)
                    else -> stringResource(id = R.string.settings_default_sms_unavailable)
                }
                val defaultSmsActionLabel = if (isDefaultSmsApp) {
                    stringResource(id = R.string.settings_default_sms_action_change)
                } else if (defaultSmsSupported) {
                    stringResource(id = R.string.settings_default_sms_action_make_default)
                } else {
                    stringResource(id = R.string.settings_default_sms_action_change)
                }
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_default_sms_title),
                    subtitle = defaultSmsSubtitle,
                    actionLabel = defaultSmsActionLabel,
                    onAction = onRequestDefaultSms,
                    leadingIcon = Icons.Filled.Message
                )
            }

            // Contacts & Sync
            CollapsibleSettingsSection(
                title = "Contacts & Sync",
                initiallyExpanded = false
            ) {
                SettingsToggleRow(
                    title = stringResource(id = R.string.settings_auto_update_contact_title),
                    subtitle = null,
                    checked = settings.autoUpdateContactInfo,
                    onCheckedChange = onToggleAutoUpdateContactInfo
                )
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_sync_contacts_title),
                    subtitle = null,
                    actionLabel = stringResource(id = R.string.settings_sync_action),
                    onAction = onSyncNow,
                    leadingIcon = Icons.Filled.Sync
                )
                SettingsActionRow(
                    title = stringResource(id = R.string.profile_update_button),
                    subtitle = null,
                    actionLabel = if (profileUpdateState.inProgress) {
                        stringResource(id = R.string.profile_update_sending)
                    } else {
                        stringResource(id = R.string.profile_update_button)
                    },
                    onAction = onBroadcastProfileUpdate,
                    leadingIcon = Icons.Filled.Sync
                )
                profileUpdateState.resultCount?.let { count ->
                    Text(
                        text = stringResource(id = R.string.profile_update_success, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                profileUpdateState.error?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Smart Features & Extensions
            if (settings.otpCleanupEnabled || settings.aiSummariesEnabled || settings.smartRepliesEnabled || settings.privateSafeEnabled) {
                CollapsibleSettingsSection(
                    title = "Smart Features",
                    initiallyExpanded = false
                ) {
                    if (settings.otpCleanupEnabled) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                text = "OTP Cleanup",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Delete one-time passwords older than ${settings.otpCleanupDays} day${if (settings.otpCleanupDays > 1) "s" else ""}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.material3.Slider(
                                value = settings.otpCleanupDays.toFloat(),
                                onValueChange = { onSetOtpCleanupDays(it.toInt()) },
                                valueRange = 1f..30f,
                                steps = 29
                            )
                        }
                    }
                    if (settings.smartRepliesEnabled) {
                         SettingsToggleRow(
                            title = "Smart Replies",
                            subtitle = "Suggest quick replies in conversations.",
                            checked = settings.smartRepliesEnabled,
                            onCheckedChange = onToggleSmartReplies
                        )
                    }
                    if (settings.aiSummariesEnabled) {
                         SettingsToggleRow(
                            title = "AI Compose",
                            subtitle = "Draft responses using AI assistance.",
                            checked = settings.aiComposeEnabled,
                            onCheckedChange = onToggleAiCompose
                        )
                        SettingsToggleRow(
                            title = "Urgency Detection",
                            subtitle = "Analyze incoming messages for urgency.",
                            checked = settings.aiUrgencyEnabled,
                            onCheckedChange = onToggleAiUrgency
                        )
                    }
                    if (settings.privateSafeEnabled) {
                        SettingsActionRow(
                            title = "Private Safe PIN",
                            subtitle = if (settings.privatePinHash != null) "Change your PIN" else "Set a PIN to protect private chats",
                            actionLabel = if (settings.privatePinHash != null) "Change" else "Set",
                            onAction = onChangePrivatePin,
                            leadingIcon = Icons.Filled.Lock
                        )
                    }
                }
            }

            // Beacon Feature
            if (settings.beaconLauncherEnabled) {
                CollapsibleSettingsSection(
                    title = "Beacon Feature",
                    initiallyExpanded = false
                ) {
                val smsStatusIcon = if (isDefaultSmsApp) Icons.Filled.CheckCircle else Icons.Filled.Error
                val smsStatusColor = if (isDefaultSmsApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                if (isDefaultSmsApp) {
                    Text(
                        text = "PulseLink is your default SMS app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = smsStatusColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    SettingsActionRow(
                        title = "Default SMS",
                        subtitle = "PulseLink is NOT set as default",
                        actionLabel = "Make default",
                        onAction = onRequestDefaultSms,
                        leadingIcon = smsStatusIcon,
                        iconTint = smsStatusColor
                    )
                }

                SettingsToggleRow(
                    title = stringResource(id = R.string.settings_beacon_icon_title),
                    subtitle = stringResource(id = R.string.settings_beacon_icon_subtitle),
                    checked = beaconLauncherEnabled,
                    onCheckedChange = onToggleBeaconLauncher
                )
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_beacon_title),
                    subtitle = if (isDefaultSmsApp && beaconLauncherEnabled) {
                        stringResource(id = R.string.settings_beacon_enabled_subtitle)
                    } else {
                        stringResource(id = R.string.settings_beacon_subtitle)
                    },
                    actionLabel = if (isDefaultSmsApp && beaconLauncherEnabled) {
                        stringResource(id = R.string.settings_beacon_action_open)
                    } else {
                        stringResource(id = R.string.settings_beacon_action_enable)
                    },
                    onAction = onOpenBeacon,
                    leadingIcon = Icons.Outlined.WifiTethering
                )
            }
            } // End Beacon Feature visibility check

            // Support & Account
            CollapsibleSettingsSection(
                title = "Support & Account",
                initiallyExpanded = false
            ) {
                // Hint for hidden extensions
                if (!settings.beaconLauncherEnabled || !settings.firebaseMessagingEnabled || !settings.emailFallbackEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(R.string.settings_extensions_hint_header),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.settings_extensions_hint_body),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                AssistantCommandsCard(
                    proEnabled = !BuildConfig.ADS_ENABLED || settings.proUnlocked,
                    onOpenHelp = onOpenHelp
                )
                if (showAddLogin) {
                    SettingsActionRow(
                        title = "Add login",
                        subtitle = "Sign in to sync across devices and web.",
                        actionLabel = "Sign in",
                        onAction = onAddLogin,
                        leadingIcon = Icons.Filled.Person
                    )
                }
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_report_bug),
                    actionLabel = "Report",
                    onAction = onReportBug,
                    leadingIcon = Icons.Filled.BugReport
                )
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_beta_testers),
                    actionLabel = "Manage",
                    onAction = onBetaTesters,
                    leadingIcon = Icons.Filled.Science
                )
                SettingsActionRow(
                    title = stringResource(id = R.string.settings_sign_out_title),
                    subtitle = stringResource(id = R.string.settings_sign_out_subtitle),
                    actionLabel = stringResource(id = R.string.settings_sign_out_action),
                    onAction = onSignOut,
                    leadingIcon = Icons.Filled.PowerSettingsNew
                )
                Text(
                    text = "Link ID: ${settings.deviceId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "expandIconRotation")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp) // Little vertical spacing for the header itself
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple()
                ) { expanded = !expanded },
            shape = RoundedCornerShape(18.dp),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp), // Spacing between header and content
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch
            ),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.4f else 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f)
                    )
                }
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null // Handled by toggleable container
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector = Icons.Filled.NotificationsActive,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = onAction,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
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
                tint = iconTint
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
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun AssistantCommandsCard(
    proEnabled: Boolean,
    onOpenHelp: () -> Unit
) {
    val title = if (proEnabled) {
        stringResource(R.string.assistant_commands_title)
    } else {
        stringResource(R.string.assistant_commands_title_free)
    }
    val body = if (proEnabled) {
        stringResource(R.string.assistant_commands_body_pro)
    } else {
        stringResource(R.string.assistant_commands_body_free)
    }
    val bullets = stringResource(R.string.assistant_commands_examples)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(bullets, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenHelp) {
                Text(
                    text = if (proEnabled) {
                        stringResource(R.string.assistant_commands_manage)
                    } else {
                        stringResource(R.string.assistant_commands_learn)
                    }
                )
            }
        }
    }
}
