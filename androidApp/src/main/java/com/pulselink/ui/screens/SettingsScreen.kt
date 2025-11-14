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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
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
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PulseLinkSettings,
    hasDndAccess: Boolean,
    onAssistantShortcuts: () -> Unit,
    onToggleIncludeLocation: (Boolean) -> Unit,
    onRequestDndAccess: () -> Unit,
    onToggleAutoAllowRemoteSoundChange: (Boolean) -> Unit,
    onEditEmergencyTone: () -> Unit,
    onEditCheckInTone: () -> Unit,
    onEditCallTone: () -> Unit,
    onReportBug: () -> Unit,
    onBetaTesters: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsActionCard(
                title = stringResource(R.string.assistant_setup_title),
                subtitle = stringResource(R.string.assistant_setup_subtitle),
                actionLabel = stringResource(R.string.assistant_setup_action),
                onAction = onAssistantShortcuts,
                leadingIcon = Icons.Filled.Mic
            )
            SettingsToggleCard(
                title = "Share location in alerts",
                subtitle = "Include your latest location when PulseLink sends emergency or check-in notifications.",
                checked = settings.includeLocation,
                onCheckedChange = onToggleIncludeLocation
            )
            val dndSubtitle = if (hasDndAccess) {
                val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    stringResource(R.string.dnd_override_android15_note)
                } else {
                    stringResource(R.string.dnd_override_ready)
                }
                "$base\n${stringResource(R.string.dnd_override_troubleshooting)}"
            } else {
                stringResource(R.string.dnd_override_permission_prompt)
            }
            SettingsActionCard(
                title = stringResource(R.string.dnd_override_title),
                subtitle = dndSubtitle,
                actionLabel = if (hasDndAccess) {
                    stringResource(R.string.dnd_override_action_manage)
                } else {
                    stringResource(R.string.dnd_override_action_allow)
                },
                onAction = onRequestDndAccess
            )
            SettingsToggleCard(
                title = "Auto-allow remote sound change",
                subtitle = "Automatically let newly linked contacts update alert tones on this device.",
                checked = settings.autoAllowRemoteSoundChange,
                onCheckedChange = onToggleAutoAllowRemoteSoundChange
            )
            SettingsActionCard(
                title = "Emergency alert tone",
                subtitle = "Choose the default siren that plays during emergency alerts.",
                actionLabel = "Edit tone",
                onAction = onEditEmergencyTone
            )
            SettingsActionCard(
                title = "Check-in alert tone",
                subtitle = "Set the chime that plays when you send a check-in.",
                actionLabel = "Edit tone",
                onAction = onEditCheckInTone
            )
            SettingsActionCard(
                title = stringResource(R.string.settings_call_tone_title),
                subtitle = stringResource(R.string.settings_call_tone_subtitle),
                actionLabel = "Edit tone",
                onAction = onEditCallTone
            )
            SettingsActionCard(
                title = stringResource(id = R.string.settings_report_bug),
                subtitle = "Help us improve PulseLink by reporting issues you encounter.",
                actionLabel = "Report",
                onAction = onReportBug,
                leadingIcon = Icons.Filled.BugReport
            )
            SettingsActionCard(
                title = stringResource(id = R.string.settings_beta_testers),
                subtitle = "Manage beta testing status and view tester information.",
                actionLabel = "Manage",
                onAction = onBetaTesters,
                leadingIcon = Icons.Filled.Science
            )
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
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
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector = Icons.Filled.NotificationsActive
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAction) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
