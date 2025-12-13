package com.pulselink.beacon.ui.contact

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.model.ContactDetails
import com.pulselink.beacon.ui.BeaconHeader
import com.pulselink.beacon.ui.InitialsAvatar
import com.pulselink.beacon.ui.Sender
import com.pulselink.beacon.ui.colorFromName

@Composable
fun EditContactScreen(
    contact: ContactDetails,
    onBack: () -> Unit,
    onSave: (ContactDetails) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(contact.name) }
    var phone by rememberSaveable { mutableStateOf(contact.phone) }
    var email by rememberSaveable { mutableStateOf(contact.email) }
    var address by rememberSaveable { mutableStateOf(contact.address) }
    var trusted by rememberSaveable { mutableStateOf(contact.trusted) }

    val avatarBrush = Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), MaterialTheme.colorScheme.primary)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            BeaconHeader(
                onBack = onBack,
                onSettings = null, // No settings for edit contact screen
                title = "Edit Contact",
                centerTitle = true,
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            InitialsAvatar(
                                sender = Sender(
                                    initials = contact.name.firstOrNull()?.uppercase()?.toString() ?: "?",
                                    color = colorFromName(contact.name)
                                ),
                                size = 120,
                                modifier = Modifier
                                    .clip(CircleShape)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.error, // Use theme color
                                shape = CircleShape,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(34.dp)
                                    .clickable { /* TODO photo picker */ },
                                tonalElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Photo",
                                        tint = MaterialTheme.colorScheme.onError // Use theme color
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Edit Photo", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LabeledField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Contact Name",
                    leadingIcon = Icons.Outlined.Person
                )
                LabeledField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = "1(234)567-7890",
                    leadingIcon = Icons.Outlined.Phone
                )
                LabeledField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email@Contact.com",
                    leadingIcon = Icons.Outlined.Email
                )
                LabeledField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "Address",
                    leadingIcon = Icons.Outlined.LocationOn
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "More info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, // Use theme color
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text("More Info", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Add notes or secondary number",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = "Trusted",
                        tint = MaterialTheme.colorScheme.secondary, // Use theme color (for trusted)
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trusted Contact", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Allow priority alerts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = { trusted = !trusted }
                    ) {
                        Text(if (trusted) "On" else "Off", color = MaterialTheme.colorScheme.primary) // Use theme color
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onSave(
                            contact.copy(
                                name = name,
                                phone = phone,
                                email = email,
                                address = address,
                                trusted = trusted
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Use theme color
                ) {
                    Text("Update Contact", modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Removed the hardcoded bottom bar
        }
    }
}

@Composable
private fun LabeledField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant // Use theme color
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = fieldColors // Apply themed colors
    )
}
