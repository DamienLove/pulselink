package com.pulselink.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulselink.data.sms.SmsThreadItem
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.ui.state.PulseLinkUiState
import com.pulselink.ui.state.SmsInboxViewModel
import com.pulselink.ui.state.SmsLinesViewModel
import com.pulselink.util.formatTimestamp
import com.pulselink.util.normalizeSmsAddress
import com.pulselink.util.parseColorOr
import com.pulselink.util.splitSmsDisplayAddress

@Composable
fun UnifiedHomeScreen(
    state: PulseLinkUiState,
    onDismissAssistantShortcuts: () -> Unit,
    onTriggerEmergency: () -> Unit,
    onSendCheckIn: () -> Unit,
    onSettingsClick: () -> Unit,
    onFaqClick: () -> Unit,
    onReportBugClick: () -> Unit = {},
    onOpenContacts: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenThemes: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onContactSelected: (Long) -> Unit,
    onContactSettings: (Long) -> Unit,
    onSendLink: (Long) -> Unit,
    onApproveLink: (Long) -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onReorderContacts: (List<Long>) -> Unit,
    onRequestCancelEmergency: () -> Unit,
    onViewEmergencyMap: () -> Unit,
    isCancelingEmergency: Boolean,
    onAlertsClick: () -> Unit,
    showAddLoginPrompt: Boolean,
    onAddLoginClick: () -> Unit,
    showWebAccessHint: Boolean,
    onWebAccessHintDismiss: () -> Unit,
    onWebAccessHintAction: () -> Unit,
    onUpgradeClick: () -> Unit,
    brandName: String,
    isPremium: Boolean,
    isPro: Boolean,
    // Unified-specific
    onComposeMessage: () -> Unit,
    onOpenThread: (SmsThreadItem) -> Unit,
    onViewAllMessages: () -> Unit,
    onOpenContactForThread: (SmsThreadItem) -> Unit
) {
    val smsInboxViewModel: SmsInboxViewModel = hiltViewModel()
    val linesViewModel: SmsLinesViewModel = hiltViewModel()
    
    val threads by smsInboxViewModel.threads.collectAsStateWithLifecycle()
    val inboxBusy by smsInboxViewModel.isDatabaseBusy.collectAsStateWithLifecycle()
    val activeLineId by linesViewModel.activeLineId.collectAsStateWithLifecycle()
    val deviceLineId by linesViewModel.deviceLineId.collectAsStateWithLifecycle()
    val lines by linesViewModel.lines.collectAsStateWithLifecycle()

    val orderedLines = remember(lines) {
        lines.sortedWith(
            compareBy<com.pulselink.domain.model.SmsLine> { it.phoneNumber.ifBlank { "~" } }
                .thenBy { it.createdAt }
        )
    }
    
    val lineColors = remember(state.settings.themePreferences) {
        listOf(
            parseColorOr(Color(0xFF22d3ee), state.settings.themePreferences.primaryColor),
            parseColorOr(Color(0xFF0ea5e9), state.settings.themePreferences.secondaryColor),
            parseColorOr(Color(0xFF22d3ee), state.settings.themePreferences.bubbleOutgoing),
            parseColorOr(Color(0xFF1a2236), state.settings.themePreferences.bubbleIncoming)
        )
    }
    
    val lineIndexMap = remember(orderedLines) { orderedLines.mapIndexed { index, line -> line.id to index }.toMap() }

    LaunchedEffect(Unit) {
        smsInboxViewModel.refresh()
    }

    val contactsByNumber = remember(state.contacts) {
        val map = mutableMapOf<String, Contact>()
        state.contacts.forEach { contact ->
            val numbers = listOf(contact.phoneNumber) + contact.additionalPhones
            numbers.filter { it.isNotBlank() }.forEach { number ->
                map.putIfAbsent(normalizeSmsAddress(number), contact)
            }
        }
        map.toMap()
    }

    val context = LocalContext.current
    var selectedFilter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    val totalUnread = remember(threads) { threads.count { it.unread } }
    val previewThreads = remember(threads, isPremium, activeLineId, deviceLineId, selectedFilter, contactsByNumber) {
        val lineFiltered = if (isPremium && orderedLines.isNotEmpty()) {
            val effectiveActive = activeLineId ?: deviceLineId
            if (effectiveActive != null) {
                threads.filter { it.lineId.isNullOrBlank() || it.lineId == effectiveActive }
            } else threads
        } else {
            threads
        }
        val filteredByFilter = when (selectedFilter) {
            InboxFilter.ALL -> lineFiltered
            InboxFilter.OTP -> lineFiltered.filter { it.isOtp }
            InboxFilter.TRUSTED -> lineFiltered.filter { it.isTrusted }
            InboxFilter.FAVORITES -> lineFiltered.filter { it.isFavorite }
            InboxFilter.PRIVATE -> lineFiltered.filter { it.isPrivate }
            InboxFilter.CONTACTS -> lineFiltered.filter { contactsByNumber.containsKey(normalizeSmsAddress(it.address)) }
            InboxFilter.READ -> lineFiltered.filter { !it.unread }
            InboxFilter.UNREAD -> lineFiltered.filter { it.unread }
            InboxFilter.ARCHIVED -> emptyList() // preview does not show archived
        }
        filteredByFilter.take(8)
    }

    val shouldShowPreview = inboxBusy || threads.isNotEmpty()

    HomeScreen(
        state = state,
        onDismissAssistantShortcuts = onDismissAssistantShortcuts,
        onTriggerEmergency = onTriggerEmergency,
        onSendCheckIn = onSendCheckIn,
        onSettingsClick = onSettingsClick,
        onFaqClick = onFaqClick,
        onReportBugClick = onReportBugClick,
        onBeaconClick = onViewAllMessages, // Redirect Beacon header click to full inbox
        onOpenContacts = onOpenContacts,
        onOpenNotifications = onOpenNotifications,
        onOpenThemes = onOpenThemes,
        isUnifiedMode = true, // Force true to show content slot
        showBeaconIcon = false, // Hide header icon if we are unified
        showBeaconHint = false,
        onAddContact = onAddContact,
        onContactSelected = onContactSelected,
        onContactSettings = onContactSettings,
        onSendLink = onSendLink,
        onApproveLink = onApproveLink,
        onCallContact = onCallContact,
        onReorderContacts = onReorderContacts,
        onRequestCancelEmergency = onRequestCancelEmergency,
        onViewEmergencyMap = onViewEmergencyMap,
        isCancelingEmergency = isCancelingEmergency,
        onAlertsClick = onAlertsClick,
        showAddLoginPrompt = showAddLoginPrompt,
        onAddLoginClick = onAddLoginClick,
        showWebAccessHint = showWebAccessHint,
        onWebAccessHintDismiss = onWebAccessHintDismiss,
        onWebAccessHintAction = onWebAccessHintAction,
        onUpgradeClick = onUpgradeClick,
        brandName = brandName,
        isPremium = isPremium,
        isPro = isPro,
        showComposeButton = true,
        onComposeMessage = onComposeMessage,
        smsPreviewContent = if (shouldShowPreview) {
            {
                SmsInboxPreviewSection(
                    threads = previewThreads,
                    totalUnread = totalUnread,
                    filter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onOpenThread = onOpenThread,
                    onViewAll = onViewAllMessages,
                    onOpenContact = onOpenContactForThread,
                    onComposeMessage = onComposeMessage,
                    theme = state.settings.themePreferences,
                    dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                    contactsByNumber = contactsByNumber,
                    isDatabaseBusy = inboxBusy,
                    lineIndexMap = lineIndexMap,
                    lineColors = lineColors,
                    lineCount = orderedLines.size
                )
            }
        } else null
    )
}

@Composable
fun SmsInboxPreviewSection(
    threads: List<SmsThreadItem>,
    totalUnread: Int,
    filter: InboxFilter,
    onFilterChange: (InboxFilter) -> Unit,
    onOpenThread: (SmsThreadItem) -> Unit,
    onViewAll: () -> Unit,
    onOpenContact: (SmsThreadItem) -> Unit,
    onComposeMessage: () -> Unit,
    theme: ThemePreferences,
    dateFormatter: (Long) -> String,
    contactsByNumber: Map<String, Contact>,
    isDatabaseBusy: Boolean,
    lineIndexMap: Map<String, Int>,
    lineColors: List<Color>,
    lineCount: Int
) {
    val shape = RoundedCornerShape(22.dp)
    val backgroundColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.backgroundColor)
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = parseColorOr(MaterialTheme.colorScheme.onBackground, theme.onBackground)
                )
                if (totalUnread > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Text(
                            text = totalUnread.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        TabsRow(
            filter = filter,
            unreadCount = totalUnread,
            onFilterChange = onFilterChange,
            theme = theme,
            isUnifiedMode = true
        )

        if (threads.isEmpty()) {
            if (isDatabaseBusy) {
                // Skeletons
                repeat(3) {
                    ThreadRowSkeleton(theme = theme)
                }
            } else {
                // Empty State
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No recent messages",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onComposeMessage) {
                            Text("Start conversation")
                        }
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                threads.forEach { thread ->
                    val contact = contactsByNumber[normalizeSmsAddress(thread.address)]
                    val lineIndex = thread.lineId?.let { lineIndexMap[it] }
                    // Reusing the exposed internal ThreadRow
                    ThreadRow(
                        thread = thread,
                        onOpen = onOpenThread,
                        onAvatarClick = onOpenContact,
                        onArchive = { /* No archive action in preview swipe */ },
                        onUnarchive = { },
                        onDelete = { /* No delete action in preview swipe */ },
                        onPin = { },
                        onUnpin = { },
                        dateFormatter = dateFormatter,
                        isArchived = false,
                        isPrivate = thread.isPrivate,
                        onTogglePrivate = { _, _ -> /* No toggle private in preview */ },
                        theme = theme,
                        contact = contact,
                        lineIndex = lineIndex,
                        lineColors = lineColors,
                        lineCount = lineCount,
                        actionsEnabled = false // Disable swipe actions in preview to avoid conflict/complexity
                    )
                }
                
                Button(
                    onClick = onComposeMessage,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Start new message")
                }
            }
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Open full inbox")
            }
        }
    }
}
