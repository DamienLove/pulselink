package com.pulselink.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit,
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

    Scaffold(
        topBar = {
            HomeTopBar(
                isListening = state.isListening,
                onAlertsClick = onAlertsClick,
                onSettingsClick = onSettingsClick,
                onUpgradeClick = onUpgradeClick,
                showUpgrade = !state.isProUser
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add contact") },
                text = { Text("Add contact") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    ListeningCard(
                        isListening = state.isListening,
                        onToggleListening = onToggleListening
                    )
                }
                item {
                    QuickActionsCard(
                        onSendCheckIn = onSendCheckIn,
                        onTriggerTest = onTriggerTest
                    )
                }
                item {
                    SearchBar(
                        value = searchValue,
                        onValueChange = { searchValue = it },
                        onClear = { searchValue = TextFieldValue() }
                    )
                }
                item {
                    ContactListCard(
                        contacts = state.contacts,
                        searchQuery = searchValue.text,
                        onContactSelected = onContactSelected,
                        onContactSettings = onContactSettings,
                        onDeleteContact = onDeleteContact,
                        onCallContact = { contact ->
                            coroutineScope.launch { onCallContact(contact) }
                        },
                        onMessageContact = { contact ->
                            if (contact.linkCode.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.contact_action_message_requires_link),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                if (contact.linkStatus != LinkStatus.LINKED) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.contact_action_message_pending_warning),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                messageContact = contact
                                messageBody = TextFieldValue()
                            }
                        },
                        onSendLink = onSendLink,
                        onApproveLink = onApproveLink
                    )
                }
                if (!state.isProUser && !state.showAds) {
                    item {
                        UpgradePromoCard(onToggleProMode = onToggleProMode, isPro = state.isProUser)
                    }
                }
                if (state.showAds) {
                    item { NativeAdCard(enabled = true) }
                    item { BannerAdSlot(enabled = true, modifier = Modifier.fillMaxWidth()) }
                }
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
        MessageDialog(
            contact = contact,
            initialValue = messageBody,
            onDismiss = {
                messageContact = null
                messageBody = TextFieldValue()
            },
            onSend = { body ->
                coroutineScope.launch {
                    var outcome: ManualMessageResult? = null
                    val toastText = try {
                        val result = onSendManualMessage(contact, body)
                        outcome = result
                        when (result) {
                            is ManualMessageResult.Success -> when {
                                result.deliveryPending -> context.getString(R.string.manual_message_sent_pending)
                                result.overrideApplied -> context.getString(R.string.manual_message_sent)
                                else -> context.getString(R.string.manual_message_sent_muted)
                            }

                            is ManualMessageResult.Failure -> when (result.reason) {
                                ManualMessageResult.Failure.Reason.CONTACT_MISSING -> context.getString(R.string.manual_message_error_contact_missing)
                                ManualMessageResult.Failure.Reason.NOT_LINKED -> context.getString(R.string.manual_message_error_not_linked)
                                ManualMessageResult.Failure.Reason.SMS_FAILED -> context.getString(R.string.manual_message_error_failed)
                                ManualMessageResult.Failure.Reason.UNKNOWN -> context.getString(R.string.manual_message_error_failed)
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        context.getString(R.string.manual_message_error_failed)
                    }
                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    if (outcome is ManualMessageResult.Success) {
                        messageContact = null
                        messageBody = TextFieldValue()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    isListening: Boolean,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    showUpgrade: Boolean
) {
    val topAppBarState = rememberTopAppBarState()
    CenterAlignedTopAppBar(
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PulseLink",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (isListening) "Listening for safewords" else "Listening paused",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onAlertsClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "Alerts")
            }
        },
        actions = {
            if (showUpgrade) {
                IconButton(onClick = onUpgradeClick) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Upgrade")
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        },
        scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    )
}

@Composable
private fun ListeningCard(
    isListening: Boolean,
    onToggleListening: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (isListening) "PulseLink is listening" else "Listening is paused",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isListening) {
                        "Safewords will trigger alerts instantly."
                    } else {
                        "Turn listening back on so PulseLink can monitor safewords."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isListening,
                onCheckedChange = onToggleListening
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onSendCheckIn: () -> Unit,
    onTriggerTest: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Quick actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    TextButton(
                        onClick = onSendCheckIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send check-in")
                    }
                }
                OutlinedButton(
                    onClick = onTriggerTest,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Trigger test")
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear search")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search contacts") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors()
    )
}

@Composable
private fun ContactListCard(
    contacts: List<Contact>,
    searchQuery: String,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onCallContact: (Contact) -> Unit,
    onMessageContact: (Contact) -> Unit,
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit
) {
    val filtered = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Support circle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (contacts.isEmpty()) "Add trusted partners to receive your alerts." else "Manage alert recipients and stay in sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            when {
                contacts.isEmpty() -> {
                    EmptyStateCard(
                        title = "No contacts yet",
                        description = "Add someone you trust so PulseLink can reach them during emergencies."
                    )
                }

                filtered.isEmpty() -> {
                    EmptyStateCard(
                        title = "No results found",
                        description = "Try searching with a different name or phone number."
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filtered.forEach { contact ->
                            ContactRow(
                                contact = contact,
                                onOpenMessages = { onContactSelected(contact.id) },
                                onOpenSettings = { onContactSettings(contact.id) },
                                onDelete = { onDeleteContact(contact.id) },
                                onSendLinkRequest = { onSendLink(contact.id) },
                                onApproveLink = { onApproveLink(contact.id) },
                                onCall = { onCallContact(contact) },
                                onMessage = { onMessageContact(contact) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {}
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: () -> Unit,
    onSendLinkRequest: () -> Unit,
    onApproveLink: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    val statusLabel = when (contact.linkStatus) {
        LinkStatus.NONE -> stringResource(R.string.contact_status_not_linked)
        LinkStatus.OUTBOUND_PENDING -> stringResource(R.string.contact_status_outbound_pending)
        LinkStatus.INBOUND_REQUEST -> stringResource(R.string.contact_status_inbound_request)
        LinkStatus.LINKED -> stringResource(R.string.contact_status_linked)
    }
    val statusColor = when (contact.linkStatus) {
        LinkStatus.NONE -> Color(0xFFF87171)
        LinkStatus.OUTBOUND_PENDING -> Color(0xFF60A5FA)
        LinkStatus.INBOUND_REQUEST -> Color(0xFFFBBF24)
        LinkStatus.LINKED -> Color(0xFF34D399)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = contact.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = statusColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onCall) { Icon(Icons.Filled.Call, contentDescription = "Call") }
                        IconButton(onClick = onMessage) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message") }
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
                    }
                }
            }
            LinkActionButtons(
                contact = contact,
                onSendLinkRequest = onSendLinkRequest,
                onApproveLink = onApproveLink
            )
        }
    }
}

@Composable
private fun LinkActionButtons(
    contact: Contact,
    onSendLinkRequest: () -> Unit,
    onApproveLink: () -> Unit
) {
    when (contact.linkStatus) {
        LinkStatus.NONE -> {
            Text(
                text = stringResource(R.string.contact_action_help_not_linked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onSendLinkRequest, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.contact_action_send_link))
            }
        }

        LinkStatus.OUTBOUND_PENDING -> {
            Text(
                text = stringResource(R.string.contact_action_help_outbound_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onSendLinkRequest, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.contact_action_resend_link))
            }
        }

        LinkStatus.INBOUND_REQUEST -> {
            Text(
                text = stringResource(R.string.contact_action_help_inbound_request),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onApproveLink) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.contact_action_approve_link))
                }
            }
        }

        LinkStatus.LINKED -> Unit
    }
}

@Composable
private fun UpgradePromoCard(
    onToggleProMode: (Boolean) -> Unit,
    isPro: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Upgrade to PulseLink Pro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Remove ads, unlock premium automations, and enhance remote overrides.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = { onToggleProMode(true) }) {
                Text(text = "Enable Pro mode")
            }
        }
    }
}

@Composable
private fun MessageDialog(
    contact: Contact,
    initialValue: TextFieldValue,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var body by remember { mutableStateOf(initialValue) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Message") },
        text = {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmed = body.text.trim()
                if (trimmed.isEmpty()) {
                    Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    onSend(trimmed)
                }
            }) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add trusted contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Allow remote alert changes", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Let this contact update which sounds play on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = allowRemoteSound, onCheckedChange = onAllowRemoteSoundChange)
                    }
                }
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import from contacts")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.text.trim()
                val trimmedPhone = phone.text.trim()
                if (trimmedName.isEmpty() || trimmedPhone.isEmpty()) {
                    Toast.makeText(context, "Name and phone are required", Toast.LENGTH_SHORT).show()
                } else {
                    onSave()
                }
            }) {
                Text("Save contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun resolveContact(context: Context, uri: Uri): Pair<String, String>? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
        val contactId = if (idIndex >= 0) cursor.getString(idIndex) else return null
        val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else ""
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            " = ?",
            arrayOf(contactId),
            null
        )?.use { phoneCursor ->
            if (phoneCursor.moveToFirst()) {
                val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val number = if (numberIndex >= 0) phoneCursor.getString(numberIndex) ?: "" else ""
                return displayName to number
            }
        }
        displayName to ""
    }
}
