package com.pulselink.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R

data class BugReportData(
    val summary: String = "",
    val stepsToReproduce: String = "",
    val expectedBehavior: String = "",
    val actualBehavior: String = "",
    val frequency: String = FREQUENCY_OPTIONS.first(),
    val severity: String = SEVERITY_OPTIONS[1],
    val userEmail: String = ""
)

val BugReportDataSaver: Saver<BugReportData, List<String>> = Saver(
    save = {
        listOf(
            it.summary,
            it.stepsToReproduce,
            it.expectedBehavior,
            it.actualBehavior,
            it.frequency,
            it.severity,
            it.userEmail
        )
    },
    restore = {
        BugReportData(
            summary = it.getOrElse(0) { "" },
            stepsToReproduce = it.getOrElse(1) { "" },
            expectedBehavior = it.getOrElse(2) { "" },
            actualBehavior = it.getOrElse(3) { "" },
            frequency = it.getOrElse(4) { FREQUENCY_OPTIONS.first() },
            severity = it.getOrElse(5) { SEVERITY_OPTIONS[1] },
            userEmail = it.getOrElse(6) { "" }
        )
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    data: BugReportData,
    onDataChange: (BugReportData) -> Unit,
    onBack: () -> Unit,
    onSubmit: (BugReportData) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var frequencyExpanded by rememberSaveable { mutableStateOf(false) }
    var severityExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10131F), Color(0xFF090B11))
                )
            ),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.bug_report_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = android.R.string.cancel)
                        )
                    }
                },
                scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BugReportTextField(
                        label = stringResource(R.string.bug_report_summary_label),
                        value = data.summary,
                        onValueChange = { onDataChange(data.copy(summary = it)) },
                        placeholder = stringResource(R.string.bug_report_summary_hint)
                    )
                    BugReportTextField(
                        label = stringResource(R.string.bug_report_actual_label),
                        value = data.actualBehavior,
                        onValueChange = { onDataChange(data.copy(actualBehavior = it)) },
                        placeholder = stringResource(R.string.bug_report_actual_hint),
                        minLines = 3
                    )
                    BugReportTextField(
                        label = stringResource(R.string.bug_report_steps_label),
                        value = data.stepsToReproduce,
                        onValueChange = { onDataChange(data.copy(stepsToReproduce = it)) },
                        placeholder = stringResource(R.string.bug_report_steps_hint),
                        minLines = 3
                    )
                    BugReportTextField(
                        label = stringResource(R.string.bug_report_expected_label),
                        value = data.expectedBehavior,
                        onValueChange = { onDataChange(data.copy(expectedBehavior = it)) },
                        placeholder = stringResource(R.string.bug_report_expected_hint),
                        minLines = 2
                    )
                    BugReportTextField(
                        label = stringResource(R.string.bug_report_contact_label),
                        value = data.userEmail,
                        onValueChange = { onDataChange(data.copy(userEmail = it)) },
                        placeholder = stringResource(R.string.bug_report_contact_hint)
                    )
                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            value = data.frequency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.bug_report_frequency_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                            colors = TextFieldDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false }
                        ) {
                            FREQUENCY_OPTIONS.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        frequencyExpanded = false
                                        onDataChange(data.copy(frequency = option))
                                    }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = severityExpanded,
                        onExpandedChange = { severityExpanded = !severityExpanded }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            value = data.severity,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.bug_report_severity_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = severityExpanded) },
                            colors = TextFieldDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = severityExpanded,
                            onDismissRequest = { severityExpanded = false }
                        ) {
                            SEVERITY_OPTIONS.forEach { option ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        severityExpanded = false
                                        onDataChange(data.copy(severity = option))
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (data.summary.isBlank() || data.actualBehavior.isBlank()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.bug_report_fields_required),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onSubmit(data)
                            }
                        }
                    ) {
                        Text(text = stringResource(R.string.bug_report_submit))
                    }
                }
            }
        }
    }
}

@Composable
private fun BugReportTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        minLines = minLines
    )
}

private val FREQUENCY_OPTIONS = listOf("Always", "Sometimes", "Rarely")
private val SEVERITY_OPTIONS = listOf("Critical", "High", "Medium", "Low")
