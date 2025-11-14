package com.pulselink.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.widthIn
import com.pulselink.R
import com.pulselink.data.assistant.VoiceCommandResult
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ContactMessage
import com.pulselink.domain.model.LinkStatus
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.MessageDirection
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun ContactConversationScreen(
    contact: Contact?,
    messages: List<ContactMessage>,
    isProUser: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onSendMessage: suspend (String) -> ManualMessageResult,
    onPing: suspend () -> Boolean,
    onVoiceCommand: suspend (String) -> VoiceCommandResult,
    onUpgradeClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0B0E1A), Color(0xFF151826))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(gradient)
    ) {
        if (contact == null) {
            EmptyConversationState(onBack = onBack)
        } else {
            ConversationBody(
                contact = contact,
                messages = messages,
                isProUser = isProUser,
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                onCallContact = onCallContact,
                onSendMessage = onSendMessage,
                onPing = onPing,
                onVoiceCommand = onVoiceCommand,
                onUpgradeClick = onUpgradeClick
            )
        }
    }
}

@Composable
private fun EmptyConversationState(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Contact not found", color = Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun ConversationBody(
    contact: Contact,
    messages: List<ContactMessage>,
    isProUser: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onCallContact: suspend (Contact) -> Unit,
    onSendMessage: suspend (String) -> ManualMessageResult,
    onPing: suspend () -> Boolean,
    onVoiceCommand: suspend (String) -> VoiceCommandResult,
    onUpgradeClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(TextFieldValue("")) }
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (spoken.isNullOrEmpty()) {
            Toast.makeText(context, context.getString(R.string.voice_command_error_unknown), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val outcome = onVoiceCommand(spoken)
            val message = when (outcome) {
                is VoiceCommandResult.Success -> outcome.message
                is VoiceCommandResult.Error -> outcome.message
                VoiceCommandResult.UpgradeRequired -> context.getString(R.string.voice_command_upgrade_required)
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Row {
                IconButton(onClick = {
                    scope.launch { onCallContact(contact) }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Call contact",
                        tint = Color(0xFF34D399)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Contact settings",
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.08f)) {
                Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "PulseLink logo",
                    modifier = Modifier
                        .size(84.dp)
                        .padding(16.dp)
                )
            }
            Text(text = "PulseLink", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = contact.displayName,
                color = Color(0xFFD6DCFF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            StatusRow(contact)
            if (contact.linkStatus == LinkStatus.LINKED) {
                TextButton(onClick = {
                    scope.launch {
                        val result = runCatching { onPing() }
                        val toastText = when {
                            result.isFailure -> "Check-in failed to send"
                            result.getOrDefault(false) -> "Check-in sent"
                            else -> "Check-in sent (receiver may still be on silent)"
                        }
                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(text = "Send check-in", color = Color(0xFF67DBA0))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        ComposerRow(
            input = input,
            onInputChange = { input = it },
            onSend = {
                val body = input.text.trim()
                if (body.isEmpty()) {
                    Toast.makeText(context, "Message cannot be empty", Toast.LENGTH_SHORT).show()
                    return@ComposerRow
                }
                scope.launch {
                    var outcome: ManualMessageResult? = null
                    val toastText = try {
                        val result = onSendMessage(body)
                        outcome = result
                        when (result) {
                            is ManualMessageResult.Success -> {
                                if (result.overrideApplied) {
                                    "Message sent"
                                } else {
                                    "Message sent (receiver may still be on silent)"
                                }
                            }
                            is ManualMessageResult.Failure -> {
                                when (result.reason) {
                                    ManualMessageResult.Failure.Reason.CONTACT_MISSING -> "Contact no longer available"
                                    ManualMessageResult.Failure.Reason.NOT_LINKED -> "Link this contact before messaging"
                                    ManualMessageResult.Failure.Reason.SMS_FAILED -> "Message failed to send"
                                    ManualMessageResult.Failure.Reason.UNKNOWN -> "Message failed to send"
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        "Message failed to send"
                    }
                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                    if (outcome is ManualMessageResult.Success) {
                        input = TextFieldValue("")
                    }
                }
            },
            onVoice = {
                if (!isProUser) {
                    Toast.makeText(context, context.getString(R.string.voice_command_upgrade_required), Toast.LENGTH_SHORT).show()
                    onUpgradeClick()
                    return@ComposerRow
                }
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Toast.makeText(context, context.getString(R.string.voice_command_no_recognizer), Toast.LENGTH_SHORT).show()
                    return@ComposerRow
                }
                val prompt = context.getString(R.string.voice_command_prompt)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                }
                voiceLauncher.launch(intent)
            }
        )
    }
}

@Composable
private fun StatusRow(contact: Contact) {
    val (label, color) = when (contact.linkStatus) {
        LinkStatus.LINKED -> "linked" to Color(0xFF67DBA0)
        LinkStatus.OUTBOUND_PENDING -> "awaiting approval" to Color(0xFFFFB74D)
        LinkStatus.INBOUND_REQUEST -> "approve request" to Color(0xFFFFB74D)
        LinkStatus.NONE -> "not linked" to Color(0xFFF87171)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}

@Composable
private fun ComposerRow(
    input: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6)))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontFamily = FontFamily.Default),
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            cursorBrush = Brush.verticalGradient(listOf(Color.White, Color.White)),
            decorationBox = { innerField ->
                if (input.text.isEmpty()) {
                    Text(
                        text = "Type your message...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
                innerField()
            }
        )
        IconButton(onClick = onVoice) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice message", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onSend) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ContactMessage) {
    val isOutbound = message.direction == MessageDirection.OUTBOUND
    val alignment = if (isOutbound) Alignment.End else Alignment.Start
    val bubbleBrush = if (isOutbound) {
        Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF60A5FA)))
    } else {
        null
    }
    val bubbleColor = if (isOutbound) Color.Transparent else Color(0x33212533)
    val textColor = if (isOutbound) Color.White else Color(0xFFE2E8F0)
    val timestamp = remember(message.timestamp) {
        android.text.format.DateFormat.format("h:mm a", message.timestamp).toString()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = bubbleBrush ?: Brush.verticalGradient(listOf(bubbleColor, bubbleColor)),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .widthIn(max = 320.dp)
        ) {
            Text(text = message.body, color = textColor, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isOutbound) {
                Text(
                    text = if (message.overrideSucceeded) "override ready" else "may be silenced",
                    color = if (message.overrideSucceeded) Color(0xFF67DBA0) else Color(0xFFFFB74D),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(text = timestamp, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
    }
}
