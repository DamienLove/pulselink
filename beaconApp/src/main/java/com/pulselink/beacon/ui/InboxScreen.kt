package com.pulselink.beacon.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.MarkChatUnread
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.SmsThreadItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThreadCategory
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
    onFilterChange: (InboxFilter) -> Unit,
    searchText: String,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    selectionMode: Boolean = false,
    selectedThreadIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onArchiveSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onMarkSelectedRead: () -> Unit = {},
    onMarkSelectedUnread: () -> Unit = {},
    onPinSelected: () -> Unit = {},
    onMarkAsUnread: (Long) -> Unit = {},
    userMessage: String? = null,
    onClearUserMessage: () -> Unit = {},
    delayedSendTimeout: Int = 5,
    onSetDelayedSendTimeout: (Int) -> Unit = {}
) {
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var navigatedFromSearch by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val iconTint = theme.accentColor

    LaunchedEffect(userMessage) {
        userMessage?.let {
            host.showSnackbar(it)
            onClearUserMessage()
        }
    }

    if (showSettingsDialog) {
        Dialog(onDismissRequest = { showSettingsDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Beacon Settings", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Delayed Send Timeout: ${delayedSendTimeout}s")
                    Slider(
                        value = delayedSendTimeout.toFloat(),
                        onValueChange = { onSetDelayedSendTimeout(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = theme.accentColor,
                            activeTrackColor = theme.accentColor
                        )
                    )
                    Text(
                        if (delayedSendTimeout == 0) "Disabled" else "Delays sending by $delayedSendTimeout seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.frameColor.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Done")
                    }
                }
            }
        }
    }

    // Use passed threads which are already filtered by ViewModel
    val filtered = threads

    // Calculate unread count (this might be inaccurate if threads is filtered by search, but okay for now)
    // Ideally ViewModel should pass unread count too.
    val unreadCount = remember(threads) { threads.count { it.unread && !it.isArchived } }
    val mutedTint = theme.frameColor.copy(alpha = 0.7f)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val beaconIconAlpha = (1f - scrollBehavior.state.collapsedFraction).coerceIn(0f, 1f)

    LaunchedEffect(searchState) {
        if (searchState is SearchResultState.Contact && !navigatedFromSearch) {
            navigatedFromSearch = true
            onOpenThread(searchState.threadId, searchState.address)
            // Keep search text to allow user to come back and see what they typed?
            // Usually clearing is better for "Jump to".
            onClearSearch()
            searchText = ""
        } else if (searchState !is SearchResultState.Contact) {
            navigatedFromSearch = false
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedThreadIds.size} selected",
                            color = theme.frameColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Clear selection", tint = iconTint)
                        }
                    },
                    actions = {
                        IconButton(onClick = onArchiveSelected) {
                            Icon(Icons.Default.Inbox, contentDescription = "Archive", tint = iconTint)
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = iconTint)
                        }
                        var showMore by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMore = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "More", tint = iconTint)
                            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                                DropdownMenuItem(
                                    text = { Text("Mark read") },
                                    onClick = {
                                        onMarkSelectedRead()
                                        showMore = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mark unread") },
                                    onClick = {
                                        onMarkSelectedUnread()
                                        showMore = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pin/Unpin") },
                                    onClick = {
                                        onPinSelected()
                                        showMore = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.inboxBackgroundColor,
                        titleContentColor = theme.frameColor,
                        actionIconContentColor = iconTint
                    )
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Beacon Inbox",
                                fontWeight = FontWeight.SemiBold,
                                color = theme.frameColor
                            )
                            if (isRefreshing) {
                                Spacer(modifier = Modifier.size(12.dp))
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                    color = theme.frameColor
                                )
                            }
                        }
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
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = iconTint)
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = theme.inboxBackgroundColor,
                        titleContentColor = theme.frameColor,
                        actionIconContentColor = iconTint
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
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
            // Search Bar
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        onSearch(it)
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = mutedTint) },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = {
                                onClearSearch()
                            }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = mutedTint) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search messages & contacts") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.frameColor.copy(alpha = 0.2f),
                        focusedContainerColor = theme.frameColor.copy(alpha = 0.05f),
                        unfocusedContainerColor = theme.frameColor.copy(alpha = 0.05f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* Handled by debounce */ })
                )
            }

            AnimatedVisibility(visible = searchText.isBlank()) {
                TabsRow(
                    filter = filter,
                    unreadCount = unreadCount,
                    onFilterChange = onFilterChange,
                    theme = theme
                )
            }

            // Search Results or List
            Box(modifier = Modifier.weight(1f)) {
                if (searchText.isNotBlank()) {
                     when (searchState) {
                        is SearchResultState.Messages -> SearchResults(
                            hits = searchState.hits,
                            theme = theme,
                query = searchText,
                            onOpenThread = onOpenThread
                        )
                        SearchResultState.Empty -> {
                             Column(
                                modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 Icon(Icons.Default.Search, contentDescription = null, tint = mutedTint, modifier = Modifier.size(48.dp))
                                 Spacer(modifier = Modifier.height(16.dp))
                                 Text("No results found", color = mutedTint)
                             }
                        }
                        SearchResultState.Searching -> {
                             Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                 CircularProgressIndicator(color = theme.accentColor)
                             }
                        }
                        else -> Unit
                    }
                } else if (isLoading && filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = theme.accentColor)
                    }
                } else if (filtered.isEmpty()) {
                    EmptyState(filter, theme, iconTint)
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
                                            onMarkAsUnread(item.threadId) // This is now toggle unread/read
                                            val msg = if (item.unread) "Marked as read" else "Marked as unread"
                                            scope.launch { host.showSnackbar(msg) }
                                        }
                                        else -> {}
                                    }
                                    false
                                }
                            )
                            if (selectionMode) {
                                ThreadRow(
                                    thread = item,
                                    theme = theme,
                                    isSelected = selectedThreadIds.contains(item.threadId),
                                    selectionMode = true,
                                    onClick = { onToggleSelection(item.threadId) },
                                    onDelete = { onDeleteThread(item.threadId) },
                                    onTogglePin = { onTogglePin(item.threadId) },
                                    onToggleArchive = { onToggleArchive(item.threadId) },
                                    onMarkAsUnread = { onMarkAsUnread(item.threadId) }
                                )
                            } else {
                                SwipeableThreadRow(
                                    thread = item,
                                    state = dismissState,
                                    theme = theme,
                                    onClick = { onOpenThread(item.threadId, item.address) },
                                    onDelete = { onDeleteThread(item.threadId) },
                                    onTogglePin = { onTogglePin(item.threadId) },
                                    onToggleArchive = { onToggleArchive(item.threadId) },
                                    onMarkAsUnread = { onMarkAsUnread(item.threadId) },
                                    onLongClick = { onToggleSelection(item.threadId) },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }

            // Bottom Permissions/Status Cards (only if needed)
            PermissionsBanners(
                missingPermissions = missingPermissions,
                onRequestPermissions = onRequestPermissions,
                isDefaultSms = isDefaultSms,
                isCheckingDefaultSms = isCheckingDefaultSms,
                onRequestDefault = onRequestDefault,
                onRefreshDefaultStatus = onRefreshDefaultStatus,
                notificationsEnabled = notificationsEnabled,
                notificationsSilent = notificationsSilent,
                onOpenNotificationSettings = onOpenNotificationSettings,
                iconTint = iconTint
            )
        }
    }
}

@Composable
private fun EmptyState(filter: InboxFilter, theme: ThemePalette, iconTint: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(0.1f)
                        .background(iconTint, CircleShape)
                )
                // Icon container
                Surface(
                    shape = CircleShape,
                    color = theme.frameColor.copy(alpha = 0.08f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when(filter) {
                                InboxFilter.ARCHIVED -> Icons.Default.Inbox
                                InboxFilter.UNREAD -> Icons.Default.CheckCircle
                                else -> Icons.Default.Sms
                            },
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = when (filter) {
                    InboxFilter.UNREAD -> "All caught up"
                    InboxFilter.ARCHIVED -> "No archives"
                    else -> "Inbox Empty"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = theme.frameColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (filter) {
                    InboxFilter.UNREAD -> "No unread messages. Nice work!"
                    InboxFilter.PERSONAL -> "Personal conversations will appear here."
                    InboxFilter.TRANSACTIONS -> "Bank alerts and codes appear here."
                    InboxFilter.PROMOTIONS -> "Marketing offers appear here."
                    InboxFilter.ARCHIVED -> "Archived threads are hidden here."
                    else -> "Your messages will appear here once you start chatting."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = theme.frameColor.copy(alpha = 0.6f),
                modifier = Modifier.alpha(0.8f)
            )
        }
    }
}

@Composable
private fun PermissionsBanners(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit,
    isDefaultSms: Boolean,
    isCheckingDefaultSms: Boolean,
    onRequestDefault: () -> Unit,
    onRefreshDefaultStatus: () -> Unit,
    notificationsEnabled: Boolean,
    notificationsSilent: Boolean,
    onOpenNotificationSettings: () -> Unit,
    iconTint: Color
) {
    Column {
        if (!notificationsEnabled || notificationsSilent) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = iconTint)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (!notificationsEnabled) "Notifications off" else "Alerts silent",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Enable notifications for alerts.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedButton(onClick = onOpenNotificationSettings) {
                        Text("Fix")
                    }
                }
            }
        }

        if (missingPermissions.isNotEmpty()) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp)
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
                            "Grant SMS permission to function.",
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
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = iconTint)
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isCheckingDefaultSms) "Checking..." else "Set Default SMS",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Required to send & receive.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isCheckingDefaultSms) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        OutlinedButton(onClick = onRequestDefault) {
                            Text("Set")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    hits: List<SmsMessageItem>,
    theme: ThemePalette,
    query: String,
    onOpenThread: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(hits) { msg ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenThread(msg.threadId, msg.address) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row {
                        Text(
                            text = msg.address.ifBlank { "Unknown" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.accentColor,
                            modifier = Modifier.weight(1f)
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
                    Spacer(modifier = Modifier.height(4.dp))

                    val annotatedString = remember(msg.body, query) {
                        val builder = androidx.compose.ui.text.AnnotatedString.Builder(msg.body)
                        val startIndex = msg.body.indexOf(query, ignoreCase = true)
                        if (startIndex >= 0) {
                            builder.addStyle(
                                style = androidx.compose.ui.text.SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    background = theme.accentColor.copy(alpha = 0.2f),
                                    color = theme.frameColor
                                ),
                                start = startIndex,
                                end = startIndex + query.length
                            )
                        }
                        builder.toAnnotatedString()
                    }

                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        color = theme.frameColor.copy(alpha = 0.8f)
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
    onToggleArchive: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val (color, alignment, icon) = when (state.targetValue) {
                SwipeToDismissBoxValue.EndToStart -> Triple(theme.frameColor.copy(alpha = 0.2f), Alignment.CenterEnd, Icons.Default.Inbox)
                SwipeToDismissBoxValue.StartToEnd -> Triple(theme.accentColor.copy(alpha = 0.8f), Alignment.CenterStart, if (thread.unread) Icons.Default.CheckCircle else Icons.Default.MarkChatUnread)
                else -> Triple(Color.Transparent, Alignment.CenterEnd, Icons.Default.Inbox)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(theme.bubbleRadius.dp))
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
                isSelected = false,
                selectionMode = false,
                onClick = onClick,
                onDelete = onDelete,
                onTogglePin = onTogglePin,
                onToggleArchive = onToggleArchive,
                onMarkAsUnread = onMarkAsUnread,
                onLongClick = onLongClick
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
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onMarkAsUnread: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(theme.bubbleRadius.dp),
        tonalElevation = if (isSelected) 4.dp else if (thread.unread) 2.dp else 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 0.dp,
            color = if (isSelected) theme.accentColor else Color.Transparent
        ),
        color = if (isSelected) theme.accentColor.copy(alpha = 0.1f) else theme.inboxBackgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar / Selection State
            Box(
                modifier = Modifier.padding(end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (isSelected) {
                    Surface(shape = CircleShape, color = theme.accentColor, modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = theme.inboxBackgroundColor)
                        }
                    }
                } else {
                    LetterAvatar(name = thread.address, theme = theme, size = 48.dp)
                }

                if (thread.isPinned && !isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = theme.inboxBackgroundColor,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(18.dp).offset(x = 4.dp, y = 4.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = theme.accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!selectionMode) {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(text = { Text(if (thread.isPinned) "Unpin" else "Pin") }, onClick = { onTogglePin(); showMenu = false })
                        DropdownMenuItem(text = { Text(if (thread.isArchived) "Unarchive" else "Archive") }, onClick = { onToggleArchive(); showMenu = false })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { onDelete(); showMenu = false })
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = thread.address.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.SemiBold,
                        color = theme.frameColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(thread.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (thread.unread) theme.accentColor else theme.frameColor.copy(alpha = 0.6f),
                        fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (thread.snippet.isBlank()) "Media" else thread.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    fontWeight = if (thread.unread) FontWeight.Medium else FontWeight.Normal,
                    color = if (thread.unread) theme.frameColor else theme.frameColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LetterAvatar(name: String, theme: ThemePalette, size: androidx.compose.ui.unit.Dp) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    // Deterministic color based on name hash
    val colorIndex = kotlin.math.abs(name.hashCode()) % 5
    val avatarColor = when(colorIndex) {
        0 -> theme.accentColor
        1 -> Color(0xFF4CAF50) // Green
        2 -> Color(0xFFFF9800) // Orange
        3 -> Color(0xFFE91E63) // Pink
        else -> Color(0xFF9C27B0) // Purple
    }

    Surface(
        shape = CircleShape,
        color = avatarColor.copy(alpha = 0.2f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = avatarColor,
                fontWeight = FontWeight.Bold
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
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact Tabs
        TabChip("All", filter == InboxFilter.ALL, theme) { onFilterChange(InboxFilter.ALL) }
        TabChip("Personal", filter == InboxFilter.PERSONAL, theme) { onFilterChange(InboxFilter.PERSONAL) }
        TabChip("Transactions", filter == InboxFilter.TRANSACTIONS, theme) { onFilterChange(InboxFilter.TRANSACTIONS) }
        TabChip("Promotions", filter == InboxFilter.PROMOTIONS, theme) { onFilterChange(InboxFilter.PROMOTIONS) }
        TabChip("Unread${if(unreadCount > 0) " ($unreadCount)" else ""}", filter == InboxFilter.UNREAD, theme) { onFilterChange(InboxFilter.UNREAD) }
        TabChip("Archived", filter == InboxFilter.ARCHIVED, theme) { onFilterChange(InboxFilter.ARCHIVED) }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, theme: ThemePalette, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) theme.accentColor.copy(alpha = 0.15f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.5f)) else BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.2f)),
        modifier = Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) theme.accentColor else theme.frameColor.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

enum class InboxFilter { ALL, READ, UNREAD, ARCHIVED, PERSONAL, TRANSACTIONS, PROMOTIONS }
