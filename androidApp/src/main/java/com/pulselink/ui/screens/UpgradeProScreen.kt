package com.pulselink.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeProScreen(
    isPro: Boolean,
    onUpgradeClick: () -> Unit,
    onBack: () -> Unit
) {
    val features = listOf(
        stringResource(id = R.string.pro_feature_voice),
        stringResource(id = R.string.pro_feature_realtime),
        stringResource(id = R.string.pro_feature_biometric_cancel),
        stringResource(id = R.string.pro_feature_override),
        stringResource(id = R.string.pro_feature_ads)
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
        },
        contentWindowInsets = WindowInsets.safeDrawing
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
                    painter = painterResource(id = R.drawable.ic_pulselink_pro),
                    contentDescription = null,
                    modifier = Modifier.height(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Go Pro",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Unlock the full emergency toolkit, silence every ad, and bring Gemini-powered hands-free control to your alerts.",
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.pro_upgrade_price_blurb), color = Color(0xFF9AA0B4))
                if (isPro) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34D399))
                        Text(
                            text = stringResource(id = R.string.pro_upgrade_active),
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF34D399)
                        )
                    }
                }

                Button(
                    onClick = onUpgradeClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPro
                ) {
                    Text(
                        text = if (isPro) {
                            stringResource(id = R.string.pro_upgrade_button_active)
                        } else {
                            stringResource(id = R.string.pro_upgrade_cta)
                        }
                    )
                }
            }
        }
    }
}
