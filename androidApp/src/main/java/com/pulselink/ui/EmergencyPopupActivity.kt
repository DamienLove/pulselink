package com.pulselink.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulselink.ui.theme.PulseLinkTheme

class EmergencyPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window flags to show on top of lock screen and other apps
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
        val tier = intent.getStringExtra(EXTRA_ESCALATION_TIER) ?: "Emergency"

        setContent {
            PulseLinkTheme {
                EmergencyPopupScreen(contactName = contactName, tier = tier) {
                    finish()
                }
            }
        }
    }

    companion object {
        private const val EXTRA_CONTACT_NAME = "extra_contact_name"
        private const val EXTRA_ESCALATION_TIER = "extra_escalation_tier"

        fun newIntent(context: Context, contactName: String, tier: String): Intent {
            return Intent(context, EmergencyPopupActivity::class.java).apply {
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_ESCALATION_TIER, tier)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }
    }
}

@Composable
fun EmergencyPopupScreen(contactName: String, tier: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.8f))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EMERGENCY ALERT!",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Alert from $contactName",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tier: $tier",
            color = Color.White,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onDismiss) {
            Text("Dismiss", fontSize = 20.sp)
        }
    }
}
