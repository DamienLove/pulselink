package com.pulselink.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import com.pulselink.domain.model.Contact
import com.pulselink.ui.screens.HomeScreen
import com.pulselink.ui.screens.ContactDetailScreen
import com.pulselink.ui.screens.NotificationSoundScreen
import com.pulselink.ui.screens.ContactConversationScreen
import com.pulselink.ui.screens.OnboardingScreen
import com.pulselink.ui.screens.OnboardingIntroScreen
import com.pulselink.ui.screens.SettingsScreen
import com.pulselink.ui.screens.SplashScreen
import com.pulselink.ui.screens.UpgradeProScreen
import com.pulselink.ui.screens.OnboardingPermissionState
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.theme.PulseLinkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.LocationOn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var appOpenAdController: AppOpenAdController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulseLinkTheme {
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                val ownerName = state.settings.ownerName

                var onboardingName by rememberSaveable { mutableStateOf("") }
                var onboardingNameDirty by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(ownerName) {
                    if (!onboardingNameDirty) {
                        onboardingName = ownerName
                    }
                }

                val requiredPermissions = remember {
                    buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        add(Manifest.permission.SEND_SMS)
                        add(Manifest.permission.RECEIVE_SMS)
                        add(Manifest.permission.READ_CONTACTS)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var hasMicrophonePermission by remember { mutableStateOf(hasMicrophone(context)) }
                var pendingPermissionCheck by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    pendingPermissionCheck = true
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
                        hasMicrophonePermission = hasMicrophone(context)
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

                LaunchedEffect(state.isListening, hasMicrophonePermission, state.onboardingComplete) {
                    if (state.onboardingComplete && state.isListening && hasMicrophonePermission) {
                        viewModel.ensureServiceRunning(context)
                    } else {
                        viewModel.stopService(context)
                    }
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
                                hasMicrophonePermission = hasMicrophone(context)
                                viewModel.completeOnboarding()
                            }
                        }

                        val smsGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        val micGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val locationGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val contactsGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

                        val permissionCards = listOf(
                            OnboardingPermissionState(
                                icon = Icons.Filled.Call,
                                title = "SMS & Call",
                                description = "Allow PulseLink to send emergency messages and place calls.",
                                granted = smsGranted,
                                manualHelp = if (!smsGranted) {
                                    "If SMS stays disabled: open Settings -> Apps -> PulseLink -> Permissions, tap SMS, open the 3-dot menu, choose \"Allow disallowed permissions\", confirm with fingerprint or PIN, then switch SMS to Allow."
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
                                icon = Icons.Filled.Mic,
                                title = "Microphone",
                                description = "PulseLink listens for your safewords in the background.",
                                granted = micGranted
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
                                        hasMicrophonePermission = hasMicrophone(context)
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
                            onToggleListening = viewModel::toggleListening,
                            onSendCheckIn = viewModel::sendCheckIn,
                            onTriggerTest = viewModel::triggerTest,
                            onAddContact = viewModel::saveContact,
                            onDeleteContact = viewModel::deleteContact,
                            onToggleProMode = viewModel::setProUnlocked,
                            onContactSelected = { contactId -> navController.navigate("contact/$contactId") },
                            onContactSettings = { contactId -> navController.navigate("contact/$contactId/settings") },
                            onSendLink = viewModel::sendLinkRequest,
                            onApproveLink = viewModel::approveLink,
                            onCallContact = { contact ->
                                val succeeded = viewModel.prepareRemoteCall(contact.id)
                                dialContact(context, contact)
                                if (!succeeded) {
                                    Toast.makeText(context, "Call started (receiver may still be on silent)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSendManualMessage = { contact, body ->
                                viewModel.sendManualMessage(contact.id, body)
                            },
                            onAlertsClick = { navController.navigate("alerts/default") },
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
                            onCallContact = { contactToCall ->
                                val succeeded = viewModel.prepareRemoteCall(contactToCall.id)
                                dialContact(context, contactToCall)
                                if (!succeeded) {
                                    Toast.makeText(context, "Call started (receiver may still be on silent)", Toast.LENGTH_SHORT).show()
                                }
                            },
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
                            onCallContact = { target ->
                                val succeeded = viewModel.prepareRemoteCall(target.id)
                                dialContact(context, target)
                                if (!succeeded) {
                                    Toast.makeText(context, "Call started (receiver may still be on silent)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onEditEmergencyAlert = { navController.navigate("alerts/contact/$contactId") },
                            onEditCheckInAlert = { navController.navigate("alerts/contact/$contactId") },
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
                    composable("alerts/default") {
                        NotificationSoundScreen(
                            title = "Default Alerts",
                            emergencyOptions = state.emergencySoundOptions,
                            checkInOptions = state.checkInSoundOptions,
                            selectedEmergency = state.settings.emergencyProfile.soundKey,
                            selectedCheckIn = state.settings.checkInProfile.soundKey,
                            onSelectEmergency = viewModel::updateEmergencySound,
                            onSelectCheckIn = viewModel::updateCheckInSound,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "alerts/contact/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        NotificationSoundScreen(
                            title = "${contact?.displayName ?: "Contact"} Alerts",
                            emergencyOptions = state.emergencySoundOptions,
                            checkInOptions = state.checkInSoundOptions,
                            selectedEmergency = contact?.emergencySoundKey,
                            selectedCheckIn = contact?.checkInSoundKey,
                            onSelectEmergency = { key -> viewModel.updateContactSounds(contactId, key, contact?.checkInSoundKey) },
                            onSelectCheckIn = { key -> viewModel.updateContactSounds(contactId, contact?.emergencySoundKey, key) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true
                        SettingsScreen(
                            settings = state.settings,
                            hasDndAccess = hasDndAccess,
                            onToggleIncludeLocation = viewModel::setIncludeLocation,
                            onRequestDndAccess = { openDndSettings(context) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("upgrade") {
                        UpgradeProScreen(
                            isPro = state.isProUser,
                            onTogglePro = { viewModel.setProUnlocked(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appOpenAdController.maybeShow(this)
    }
}

private fun hasMicrophone(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

private fun dialContact(context: android.content.Context, contact: Contact) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
    context.startActivity(intent)
}

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
