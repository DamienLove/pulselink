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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    showAds: Boolean,
    onBack: () -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onEditContact: (String, String, String?) -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onToggleRemoteOverride: (Boolean) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onSendLink: () -> Unit,
    onApproveLink: () -> Unit,
    onSetRemotePin: (String?) -> Unit,
    onPing: suspend () -> Boolean,
    onDelete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = contact?.displayName ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    contact?.let { target ->
                        if (target.phoneNumber.isNotBlank()) {
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    onCallContact(target)
                                }
                            }) {
                                Icon(Icons.Filled.Call, contentDescription = "Call contact")
                            }
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit contact")
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
                    onSetRemotePin = onSetRemotePin,
                    onToggleRemoteSound = onToggleRemoteSound,
                    onPing = launchPing
                )
                SettingsCard(
                    contact = contact,
                    onToggleLocation = onToggleLocation,
                    onToggleCamera = onToggleCamera,
                    onToggleAutoCall = onToggleAutoCall,
                    onToggleFavorite = onToggleFavorite,
                    onTogglePrivate = onTogglePrivate,
                    onEditEmergencyAlert = onEditEmergencyAlert,
                    onEditCheckInAlert = onEditCheckInAlert,
                    onToggleRemoteOverride = onToggleRemoteOverride,
                    onToggleRemoteSound = onToggleRemoteSound,
                    onDelete = onDelete
                )
            }
        }
    }

    if (showEditDialog && contact != null) {
        EditContactDialog(
            contact = contact,
            onDismiss = { showEditDialog = false },
            onSave = { name, phone, email ->
                onEditContact(name, phone, email)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun Header(contact: Contact) {
    val primaryPhone = (listOf(contact.phoneNumber) + contact.additionalPhones).firstOrNull { it.isNotBlank() }
    val primaryEmail = contact.email?.takeIf { it.isNotBlank() } ?: contact.additionalEmails.firstOrNull { it.isNotBlank() }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.height(72.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = contact.displayName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        if (!primaryPhone.isNullOrBlank()) {
            Text(
                text = primaryPhone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        primaryEmail?.let { email ->
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    onSetRemotePin: (String?) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onPing: () -> Unit
) {
    val canSendLink = contact.phoneNumber.isNotBlank() || contact.email?.isNotBlank() == true
    val context = LocalContext.current
    var showPinDialog by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Link status", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            when (contact.linkStatus) {
                LinkStatus.NONE -> {
                    Text(text = "This contact is not linked yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSendLink, enabled = canSendLink, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send link request")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Text(text = "Or set up SMS override:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "If they don't have the app, set a PIN so they can text 'pulselink <PIN> emergency' to trigger an alert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    val hasPin = !contact.remotePin.isNullOrBlank()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { showPinDialog = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (hasPin) "Change PIN" else "Set PIN")
                        }
                        if (hasPin) {
                            OutlinedButton(
                                onClick = {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email ?: ""))
                                        putExtra(Intent.EXTRA_SUBJECT, "Override Instructions")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "You have been set as a trusted contact. Even without the PulseLink app, you can trigger an emergency alert on my phone by texting exactly:\n\n'pulselink ${contact.remotePin} emergency'\n\nto my number."
                                        )
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                        Toast.makeText(context, "Opening email client...", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Mail, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Info")
                            }
                        }
                    }
                    if (hasPin) {
                        Text(text = "Active PIN: ${contact.remotePin}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Download PulseLink")
                                putExtra(Intent.EXTRA_TEXT, "Download PulseLink for Android: https://play.google.com/store/apps/details?id=com.pulselink\n\niOS Coming Soon!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share PulseLink"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Download Link")
                    }
                }
                LinkStatus.OUTBOUND_PENDING -> {
                    Text(text = "Awaiting their approval.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onSendLink, enabled = canSendLink, modifier = Modifier.fillMaxWidth()) {
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

    if (showPinDialog) {
        SetPinDialog(
            currentPin = contact.remotePin,
            onDismiss = { showPinDialog = false },
            onSave = { newPin ->
                onSetRemotePin(newPin)
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun SetPinDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var pin by remember { mutableStateOf(currentPin ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Set Emergency PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter a code (e.g. 0000). The contact can text 'pulselink <code> emergency' to force an alert.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter { char -> char.isDigit() } },
                    label = { Text("PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(pin.takeIf { it.isNotBlank() }) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            if (currentPin != null) {
                TextButton(onClick = { onSave(null) }) { Text("Clear PIN") }
            }
        }
    )
}

@Composable
private fun SettingsCard(
    contact: Contact,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onToggleFavorite: (Boolean) -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleRemoteOverride: (Boolean) -> Unit,
    onToggleRemoteSound: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = "Contact settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            ToggleRow(
                title = "Favorite contact",
                subtitle = "Show in Favorites tab",
                checked = contact.isFavorite,
                onCheckedChange = onToggleFavorite
            )
            ToggleRow(
                title = "Private contact",
                subtitle = "Show only in Private tab",
                checked = contact.isPrivate,
                onCheckedChange = onTogglePrivate
            )
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
private fun EditContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(contact.displayName) }
    var phone by remember { mutableStateOf(contact.phoneNumber) }
    var email by remember { mutableStateOf(contact.email.orEmpty()) }
    val canSave = name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (preferred)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSave) {
                                onSave(name.trim(), phone.trim(), email.trim().ifBlank { null })
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name.trim(), phone.trim(), email.trim().ifBlank { null })
                },
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
