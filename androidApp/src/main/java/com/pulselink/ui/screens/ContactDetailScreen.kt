package com.pulselink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact?,
    onBack: () -> Unit,
    onEditEmergencyAlert: () -> Unit,
    onEditCheckInAlert: () -> Unit,
    onToggleLocation: (Boolean) -> Unit,
    onToggleCamera: (Boolean) -> Unit,
    onToggleAutoCall: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (contact == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "Contact not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = null,
                        modifier = Modifier.height(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = contact.displayName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9AA0B4))
                }

                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C26))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(text = "Contact Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        ToggleRow(
                            label = "Location Share",
                            isChecked = contact.includeLocation,
                            onCheckedChange = onToggleLocation
                        )
                        ToggleRow(
                            label = "Emergency Alert",
                            supportingText = contact.emergencySoundKey ?: "Default",
                            isChecked = true,
                            onCheckedChange = { onEditEmergencyAlert() },
                            readOnly = true
                        )
                        ToggleRow(
                            label = "Non-Emergency Alert",
                            supportingText = contact.checkInSoundKey ?: "Default",
                            isChecked = true,
                            onCheckedChange = { onEditCheckInAlert() },
                            readOnly = true
                        )
                        ToggleRow(
                            label = "Camera Enable",
                            isChecked = contact.cameraEnabled,
                            onCheckedChange = onToggleCamera
                        )
                        ToggleRow(
                            label = "Auto Call",
                            isChecked = contact.autoCall,
                            onCheckedChange = onToggleAutoCall
                        )
                    }
                }

                Button(
                    onClick = onDelete,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Contact")
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    supportingText: String? = null,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    readOnly: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold)
            supportingText?.let {
                Text(text = it, color = Color(0xFF8C92A4), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (readOnly) {
            IconButton(onClick = { onCheckedChange(true) }) {
                Icon(Icons.Filled.Settings, contentDescription = label, tint = Color.White)
            }
        } else {
            Switch(checked = isChecked, onCheckedChange = onCheckedChange)
        }
    }
}
