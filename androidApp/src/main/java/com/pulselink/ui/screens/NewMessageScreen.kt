package com.pulselink.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.ui.model.MessageRecipient
import com.pulselink.util.parseColorOr

/**
 * Clean, intuitive new message screen.
 * Design principles:
 * - Search/recipient input is the focal point
 * - Selected contacts are clearly visible
 * - One-tap to start messaging
 * - Attachment option is always accessible
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewMessageScreen(
    contacts: List<MessageRecipient>,
    onBack: () -> Unit,
    onCreateConversation: (List<String>) -> Unit,
    onManualInput: (String) -> Unit,
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onSendAttachment: (List<String>, Uri) -> Unit = { _, _ -> },
    theme: ThemePreferences
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedContacts by remember { mutableStateOf(emptyList<MessageRecipient>()) }
    val context = LocalContext.current

    val attachmentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val numbers = selectedContacts.map { it.phoneNumber }.filter { it.isNotBlank() }
            if (numbers.isNotEmpty()) {
                onSendAttachment(numbers, uri)
            } else {
                Toast.makeText(context, "Select a recipient first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredContacts = remember(searchQuery, contacts, selectedContacts) {
        val candidates = if (searchQuery.isBlank()) contacts else contacts.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery)
        }
        candidates.filter { c -> selectedContacts.none { it.phoneNumber == c.phoneNumber } }
    }

    val primaryColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onPrimaryColor = parseColorOr(MaterialTheme.colorScheme.onPrimary, theme.onBubbleOutgoing)
    val textColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)
    val placeholderColor = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBackground).copy(alpha = 0.6f)

    Scaffold(
        containerColor = parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "New message",
                        style = MaterialTheme.typography.titleMedium,
                        color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor)
                )
            )
        },
        bottomBar = {
            // Action bar at bottom - always visible when recipients selected
            if (selectedContacts.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment button
                        OutlinedButton(
                            onClick = { attachmentPicker.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Filled.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach")
                        }

                        // Start conversation button
                        Button(
                            onClick = { onCreateConversation(selectedContacts.map { it.phoneNumber }) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = onPrimaryColor
                            )
                        ) {
                            Text("Message", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // To: field with selected contacts
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Selected contacts as chips
                    if (selectedContacts.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedContacts.forEach { contact ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedContacts = selectedContacts - contact },
                                    label = {
                                        Text(
                                            contact.displayName,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = primaryColor.copy(alpha = 0.12f),
                                        selectedLabelColor = primaryColor
                                    )
                                )
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (selectedContacts.isEmpty()) "To: Name or phone number"
                                else "Add more recipients",
                                color = placeholderColor
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = primaryColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = placeholderColor.copy(alpha = 0.3f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }

            // Permission prompt
            if (!hasContactsPermission) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable contacts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                            Text(
                                text = "See your contacts here for faster messaging",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                        Button(
                            onClick = onRequestContactsPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }

            // Quick send to number option
            if (searchQuery.isNotBlank() && searchQuery.any { it.isDigit() }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onManualInput(searchQuery) },
                    shape = RoundedCornerShape(12.dp),
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Send to $searchQuery",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Text(
                                "Tap to start messaging",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Contact list
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = if (selectedContacts.isNotEmpty()) 80.dp else 24.dp
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredContacts) { contact ->
                    ContactRow(contact, theme) {
                        selectedContacts = selectedContacts + contact
                        searchQuery = ""
                    }
                }

                if (filteredContacts.isEmpty() && searchQuery.isBlank() && hasContactsPermission) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Start typing to find contacts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean contact row with avatar circle.
 */
@Composable
private fun ContactRow(contact: MessageRecipient, theme: ThemePreferences, onClick: () -> Unit) {
    val primaryColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val textColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onBackground)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = primaryColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "#",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                if (contact.phoneNumber.isNotBlank()) {
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }

            if (contact.isTrusted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Trusted",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
