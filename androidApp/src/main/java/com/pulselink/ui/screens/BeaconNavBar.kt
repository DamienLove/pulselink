package com.pulselink.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulselink.domain.model.ThemePreferences
import com.pulselink.util.parseColorOr

@Composable
fun BeaconNavBar(
    currentRoute: BeaconNavRoute,
    onNavigate: (BeaconNavRoute) -> Unit,
    theme: ThemePreferences,
    privateSafeEnabled: Boolean = false
) {
    val selectedColor = parseColorOr(MaterialTheme.colorScheme.primary, theme.primaryColor)
    val unselectedColor = parseColorOr(MaterialTheme.colorScheme.onSurfaceVariant, theme.onTopBarColor).copy(alpha = 0.7f)
    val indicatorColor = selectedColor.copy(alpha = 0.14f)
    val iconSize = (24f * theme.iconSizeFactor).coerceIn(18f, 30f).dp
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = selectedColor,
        selectedTextColor = selectedColor,
        unselectedIconColor = unselectedColor,
        unselectedTextColor = unselectedColor,
        indicatorColor = indicatorColor
    )
    NavigationBar(
        containerColor = parseColorOr(MaterialTheme.colorScheme.surface, theme.topBarColor),
        contentColor = parseColorOr(MaterialTheme.colorScheme.onSurface, theme.onTopBarColor)
    ) {
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Inbox,
            onClick = { onNavigate(BeaconNavRoute.Inbox) },
            icon = { Icon(Icons.Filled.History, contentDescription = "Recent", modifier = Modifier.size(iconSize)) },
            label = { Text("All") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Otp,
            onClick = { onNavigate(BeaconNavRoute.Otp) },
            icon = { Icon(Icons.Filled.VpnKey, contentDescription = "2-step", modifier = Modifier.size(iconSize)) },
            label = { Text("2-step") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Trusted,
            onClick = { onNavigate(BeaconNavRoute.Trusted) },
            icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = "Trusted", modifier = Modifier.size(iconSize)) },
            label = { Text("Trusted") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Contacts,
            onClick = { onNavigate(BeaconNavRoute.Contacts) },
            icon = { Icon(Icons.Filled.Contacts, contentDescription = "Contacts", modifier = Modifier.size(iconSize)) },
            label = { Text("Contacts") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Favorites,
            onClick = { onNavigate(BeaconNavRoute.Favorites) },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorites", modifier = Modifier.size(iconSize)) },
            label = { Text("Favorites") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentRoute == BeaconNavRoute.Archived,
            onClick = { onNavigate(BeaconNavRoute.Archived) },
            icon = { Icon(Icons.Filled.Archive, contentDescription = "Archived", modifier = Modifier.size(iconSize)) },
            label = { Text("Archived") },
            colors = itemColors
        )
        if (privateSafeEnabled) {
            NavigationBarItem(
                selected = currentRoute == BeaconNavRoute.Private,
                onClick = { onNavigate(BeaconNavRoute.Private) },
                icon = { Icon(Icons.Filled.Lock, contentDescription = "Private", modifier = Modifier.size(iconSize)) },
                label = { Text("Private") },
                colors = itemColors
            )
        }
    }
}

enum class BeaconNavRoute {
    Inbox,
    Otp,
    Trusted,
    Favorites,
    Archived,
    Private,
    Contacts
}
