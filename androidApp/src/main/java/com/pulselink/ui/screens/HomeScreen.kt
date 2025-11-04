package com.pulselink.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.ads.NativeAdCard
import com.pulselink.ui.state.PulseLinkUiState
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun HomeScreen(
    state: PulseLinkUiState,
    onToggleListening: (Boolean) -> Unit,
    onSendCheckIn: () -> Unit,
    onTriggerTest: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onToggleProMode: (Boolean) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onSendManualMessage: suspend (Contact, String) -> ManualMessageResult,
    onAlertsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf(TextFieldValue()) }
    var newContactPhone by remember { mutableStateOf(TextFieldValue()) }
    var allowRemoteSound by remember { mutableStateOf(false) }
    var searchValue by remember { mutableStateOf(TextFieldValue()) }

    var messageContact by remember { mutableStateOf<Contact?>(null) }
    var messageBody by remember { mutableStateOf(TextFieldValue()) }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            resolveContact(context, uri)?.let { (name, number) ->
                newContactName = TextFieldValue(name)
                if (number.isNotBlank()) {
                    newContactPhone = TextFieldValue(number)
                }
            }
        }
    }

    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            newContactName = TextFieldValue()
            newContactPhone = TextFieldValue()
            allowRemoteSound = false
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10131F), Color(0xFF090B11))
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(state, onToggleListening)
            NavigationRow(
                onAlertsClick = onAlertsClick,
                onSettingsClick = onSettingsClick,
                onUpgradeClick = onUpgradeClick
            )
            QuickActionsRow(onSendCheckIn = onSendCheckIn, onTriggerTest = onTriggerTest)
            SearchAndAddRow(
                searchValue = searchValue,
                onSearchChange = { searchValue = it },
                onAddClick = { showAddDialog = true }
            )
            ContactsList(
                state = state,
                searchQuery = searchValue.text,
                onDeleteContact = onDeleteContact,
                onCallContact = { contact ->
                    coroutineScope.launch { onCallContact(contact) }
                },
                onMessageContact = { contact ->
                    if (contact.linkStatus != LinkStatus.LINKED || contact.linkCode.isNullOrBlank()) {
                        Toast.makeText(context, "Contact must approve link before messaging", Toast.LENGTH_SHORT).show()
                    } else {
                        messageContact = contact
                        messageBody = TextFieldValue()
                    }
                },
                onContactSelected = onContactSelected,
                onContactSettings = onContactSettings
            )
            if (state.adsAvailable) {
                UpgradeCard(isPro = state.isProUser, onTogglePro = onToggleProMode)
            }
            if (state.showAds) {
                NativeAdCard(enabled = true)
                BannerAdSlot(enabled = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            name = newContactName,
            onNameChange = { newContactName = it },
            phone = newContactPhone,
            onPhoneChange = { newContactPhone = it },
            allowRemoteSound = allowRemoteSound,
            onAllowRemoteSoundChange = { allowRemoteSound = it },
            onImport = { contactPicker.launch(null) },
            onDismiss = { showAddDialog = false },
            onSave = {
                val name = newContactName.text.trim()
                val phone = newContactPhone.text.trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    onAddContact(
                        Contact(
                            displayName = name,
                            phoneNumber = phone,
                            allowRemoteSoundChange = allowRemoteSound
                        )
                    )
                    showAddDialog = false
                } else {
                    Toast.makeText(context, "Name and phone are required", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    messageContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { messageContact = null },
            title = { Text(text = "Message ${contact.displayName}") },
            text = {
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val body = messageBody.text.trim()
                    if (body.isEmpty()) {
                        Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                    } else {
                        coroutineScope.launch {
                            var outcome: ManualMessageResult? = null
                            val toastText = try {
                                val result = onSendManualMessage(contact, body)
                                outcome = result
                                when (result) {
                                    is ManualMessageResult.Success -> {
                                        if (result.overrideApplied) {
                                            "Message sent"
                                        } else {
                                            "Message sent (receiver may still be on silent)"
                                        }
                                    }
                                    is ManualMessageResult.Failure -> when (result.reason) {
                                        ManualMessageResult.Failure.Reason.CONTACT_MISSING -> "Contact no longer available"
                                        ManualMessageResult.Failure.Reason.NOT_LINKED -> "Link this contact before messaging"
                                        ManualMessageResult.Failure.Reason.SMS_FAILED -> "Message failed to send"
                                        ManualMessageResult.Failure.Reason.UNKNOWN -> "Message failed to send"
                                    }
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                "Message failed to send"
                            }
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                            if (outcome is ManualMessageResult.Success) {
                                messageContact = null
                            }
                        }
                    }
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { messageContact = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HeaderSection(state: PulseLinkUiState, onToggleListening: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "PulseLink logo",
                    modifier = Modifier
                        .size(56.dp)
                        .padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "PulseLink",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Your hands-free safety net",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B4)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (state.isListening) "Listening on" else "Listening off",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Switch(checked = state.isListening, onCheckedChange = onToggleListening)
        }
    }
}

@Composable
private fun NavigationRow(
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(icon = Icons.Filled.Notifications, label = "Alerts", onClick = onAlertsClick)
        NavButton(icon = Icons.Filled.Settings, label = "Settings", onClick = onSettingsClick)
        NavButton(icon = Icons.Filled.Star, label = "Pro", onClick = onUpgradeClick)
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.08f)) {
            IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDC3D1))
    }
}

@Composable
private fun QuickActionsRow(onSendCheckIn: () -> Unit, onTriggerTest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSendCheckIn,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send check-in")
        }
        Button(
            onClick = onTriggerTest,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Trigger test")
        }
    }
}

@Composable
private fun SearchAndAddRow(
    searchValue: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    onAddClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchValue,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Search contacts") }
        )
        OutlinedButton(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add trusted contact")
        }
    }
}

@Composable
private fun ContactsList(
    state: PulseLinkUiState,
    searchQuery: String,
    onDeleteContact: (Long) -> Unit,
    onCallContact: (Contact) -> Unit,
    onMessageContact: (Contact) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x141C1C2A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Safeword contacts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Linked partners will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9AA0B4)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val contacts = state.contacts
            val filtered = contacts.filter {
                searchQuery.isBlank() ||
                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
            if (contacts.isEmpty()) {
                Text(
                    text = "No contacts yet. Add someone to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B4)
                )
            } else if (filtered.isEmpty()) {
                Text(
                    text = "No contacts match your search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B4)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onOpenMessages = { onContactSelected(contact.id) },
                            onOpenSettings = { onContactSettings(contact.id) },
                            onDelete = { onDeleteContact(contact.id) },
                            onCall = { onCallContact(contact) },
                            onMessage = { onMessageContact(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    val statusLabel = when (contact.linkStatus) {
        LinkStatus.NONE -> null
        LinkStatus.OUTBOUND_PENDING -> "Awaiting approval"
        LinkStatus.INBOUND_REQUEST -> "Approve request"
        LinkStatus.LINKED -> "Linked"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMessages() },
        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = contact.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Contact settings",
                                tint = Color(0xFF9AA0B4)
                            )
                        }
                    }
                    Text(text = contact.phoneNumber, color = Color(0xFFB7BECF), style = MaterialTheme.typography.bodySmall)
                    statusLabel?.let {
                        Text(text = it, color = Color(0xFF7DD3FC), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onCall) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color(0xFF34D399))
                    }
                    IconButton(onMessage) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message", tint = Color(0xFF60A5FA))
                    }
                    IconButton(onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Color(0xFFF87171))
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeCard(isPro: Boolean, onTogglePro: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x141C1C2A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PulseLink Pro",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = if (isPro) {
                    "Pro mode is active on this device."
                } else {
                    "Unlock Pro to remove ads and enable premium automations."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9AA0B4)
            )
            Button(
                onClick = { onTogglePro(!isPro) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPro) Color(0xFF2563EB) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = if (isPro) "Disable Pro mode" else "Enable Pro mode")
            }
        }
    }
}

@Composable
private fun AddContactDialog(
    name: TextFieldValue,
    onNameChange: (TextFieldValue) -> Unit,
    phone: TextFieldValue,
    onPhoneChange: (TextFieldValue) -> Unit,
    allowRemoteSound: Boolean,
    onAllowRemoteSoundChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add trusted contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Allow remote alert changes", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Let this contact update which sounds play on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9AA0B4)
                        )
                    }
                    Switch(checked = allowRemoteSound, onCheckedChange = onAllowRemoteSoundChange)
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Import from contacts")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun resolveContact(context: Context, uri: Uri): Pair<String, String>? {
    val resolver = context.contentResolver
    resolver.query(uri, arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (idIndex < 0) return null
            val contactId = cursor.getString(idIndex)
            val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phones ->
                if (phones.moveToFirst()) {
                    val numberIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val number = if (numberIndex >= 0) phones.getString(numberIndex) ?: "" else ""
                    return displayName to number
                }
            }
            return displayName to ""
        }
    return null
}
