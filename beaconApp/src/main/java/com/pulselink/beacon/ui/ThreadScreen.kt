package com.pulselink.beacon.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    theme: ThemePalette,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onScheduleMessage: (String, Long) -> Unit = { _, _ -> },
    onDeleteThread: () -> Unit,
    onEditNotificationSound: () -> Unit,
    onCustomize: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Ensure new messages are seen first (at bottom)
    LaunchedEffect(messages.firstOrNull()?.id) {
        if (messages.isNotEmpty()) {
            // Only scroll if we are near the bottom (index 0 in reverse layout) or it's initial load
            val firstVisible = listState.firstVisibleItemIndex
            if (firstVisible < 2) {
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
                    // dateMillis is UTC midnight of the selected date.
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
                title = { Text(address, maxLines = 1, color = theme.frameColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iconTint)
                    }
                },
                actions = {
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
                    containerColor = theme.threadBackgroundColor,
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                item { Spacer(modifier = Modifier.height(40.dp)) }
                item {
                    if (messages.size > 3) {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg, theme = theme)
                }
            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
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
                                    containerColor = theme.inboxBackgroundColor.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(onClick = {
                            if (draft.isNotBlank()) showDatePicker = true
                        }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Schedule", tint = iconTint)
                        }
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Write your message") }
                        )
                        IconButton(
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    onSend(text)
                                    draft = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = iconTint)
                        }
                    }
                }
            }
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
private fun MessageBubble(message: SmsMessageItem, theme: ThemePalette) {
    val isOutgoing = message.outgoing
    val background = if (isOutgoing) theme.outgoingColor else theme.incomingColor
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val frameColor = theme.frameColor
    val bubbleShape = RoundedCornerShape(theme.bubbleRadius.dp)
    val clipboardManager = LocalClipboardManager.current
    val otp = remember(message.body) { extractOtp(message.body) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = background,
            shape = bubbleShape,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, frameColor.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(bubbleShape)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black
                )
                if (message.isMms && message.mediaParts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.mediaParts.filter { it.dataUri != null && it.contentType.startsWith("image") }
                        .forEach { part ->
                            AsyncImage(
                                model = part.dataUri,
                                contentDescription = "MMS image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                }
                if (otp != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    AssistChip(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(otp))
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
                            containerColor = Color.White.copy(alpha = 0.8f),
                            labelColor = Color.Black,
                            leadingIconContentColor = Color.Black
                        )
                    )
                }
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        message.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.DarkGray
                )
            }
        }
    }
}

private fun extractOtp(body: String): String? {
    val regex = Regex("(?<!\\d)\\d{4,8}(?!\\d)")
    // Filter out things that are obviously not OTPs if needed, but 4-8 isolated digits is a good heuristic
    // Check if body contains common OTP keywords to reduce false positives
    val keywords = listOf("code", "pin", "verification", "otp", "password")
    if (keywords.none { body.contains(it, ignoreCase = true) }) {
        // Relaxing this check because sometimes it's just "123456"
        // But for "Best App" experience, maybe strict is better?
        // Let's keep it simple: matches regex and body length is short (< 160 chars)
        if (body.length > 160) return null
    }
    return regex.find(body)?.value
}
