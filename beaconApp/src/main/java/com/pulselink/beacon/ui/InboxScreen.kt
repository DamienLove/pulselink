package com.pulselink.beacon.ui

import android.provider.Telephony
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard
import com.pulselink.beacon.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    threads: List<SmsThreadItem>,
    theme: ThemePalette,
    searchState: SearchResultState,
    isDefaultSms: Boolean,
    isCheckingDefaultSms: Boolean,
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    onRequestDefault: () -> Unit,
    onRefreshDefaultStatus: () -> Unit,
    onOpenThread: (Long, String) -> Unit,
    onCompose: () -> Unit,
    onDeleteThread: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onToggleArchive: (Long) -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCustomize: () -> Unit,
    onOpenNotifications: () -> Unit,
    notificationsEnabled: Boolean,
    notificationsSilent: Boolean,
    onOpenNotificationSettings: () -> Unit,
    filter: InboxFilter,
    onFilterChange: (InboxFilter) -> Unit
) {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchText by rememberSaveable { mutableStateOf("") }
    var navigatedFromSearch by remember { mutableStateOf(false) }
    val iconTint = theme.accentColor

    val filtered = remember(filter, threads) {
        threads.filter { thread ->
            when (filter) {
                // ALL and ARCHIVED modes are pre-filtered by the Repository query.
                // We trust the repository to return the correct set (Inbox/Pinned vs Archived).
                InboxFilter.ALL -> true
                InboxFilter.READ -> !thread.unread
                InboxFilter.UNREAD -> thread.unread
                InboxFilter.ARCHIVED -> true
            }
        }
    }
    val unreadCount = remember(threads) { threads.count { it.unread } }
    val mutedTint = theme.frameColor.copy(alpha = 0.7f)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val beaconIconAlpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)

    LaunchedEffect(searchState) {
        if (searchState is SearchResultState.Contact && !navigatedFromSearch) {
            navigatedFromSearch = true
            onOpenThread(searchState.threadId, searchState.address)
            onClearSearch()
        } else if (searchState !is SearchResultState.Contact) {
            navigatedFromSearch = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Beacon Inbox",
                        fontWeight = FontWeight.SemiBold,
                        color = theme.frameColor
                    )
                },
                navigationIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.beacon_logo),
                        contentDescription = "Beacon",
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .alpha(beaconIconAlpha)
                    )
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = iconTint)
                    }
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Notifications", tint = iconTint)
                    }
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Customize", tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = theme.inboxBackgroundColor,
                    titleContentColor = theme.frameColor,
                    actionIconContentColor = iconTint
                ),
                scrollBehavior = scrollBehavior
            )
        },
                floatingActionButton = {
            FloatingActionButton(
                onClick = onCompose,
                containerColor = theme.accentColor
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "New message"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        snackbarHost = { SnackbarHost(host) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.inboxBackgroundColor)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    if (it.isBlank()) onClearSearch()
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTint) },
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        IconButton(onClick = {
                            searchText = ""
                            onClearSearch()
                        }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = mutedTint) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search contacts or messages") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.accentColor,
                    unfocusedBorderColor = theme.frameColor.copy(alpha = 0.4f),
                    focusedContainerColor = theme.inboxBackgroundColor,
                    unfocusedContainerColor = theme.inboxBackgroundColor
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch(searchText)
                    }
                )
            )

            TabsRow(
                filter = filter,
                unreadCount = unreadCount,
                onFilterChange = onFilterChange,
                theme = theme
            )

            when (searchState) {
                is SearchResultState.Messages -> SearchResults(
                    hits = searchState.hits,
                    theme = theme,
                    onOpenThread = onOpenThread
                )
                SearchResultState.Empty -> Text(
                    "No matches. Try a contact name or phrase.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                SearchResultState.Searching -> Text(
                    "Searching…",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                else -> Unit
            }

            if (!notificationsEnabled || notificationsSilent) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (!notificationsEnabled) "Message notifications are off" else "Message alerts are silent",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (!notificationsEnabled) {
                                    "Turn on notifications so Beacon can alert you."
                                } else {
                                    "Enable sound or vibration for incoming texts."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = onOpenNotificationSettings) {
                            Text("Open")
                        }
                    }
                }
            }

            if (missingPermissions.isNotEmpty()) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text("Permissions needed", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Grant SMS permission so Beacon can read and show messages.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text("Grant")
                        }
                    }
                }
            }

            if (!isDefaultSms || isCheckingDefaultSms) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = iconTint)
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isCheckingDefaultSms) "Checking default SMS status..." else "Set as default SMS",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Required for receiving texts and showing notifications.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (isCheckingDefaultSms) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            OutlinedButton(onClick = onRequestDefault) {
                                Text("Set")
                            }
                        }
                        OutlinedButton(onClick = onRefreshDefaultStatus, enabled = !isCheckingDefaultSms) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = iconTint)
                            Text("Refresh", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text("No messages yet")
                        Text(
                            when (filter) {
                                InboxFilter.UNREAD -> "No unread messages."
                                InboxFilter.READ -> "No read messages."
                                else -> "New texts will appear here once Beacon is the default SMS app."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(filtered, key = { _, item -> item.threadId }) { index, item ->
                        if (index == 3) {
                            NativeAdCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onToggleArchive(item.threadId)
                                        val msg = if (item.isArchived) "Unarchived" else "Archived"
                                        scope.launch { host.showSnackbar(msg) }
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onTogglePin(item.threadId)
                                        val msg = if (item.isPinned) "Unpinned" else "Pinned"
                                        scope.launch { host.showSnackbar(msg) }
                                    }
                                    else -> {}
                                }
                                false
                            }
                        )
                        SwipeableThreadRow(
                            thread = item,
                            state = dismissState,
                            theme = theme,
                            onClick = { onOpenThread(item.threadId, item.address) },
                            onDelete = { onDeleteThread(item.threadId) },
                            onTogglePin = { onTogglePin(item.threadId) },
                            onToggleArchive = { onToggleArchive(item.threadId) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    hits: List<SmsMessageItem>,
    theme: ThemePalette,
    onOpenThread: (Long, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        hits.take(5).forEach { msg ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onOpenThread(msg.threadId, msg.address) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = msg.address.ifBlank { "Unknown sender" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.accentColor
                    )
                    Text(
                        text = msg.body,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            msg.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableThreadRow(
    thread: SmsThreadItem,
    state: SwipeToDismissBoxState,
    theme: ThemePalette,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit
) {
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            // Use muted theme color for archive, accent for pin
            val (color, alignment, icon) = when (state.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Triple(theme.frameColor.copy(alpha = 0.5f), Alignment.CenterEnd, Icons.Default.Inbox) // Archive
                SwipeToDismissBoxValue.StartToEnd -> Triple(theme.accentColor, Alignment.CenterStart, if (thread.isPinned) Icons.Default.PushPin else Icons.Default.PushPin) // Pin/Unpin
                else -> Triple(Color.Transparent, Alignment.CenterEnd, Icons.Default.Inbox)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                if (state.targetValue != SwipeToDismissBoxValue.Settled) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
        },
        content = {
            ThreadRow(
                thread = thread,
                theme = theme,
                onClick = onClick,
                onDelete = onDelete,
                onTogglePin = onTogglePin,
                onToggleArchive = onToggleArchive
            )
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThreadRow(
    thread: SmsThreadItem,
    theme: ThemePalette,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleArchive: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(theme.bubbleRadius.dp),
        tonalElevation = if (thread.unread) 2.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.35f)),
        color = theme.inboxBackgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (thread.isPinned) "Unpin" else "Pin") },
                    onClick = {
                        onTogglePin()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (thread.isPinned) "Unpin conversation" else "Pin conversation"
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (thread.isArchived) "Unarchive" else "Archive") },
                    onClick = {
                        onToggleArchive()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = if (thread.isArchived) "Unarchive conversation" else "Archive conversation"
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete conversation"
                        )
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (thread.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = theme.accentColor,
                        modifier = Modifier.padding(end = 4.dp).size(16.dp)
                    )
                }
                Text(
                    text = thread.address.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (thread.unread) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        thread.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = thread.snippet,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TabsRow(
    filter: InboxFilter,
    unreadCount: Int,
    onFilterChange: (InboxFilter) -> Unit,
    theme: ThemePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TabText(label = "All", selected = filter == InboxFilter.ALL, theme = theme) {
            onFilterChange(InboxFilter.ALL)
        }
        TabText(label = "Read", selected = filter == InboxFilter.READ, theme = theme) {
            onFilterChange(InboxFilter.READ)
        }
        TabText(
            label = "Unread${if (unreadCount > 0) " ($unreadCount)" else ""}",
            selected = filter == InboxFilter.UNREAD,
            theme = theme
        ) {
            onFilterChange(InboxFilter.UNREAD)
        }
        TabText(
            label = "Archived",
            selected = filter == InboxFilter.ARCHIVED,
            theme = theme
        ) {
            onFilterChange(InboxFilter.ARCHIVED)
        }
    }
}

@Composable
private fun TabText(label: String, selected: Boolean, theme: ThemePalette, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.selectable(
            selected = selected,
            role = Role.Tab,
            onClick = onClick
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) theme.frameColor else theme.frameColor.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .fillMaxWidth(0.8f)
                .background(if (selected) theme.accentColor else Color.Transparent)
        )
    }
}

enum class InboxFilter { ALL, READ, UNREAD, ARCHIVED }
