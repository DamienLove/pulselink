package com.pulselink.beacon.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.R
import kotlinx.coroutines.delay
import com.pulselink.beacon.ui.colorFromName

data class Sender(val initials: String, val color: Color = colorFromName(initials))

data class ThreadPreview(
    val id: String,
    val title: String,
    val subtitle: String,
    val isVip: Boolean,
    val unread: Boolean,
    val sender: Sender
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    threads: List<ThreadPreview>,
    onThreadClick: (ThreadPreview) -> Unit,
    onAvatarClick: (ThreadPreview) -> Unit = {},
    onTrustedClick: (ThreadPreview) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onSettings: (() -> Unit)? = null
 ) {
    val tabs = listOf("All Messages", "Read", "Unread")
    val selectedTab = rememberSaveable { mutableIntStateOf(0) }
    val items = remember { mutableStateListOf(*threads.toTypedArray()) }
    val filteredThreads by remember(selectedTab.intValue, items) {
        derivedStateOf {
            when (selectedTab.intValue) {
                1 -> items.filter { !it.unread }
                2 -> items.filter { it.unread }
                else -> items
            }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp)
                            .clickable { onBack?.invoke() },
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "PulseLink Beacon",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onSettings?.invoke() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(8.dp))
                TabRow(
                    selectedTabIndex = selectedTab.intValue,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    indicator = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab.intValue == index,
                            onClick = { selectedTab.intValue = index },
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (selectedTab.intValue == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color(0xFF3B3B3B)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    Icon(Icons.Outlined.CropSquare, contentDescription = null, tint = Color.White)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(
                items = filteredThreads,
                key = { it.id }
            ) { thread ->
                val dismissState = rememberSwipeToDismissBoxState()
                val bgColor by animateColorAsState(
                    when (dismissState.currentValue) {
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFF04444)
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF60C659)
                        else -> Color.Transparent
                    }, label = "bg"
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                ActionPill("Archive", Color.White, Color(0xFF4CAF50))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                ActionPill("Delete", Color.White, Color(0xFFF04444))
                            }
                        }
                    },
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    content = {
                        ThreadRow(
                            thread = thread,
                            onClick = { onThreadClick(thread) },
                            onAvatarClick = { onAvatarClick(thread) },
                            onTrustedClick = { onTrustedClick(thread) }
                        )
                    }
                )
                HorizontalDivider()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                        delay(250)
                        when (dismissState.currentValue) {
                            SwipeToDismissBoxValue.StartToEnd,
                            SwipeToDismissBoxValue.EndToStart -> items.remove(thread)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(
    thread: ThreadPreview,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onTrustedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        InitialsAvatar(
            sender = thread.sender,
            modifier = Modifier
                .clickable { onAvatarClick() }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = thread.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = thread.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (thread.isVip) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "VIP",
                tint = Color(0xFF4F54F5),
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onTrustedClick() }
            )
        }
    }
}

@Composable
private fun ActionPill(label: String, textColor: Color, background: Color) {
    ElevatedCard(
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(text = label, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InitialsAvatar(
    sender: Sender,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(sender.color.copy(alpha = 0.9f), sender.color)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sender.initials,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
