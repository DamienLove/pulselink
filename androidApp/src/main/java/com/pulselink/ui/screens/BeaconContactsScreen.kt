package com.pulselink.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.ui.components.ThemeIcon
import com.pulselink.ui.components.ThemeIconKey
import com.pulselink.util.parseColorOr

@Composable
fun BeaconContactsScreen(
    contacts: List<Contact>,
    theme: ThemePreferences,
    hasContactsPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onSelect: (Contact) -> Unit,
    searchQuery: String = "",
    onSearchChange: (String) -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    val primary = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onPrimary = parseColorOr(MaterialTheme.colorScheme.onPrimary, theme.onBubbleOutgoing)
    val onBackgroundColor = parseColorOr(MaterialTheme.colorScheme.onBackground, theme.onTopBarColor)
    val onBackgroundMuted = parseColorOr(
        MaterialTheme.colorScheme.onSurfaceVariant,
        theme.onBubbleIncoming
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = parseColorOr(MaterialTheme.colorScheme.background, theme.backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = parseColorOr(MaterialTheme.colorScheme.onBackground, theme.onTopBarColor)
                )
                Text(
                    text = "Phone & Google contacts. Tap to view or edit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackgroundMuted
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchChange(it)
                        if (it.isBlank()) {
                            onClearSearch()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Search contacts") },
                    leadingIcon = {
                        ThemeIcon(
                            iconKey = ThemeIconKey.SEARCH,
                            theme = theme,
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = {
                                onSearchChange("")
                                onClearSearch()
                            }) {
                                ThemeIcon(
                                    iconKey = ThemeIconKey.CLOSE,
                                    theme = theme,
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchChange(searchQuery) }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = onBackgroundMuted.copy(alpha = 0.4f),
                        focusedContainerColor = parseColorOr(
                            MaterialTheme.colorScheme.surface,
                            theme.backgroundColor
                        ),
                        unfocusedContainerColor = parseColorOr(
                            MaterialTheme.colorScheme.surface,
                            theme.backgroundColor
                        ),
                        focusedTextColor = onBackgroundColor,
                        unfocusedTextColor = onBackgroundColor,
                        cursorColor = primary
                    ),
                    shape = RoundedCornerShape(18.dp)
                )

                if (!hasContactsPermission) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Contacts permission needed",
                                style = MaterialTheme.typography.titleMedium,
                                color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
                            )
                            Text(
                                text = "Allow Contacts to show your phone and Google contacts here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
                            )
                            androidx.compose.material3.Button(onClick = onRequestContactsPermission) {
                                Text("Allow contacts access")
                            }
                        }
                    }
                }

                val filteredContacts = remember(contacts, searchQuery) {
                    if (searchQuery.isBlank()) contacts
                    else contacts.filter {
                        it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery)
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredContacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(contact) },
                        colors = CardDefaults.cardColors(
                            containerColor = parseColorOr(
                                MaterialTheme.colorScheme.surface,
                                theme.backgroundColor
                            )
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.displayName.ifBlank { contact.phoneNumber },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
                                )
                                if (contact.phoneNumber.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            Icons.Filled.Phone,
                                            contentDescription = null,
                                            tint = parseColorOr(MaterialTheme.colorScheme.secondary, theme.secondaryColor)
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (filteredContacts.isEmpty()) {
                    item {
                        Text(
                            text = when {
                                !hasContactsPermission -> "Contacts are hidden until you allow access."
                                searchQuery.isNotBlank() -> "No contacts match \"$searchQuery\"."
                                else -> "No contacts found. Add contacts on your phone or sync Google contacts."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = onBackgroundMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    primary: androidx.compose.ui.graphics.Color,
    onPrimary: androidx.compose.ui.graphics.Color,
    leading: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = onPrimary
        )
    }
}
