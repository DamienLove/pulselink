package com.pulselink.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.BuildConfig
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.state.ProfileUpdateUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PulseLinkSettings,
    hasDndAccess: Boolean,
    showAds: Boolean,
    onToggleIncludeLocation: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onRequestBatteryOpt: () -> Unit,
    onRequestUnusedApps: () -> Unit,
    onToggleAutoAllowRemoteSoundChange: (Boolean) -> Unit,
    onToggleAutoUpdateContactInfo: (Boolean) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    profileUpdateState: ProfileUpdateUiState,
    onBroadcastProfileUpdate: () -> Unit,
    onEditEmergencyTone: () -> Unit,
    onEditCheckInTone: () -> Unit,
    onEditCallTone: () -> Unit,
    onReportBug: () -> Unit,
    onBetaTesters: () -> Unit,
    onOpenHelp: () -> Unit,
    onSignOut: () -> Unit,
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
        ,
        bottomBar = {
            if (BuildConfig.ADS_ENABLED && showAds && !settings.proUnlocked) {
                BannerAdSlot(enabled = true, modifier = Modifier.fillMaxWidth())
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsToggleRow(
                title = "Share location in alerts",
                subtitle = "Include GPS when alerting your circle.",
                checked = settings.includeLocation,
                onCheckedChange = onToggleIncludeLocation
            )
            val dndSubtitle = if (hasDndAccess) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    stringResource(R.string.dnd_override_android15_note)
                } else {
                    stringResource(R.string.dnd_override_ready)
                }
            } else {
                stringResource(R.string.dnd_override_permission_prompt)
            }
            SettingsActionRow(
                title = stringResource(R.string.dnd_override_title),
                subtitle = dndSubtitle,
                actionLabel = if (hasDndAccess) {
                    stringResource(R.string.dnd_override_action_manage)
                } else {
                    stringResource(R.string.dnd_override_action_allow)
                },
                onAction = onRequestDndAccess,
                leadingIcon = Icons.Filled.NotificationsActive
            )
            SettingsActionRow(
                title = stringResource(R.string.permission_battery_opt_title),
                subtitle = stringResource(R.string.permission_battery_opt_description),
                actionLabel = stringResource(R.string.permission_battery_opt_action),
                onAction = onRequestBatteryOpt,
                leadingIcon = Icons.Filled.PowerSettingsNew
            )
            SettingsActionRow(
                title = stringResource(R.string.permission_unused_apps_title),
                subtitle = stringResource(R.string.permission_unused_apps_description),
                actionLabel = stringResource(R.string.permission_unused_apps_action),
                onAction = onRequestUnusedApps,
                leadingIcon = Icons.Filled.Schedule
            )
            SettingsToggleRow(
                title = "Auto-allow remote sound change",
                subtitle = "Automatically approve tone overrides from new links.",
                checked = settings.autoAllowRemoteSoundChange,
                onCheckedChange = onToggleAutoAllowRemoteSoundChange
            )
            SettingsToggleRow(
                title = stringResource(id = R.string.settings_auto_update_contact_title),
                subtitle = stringResource(id = R.string.settings_auto_update_contact_subtitle),
                checked = settings.autoUpdateContactInfo,
                onCheckedChange = onToggleAutoUpdateContactInfo
            )
            SettingsToggleRow(
                title = stringResource(id = R.string.settings_realtime_title),
                subtitle = stringResource(id = R.string.settings_realtime_subtitle),
                checked = settings.realtimeEnabled,
                onCheckedChange = onToggleRealtime
            )
            SettingsActionRow(
                title = stringResource(id = R.string.settings_sync_contacts_title),
                subtitle = stringResource(id = R.string.settings_sync_contacts_subtitle),
                actionLabel = stringResource(id = R.string.settings_sync_action),
                onAction = onSyncNow,
                leadingIcon = Icons.Filled.Sync
            )
            SettingsActionRow(
                title = stringResource(id = R.string.profile_update_button),
                subtitle = stringResource(id = R.string.profile_update_subtitle),
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
            SettingsActionRow(
                title = "Emergency alert tone",
                actionLabel = "Edit",
                onAction = onEditEmergencyTone
            )
            SettingsActionRow(
                title = "Check-in alert tone",
                actionLabel = "Edit",
                onAction = onEditCheckInTone
            )
            SettingsActionRow(
                title = stringResource(R.string.settings_call_tone_title),
                actionLabel = "Edit",
                onAction = onEditCallTone
            )
            AssistantCommandsCard(
                proEnabled = !BuildConfig.ADS_ENABLED || settings.proUnlocked,
                onOpenHelp = onOpenHelp
            )
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
            Text(
                text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp, bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector = Icons.Filled.NotificationsActive
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
