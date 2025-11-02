package com.pulselink.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
import com.pulselink.ui.screens.OnboardingScreen
import com.pulselink.ui.screens.SplashScreen
import com.pulselink.ui.screens.UpgradeProScreen
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.theme.PulseLinkTheme
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

                val requiredPermissions = remember {
                    buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        add(Manifest.permission.SEND_SMS)
                        add(Manifest.permission.RECEIVE_SMS)
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

                LaunchedEffect(pendingPermissionCheck) {
                    if (pendingPermissionCheck) {
                        pendingPermissionCheck = false
                        val missing = requiredPermissions.filter { perm ->
                            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                        }
                        hasMicrophonePermission = hasMicrophone(context)
                        if (missing.isEmpty()) {
                            viewModel.completeOnboarding()
                        }
                    }
                }

                LaunchedEffect(state.onboardingComplete) {
                    if (state.onboardingComplete) {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                            popUpTo("splash") { inclusive = true }
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
                            val destination = if (state.onboardingComplete) "home" else "onboarding"
                            navController.navigate(destination) {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                    composable("onboarding") {
                        OnboardingScreen(
                            onGrantPermissions = {
                                val missing = requiredPermissions.filter { perm ->
                                    ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                                }
                                if (missing.isEmpty()) {
                                    hasMicrophonePermission = hasMicrophone(context)
                                    viewModel.completeOnboarding()
                                } else {
                                    permissionLauncher.launch(missing.toTypedArray())
                                }
                            }
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
                            onSelectEmergencySound = viewModel::updateEmergencySound,
                            onSelectCheckInSound = viewModel::updateCheckInSound,
                            onToggleProMode = viewModel::setProUnlocked,
                            onContactSelected = { contactId -> navController.navigate("contact/$contactId") },
                            onCallContact = { contact -> dialContact(context, contact) },
                            onMessageContact = { contact -> messageContact(context, contact) },
                            onAlertsClick = { navController.navigate("alerts/default") },
                            onUpgradeClick = { navController.navigate("upgrade") }
                        )
                    }
                    composable(
                        route = "contact/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId")
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        ContactDetailScreen(
                            contact = contact,
                            onBack = { navController.popBackStack() },
                            onEditEmergencyAlert = { contactId?.let { navController.navigate("alerts/contact/$it") } },
                            onEditCheckInAlert = { contactId?.let { navController.navigate("alerts/contact/$it") } },
                            onToggleLocation = { enabled -> contact?.let { viewModel.updateContact(it.copy(includeLocation = enabled)) } },
                            onToggleCamera = { enabled -> contact?.let { viewModel.updateContact(it.copy(cameraEnabled = enabled)) } },
                            onToggleAutoCall = { enabled -> contact?.let { viewModel.updateContact(it.copy(autoCall = enabled)) } },
                            onToggleRemoteOverride = { allow -> contactId?.let { viewModel.setRemoteOverridePermission(it, allow) } },
                            onToggleRemoteSound = { allow -> contactId?.let { viewModel.setRemoteSoundPermission(it, allow) } },
                            onSendLink = { contactId?.let { viewModel.sendLinkRequest(it) } },
                            onApproveLink = { contactId?.let { viewModel.approveLink(it) } },
                            onPing = { contactId?.let { viewModel.sendPing(it) } },
                            onDelete = {
                                contactId?.let { viewModel.deleteContact(it) }
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

private fun messageContact(context: android.content.Context, contact: Contact) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
    context.startActivity(intent)
}
