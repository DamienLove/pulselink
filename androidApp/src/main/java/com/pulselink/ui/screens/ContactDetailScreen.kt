package com.pulselink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import android.widget.Toast
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.RemotePresence
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact?,
    onBack: () -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onToggleRemoteOverride: (Boolean) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onSendLink: () -> Unit,
    onApproveLink: () -> Unit,
    onPing: suspend () -> Boolean,
    onDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = contact?.displayName ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    contact?.let { target ->
                        IconButton(onClick = {
                            coroutineScope.launch {
                                onCallContact(target)
                            }
                        }) {
                            Icon(Icons.Filled.Call, contentDescription = "Call contact")
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        if (contact == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Contact not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        } else {
            val launchPing: () -> Unit = {
                coroutineScope.launch {
                    val result = runCatching { onPing() }
                    val toastText = when {
                        result.isFailure -> "Check-in failed to send"
                        result.getOrDefault(false) -> "Check-in sent"
                        else -> "Check-in sent (receiver may still be on silent)"
                    }
                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Header(contact)
                LinkStatusSection(
                    contact = contact,
                    onSendLink = onSendLink,
                    onApproveLink = onApproveLink,
                    onToggleRemoteSound = onToggleRemoteSound,
                    onPing = launchPing
                )
                SettingsCard(
                    contact = contact,
                    onToggleLocation = onToggleLocation,
                    onToggleCamera = onToggleCamera,
                    onToggleAutoCall = onToggleAutoCall,
                    onEditEmergencyAlert = onEditEmergencyAlert,
                    onEditCheckInAlert = onEditCheckInAlert,
                    onToggleRemoteOverride = onToggleRemoteOverride,
                    onToggleRemoteSound = onToggleRemoteSound,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun Header(contact: Contact) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.height(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = contact.displayName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (contact.linkStatus == LinkStatus.LINKED) {
            Spacer(modifier = Modifier.height(8.dp))
            PresenceBadge(
                presence = contact.remotePresence,
                lastSeen = contact.remoteLastSeen
            )
        }
    }
}

@Composable
private fun LinkStatusSection(
    contact: Contact,
    onSendLink: () -> Unit,
    onApproveLink: () -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onPing: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Link status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            when (contact.linkStatus) {
                LinkStatus.NONE -> {
                    Text(text = "This contact is not linked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSendLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send link request")
                    }
                }
                LinkStatus.OUTBOUND_PENDING -> {
                    Text(text = "Awaiting their approval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSendLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resend link request")
                    }
                }
                LinkStatus.INBOUND_REQUEST -> {
                    Text(text = "This contact wants to connect.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow remote sound change",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Let this contact update the alert tones on this device right away.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = contact.allowRemoteSoundChange,
                            onCheckedChange = onToggleRemoteSound
                        )
                    }
                    Button(onClick = onApproveLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve link")
                    }
                }
                LinkStatus.LINKED -> {
                    Text(text = "Linked", color = MaterialTheme.colorScheme.tertiary)
                    OutlinedButton(onClick = onPing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send check-in")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    contact: Contact,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleRemoteOverride: (Boolean) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Contact settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            ToggleRow(title = "Location share", subtitle = "Include GPS when alerting", checked = contact.includeLocation, onCheckedChange = onToggleLocation)
            ActionRow(title = "Emergency alert tone", subtitle = contact.emergencySoundKey ?: "Default", onClick = onEditEmergencyAlert)
            ActionRow(title = "Check-in alert tone", subtitle = contact.checkInSoundKey ?: "Default", onClick = onEditCheckInAlert)
            Text(
                text = stringResource(R.string.contact_detail_sound_sync_explanation, contact.displayName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.contact_detail_sound_sync_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToggleRow(title = "Camera enable", subtitle = "Capture short evidence clip", checked = contact.cameraEnabled, onCheckedChange = onToggleCamera)
            ToggleRow(title = "Auto call after alert", subtitle = "Call after sending SMS", checked = contact.autoCall, onCheckedChange = onToggleAutoCall)
            ToggleRow(
                title = "Allow remote ringer override",
                subtitle = "Let this contact change your DND",
                checked = contact.allowRemoteOverride,
                enabled = contact.linkStatus == LinkStatus.LINKED,
                onCheckedChange = onToggleRemoteOverride
            )
            ToggleRow(
                title = "Allow remote sound change",
                subtitle = "They can update your alert tone",
                checked = contact.allowRemoteSoundChange,
                enabled = contact.linkStatus == LinkStatus.LINKED,
                onCheckedChange = onToggleRemoteSound
            )
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Button(
                onClick = onDelete,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete contact")
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 8.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PresenceBadge(presence: RemotePresence, lastSeen: Long?) {
    val (label, dotColor) = when (presence) {
        RemotePresence.ONLINE -> stringResource(id = R.string.presence_online) to Color(0xFF12C26B)
        RemotePresence.RECENT -> stringResource(id = R.string.presence_recent) to Color(0xFFF59E0B)
        RemotePresence.OFFLINE -> stringResource(id = R.string.presence_offline) to Color(0xFFEF4444)
        RemotePresence.STALE -> stringResource(id = R.string.presence_stale) to MaterialTheme.colorScheme.outline
        RemotePresence.UNKNOWN -> stringResource(id = R.string.presence_unknown) to MaterialTheme.colorScheme.outlineVariant
    }
    val lastActive = lastSeen?.let {
        android.text.format.DateUtils.getRelativeTimeSpanString(
            it,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
            android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dotColor, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            lastActive?.let {
                Text(
                    text = "Last active $it",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
