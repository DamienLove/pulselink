package com.pulselink.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulselink.data.ads.AppOpenAdController
import com.pulselink.assistant.AssistantShortcuts
import com.pulselink.domain.model.Contact
import com.pulselink.R
import com.pulselink.ui.screens.BetaTesterListScreen
import com.pulselink.ui.screens.BugReportData
import com.pulselink.ui.screens.BugReportDataSaver
import com.pulselink.ui.screens.BugReportScreen
import com.pulselink.ui.screens.HomeScreen
import com.pulselink.ui.screens.ContactDetailScreen
import com.pulselink.ui.screens.AlertTonePickerScreen
import com.pulselink.ui.screens.ContactConversationScreen
import com.pulselink.ui.screens.OnboardingScreen
import com.pulselink.ui.screens.OnboardingIntroScreen
import com.pulselink.ui.screens.SettingsScreen
import com.pulselink.ui.screens.SplashScreen
import com.pulselink.ui.screens.UpgradeProScreen
import com.pulselink.ui.screens.OnboardingPermissionState
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.state.MainViewModel.CallInitiationResult
import com.pulselink.ui.theme.PulseLinkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint
import com.pulselink.util.CallStateMonitor
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var appOpenAdController: AppOpenAdController
    @Inject lateinit var callStateMonitor: CallStateMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseLinkTheme {
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                val ownerName = state.settings.ownerName
                var isPreparingCall by remember { mutableStateOf(false) }
                val activity = this@MainActivity
                var isCancelingEmergency by remember { mutableStateOf(false) }
                val cancelEmergencyLauncher = rememberCancelEmergencyLauncher(
                    activity = activity,
                    onAuthenticated = {
                        isCancelingEmergency = true
                        viewModel.cancelEmergency { success ->
                            isCancelingEmergency = false
                            val message = if (success) {
                                R.string.cancel_emergency_success
                            } else {
                                R.string.cancel_emergency_failure
                            }
                            Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
                        }
                    },
                    onError = { error ->
                        error?.takeIf { it.isNotBlank() }?.let {
                            Toast.makeText(context, it.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                val cancelEmergencyHandler = remember(cancelEmergencyLauncher, context) {
                    {
                        val biometricResult = BiometricManager.from(context)
                            .canAuthenticate(CANCEL_EMERGENCY_AUTHENTICATORS)
                        if (biometricResult == BiometricManager.BIOMETRIC_SUCCESS) {
                            cancelEmergencyLauncher()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.cancel_emergency_biometric_unavailable),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                var onboardingName by rememberSaveable { mutableStateOf("") }
                var onboardingNameDirty by rememberSaveable { mutableStateOf(false) }
                var bugReportDraft by rememberSaveable(stateSaver = BugReportDataSaver) {
                    mutableStateOf(BugReportData())
                }

                LaunchedEffect(ownerName) {
                    if (!onboardingNameDirty) {
                        onboardingName = ownerName
                    }
                }
                LaunchedEffect(state.dndStatus) {
                    state.dndStatus?.let { status ->
                        Toast.makeText(context, context.getString(status.messageResId), Toast.LENGTH_LONG).show()
                        viewModel.clearDndStatusMessage()
                    }
                }

                val requiredPermissions = remember {
                    buildList {
                        add(Manifest.permission.SEND_SMS)
                        add(Manifest.permission.RECEIVE_SMS)
                        add(Manifest.permission.CALL_PHONE)
                        add(Manifest.permission.READ_CONTACTS)
                        add(Manifest.permission.READ_CALL_LOG)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var pendingPermissionCheck by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    pendingPermissionCheck = true
                }

                val callContactHandler: suspend (Contact) -> Unit = { contact ->
                    isPreparingCall = true
                    Toast.makeText(context, context.getString(R.string.call_preparing), Toast.LENGTH_SHORT).show()
                    val result = try {
                        viewModel.initiateCall(contact.id, contact.phoneNumber)
                    } finally {
                        isPreparingCall = false
                    }
                    when (result) {
                        CallInitiationResult.Ready -> {
                            Toast.makeText(context, context.getString(R.string.call_ready), Toast.LENGTH_SHORT).show()
                            val placed = placeCall(activity, contact, callStateMonitor) { duration ->
                                viewModel.notifyCallEnded(contact.id, duration)
                            }
                            if (!placed) {
                                Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        CallInitiationResult.Timeout -> {
                            Toast.makeText(context, context.getString(R.string.call_timeout), Toast.LENGTH_SHORT).show()
                            val placed = placeCall(activity, contact, callStateMonitor) { duration ->
                                viewModel.notifyCallEnded(contact.id, duration)
                            }
                            if (!placed) {
                                Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        CallInitiationResult.Failure -> {
                            Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            pendingPermissionCheck = true
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(pendingPermissionCheck) {
                    if (pendingPermissionCheck) {
                        pendingPermissionCheck = false
                        val missing = requiredPermissions.filter { perm ->
                            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                        }
                        val dndGranted = notificationManager?.isNotificationPolicyAccessGranted == true
                        val sanitizedName = onboardingName.trim()
                        if (missing.isEmpty() && sanitizedName.isNotBlank() && dndGranted) {
                            onboardingNameDirty = false
                            if (ownerName != sanitizedName) {
                                viewModel.setOwnerName(sanitizedName)
                            }
                            viewModel.completeOnboarding()
                        }
                    }
                }

                LaunchedEffect(state.onboardingComplete) {
                    if (state.onboardingComplete) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                LaunchedEffect(state.showAds) {
                    appOpenAdController.updateAvailability(state.showAds)
                }

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen {
                            val destination = if (state.onboardingComplete) "home" else "onboarding_intro"
                            navController.navigate(destination) {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                    composable("onboarding_intro") {
                        OnboardingIntroScreen(
                            ownerName = onboardingName,
                            onOwnerNameChange = { updated ->
                                onboardingName = updated
                                onboardingNameDirty = true
                            },
                            onContinue = {
                                val sanitized = onboardingName.trim()
                                if (sanitized.isBlank()) {
                                    Toast.makeText(context, "Add your name to continue.", Toast.LENGTH_SHORT).show()
                                } else {
                                    onboardingName = sanitized
                                    onboardingNameDirty = false
                                    viewModel.setOwnerName(sanitized)
                                    navController.navigate("onboarding_permissions")
                                }
                            }
                        )
                    }
                    composable("onboarding_permissions") {
                        val missingPermissions = requiredPermissions.filter { perm ->
                            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                        }

                        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true

                        LaunchedEffect(state.onboardingComplete, missingPermissions, onboardingName, hasDndAccess) {
                            val sanitized = onboardingName.trim()
                            if (!state.onboardingComplete && missingPermissions.isEmpty() && sanitized.isNotBlank() && hasDndAccess) {
                                if (ownerName != sanitized) {
                                    viewModel.setOwnerName(sanitized)
                                }
                                onboardingNameDirty = false
                                viewModel.completeOnboarding()
                            }
                        }

                        val smsGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        val callPermissionGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                        val locationGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val contactsGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        val callLogGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

                        val permissionCards = listOf(
                            OnboardingPermissionState(
                                icon = Icons.Filled.Call,
                                title = "SMS & Call",
                                description = "Allow PulseLink to send emergency messages and place calls.",
                                granted = smsGranted && callPermissionGranted,
                                manualHelp = if (!smsGranted || !callPermissionGranted) {
                                    "If SMS or Call stays disabled: open Settings -> Apps -> PulseLink -> Permissions, tap SMS and Phone, open the 3-dot menu, choose \"Allow disallowed permissions\", confirm with fingerprint or PIN, then switch both to Allow."
                                } else null
                            ),
                            OnboardingPermissionState(
                                icon = Icons.Filled.Lock,
                                title = "Override Silent / DND",
                                description = "Needed so critical alerts ring even when the phone is muted.",
                                granted = hasDndAccess,
                                actionLabel = if (hasDndAccess) "Manage" else "Allow",
                                onAction = { openDndSettings(context) }
                            ),
                            OnboardingPermissionState(
                                icon = Icons.Filled.Person,
                                title = stringResource(R.string.permission_call_log_title),
                                description = stringResource(R.string.permission_call_log_description),
                                granted = callLogGranted,
                                manualHelp = if (!callLogGranted) {
                                    "Open Settings -> Apps -> PulseLink -> Permissions and allow Call logs so linked contacts can ring through."
                                } else null
                            ),
                            OnboardingPermissionState(
                                icon = Icons.Filled.PowerSettingsNew,
                                title = "Background activity",
                                description = "Keep PulseLink active so alerts work whenever you need them.",
                                granted = true
                            ),
                            OnboardingPermissionState(
                                icon = Icons.Filled.LocationOn,
                                title = "Location",
                                description = "Include precise location when you trigger an alert.",
                                granted = locationGranted
                            ),
                            OnboardingPermissionState(
                                icon = Icons.Filled.Person,
                                title = "Contacts",
                                description = "Link trusted partners so they receive your alerts.",
                                granted = contactsGranted
                            )
                        )

                        val sanitizedOnboardingName = onboardingName.trim()
                        val canContinue = missingPermissions.isEmpty() && sanitizedOnboardingName.isNotBlank() && hasDndAccess

                        OnboardingScreen(
                            permissions = permissionCards,
                            isReadyToFinish = canContinue,
                            onGrantPermissions = {
                                if (missingPermissions.isEmpty()) {
                                    val currentName = onboardingName.trim()
                                    if (!hasDndAccess) {
                                        openDndSettings(context)
                                    } else if (currentName.isBlank()) {
                                        Toast.makeText(context, "Add your name to finish setup.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onboardingName = currentName
                                        onboardingNameDirty = false
                                        if (ownerName != currentName) {
                                            viewModel.setOwnerName(currentName)
                                        }
                                        viewModel.completeOnboarding()
                                    }
                                } else {
                                    permissionLauncher.launch(missingPermissions.toTypedArray())
                                }
                            },
                            onOpenAppSettings = { openAppSettings(context) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("home") {
                        HomeScreen(
                            state = state,
                            onDismissAssistantShortcuts = viewModel::dismissAssistantHint,
                            onTriggerEmergency = viewModel::triggerEmergency,
                            onSendCheckIn = viewModel::sendCheckIn,
                            onAddContact = viewModel::saveContact,
                            onDeleteContact = viewModel::deleteContact,
                            onContactSelected = { contactId -> navController.navigate("contact/$contactId") },
                            onContactSettings = { contactId -> navController.navigate("contact/$contactId/settings") },
                            onSendLink = viewModel::sendLinkRequest,
                            onApproveLink = viewModel::approveLink,
                            onCallContact = callContactHandler,
                            onSendManualMessage = { contact, body ->
                                viewModel.sendManualMessage(contact.id, body)
                            },
                            onReorderContacts = viewModel::reorderContacts,
                            onRequestCancelEmergency = cancelEmergencyHandler,
                            isCancelingEmergency = isCancelingEmergency,
                            onAlertsClick = { navController.navigate("alerts/default/emergency") },
                            onSettingsClick = { navController.navigate("settings") },
                            onUpgradeClick = { navController.navigate("upgrade") }
                        )
                    }
                    composable(
                        route = "contact/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        val messages by viewModel.messagesForContact(contactId).collectAsStateWithLifecycle(initialValue = emptyList())
                        ContactConversationScreen(
                            contact = contact,
                            messages = messages,
                            onBack = { navController.popBackStack() },
                            onOpenSettings = { navController.navigate("contact/$contactId/settings") },
                            onCallContact = callContactHandler,
                            onSendMessage = { body -> viewModel.sendManualMessage(contactId, body) },
                            onPing = { viewModel.sendPing(contactId) }
                        )
                    }
                    composable(
                        route = "contact/{contactId}/settings",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        ContactDetailScreen(
                            contact = contact,
                            onBack = { navController.popBackStack() },
                            onCallContact = callContactHandler,
                            onEditEmergencyAlert = { navController.navigate("alerts/contact/$contactId/emergency") },
                            onEditCheckInAlert = { navController.navigate("alerts/contact/$contactId/checkin") },
                            onToggleLocation = { enabled -> contact?.let { viewModel.updateContact(it.copy(includeLocation = enabled)) } },
                            onToggleCamera = { enabled -> contact?.let { viewModel.updateContact(it.copy(cameraEnabled = enabled)) } },
                            onToggleAutoCall = { enabled -> contact?.let { viewModel.updateContact(it.copy(autoCall = enabled)) } },
                            onToggleRemoteOverride = { allow -> viewModel.setRemoteOverridePermission(contactId, allow) },
                            onToggleRemoteSound = { allow -> viewModel.setRemoteSoundPermission(contactId, allow) },
                            onSendLink = { viewModel.sendLinkRequest(contactId) },
                            onApproveLink = { viewModel.approveLink(contactId) },
                            onPing = { viewModel.sendPing(contactId) },
                            onDelete = {
                                viewModel.deleteContact(contactId)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("alerts/default/emergency") {
                        AlertTonePickerScreen(
                            title = "Emergency alert tone",
                            subtitle = "Choose the default siren that plays during emergency alerts.",
                            options = state.emergencySoundOptions,
                            selectedKey = state.settings.emergencyProfile.soundKey,
                            onSelect = viewModel::updateEmergencySound,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("alerts/default/checkin") {
                        AlertTonePickerScreen(
                            title = "Check-in alert tone",
                            subtitle = "Pick the chime used when you send a check-in.",
                            options = state.checkInSoundOptions,
                            selectedKey = state.settings.checkInProfile.soundKey,
                            onSelect = viewModel::updateCheckInSound,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "alerts/contact/{contactId}/emergency",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        AlertTonePickerScreen(
                            title = "Emergency tone",
                            subtitle = contact?.displayName?.let { "Overrides the default tone when alerting ${it}." }
                                ?: "Overrides the default emergency tone for this contact.",
                            options = state.emergencySoundOptions,
                            selectedKey = contact?.emergencySoundKey ?: state.settings.emergencyProfile.soundKey,
                            onSelect = { key -> viewModel.updateContactSounds(contactId, key, null) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "alerts/contact/{contactId}/checkin",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        AlertTonePickerScreen(
                            title = "Check-in tone",
                            subtitle = contact?.displayName?.let { "Choose the chime used for ${it} check-ins." }
                                ?: "Choose the check-in chime for this contact.",
                            options = state.checkInSoundOptions,
                            selectedKey = contact?.checkInSoundKey ?: state.settings.checkInProfile.soundKey,
                            onSelect = { key -> viewModel.updateContactSounds(contactId, null, key) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
                        SettingsScreen(
                            settings = state.settings,
                            hasDndAccess = hasDndAccess,
                            onAssistantShortcuts = { AssistantShortcuts.launchShortcutSettings(context) },
                            onToggleIncludeLocation = viewModel::setIncludeLocation,
                            onRequestDndAccess = { openDndSettings(context) },
                            onToggleAutoAllowRemoteSoundChange = viewModel::setAutoAllowRemoteSoundChange,
                            onEditEmergencyTone = { navController.navigate("alerts/default/emergency") },
                            onEditCheckInTone = { navController.navigate("alerts/default/checkin") },
                            onReportBug = { navController.navigate("bug_report") },
                            onBetaTesters = { navController.navigate("beta_testers") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("bug_report") {
                        BugReportScreen(
                            data = bugReportDraft,
                            onDataChange = { bugReportDraft = it },
                            onBack = {
                                bugReportDraft = BugReportData()
                                navController.popBackStack()
                            },
                            onSubmit = { report ->
                                val bugUri = viewModel.buildBugReportUri(context, report)
                                val viewIntent = Intent(Intent.ACTION_VIEW, bugUri)
                                runCatching {
                                    startActivity(viewIntent)
                                }.onSuccess {
                                    Toast.makeText(
                                        context,
                                        activity.getString(R.string.bug_report_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    bugReportDraft = BugReportData()
                                    navController.popBackStack()
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        activity.getString(R.string.bug_report_portal_unavailable),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                    composable("beta_testers") {
                        BetaTesterListScreen(
                            isBetaTester = state.settings.isBetaTester,
                            onToggleBetaTester = { enabled -> viewModel.setBetaTesterStatus(enabled) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("upgrade") {
                        UpgradeProScreen(
                            isPro = state.isProUser,
                            onUpgradeClick = { viewModel.setProUnlocked(true) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (isPreparingCall) {
                    CallPreparationDialog()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appOpenAdController.maybeShow(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        callStateMonitor.cancel()
    }
}

@Composable
private fun rememberCancelEmergencyLauncher(
    activity: AppCompatActivity,
    onAuthenticated: () -> Unit,
    onError: (CharSequence?) -> Unit
): () -> Unit {
    val promptInfo = remember {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.cancel_emergency_prompt_title))
            .setSubtitle(activity.getString(R.string.cancel_emergency_prompt_subtitle))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(CANCEL_EMERGENCY_AUTHENTICATORS)
        } else {
            builder.setNegativeButtonText(activity.getString(android.R.string.cancel))
        }
        builder.build()
    }
    val executor = remember { ContextCompat.getMainExecutor(activity) }
    return remember {
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        return
                    }
                    onError(errString)
                }

                override fun onAuthenticationFailed() {
                    onError(activity.getString(R.string.cancel_emergency_failure))
                }
            }
        )
        return@remember {
            prompt.authenticate(promptInfo)
        }
    }
}

private const val CANCEL_EMERGENCY_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

private fun placeCall(
    activity: MainActivity,
    contact: Contact,
    monitor: CallStateMonitor,
    onCallEnded: (Long) -> Unit
): Boolean {
    val callPermission = Manifest.permission.CALL_PHONE
    if (ContextCompat.checkSelfPermission(activity, callPermission) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
            REQUEST_CALL_PERMISSIONS
        )
        Toast.makeText(activity, activity.getString(R.string.call_permission_required), Toast.LENGTH_SHORT).show()
        return false
    }
    val readPhoneStateGranted =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    if (!readPhoneStateGranted) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_PHONE_STATE),
            REQUEST_CALL_PERMISSIONS
        )
    }
    if (readPhoneStateGranted) {
        runCatching {
            monitor.monitorOutgoingCall(onCallEnded)
        }.onFailure {
            monitor.cancel()
        }
    } else {
        monitor.cancel()
    }
    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phoneNumber}"))
    return try {
        activity.startActivity(intent)
        true
    } catch (error: SecurityException) {
        monitor.cancel()
        false
    }
}

@androidx.compose.runtime.Composable
private fun CallPreparationDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(text = stringResource(id = R.string.call_preparing)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    )
}

private const val REQUEST_CALL_PERMISSIONS = 2001

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openDndSettings(context: android.content.Context) {
    context.startActivity(
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
