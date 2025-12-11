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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.R
import com.pulselink.beacon.ui.InitialsAvatar
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
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = "PulseLink Beacon",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable { onBack() }
                            .padding(2.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isGroup) "Group Chat" else title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (isGroup) {
                            Text(
                                text = "${members.size} Members",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onSettings() },
                        tint = MaterialTheme.colorScheme.onSurface
                            .copy(alpha = 0.9f)
                    )
                }
                if (isGroup) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        MemberRow(members)
                        Surface(
                            color = Color(0xFFFF6F61),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "+4",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
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
private fun MemberRow(members: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        members.take(4).forEach { name ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = name.first().uppercase(), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val alignment = if (message.fromMe) Alignment.End else Alignment.Start
    val background = if (message.fromMe) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
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

@Composable
private fun MessageInputBar() {
    var text by rememberSaveable { mutableStateOf("") }
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        color = Color(0xFFE3F4FF)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write your message here") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFE3F4FF),
                    focusedContainerColor = Color(0xFFE3F4FF),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Surface(
                color = Color(0xFF45B6FF),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .clickable { text = "" }
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
