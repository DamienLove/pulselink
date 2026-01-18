package com.pulselink.beacon.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pulselink.beacon.data.MmsPart
import com.pulselink.beacon.data.ThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailsScreen(
    threadId: Long,
    address: String,
    theme: ThemePalette,
    sharedMedia: List<MmsPart>,
    onLoadMedia: () -> Unit,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onBlock: () -> Unit,
    onNotificationSettings: () -> Unit,
    onDeleteThread: () -> Unit
) {
    LaunchedEffect(threadId) {
        onLoadMedia()
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details", color = theme.frameColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.accentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.inboxBackgroundColor
                )
            )
        },
        containerColor = theme.inboxBackgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            DetailsAvatar(name = address, theme = theme, size = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = theme.frameColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(icon = Icons.Default.Call, label = "Call", theme = theme, onClick = onCall)
                ActionButton(icon = Icons.Default.Notifications, label = "Sound", theme = theme, onClick = onNotificationSettings)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = theme.frameColor.copy(alpha = 0.1f))

            // Shared Media
            if (sharedMedia.isNotEmpty()) {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text(
                        text = "Shared Media",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.frameColor,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sharedMedia) { part ->
                            part.dataUri?.let { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(theme.frameColor.copy(alpha = 0.05f))
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = theme.frameColor.copy(alpha = 0.1f))
            }

            // Options
            Column(Modifier.padding(vertical = 16.dp)) {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Custom sound & vibration",
                    theme = theme,
                    onClick = onNotificationSettings
                )
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Block & Report Spam",
                    subtitle = "Stop receiving messages",
                    theme = theme,
                    onClick = onBlock,
                    textColor = MaterialTheme.colorScheme.error
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Delete Conversation",
                    subtitle = "Permanently delete this thread",
                    theme = theme,
                    onClick = onDeleteThread,
                    textColor = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: ThemePalette,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = theme.accentColor.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp).clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = theme.accentColor)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = theme.frameColor)
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    theme: ThemePalette,
    onClick: () -> Unit,
    textColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = textColor ?: theme.frameColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor ?: theme.frameColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = theme.frameColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DetailsAvatar(name: String, theme: ThemePalette, size: androidx.compose.ui.unit.Dp) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"
    val colorIndex = kotlin.math.abs(name.hashCode()) % 5
    val avatarColor = when(colorIndex) {
        0 -> theme.accentColor
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFE91E63)
        else -> Color(0xFF9C27B0)
    }

    Surface(
        shape = CircleShape,
        color = avatarColor.copy(alpha = 0.15f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initial,
                style = MaterialTheme.typography.displayMedium,
                color = avatarColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
