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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF111119))
    )
    val primaryTextColor = Color(0xFFF8F9FF)
    val secondaryTextColor = Color(0xFFDEE2FF)

    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "PulseLink logo"
            )
            Text(
                text = "Welcome to PulseLink",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = primaryTextColor
            )
            Text(
                text = "Your phone listens for safewords, alerts trusted contacts, and pushes through Do Not Disturb when it matters most.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IntroBullet(text = "Hands-free safewords trigger emergency or check-in alerts.")
                IntroBullet(text = "Escalations send SMS, play tones, and can auto-dial help.")
                IntroBullet(text = "PulseLink can override silent/DND so partners hear urgent alerts.")
            }
            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                label = { Text("Your name", color = secondaryTextColor) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF94A3B8),
                unfocusedBorderColor = Color(0xFF4B5563),
                cursorColor = primaryTextColor,
                focusedTextColor = primaryTextColor,
                unfocusedTextColor = primaryTextColor,
                focusedLabelColor = secondaryTextColor,
                unfocusedLabelColor = secondaryTextColor
            )
            )
            Text(
                text = "We include this name when contacting your trusted partners so they know it's you.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFB74D)
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = ownerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ownerName.isNotBlank()) Color(0xFF15803D) else Color(0xFF2563EB)
                )
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
        colors = listOf(Color(0xFF1E1E2C), Color(0xFF111119))
    )
    val primaryTextColor = Color(0xFFF8F9FF)
    val secondaryTextColor = Color(0xFFDEE2FF)
    val manualHelp = permissions.firstOrNull { it.manualHelp != null && !it.granted }?.manualHelp

    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryTextColor
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "PulseLink logo"
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Enable critical permissions",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = primaryTextColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant these permissions so PulseLink can protect you even when your phone is silenced.",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(24.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(permissions) { card ->
                    PermissionCard(
                        state = card,
                        primaryTextColor = primaryTextColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }
            if (!manualHelp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = manualHelp,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB74D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937))
                ) {
                    Text("Open App Settings")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onGrantPermissions,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isReadyToFinish) Color(0xFF15803D) else Color(0xFF2563EB)
                )
            ) {
                Text(text = if (isReadyToFinish) "Finish setup" else "Grant permissions")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    state: OnboardingPermissionState,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (state.granted) Color(0xFF1E1E2C) else Color(0xFF2B2340)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = state.icon,
                    contentDescription = state.title,
                    tint = if (state.granted) Color(0xFF67DBA0) else Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = primaryTextColor
                    )
                    Text(
                        text = state.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryTextColor
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.granted) "Granted" else "Action required",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.granted) Color(0xFF67DBA0) else Color(0xFFFFB74D)
                )
                if (!state.granted && state.actionLabel != null && state.onAction != null) {
                    TextButton(onClick = state.onAction) {
                        Text(text = state.actionLabel)
                    }
                }
            }
        }
    }
}
