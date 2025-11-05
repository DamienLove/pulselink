package com.pulselink.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.pulselink.R

data class OnboardingPermissionState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val granted: Boolean,
    val manualHelp: String? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingIntroScreen(
    modifier: Modifier = Modifier,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10131F), Color(0xFF0B0D16))
    )
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Transparent)
        ) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "PulseLink logo",
                modifier = Modifier.size(72.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Welcome to PulseLink",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "PulseLink listens for safewords, alerts trusted contacts, and pushes through Do Not Disturb when it matters most.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5F5)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IntroBullet(text = "Hands-free safewords trigger emergency or check-in alerts.")
                IntroBullet(text = "Escalations send SMS, play tones, and can auto-dial help.")
                IntroBullet(text = "PulseLink can override silent/DND so partners hear urgent alerts.")
            }
            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "We include this name when contacting your trusted partners so they know it's you.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFCD34D)
            )
            androidx.compose.material3.Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth(),
                enabled = ownerName.isNotBlank()
            ) {
                Text(text = "Continue to permissions")
            }
        }
    }
}

@Composable
private fun IntroBullet(text: String) {
    val secondaryTextColor = Color(0xFFDEE2FF)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF67DBA0)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryTextColor
        )
    }
}

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    permissions: List<OnboardingPermissionState>,
    isReadyToFinish: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF10131F), Color(0xFF0B0D16))
    )
    val manualHelp = permissions.firstOrNull { it.manualHelp != null && !it.granted }?.manualHelp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .clip(RoundedCornerShape(28.dp)),
            color = Color.White.copy(alpha = 0.04f)
        ) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "PulseLink logo",
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Enable critical permissions",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant these permissions so PulseLink can protect you even when your phone is silenced.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5F5),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(permissions) { card ->
                    PermissionCard(state = card)
                }
            }
            if (!manualHelp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = manualHelp,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCD34D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open app settings")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onGrantPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isReadyToFinish) "Finish setup" else "Grant permissions")
            }
        }
    }
}

@Composable
private fun PermissionCard(state: OnboardingPermissionState) {
    val statusColor = if (state.granted) MaterialTheme.colorScheme.primary else Color(0xFFF59E0B)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.18f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = state.icon,
                        contentDescription = state.title,
                        tint = statusColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5F5)
                    )
                }
                Text(
                    text = if (state.granted) "Granted" else "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
            if (!state.granted && state.actionLabel != null && state.onAction != null) {
                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.manualHelp ?: "Tap to grant",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCD34D),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = state.onAction) {
                        Text(text = state.actionLabel)
                    }
                }
            }
        }
    }
}
