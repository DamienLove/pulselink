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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@Composable
fun BeaconContactsScreen(
    contacts: List<Contact>,
    theme: ThemePreferences,
    onSelect: (Contact) -> Unit
) {
    val primary = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val onPrimary = parseColorOr(MaterialTheme.colorScheme.onPrimary, theme.onBubbleOutgoing)

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
                text = "Synced from PulseLink. Tap to jump to messaging.",
                style = MaterialTheme.typography.bodyMedium,
                color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(contacts.sortedBy { it.displayName.lowercase() }) { contact ->
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (contact.linkStatus == LinkStatus.LINKED) {
                                        StatusChip(
                                            text = "Linked",
                                            primary = primary,
                                            onPrimary = onPrimary,
                                            leading = { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                                        )
                                    }
                                    if (!contact.linkCode.isNullOrBlank()) {
                                        StatusChip(
                                            text = "Code ${contact.linkCode.takeLast(4)}",
                                            primary = primary.copy(alpha = 0.12f),
                                            onPrimary = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor),
                                            leading = { Icon(Icons.Filled.Link, contentDescription = null) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (contacts.isEmpty()) {
                    item {
                        Text(
                            text = "No contacts yet. Add trusted contacts from the PulseLink home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onBubbleIncoming)
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
