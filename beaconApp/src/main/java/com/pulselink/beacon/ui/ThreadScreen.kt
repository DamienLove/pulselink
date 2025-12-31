package com.pulselink.beacon.ui

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.pulselink.beacon.data.SmsMessageItem
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.ui.ads.NativeAdCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    messages: List<SmsMessageItem>,
    theme: ThemePalette,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onDeleteThread: () -> Unit,
    onEditNotificationSound: () -> Unit,
    onCustomize: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val iconTint = theme.accentColor

        val listState = remember(address) { LazyListState() }
            val scope = rememberCoroutineScope()
                var initialScrollDone by remember(address) { mutableStateOf(false) }
                    val isNearBottom by remember {
                                derivedStateOf {
                                                val layout = listState.layoutInfo
                                                            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
                                                                        lastVisible >= (layout.totalItemsCount - 2).coerceAtLeast(0)
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp, top = 8.dp)
                                state = listState,
                                            reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(message = msg, theme = theme)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    if (messages.size > 3) {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

                    LaunchedEffect(messages.size) {
                                    if (messages.isNotEmpty() && (!initialScrollDone || isNearBottom)) {
                                                        listState.animateScrollToItem(messages.size - 1)
                                                                        initialScrollDone = true
                                                                                    }
                                                                                            }

            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write your message") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
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

@Composable
private fun MessageBubble(message: SmsMessageItem, theme: ThemePalette) {
    val isOutgoing = message.outgoing
    val background = if (isOutgoing) theme.outgoingColor else theme.incomingColor
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val frameColor = theme.frameColor
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isOutgoing) 16.dp else 4.dp,
        bottomEnd = if (isOutgoing) 4.dp else 16.dp
    )

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
                .fillMaxWidth(0.85f)
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
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                }
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        message.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Light,
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
