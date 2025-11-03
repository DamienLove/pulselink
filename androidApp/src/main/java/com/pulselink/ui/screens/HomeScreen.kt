package com.pulselink.ui.screens

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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.ads.NativeAdCard
import com.pulselink.ui.state.PulseLinkUiState

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
    onCallContact: (Contact) -> Unit,
    onMessageContact: (Contact) -> Unit,
    onEmergencyClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchValue by remember { mutableStateOf(TextFieldValue()) }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF10131F), Color(0xFF090B11))
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
                onEmergencyClick = onEmergencyClick,
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
                onCallContact = onCallContact,
                onMessageContact = onMessageContact,
                onContactSelected = onContactSelected
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
            onDismiss = { showAddDialog = false },
            onSave = { contact ->
                onAddContact(contact)
                showAddDialog = false
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
    onEmergencyClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(icon = Icons.Filled.Warning, label = "Emergency", onClick = onEmergencyClick)
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
            Text("Check-in ping")
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
    onContactSelected: (Long) -> Unit
) {
    val filtered = state.contacts.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery, ignoreCase = true)
    }
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
            if (filtered.isEmpty()) {
                Text(
                    text = "No contacts yet. Add someone to get started.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B4)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            onOpen = { onContactSelected(contact.id) },
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
    onOpen: () -> Unit,
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
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = contact.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(text = contact.phoneNumber, color = Color(0xFFB7BECF), style = MaterialTheme.typography.bodySmall)
                    statusLabel?.let {
                        Text(text = it, color = Color(0xFF7DD3FC), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onCall) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color(0xFF34D399))
                    }
                    IconButton(onClick = onMessage) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message", tint = Color(0xFF60A5FA))
                    }
                    IconButton(onClick = onDelete) {
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
        colors = CardDefaults.cardColors(containerColor = Color(0x1A9155FD))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "PulseLink Pro", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isPro) "Pro active on this device" else "Unlock concierge escalation",
                    color = Color(0xFFE3E8FF),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = isPro, onCheckedChange = onTogglePro)
        }
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    val nameState = remember { mutableStateOf(TextFieldValue()) }
    val phoneState = remember { mutableStateOf(TextFieldValue()) }
    val allowRemoteSoundState = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add trusted contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = phoneState.value,
                    onValueChange = { phoneState.value = it },
                    label = { Text("Phone") }
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
                    Switch(
                        checked = allowRemoteSoundState.value,
                        onCheckedChange = { allowRemoteSoundState.value = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameState.value.text.isNotBlank() && phoneState.value.text.isNotBlank()) {
                    onSave(
                        Contact(
                            displayName = nameState.value.text,
                            phoneNumber = phoneState.value.text,
                            allowRemoteSoundChange = allowRemoteSoundState.value
                        )
                    )
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
