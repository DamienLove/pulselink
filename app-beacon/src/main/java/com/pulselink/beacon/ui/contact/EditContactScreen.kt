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
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.R
import com.pulselink.beacon.model.ContactDetails
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(top = 10.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onBack() },
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Edit Contact",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.size(28.dp)) // placeholder for symmetry
                }
                Spacer(modifier = Modifier.height(14.dp))
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
                        color = Color(0xFFF05454),
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
                                tint = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Edit Photo", style = MaterialTheme.typography.bodyMedium)
            }

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
                        tint = Color(0xFF7A7A7A),
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
                        tint = Color(0xFF4F54F5),
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
                        Text(if (trusted) "On" else "Off")
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3EC3E8))
                ) {
                    Text("Update Contact", modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    Icon(
                        imageVector = Icons.Filled.CropSquare,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
            }
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color(0xFF9E9E9E)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    )
}
