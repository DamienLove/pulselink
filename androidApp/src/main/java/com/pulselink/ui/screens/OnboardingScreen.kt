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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R

data class OnboardingPermissionState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val manualHelp: String? = null
)

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    permissions: List<OnboardingPermissionState>,
    onGrantPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF111119))
    )
    val hasMissing = permissions.any { !it.granted }
    val smsHelp = permissions.firstOrNull { it.manualHelp != null && !it.granted }?.manualHelp

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
            items(permissions.size) { index ->
                val card = permissions[index]
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (card.granted) Color(0xFF1E1E2C) else Color(0xFF2B2340)
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
                            tint = if (card.granted) Color(0xFF67DBA0) else Color.White
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (card.granted) "Granted" else "Action required",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (card.granted) Color(0xFF67DBA0) else Color(0xFFFFB74D)
                            )
                        }
                    }
                }
            }
        }
        if (!smsHelp.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = smsHelp,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFB74D)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
            ) {
                Text(text = "Open App Settings")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantPermissions,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasMissing) Color(0xFF2563EB) else Color(0xFF15803D)
                )
            ) {
                Text(text = if (hasMissing) "Grant Permissions & Continue" else "Continue")
            }
        }
    }
}
