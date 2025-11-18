package com.pulselink.ui.screens

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.AlertEvent
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.EscalationTier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertHistoryScreen(
    alerts: List<AlertEvent>,
    contacts: List<Contact>,
    onBack: () -> Unit,
    onContactClick: (Long) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val sortedEvents = remember(alerts) { alerts.sortedByDescending { it.timestamp } }
    val contactsById = remember(contacts) { contacts.associateBy { it.id } }
    val grouped = remember(sortedEvents, zoneId) {
        AlertHistorySection.values().mapNotNull { section ->
            val items = sortedEvents.filter { sectionForTimestamp(it.timestamp, zoneId) == section }
            if (items.isEmpty()) null else section to items
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.alert_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_cancel))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        if (sortedEvents.isEmpty()) {
            AlertHistoryEmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                grouped.forEach { (section, events) ->
                    item(key = section.name) {
                        AlertHistorySectionHeader(title = stringResource(section.titleRes))
                    }
                    items(events, key = { it.id }) { event ->
                        AlertHistoryCard(
                            event = event,
                            contact = event.contactId?.let { contactsById[it] },
                            onContactClick = onContactClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertHistoryCard(
    event: AlertEvent,
    contact: Contact?,
    onContactClick: (Long) -> Unit
) {
    val contextContactName = event.contactName ?: contact?.displayName
    val displayName = when {
        event.isIncoming -> contextContactName ?: event.triggeredBy
        else -> stringResource(R.string.alert_history_you)
    }
    val directionLabel = if (event.isIncoming) {
        stringResource(R.string.alert_history_incoming)
    } else {
        stringResource(R.string.alert_history_outgoing)
    }
    val (icon, tint) = when (event.tier) {
        EscalationTier.EMERGENCY -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
        EscalationTier.CHECK_IN -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.primary
    }
    val secondaryIcon = if (event.isIncoming) Icons.Filled.ArrowDownward else Icons.AutoMirrored.Filled.Send
    val relativeTime = remember(event.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            event.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    }
    val avatarInitial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (event.contactId != null) {
                    Modifier.clickable { onContactClick(event.contactId!!) }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AlertHistoryAvatar(initial = avatarInitial, icon = icon, tint = tint)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = event.triggeredBy,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Icon(imageVector = secondaryIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = relativeTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (event.isIncoming) "$directionLabel $displayName" else directionLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!event.isIncoming) {
                    Text(
                        text = stringResource(R.string.alert_history_contacts_notified, event.contactCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (event.sharedLocation) {
                    Text(
                        text = stringResource(R.string.alert_history_location_shared),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!event.soundKey.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.alert_history_sound_used, event.soundKey),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertHistorySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
}

@Composable
private fun AlertHistoryAvatar(initial: String, icon: ImageVector, tint: Color) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AlertHistoryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.alert_history_empty_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.alert_history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private enum class AlertHistorySection(@StringRes val titleRes: Int) {
    TODAY(R.string.alert_history_section_today),
    YESTERDAY(R.string.alert_history_section_yesterday),
    THIS_WEEK(R.string.alert_history_section_this_week),
    OLDER(R.string.alert_history_section_older)
}

private fun sectionForTimestamp(timestamp: Long, zoneId: ZoneId): AlertHistorySection {
    val eventDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return when {
        eventDate.isEqual(today) -> AlertHistorySection.TODAY
        eventDate.isEqual(today.minusDays(1)) -> AlertHistorySection.YESTERDAY
        eventDate.isAfter(today.minusDays(7)) -> AlertHistorySection.THIS_WEEK
        else -> AlertHistorySection.OLDER
    }
}
