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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    val tabs = listOf("All", "Read", "Unread")
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BeaconHeader(
                onBack = onBack,
                onSettings = onSettings,
                selectedTab = selectedTab.intValue,
                onTabSelected = { selectedTab.intValue = it },
                tabs = tabs
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(
                items = filteredThreads,
                key = { it.id }
            ) { thread ->
                val dismissState = rememberSwipeToDismissBoxState()
                val bgColor by animateColorAsState(
                    when (dismissState.currentValue) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF60C659) // Success green
                        else -> Color.Transparent
                    }, label = "bg"
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(bgColor)
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                Text("Archive", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
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
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BeaconHeader(
    onBack: (() -> Unit)?,
    onSettings: (() -> Unit)?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<String>
) {
    val heroShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    val heroBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF181D35), Color(0xFF0E111E))
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = heroShape,
        color = Color.Transparent,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(heroBrush)
                .statusBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onBack != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onBack() }
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                
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
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (onSettings != null) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onSettings() }
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Tab(
                        selected = selected,
                        onClick = { onTabSelected(index) },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = thread.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (thread.unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (thread.isVip) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = "VIP",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onTrustedClick() }
                )
            }
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
