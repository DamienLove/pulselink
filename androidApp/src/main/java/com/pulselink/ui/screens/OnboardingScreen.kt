package com.pulselink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R

private data class PermissionCard(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onGrantPermissions: () -> Unit
) {
    val cards = listOf(
        PermissionCard(
            icon = Icons.Filled.Call,
            title = "SMS & Call",
            description = "To alert contacts and dial 911"
        ),
        PermissionCard(
            icon = Icons.Filled.Lock,
            title = "Override Silent / DND",
            description = "To make sure your alerts get heard"
        ),
        PermissionCard(
            icon = Icons.Filled.Mic,
            title = "Microphone Access",
            description = "To detect your safewords"
        ),
        PermissionCard(
            icon = Icons.Filled.PowerSettingsNew,
            title = "Unrestricted Battery",
            description = "To protect you in the background"
        ),
        PermissionCard(
            icon = Icons.Filled.LocationOn,
            title = "Location Access",
            description = "To add precise location to alerts"
        ),
        PermissionCard(
            icon = Icons.Filled.Person,
            title = "Contacts",
            description = "To link trusted contacts in the app"
        )
    )

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF111119))
    )

    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "PulseLink logo"
            )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PulseLink",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your hands-free safety net",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(cards.size) { index ->
                val card = cards[index]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E2C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = card.title,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = card.description,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Grant Permissions & Continue")
            }
        }
    }
}
