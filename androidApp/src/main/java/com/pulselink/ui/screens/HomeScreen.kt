package com.pulselink.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.RemotePresence
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.ads.NativeAdCard
import com.pulselink.ui.state.PulseLinkUiState
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun HomeScreen(
    state: PulseLinkUiState,
    onDismissAssistantShortcuts: () -> Unit,
    onTriggerEmergency: () -> Unit,
    onSendCheckIn: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onReorderContacts: (List<Long>) -> Unit,
    onRequestCancelEmergency: () -> Unit,
    isCancelingEmergency: Boolean = false,
    onAlertsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    showAddLoginPrompt: Boolean = false,
    onAddLoginClick: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf(TextFieldValue()) }
    var newContactPhone by remember { mutableStateOf(TextFieldValue()) }
    var newContactEmail by remember { mutableStateOf(TextFieldValue()) }
    var allowRemoteSound by remember { mutableStateOf(false) }
    var searchValue by remember { mutableStateOf(TextFieldValue()) }

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
            newContactEmail = TextFieldValue()
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderSection(
                state = state,
                onDismissAssistantShortcuts = onDismissAssistantShortcuts,
                onAlertsClick = onAlertsClick,
                onSettingsClick = onSettingsClick,
                onUpgradeClick = onUpgradeClick
            )
            if (showAddLoginPrompt) {
                AddLoginCard(
                    modifier = Modifier.fillMaxWidth(),
                    onAddLoginClick = onAddLoginClick
                )
            }
            QuickActionsRow(
                onTriggerEmergency = onTriggerEmergency,
                onSendCheckInAll = onSendCheckIn,
                isEmergencyActive = state.isEmergencyActive,
                onCancelEmergency = onRequestCancelEmergency,
                isCancelingEmergency = isCancelingEmergency
            )
            SearchAndAddRow(
                searchValue = searchValue,
                onSearchChange = { searchValue = it },
                onAddClick = { showAddDialog = true }
            )
            ContactsList(
                state = state,
                searchQuery = searchValue.text,
                onCallContact = { contact ->
                    coroutineScope.launch { onCallContact(contact) }
                },
                onContactSelected = onContactSelected,
                onContactSettings = onContactSettings,
                onSendLink = onSendLink,
                onApproveLink = onApproveLink,
                onReorderContacts = onReorderContacts
            )
            if (state.adsAvailable) {
                UpgradeCard(isPro = state.isProUser, onUpgradeClick = onUpgradeClick)
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
            email = newContactEmail,
            onEmailChange = { newContactEmail = it },
            allowRemoteSound = allowRemoteSound,
            onAllowRemoteSoundChange = { allowRemoteSound = it },
            onImport = { contactPicker.launch(null) },
            onDismiss = { showAddDialog = false },
            onSave = {
                val name = newContactName.text.trim()
                val phone = newContactPhone.text.trim()
                val email = newContactEmail.text.trim()
                if (name.isNotEmpty() && (phone.isNotEmpty() || email.isNotEmpty())) {
                    onAddContact(
                        Contact(
                            displayName = name,
                            phoneNumber = phone,
                            email = email.ifBlank { null },
                            allowRemoteSoundChange = allowRemoteSound
                        )
                    )
                    showAddDialog = false
                } else {
                    Toast.makeText(context, "Name and a phone or email are required", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(
    state: PulseLinkUiState,
    onDismissAssistantShortcuts: () -> Unit,
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val heroShape = RoundedCornerShape(32.dp)
    val heroBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF181D35), Color(0xFF0E111E))
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = heroShape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(heroBrush, heroShape)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "PulseLink logo",
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                NavigationRow(
                    onAlertsClick = onAlertsClick,
                    onSettingsClick = onSettingsClick,
                    onUpgradeClick = onUpgradeClick,
                    isProUser = state.isProUser
                )
            }
            if (!state.settings.assistantShortcutsDismissed) {
                VoiceTipsCard(
                    modifier = Modifier.fillMaxWidth(),
                    isProUser = state.isProUser,
                    onUpgradeClick = onUpgradeClick,
                    onDismiss = onDismissAssistantShortcuts
                )
            }
        }
    }
}

@Composable
private fun AddLoginCard(
    modifier: Modifier = Modifier,
    onAddLoginClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(id = R.string.home_add_login_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(id = R.string.home_add_login_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Button(
                onClick = onAddLoginClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(text = stringResource(id = R.string.home_add_login_cta))
            }
        }
    }
}

@Composable
private fun VoiceTipsCard(
    modifier: Modifier = Modifier,
    isProUser: Boolean,
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (!isProUser) {
                            Text(
                                text = stringResource(R.string.assistant_hint_upgrade_badge),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.assistant_hint_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.assistant_hint_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.assistant_hint_dismiss),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = stringResource(R.string.assistant_hint_examples_title),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(R.string.assistant_hint_examples),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isProUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.assistant_hint_ready_state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.assistant_hint_upgrade_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(onClick = onUpgradeClick) {
                    Text(
                        text = stringResource(R.string.assistant_hint_upgrade_action),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationRow(
    onAlertsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit,
    isProUser: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavButton(icon = Icons.Filled.Notifications, label = "Alerts", onClick = onAlertsClick)
        NavButton(icon = Icons.Filled.Settings, label = "Settings", onClick = onSettingsClick)
        if (!isProUser) {
            NavButton(icon = Icons.Filled.Star, label = "Pro", onClick = onUpgradeClick)
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val chipShape = RoundedCornerShape(16.dp)
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(chipShape)
                .clickable(onClick = onClick),
            shape = chipShape,
            color = Color.White.copy(alpha = 0.08f),
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color.White
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun QuickActionsRow(
    onTriggerEmergency: () -> Unit,
    onSendCheckInAll: () -> Unit,
    isEmergencyActive: Boolean,
    onCancelEmergency: () -> Unit,
    isCancelingEmergency: Boolean
) {
    val fontScale = LocalDensity.current.fontScale
    val showScrollHint = fontScale >= 1.2f
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isEmergencyActive) {
            CancelEmergencyCard(
                isCanceling = isCancelingEmergency,
                onCancelEmergency = onCancelEmergency
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionTile(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.quick_action_emergency_label),
                background = Brush.verticalGradient(listOf(Color(0xFFFC4D4D), Color(0xFFB60F1F))),
                onClick = onTriggerEmergency,
                enabled = !isEmergencyActive
            )
            QuickActionTile(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.quick_action_checkin_label),
                background = Brush.verticalGradient(listOf(Color(0xFF14C997), Color(0xFF058252))),
                onClick = onSendCheckInAll
            )
        }
        if (showScrollHint) {
            ScrollHintCard()
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    label: String,
    background: Brush,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(background)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Color.White
        )
    }
}

@Composable
private fun ScrollHintCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(id = R.string.quick_action_scroll_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CancelEmergencyCard(
    isCanceling: Boolean,
    onCancelEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFFB91C1C), Color(0xFF4C1A1A))
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.cancel_emergency_card_status),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(id = R.string.cancel_emergency_card_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
            }
            Text(
                text = stringResource(id = R.string.cancel_emergency_card_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.cancel_emergency_card_secondary),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onCancelEmergency,
                    enabled = !isCanceling,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFFB91C1C),
                        disabledContainerColor = Color.White.copy(alpha = 0.7f),
                        disabledContentColor = Color(0xFFB91C1C).copy(alpha = 0.5f)
                    )
                ) {
                    if (isCanceling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFB91C1C)
                        )
                    } else {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(text = stringResource(id = R.string.cancel_emergency_card_cta))
                }
            }
        }
    }
}

@Composable
private fun SearchAndAddRow(
    searchValue: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    onAddClick: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchValue,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Search contacts") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors
        )
        val addShape = RoundedCornerShape(18.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(addShape)
                .clickable(onClick = onAddClick),
            shape = addShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add trusted contact",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContactsList(
    state: PulseLinkUiState,
    searchQuery: String,
    onCallContact: (Contact) -> Unit,
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
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colorScheme.onBackground.copy(alpha = 0.06f))
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
                            .heightIn(max = 400.dp)
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
                                    onSendLinkRequest = { onSendLink(contact.id) },
                                    onApproveLink = { onApproveLink(contact.id) },
                                    onCall = { onCallContact(contact) },
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
    onSendLinkRequest: () -> Unit,
    onApproveLink: () -> Unit,
    onCall: () -> Unit,
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
    val presenceColor = when (contact.remotePresence) {
        RemotePresence.ONLINE -> Color(0xFF12C26B)
        RemotePresence.RECENT -> Color(0xFFf59e0b)
        RemotePresence.OFFLINE -> Color(0xFFef4444)
        RemotePresence.STALE -> MaterialTheme.colorScheme.outline
        RemotePresence.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant
    }
    val phone = contact.phoneNumber.orEmpty()
    val hasSmsFallback = phone.isNotBlank()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMessages() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 0.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
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
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = contact.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = contact.phoneNumber,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(presenceColor)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = contact.displayName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    phone.isNotBlank() -> phone
                                    contact.email?.isNotBlank() == true -> contact.email
                                    else -> stringResource(id = R.string.contact_no_reachability)
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Surface(
                            color = if (hasSmsFallback) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasSmsFallback) Icons.Filled.Sms else Icons.Outlined.WifiTethering,
                                    contentDescription = if (hasSmsFallback) stringResource(R.string.contact_reachability_sms) else stringResource(
                                        R.string.contact_reachability_online_only
                                    ),
                                    tint = if (hasSmsFallback) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (hasSmsFallback) stringResource(R.string.contact_reachability_sms) else stringResource(
                                        R.string.contact_reachability_online_only
                                    ),
                                    color = if (hasSmsFallback) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCall) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Call contact",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onOpenMessages) {
                        val msgIcon = if (hasSmsFallback) Icons.Filled.Sms else Icons.AutoMirrored.Filled.Send
                        Icon(
                            msgIcon,
                            contentDescription = "Open conversation",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Contact settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
private fun ReachabilityBadge(
    hasSmsFallback: Boolean,
    modifier: Modifier = Modifier
) {
    val label = if (hasSmsFallback) {
        stringResource(R.string.contact_reachability_sms)
    } else {
        stringResource(R.string.contact_reachability_online_only)
    }
    val icon = if (hasSmsFallback) Icons.Filled.Sms else Icons.Outlined.WifiTethering
    val containerColor = if (hasSmsFallback) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    }
    val contentColor = if (hasSmsFallback) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PresenceBadge(
    label: String,
    color: Color,
    lastSeen: Long?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
                lastSeen?.let {
                    val relative = android.text.format.DateUtils.getRelativeTimeSpanString(
                        it,
                        System.currentTimeMillis(),
                        android.text.format.DateUtils.MINUTE_IN_MILLIS,
                        android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                    Text(
                        text = "Last active $relative",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
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
private fun UpgradeCard(isPro: Boolean, onUpgradeClick: () -> Unit) {
    val proShape = RoundedCornerShape(28.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = proShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF252C57), Color(0xFF131631))
                    ),
                    proShape
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PulseLink Pro",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = if (isPro) {
                    stringResource(id = R.string.upgrade_card_active_copy)
                } else {
                    stringResource(id = R.string.upgrade_card_pitch)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            if (isPro) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                    Text(
                        text = "Lifetime access activated",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.15f),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(text = stringResource(id = R.string.pro_upgrade_button_active))
                }
            } else {
                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5663FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = stringResource(id = R.string.upgrade_card_cta))
                }
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
    email: TextFieldValue,
    onEmailChange: (TextFieldValue) -> Unit,
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
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email (optional)") },
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
