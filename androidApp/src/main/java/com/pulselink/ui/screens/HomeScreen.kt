package com.pulselink.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun HomeScreen(
    state: PulseLinkUiState,
    onToggleListening: (Boolean) -> Unit,
    onTriggerEmergency: () -> Unit,
    onSendCheckIn: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onDeleteContact: (Long) -> Unit,
    onToggleProMode: (Boolean) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onSendManualMessage: suspend (Contact, String) -> ManualMessageResult,
    onReorderContacts: (List<Long>) -> Unit,
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(state, onToggleListening = { onToggleListening(!state.isListening) })
            NavigationRow(
                onAlertsClick = onAlertsClick,
                onSettingsClick = onSettingsClick,
                onUpgradeClick = onUpgradeClick
            )
            QuickActionsRow(
                onTriggerEmergency = onTriggerEmergency,
                onSendCheckInAll = onSendCheckIn
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
                onContactSelected = onContactSelected,
                onContactSettings = onContactSettings,
                onSendLink = onSendLink,
                onApproveLink = onApproveLink,
                onReorderContacts = onReorderContacts
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
private fun HeaderSection(state: PulseLinkUiState, onToggleListening: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "PulseLink logo",
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "PulseLink",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Use the mic button below or the Settings toggle to control listening.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        MicToggle(isListening = state.isListening, onToggle = onToggleListening)
    }
}

@Composable
private fun MicToggle(isListening: Boolean, onToggle: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val background = if (isListening) Color(0xFF059669) else Color(0xFFDC2626)
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(background, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = if (isListening) "Turn listening off" else "Turn listening on",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isListening) "Listening on" else "Listening off",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun QuickActionsRow(onTriggerEmergency: () -> Unit, onSendCheckInAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTriggerEmergency,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB91C1C),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = "Trigger emergency alert")
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "EMERGENCY", fontWeight = FontWeight.Black)
        }
        Button(
            onClick = onSendCheckInAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = "Send check-in to all contacts")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check-in all")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactsList(
    state: PulseLinkUiState,
    searchQuery: String,
    onDeleteContact: (Long) -> Unit,
    onCallContact: (Contact) -> Unit,
    onMessageContact: (Contact) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit,
    onReorderContacts: (List<Long>) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val secondaryText = colorScheme.onBackground.copy(alpha = 0.7f)

    val sortedContacts = remember(state.contacts) {
        state.contacts.sortedWith(
            compareBy<Contact> { it.contactOrder }
                .thenBy { it.displayName.lowercase() }
        )
    }
    var contactItems by remember { mutableStateOf(sortedContacts) }
    var isReordering by remember { mutableStateOf(false) }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            if (!isReordering) isReordering = true
            contactItems = contactItems.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { startIndex, endIndex ->
            if (isReordering && startIndex != endIndex && startIndex >= 0 && endIndex >= 0) {
                onReorderContacts(contactItems.map { it.id })
            }
            isReordering = false
        }
    )

    LaunchedEffect(sortedContacts, isReordering) {
        if (!isReordering) {
            contactItems = sortedContacts
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val displayedContacts = remember(contactItems, searchQuery) {
        if (isSearching) {
            val queryLower = searchQuery.lowercase()
            contactItems.filter {
                it.displayName.lowercase().contains(queryLower) ||
                    it.phoneNumber.lowercase().contains(queryLower)
            }
        } else {
            contactItems
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Trusted contacts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Text(
                    text = if (contactItems.size > 1) {
                        "Drag contacts to set the order used for emergency alerts."
                    } else {
                        "Add contacts to build your PulseLink support circle."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryText
                )
            }
            when {
                contactItems.isEmpty() -> {
                    Text(
                        text = "No contacts yet. Add someone to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )
                }
                displayedContacts.isEmpty() -> {
                    Text(
                        text = "No contacts match your search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText
                    )
                }
                else -> {
                    val canReorder = !isSearching && contactItems.size > 1
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .let { base ->
                                if (canReorder) {
                                    base
                                        .reorderable(reorderState)
                                        .detectReorderAfterLongPress(reorderState)
                                } else {
                                    base
                                }
                            },
                        state = reorderState.listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(displayedContacts, key = { _, contact -> contact.id }) { _, contact ->
                            ReorderableItem(
                                reorderableState = reorderState,
                                key = contact.id
                            ) { isDragging ->
                                ContactRow(
                                    contact = contact,
                                    onOpenMessages = { onContactSelected(contact.id) },
                                    onOpenSettings = { onContactSettings(contact.id) },
                                    onDelete = { onDeleteContact(contact.id) },
                                    onSendLinkRequest = { onSendLink(contact.id) },
                                    onApproveLink = { onApproveLink(contact.id) },
                                    onCall = { onCallContact(contact) },
                                    onMessage = { onMessageContact(contact) },
                                    reorderEnabled = canReorder,
                                    isDragging = isDragging
                                )
                            }
                        }
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
    onSendLinkRequest: () -> Unit,
    onApproveLink: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    reorderEnabled: Boolean,
    isDragging: Boolean
) {
    val statusLabel = when (contact.linkStatus) {
        LinkStatus.NONE -> stringResource(R.string.contact_status_not_linked)
        LinkStatus.OUTBOUND_PENDING -> stringResource(R.string.contact_status_outbound_pending)
        LinkStatus.INBOUND_REQUEST -> stringResource(R.string.contact_status_inbound_request)
        LinkStatus.LINKED -> stringResource(R.string.contact_status_linked)
    }
    val statusColor = when (contact.linkStatus) {
        LinkStatus.NONE -> MaterialTheme.colorScheme.error
        LinkStatus.OUTBOUND_PENDING -> MaterialTheme.colorScheme.primary
        LinkStatus.INBOUND_REQUEST -> MaterialTheme.colorScheme.tertiary
        LinkStatus.LINKED -> MaterialTheme.colorScheme.secondary
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMessages() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = contact.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = contact.phoneNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onCall) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onMessage) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Message", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                    if (reorderEnabled) {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = "Reorder contact",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(24.dp)
                        )
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
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    when (contact.linkStatus) {
        LinkStatus.NONE -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.contact_action_help_not_linked),
                color = secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSendLinkRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.contact_action_send_link))
            }
        }

        LinkStatus.OUTBOUND_PENDING -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.contact_action_help_outbound_pending),
                color = secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSendLinkRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.contact_action_resend_link))
            }
        }

        LinkStatus.INBOUND_REQUEST -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.contact_action_help_inbound_request),
                color = secondaryText,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onApproveLink,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.contact_action_approve_link))
            }
        }

        LinkStatus.LINKED -> Unit
    }
}

@Composable
private fun UpgradeCard(isPro: Boolean, onTogglePro: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PulseLink Pro",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPro) {
                    "Pro mode is active on this device."
                } else {
                    "Unlock Pro to remove ads and enable premium automations."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { onTogglePro(!isPro) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPro) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPro) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
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
                        Text(text = "Allow remote alert changes", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Let this contact update which sounds play on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    return runCatching {
        resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (idIndex < 0) return@use null
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
                    return@runCatching displayName to number
                }
            }
            displayName to ""
        }
    }.getOrElse { error ->
        Log.w("HomeScreen", "Unable to import contact", error)
        Toast.makeText(
            context,
            context.getString(R.string.pulselink_contact_import_failed),
            Toast.LENGTH_SHORT
        ).show()
        null
    }
}
