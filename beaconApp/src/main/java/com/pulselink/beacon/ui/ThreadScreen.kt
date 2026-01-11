package com.pulselink.beacon.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard
import com.pulselink.beacon.util.LinkPreviewData
import com.pulselink.beacon.util.LinkPreviewHelper
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    uiItems: List<ThreadUiItem>,
    theme: ThemePalette,
    pendingMessage: SmsViewModel.PendingMessage? = null,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onCancelPending: () -> Unit = {},
    onSendNow: () -> Unit = {},
    onScheduleMessage: (String, Long) -> Unit = { _, _ -> },
    onDeleteThread: () -> Unit,
    onEditNotificationSound: () -> Unit,
    onCustomize: () -> Unit,
    onCall: () -> Unit = {}
) {
    var draft by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll logic for new messages
    // Since reverseLayout=true, item 0 is at bottom.
    // If the list grows (new message), it should stay at bottom if already there.
    // However, if we just opened, we want to be at bottom.
    LaunchedEffect(uiItems.firstOrNull()) {
        if (uiItems.isNotEmpty()) {
             if (listState.firstVisibleItemIndex < 3) {
                listState.animateScrollToItem(0)
             }
        }
    }

    val iconTint = theme.accentColor
    val quickReplies = remember {
        listOf("Ok", "Yes", "No", "Thanks", "On my way!", "Can't talk now", "Call you later?")
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDateMillis = it
                        showDatePicker = false
                        showTimePicker = true
                    }
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = {
                selectedDateMillis?.let { dateMillis ->
                    val utcDate = Instant.ofEpochMilli(dateMillis)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate()

                    val localTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val targetZoned = utcDate.atTime(localTime).atZone(ZoneId.systemDefault())

                    val finalTime = targetZoned.toInstant().toEpochMilli()

                    val now = System.currentTimeMillis()
                    if (finalTime <= now) {
                        Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show()
                    } else {
                        onScheduleMessage(draft, finalTime)
                        draft = ""
                        showTimePicker = false
                        Toast.makeText(context, "Message scheduled", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(address, maxLines = 1, style = MaterialTheme.typography.titleMedium, color = theme.frameColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iconTint)
                    }
                },
                actions = {
                    IconButton(onClick = onCall) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = iconTint)
                    }
                    IconButton(onClick = onCustomize) {
                        Icon(Icons.Default.Palette, contentDescription = "Customize theme", tint = iconTint)
                    }
                    IconButton(onClick = onEditNotificationSound) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Notification sound", tint = iconTint)
                    }
                    IconButton(onClick = onDeleteThread) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete thread", tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.threadBackgroundColor.copy(alpha = 0.95f),
                    titleContentColor = theme.frameColor,
                    navigationIconContentColor = iconTint,
                    actionIconContentColor = iconTint
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(theme.threadBackgroundColor)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                reverseLayout = true
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                items(
                    items = uiItems,
                    key = { item ->
                        when(item) {
                            is ThreadUiItem.Message -> item.message.id
                            is ThreadUiItem.DateHeader -> "header_${item.date}"
                        }
                    },
                    contentType = { item ->
                        when(item) {
                            is ThreadUiItem.Message -> 1
                            is ThreadUiItem.DateHeader -> 2
                        }
                    }
                ) { item ->
                    when (item) {
                        is ThreadUiItem.Message -> MessageBubble(message = item.message, theme = theme)
                        is ThreadUiItem.DateHeader -> DateHeader(date = item.date, theme = theme)
                    }
                }

                item {
                    if (uiItems.size > 5) {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }

            // Input Area
            Surface(
                tonalElevation = 0.dp,
                color = theme.inboxBackgroundColor.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Pending Message UI (Delayed Send)
                    AnimatedVisibility(visible = pendingMessage != null) {
                        Surface(
                            color = theme.accentColor.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Sending...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = theme.accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onCancelPending) {
                                        Text("Undo", color = theme.frameColor)
                                    }
                                    TextButton(onClick = onSendNow) {
                                        Text("Send Now", color = theme.accentColor)
                                    }
                                }
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = theme.accentColor,
                                    trackColor = theme.accentColor.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickReplies) { reply ->
                            SuggestionChip(
                                onClick = { draft = reply },
                                label = { Text(reply) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = theme.accentColor.copy(alpha = 0.1f),
                                    labelColor = theme.frameColor
                                ),
                                border = null
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp)
                    ) {
                        IconButton(onClick = {
                            if (draft.isNotBlank()) showDatePicker = true
                        }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Schedule", tint = iconTint)
                        }

                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            placeholder = { Text("Text message", fontSize = 14.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = theme.threadBackgroundColor.copy(alpha = 0.5f),
                                unfocusedContainerColor = theme.threadBackgroundColor.copy(alpha = 0.5f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        IconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    onSend(text)
                                    draft = ""
                                }
                            },
                            enabled = draft.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (draft.isNotBlank()) iconTint else iconTint.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String, theme: ThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = theme.frameColor.copy(alpha = 0.1f)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = theme.frameColor.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
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
                content()
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissRequest) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SmsMessageItem, reactions: List<Reaction> = emptyList(), theme: ThemePalette) {
    val isOutgoing = message.outgoing
    val background = if (isOutgoing) theme.outgoingColor else theme.incomingColor
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val frameColor = theme.frameColor

    // Bubble Styling
    val cornerRadius = theme.bubbleRadius.dp
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = 2.dp,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    } else {
        RoundedCornerShape(
            topStart = 2.dp,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    }

    val clipboardManager = LocalClipboardManager.current
    val otp = remember(message.body) { extractOtp(message.body) }
    val extractedUrl = remember(message.body) { LinkPreviewHelper.extractUrl(message.body) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (reactions.isNotEmpty()) 8.dp else 1.dp),
        contentAlignment = alignment
    ) {
        Surface(
            color = background,
            shape = bubbleShape,
            tonalElevation = 0.dp, // Flat look
            modifier = Modifier
                .width(if (message.body.length > 40) 300.dp else Box.Unspecified) // Limit width for long text
                .padding(horizontal = 0.dp)
                .clip(bubbleShape)
                .clickable { /* Toggle timestamp expansion? */ }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (message.isMms && message.mediaParts.isNotEmpty()) {
                    message.mediaParts.filter { it.dataUri != null && it.contentType.startsWith("image") }
                        .forEach { part ->
                            AsyncImage(
                                model = part.dataUri,
                                contentDescription = "MMS image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                }

                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    color = if (isOutgoing) theme.onBubbleOutgoing else theme.onBubbleIncoming
                )

                if (extractedUrl != null) {
                    val preview by produceState<LinkPreviewData?>(initialValue = null, key1 = extractedUrl) {
                        value = LinkPreviewHelper.fetchPreview(extractedUrl)
                    }

                    if (preview != null && (preview!!.title != null || preview!!.imageUrl != null)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkPreviewCard(preview = preview!!) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(extractedUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                if (otp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssistChip(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(otp))
                            Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Copy $otp") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.9f),
                            labelColor = Color.Black,
                            leadingIconContentColor = Color.Black
                        )
                    )
                }

                // Timestamp
                Row(
                    modifier = Modifier.padding(top = 4.dp).align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Medium,
                        color = (if (isOutgoing) theme.onBubbleOutgoing else theme.onBubbleIncoming).copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Reactions Overlay
        if (reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(if (isOutgoing) Alignment.BottomEnd else Alignment.BottomStart)
                    .offset(y = 10.dp, x = if (isOutgoing) (-4).dp else 4.dp)
                    .zIndex(1f),
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                reactions.forEach { reaction ->
                    Surface(
                        shape = CircleShape,
                        color = theme.threadBackgroundColor,
                        border = BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.1f)),
                        modifier = Modifier.size(24.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = reaction.emoji, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
    return date.format(formatter)
}

@Composable
private fun LinkPreviewCard(preview: LinkPreviewData, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.1f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column {
            if (preview.imageUrl != null) {
                AsyncImage(
                    model = preview.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            Column(Modifier.padding(8.dp)) {
                if (preview.title != null) {
                    Text(
                        text = preview.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Text(
                    text = preview.siteName ?: Uri.parse(preview.url).host ?: preview.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun extractOtp(body: String): String? {
    val regex = Regex("(?<!\\d)\\d{4,8}(?!\\d)")
    val keywords = listOf("code", "pin", "verification", "otp", "password")
    if (keywords.none { body.contains(it, ignoreCase = true) }) {
        if (body.length > 160) return null
    }
    return regex.find(body)?.value
}
