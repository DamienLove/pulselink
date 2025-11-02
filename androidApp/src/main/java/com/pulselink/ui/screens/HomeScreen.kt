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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.pulselink.domain.model.EscalationTier
import com.pulselink.domain.model.SoundOption
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
    onSelectEmergencySound: (String) -> Unit,
    onSelectCheckInSound: (String) -> Unit,
    onToggleProMode: (Boolean) -> Unit,
    onContactSelected: (Long) -> Unit,
    onCallContact: (Contact) -> Unit,
    onMessageContact: (Contact) -> Unit,
    onEmergencyClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var searchValue by remember { mutableStateOf(TextFieldValue()) }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF11121B), Color(0xFF0B0C12))
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(isListening = state.isListening, onToggleListening = onToggleListening)
        NavigationRow(
            onEmergencyClick = onEmergencyClick,
            onLocationClick = onLocationClick,
            onAlertsClick = onAlertsClick,
            onSettingsClick = onSettingsClick,
            onUpgradeClick = onUpgradeClick
        )
        QuickActionsRow(
            onSendCheckIn = onSendCheckIn,
            onTriggerTest = onTriggerTest
        )
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
            DefaultAlertsCard(
                emergencyOptions = state.emergencySoundOptions,
                checkInOptions = state.checkInSoundOptions,
                selectedEmergency = state.settings.emergencyProfile.soundKey,
                selectedCheckIn = state.settings.checkInProfile.soundKey,
                onSelectEmergency = onSelectEmergencySound,
                onSelectCheckIn = onSelectCheckInSound
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
private fun HeaderSection(isListening: Boolean, onToggleListening: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.1f)
        ) {
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
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (isListening) "Listening On" else "Listening Off",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Switch(checked = isListening, onCheckedChange = onToggleListening)
        }
    }
}

@Composable
private fun NavigationRow(
    onEmergencyClick: () -> Unit,
    onLocationClick: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NavButton(icon = Icons.Filled.Warning, label = "Emergency", onClick = onEmergencyClick)
        NavButton(icon = Icons.Filled.Place, label = "Location", onClick = onLocationClick)
        NavButton(icon = Icons.Filled.Notifications, label = "Alerts", onClick = onAlertsClick)
        NavButton(icon = Icons.Filled.Settings, label = "Settings", onClick = onSettingsClick)
        NavButton(icon = Icons.Filled.Star, label = "Pro", onClick = onUpgradeClick)
    }
}

@Composable
private fun QuickActionsRow(
    onSendCheckIn: () -> Unit,
    onTriggerTest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSendCheckIn,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text = "Check-in Ping")
        }
        Button(
            onClick = onTriggerTest,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
        ) {
            Text(text = "Trigger Test")
        }
    }
}

@Composable
private fun NavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f)) {
            IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
                Icon(imageVector = icon, contentDescription = label, tint = Color.White)
            }
        }
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
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
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null)
            },
            label = { Text("Search contacts") }
        )
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add contact")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Add Trusted Contact")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Safeword Contacts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val filtered = state.contacts.filter {
                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery, ignoreCase = true)
                }
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

@Composable
private fun ContactRow(
    contact: Contact,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = contact.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = contact.phoneNumber, color = Color(0xFFBBBBBB), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onCall) {
                    Icon(imageVector = Icons.Filled.Phone, contentDescription = "Call", tint = Color(0xFF34D399))
                }
                IconButton(onClick = onMessage) {
                    Icon(imageVector = Icons.Filled.Message, contentDescription = "Message", tint = Color(0xFF60A5FA))
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF87171))
                }
            }
        }
    }
}

@Composable
private fun DefaultAlertsCard(
    emergencyOptions: List<SoundOption>,
    checkInOptions: List<SoundOption>,
    selectedEmergency: String?,
    selectedCheckIn: String?,
    onSelectEmergency: (String) -> Unit,
    onSelectCheckIn: (String) -> Unit
) {
    if (emergencyOptions.isEmpty() && checkInOptions.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color.White.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Default Alerts", color = Color.White, fontWeight = FontWeight.Bold)
            emergencyOptions.takeIf { it.isNotEmpty() }?.let {
                SoundDropdown(label = "Emergency Alert", options = it, selectedKey = selectedEmergency, onSelect = onSelectEmergency)
            }
            checkInOptions.takeIf { it.isNotEmpty() }?.let {
                SoundDropdown(label = "Check-in Alert", options = it, selectedKey = selectedCheckIn, onSelect = onSelectCheckIn)
            }
        }
    }
}

@Composable
private fun SoundDropdown(
    label: String,
    options: List<SoundOption>,
    selectedKey: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.firstOrNull { it.key == selectedKey } ?: options.firstOrNull()
    Column {
        Text(text = label, color = Color(0xFFBFBFBF), style = MaterialTheme.typography.bodySmall)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .background(Color.Transparent),
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Transparent)
                    .let { mod -> mod.clickable { expanded = true } },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedOption?.label ?: "Select", color = Color.White)
                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White)
            }
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSelect(option.key)
                    }
                )
            }
        }
    }
}

@Composable
private fun UpgradeCard(isPro: Boolean, onTogglePro: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color(0xFF1F1F2E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "PulseLink Pro", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "Unlock premium escalation controls", color = Color(0xFFBFBFBF), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = isPro, onCheckedChange = onTogglePro)
        }
    }
}

@Composable
private fun AddContactDialog(onDismiss: () -> Unit, onSave: (Contact) -> Unit) {
    val nameState = remember { mutableStateOf(TextFieldValue()) }
    val phoneState = remember { mutableStateOf(TextFieldValue()) }
    val tierState = remember { mutableStateOf(EscalationTier.EMERGENCY) }

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
                TierSelector(tierState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nameState.value.text.isNotBlank() && phoneState.value.text.isNotBlank()) {
                    onSave(
                        Contact(
                            displayName = nameState.value.text,
                            phoneNumber = phoneState.value.text,
                            escalationTier = tierState.value
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

@Composable
private fun TierSelector(state: MutableState<EscalationTier>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Escalation tier", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierButton(label = "Emergency", selected = state.value == EscalationTier.EMERGENCY) {
                state.value = EscalationTier.EMERGENCY
            }
            TierButton(label = "Check-in", selected = state.value == EscalationTier.CHECK_IN) {
                state.value = EscalationTier.CHECK_IN
            }
        }
    }
}

@Composable
private fun TierButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Color(0xFF2563EB) else Color.Transparent
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = background),
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(text = label, color = if (selected) Color.White else Color(0xFF2563EB))
    }
}
