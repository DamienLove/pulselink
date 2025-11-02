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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeProScreen(
    isPro: Boolean,
    onTogglePro: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val features = listOf(
        "Immediate 911 escalation with live location",
        "Automatic video capture and cloud backup",
        "Priority SMS delivery and retry logic",
        "Global roaming support for alerts"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "PulseLink Pro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.height(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Upgrade", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    text = "Unlock premium emergency automation and concierge support.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B4)
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(features) { feature ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F2A))) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34D399))
                            Text(text = feature, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "PulseLink Pro", fontWeight = FontWeight.Bold)
                    Text(text = "$4.99 / month", color = Color(0xFF9AA0B4))
                }
                Switch(checked = isPro, onCheckedChange = onTogglePro)
            }

            Button(onClick = { onTogglePro(true) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (isPro) "Manage subscription" else "Upgrade now")
            }
        }
    }
}
