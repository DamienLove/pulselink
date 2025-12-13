package com.pulselink.beacon.ui.message

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.R
import com.pulselink.beacon.ui.BeaconHeader
import com.pulselink.beacon.ui.InitialsAvatar
import com.pulselink.beacon.ui.Sender
import com.pulselink.beacon.ui.colorFromName

data class Message(
    val id: String,
    val senderName: String,
    val fromMe: Boolean,
    val type: MessageType,
    val timestamp: String
)

sealed class MessageType {
    data class Text(val value: String) : MessageType()
    object Image : MessageType()
}

@Composable
fun ChatScreen(
    title: String,
    isGroup: Boolean,
    members: List<String>,
    messages: List<Message>,
    onBack: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BeaconHeader(
                onBack = onBack,
                onSettings = onSettings,
                title = if (isGroup) "Group Chat" else title,
                subtitle = if (isGroup) "${members.size} Members" else null
            )
        },
        bottomBar = {
            MessageInputBar()
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = false,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(messages) { message ->
                    MessageBubble(
                        message = message
                    )
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = if (message.fromMe) TextAlign.End else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (message.fromMe) 60.dp else 54.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageInputBar() {
    var text by rememberSaveable { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant, // Use theme color
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField( // Changed to OutlinedTextField for consistent style
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write your message here") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(28.dp), // Rounded shape for input field
                colors = fieldColors
            )
            Surface(
                color = MaterialTheme.colorScheme.primary, // Use theme color
                shape = CircleShape,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .clickable { /* TODO: Send message */ text = "" }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send, // Updated to AutoMirrored
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberRow(members: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        members.take(4).forEach { name ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant), // Use theme color
                contentAlignment = Alignment.Center
            ) {
                Text(text = name.first().uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant) // Use theme color
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val alignment = if (message.fromMe) Alignment.End else Alignment.Start
    val background = if (message.fromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant // Use theme color
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start
    ) {
        if (!message.fromMe) {
            InitialsAvatar(
                sender = com.pulselink.beacon.ui.Sender(
                    initials = message.senderName.take(1).uppercase(),
                    color = colorFromName(message.senderName)
                ),
                size = 36,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Column(horizontalAlignment = alignment) {
            when (val type = message.type) {
                is MessageType.Text -> {
                    Surface(
                        color = background,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomEnd = if (message.fromMe) 4.dp else 18.dp,
                            bottomStart = if (message.fromMe) 18.dp else 4.dp
                        )
                    ) {
                        Text(
                            text = type.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (message.fromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                MessageType.Image -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mock_group_image),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(140.dp)
                        )
                    }
                }
            }
        }
    }
}
