package com.pulselink.beacon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Beacon settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHelp) {
                        Icon(imageVector = Icons.Filled.Help, contentDescription = "Help")
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
            val dndBypassEnabled = remember { mutableStateOf(true) }
            val keyphraseEnabled = remember { mutableStateOf(true) }
            val safetyInteropEnabled = remember { mutableStateOf(true) }

            SettingsToggleRow(
                title = "Favorites can break through DND",
                subtitle = "Let selected contacts ring with a special tone",
                checked = dndBypassEnabled.value,
                onCheckedChange = { dndBypassEnabled.value = it }
            )
            SettingsActionRow(
                title = "Manage favorites",
                subtitle = "Pick who can always reach you",
                actionLabel = "Open",
                onAction = onOpenFavorites,
                leadingIcon = Icons.Filled.Phone
            )

            SettingsToggleRow(
                title = "Keyphrase alarm",
                subtitle = "Trigger audible alarm when a message contains your keyphrase",
                checked = keyphraseEnabled.value,
                onCheckedChange = { keyphraseEnabled.value = it }
            )
            SettingsActionRow(
                title = "Notification policy (DND)",
                subtitle = "Grant DND override so Beacon can alert you",
                actionLabel = "Manage",
                onAction = { /* TODO: open system DND settings */ },
                leadingIcon = Icons.Filled.NotificationsActive
            )

            SettingsToggleRow(
                title = "Safety interop",
                subtitle = "Sound siren if a linked Safety contact triggers an emergency",
                checked = safetyInteropEnabled.value,
                onCheckedChange = { safetyInteropEnabled.value = it }
            )
            SettingsActionRow(
                title = "Emergency siren tone",
                actionLabel = "Edit",
                onAction = { /* TODO: open tone picker */ },
                leadingIcon = Icons.Filled.Security
            )

            AssistantNote()
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
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { onCheckedChange(!checked) }) {
                Switch(checked = checked, onCheckedChange = onCheckedChange)
                Text(if (checked) "On" else "Off", modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String? = null,
    actionLabel: String,
    onAction: () -> Unit,
    leadingIcon: ImageVector
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            RowTitle(title = title, icon = leadingIcon)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun RowTitle(title: String, icon: ImageVector) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AssistantNote() {
    Surface(
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Beacon works even if the sender doesn’t have the app. If a favorite texts you or uses your keyphrase, you’ll hear it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
