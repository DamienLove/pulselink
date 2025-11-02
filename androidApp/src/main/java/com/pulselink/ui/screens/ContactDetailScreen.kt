package com.pulselink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact?,
    onBack: () -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onToggleRemoteOverride: (Boolean) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onSendLink: () -> Unit,
    onApproveLink: () -> Unit,
    onPing: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = contact?.displayName ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Header(contact)
                LinkStatusSection(contact, onSendLink, onApproveLink, onPing)
                SettingsCard(
                    contact = contact,
                    onToggleLocation = onToggleLocation,
                    onToggleCamera = onToggleCamera,
                    onToggleAutoCall = onToggleAutoCall,
                    onEditEmergencyAlert = onEditEmergencyAlert,
                    onEditCheckInAlert = onEditCheckInAlert,
                    onToggleRemoteOverride = onToggleRemoteOverride,
                    onToggleRemoteSound = onToggleRemoteSound
                )
                Button(onClick = onDelete, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)), modifier = Modifier.fillMaxWidth()) {
                    Text("Remove contact")
                }
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
        Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9AA0B4))
    }
}

@Composable
private fun LinkStatusSection(
    contact: Contact,
    onSendLink: () -> Unit,
    onApproveLink: () -> Unit,
    onPing: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x161E2030)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Link status", style = MaterialTheme.typography.titleMedium, color = Color.White)
            when (contact.linkStatus) {
                LinkStatus.NONE -> {
                    Text(text = "This contact is not linked yet.", color = Color(0xFFB7BECF))
                    Button(onClick = onSendLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send link request")
                    }
                }
                LinkStatus.OUTBOUND_PENDING -> {
                    Text(text = "Awaiting their approval.", color = Color(0xFFB7BECF))
                    Button(onClick = onSendLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resend link request")
                    }
                }
                LinkStatus.INBOUND_REQUEST -> {
                    Text(text = "This contact wants to connect.", color = Color(0xFFB7BECF))
                    Button(onClick = onApproveLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve link")
                    }
                }
                LinkStatus.LINKED -> {
                    Text(text = "Linked", color = Color(0xFF4ADE80))
                    OutlinedButton(onClick = onPing, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send ping")
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
    onToggleRemoteSound: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0x141E2030)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Contact settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
            ToggleRow(title = "Location share", subtitle = "Include GPS when alerting", checked = contact.includeLocation, onCheckedChange = onToggleLocation)
            ActionRow(title = "Emergency alert tone", subtitle = contact.emergencySoundKey ?: "Default", onClick = onEditEmergencyAlert)
            ActionRow(title = "Check-in alert tone", subtitle = contact.checkInSoundKey ?: "Default", onClick = onEditCheckInAlert)
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color(0xFF9AA0B4), style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 8.dp)
    ) {
        Text(text = title, color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = Color(0xFF9AA0B4), style = MaterialTheme.typography.bodySmall)
    }
}
