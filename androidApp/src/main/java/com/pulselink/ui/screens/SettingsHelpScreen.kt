package com.pulselink.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pulselink.BuildConfig
import com.pulselink.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHelpScreen(
    onBack: () -> Unit,
    onOpenFaq: () -> Unit = {},
    onReportBug: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_help_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader(title = stringResource(id = R.string.settings_help_section_heading))
            HelpSection(
                title = "Add and link trusted contacts",
                body = "Tap Add trusted contact on the Home screen, enter a name with phone or email, then send a link invite. Use the toggles on the contact profile to allow remote alert changes, and mark contacts as Favorite or Private to control inbox tabs."
            )
            HelpSection(
                title = "Send emergency and check-in alerts",
                body = "Use Emergency or Check-in to notify trusted contacts. You can include location, auto-call after sending, and a short camera clip per contact. Linked contacts can override Do Not Disturb when allowed."
            )
            HelpSection(
                title = "Beacon Inbox and messaging",
                body = "Open Messages and set PulseLink or Beacon as your default SMS app to load your inbox. Use search, swipe to archive, and the tabs for All, 2-step codes, Trusted, Favorites, and Private."
            )
            HelpSection(
                title = "Customize alerts and the app look",
                body = "Settings > Alert tones lets you pick sirens, check-in chimes, and call overrides. Visual Settings lets you change colors, backgrounds, and the inbox icon. Per-contact tones live in the contact profile."
            )
            HelpSection(
                title = "Premium highlights",
                body = "Premium removes ads and unlocks AI summaries, compose assist, urgency alerts, and multi-device SMS sync with line controls. Enable Web access below to read messages on https://app.damiennichols.com."
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_battery_title),
                body = stringResource(id = R.string.settings_help_battery_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_unused_title),
                body = stringResource(id = R.string.settings_help_unused_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_default_sms_title),
                body = stringResource(id = R.string.settings_help_default_sms_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_web_title),
                body = stringResource(id = R.string.settings_help_web_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_pin_title),
                body = stringResource(id = R.string.settings_help_pin_body)
            )
            HelpSection(
                title = stringResource(id = R.string.settings_help_support_title),
                body = stringResource(id = R.string.settings_help_support_body)
            )

                        // Google Assistant App Actions section
            HelpSection(
                title = "Using Google Assistant",
                body = "You can trigger PulseLink emergency features using voice commands with Google Assistant. Try saying 'Hey Google, emergency alert with PulseLink' or 'Hey Google, check in with PulseLink'."
            )

            SectionHeader(
                title = "Troubleshooting",
                actionLabel = "Report Bug",
                onActionClick = onReportBug
            )

            // Visual separator before FAQ section
            Spacer(Modifier.height(24.dp))
            
            // FAQ section subtitle
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            FaqList(
                showSectionTitle = true,
                showOpenAllAction = true,
                onOpenAllClick = onOpenFaq
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HelpSection(
    title: String,
    body: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FaqList(
    showSectionTitle: Boolean = true,
    showOpenAllAction: Boolean = false,
    onOpenAllClick: (() -> Unit)? = null
) {
    if (showSectionTitle) {
        SectionHeader(
            title = stringResource(id = R.string.settings_faq_section_title),
            actionLabel = if (showOpenAllAction && onOpenAllClick != null) {
                stringResource(id = R.string.settings_help_faq_cta)
            } else null,
            onActionClick = onOpenAllClick
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val items = listOf(
            R.string.faq_question_1 to R.string.faq_answer_1,
            R.string.faq_question_2 to R.string.faq_answer_2,
            R.string.faq_question_3 to R.string.faq_answer_3,
            R.string.faq_question_4 to R.string.faq_answer_4,
            R.string.faq_question_5 to R.string.faq_answer_5,
            R.string.faq_question_6 to R.string.faq_answer_6
        )
        items.forEach { (qRes, aRes) ->
            FaqItem(
                question = stringResource(id = qRes),
                answer = stringResource(id = aRes)
            )
        }
        FaqItem(
            question = stringResource(id = R.string.faq_question_version),
            answer = stringResource(
                id = R.string.faq_answer_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
        )
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "faqRotation")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
                .animateContentSize()
            .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple()
                ) { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
