package com.pulselink.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomVibrationCreatorScreen(
    onSave: (String, LongArray) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    var isRecording by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var patternName by remember { mutableStateOf("") }
    var recordingStartTime by remember { mutableLongStateOf(0L) }
    var lastEventTime by remember { mutableLongStateOf(0L) }
    val tapTimestamps = remember { mutableStateListOf<Long>() }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    val maxRecordingTime = 5000L // 5 seconds max

    // Update elapsed time while recording
    LaunchedEffect(isRecording) {
        while (isRecording) {
            elapsedTime = System.currentTimeMillis() - recordingStartTime
            if (elapsedTime >= maxRecordingTime) {
                isRecording = false
            }
            delay(50)
        }
    }

    fun startRecording() {
        tapTimestamps.clear()
        recordingStartTime = System.currentTimeMillis()
        lastEventTime = recordingStartTime
        isRecording = true
        elapsedTime = 0L
    }

    fun stopRecording() {
        isRecording = false
        if (tapTimestamps.size >= 2) {
            showSaveDialog = true
        }
    }

    fun handleTap() {
        if (!isRecording) return

        val now = System.currentTimeMillis()
        tapTimestamps.add(now)
        lastEventTime = now

        // Vibrate briefly for feedback
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(50)
            }
        }
    }

    fun buildPattern(): LongArray {
        if (tapTimestamps.size < 2) return longArrayOf(0, 300)

        val pattern = mutableListOf<Long>()
        pattern.add(0) // Initial delay

        for (i in 0 until tapTimestamps.size) {
            val vibrationDuration = 100L
            pattern.add(vibrationDuration)

            if (i < tapTimestamps.size - 1) {
                val gapDuration = (tapTimestamps[i + 1] - tapTimestamps[i]) - vibrationDuration
                pattern.add(gapDuration.coerceAtLeast(50))
            }
        }

        return pattern.toLongArray()
    }

    fun testPattern() {
        val pattern = buildPattern()
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }

    val tapButtonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.05f else 1f,
        label = "tapButtonScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create custom vibration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Tap the button below to create your vibration pattern",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (isRecording) "Recording..." else "Press START to begin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            if (isRecording) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { elapsedTime.toFloat() / maxRecordingTime },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${elapsedTime / 1000}.${(elapsedTime % 1000) / 100}s / ${maxRecordingTime / 1000}s",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Large tap button
            Surface(
                modifier = Modifier
                    .size(200.dp)
                    .scale(tapButtonScale)
                    .pointerInput(isRecording) {
                        detectTapGestures(
                            onTap = { if (isRecording) handleTap() }
                        )
                    },
                shape = CircleShape,
                color = if (isRecording)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isRecording) 8.dp else 2.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Tap to vibrate",
                            modifier = Modifier.size(48.dp),
                            tint = if (isRecording)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isRecording) "TAP HERE" else "Ready",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isRecording && tapTimestamps.isNotEmpty()) {
                            Text(
                                text = "${tapTimestamps.size} taps",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isRecording) {
                    Button(
                        onClick = { startRecording() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Text("START", modifier = Modifier.padding(start = 8.dp))
                    }

                    if (tapTimestamps.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { testPattern() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test")
                        }
                    }
                } else {
                    Button(
                        onClick = { stopRecording() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Text("STOP", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save vibration pattern") },
            text = {
                Column {
                    Text("Give your pattern a name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = patternName,
                        onValueChange = { patternName = it },
                        label = { Text("Pattern name") },
                        placeholder = { Text("e.g., My Pattern") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Created ${tapTimestamps.size} vibrations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (patternName.isNotBlank()) {
                            val pattern = buildPattern()
                            onSave(patternName, pattern)
                            showSaveDialog = false
                            onBack()
                        }
                    },
                    enabled = patternName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
