package com.pulselink.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.pulselink.auth.AuthState
import com.pulselink.data.ads.AppOpenAdController
import com.pulselink.data.sms.MessageNotificationManager
import com.pulselink.domain.model.Contact
import com.pulselink.domain.model.ManualMessageResult
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.R
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.screens.BetaTesterListScreen
import com.pulselink.ui.screens.HomeScreen
import com.pulselink.ui.screens.AlertHistoryScreen
import com.pulselink.ui.screens.EmergencyMapScreen
import com.pulselink.ui.screens.AlertTonePickerScreen
import com.pulselink.ui.screens.BetaAgreementFullScreen
import com.pulselink.ui.screens.BetaAgreementScreen
import com.pulselink.ui.screens.ContactDetailScreen
import com.pulselink.ui.screens.ContactConversationScreen
import com.pulselink.ui.screens.ContactCreateScreen
import com.pulselink.ui.screens.LoginScreen
import com.pulselink.ui.screens.OnboardingScreen
import com.pulselink.ui.screens.OnboardingPermissionState
import com.pulselink.ui.screens.OtpCleanupOnboardingCard
import com.pulselink.ui.screens.OnboardingIntroScreen
import com.pulselink.ui.screens.FaqScreen
import com.pulselink.ui.branding.pulseBrandName
import com.pulselink.ui.screens.SettingsHelpScreen
import com.pulselink.ui.screens.SettingsScreen
import com.pulselink.ui.screens.MessageNotificationSoundScreen
import com.pulselink.ui.screens.VibrationPatternPickerScreen
import com.pulselink.ui.screens.MultiLineSetupDialog
import com.pulselink.ui.screens.LineLimitDialog
import com.pulselink.ui.screens.ProfileSettingsScreen
import com.pulselink.ui.screens.ExtensionsStoreScreen
import com.pulselink.ui.screens.SplashScreen
import com.pulselink.ui.screens.SmsInboxScreen
import com.pulselink.ui.screens.SmsThreadScreen
import com.pulselink.ui.screens.VisualSettingsScreen
import com.pulselink.ui.screens.PrivatePinScreen
import com.pulselink.ui.state.LoginViewModel
import com.pulselink.ui.state.ContactConversationViewModel
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.state.MainViewModel.CallInitiationResult
import com.pulselink.ui.state.SmsInboxViewModel
import com.pulselink.ui.state.SmsLinesViewModel
import com.pulselink.ui.state.SmsThreadViewModel
import com.pulselink.ui.state.PublicProfile
import com.pulselink.ui.theme.PulseLinkTheme
import com.pulselink.util.VibrationPatterns
import com.pulselink.util.normalizeSmsAddress
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import dagger.hilt.android.AndroidEntryPoint
import com.pulselink.util.CallStateMonitor
import javax.inject.Inject
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.ExperimentalMaterial3Api
import com.pulselink.BuildConfig
import com.pulselink.util.formatTimestamp
import com.pulselink.util.DefaultSmsHelper
import com.pulselink.util.BeaconIconManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import com.pulselink.util.splitSmsDisplayAddress

private data class BeaconAssistState(
    val iconEnabled: Boolean = false,
    val defaultSmsGranted: Boolean = false,
    val smsPermissionsGranted: Boolean = false,
    val message: String = "",
    val error: String? = null
) {
    val ready: Boolean get() = iconEnabled && smsPermissionsGranted
}

private enum class BeaconFlowStage {
    Idle, RequestedDefault, RequestedPermissions
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var appOpenAdController: AppOpenAdController
    @Inject lateinit var callStateMonitor: CallStateMonitor
    @Inject lateinit var defaultSmsHelper: DefaultSmsHelper
    private val inboxShortcutFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private fun updateBeaconLauncher(enable: Boolean, variant: String) {
        BeaconIconManager.apply(this, variant, enable)
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("open_sms_inbox", false) == true) {
            inboxShortcutFlow.tryEmit(Unit)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )
        setContent {
            PulseLinkTheme {
                val context = LocalContext.current
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val linesViewModel: SmsLinesViewModel = hiltViewModel()
                val lines by linesViewModel.lines.collectAsStateWithLifecycle()
                val lineDevices by linesViewModel.devices.collectAsStateWithLifecycle()
                val activeLineId by linesViewModel.activeLineId.collectAsStateWithLifecycle()
                val deviceLineId by linesViewModel.deviceLineId.collectAsStateWithLifecycle()
                val defaultSendLineId by linesViewModel.defaultSendLineId.collectAsStateWithLifecycle()
                val lineSendPreference by linesViewModel.lineSendPreference.collectAsStateWithLifecycle()
                val threadLineOverrides by linesViewModel.threadLineOverrides.collectAsStateWithLifecycle()
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                val isPremium = BuildConfig.PREMIUM_FEATURES || state.settings.premiumUnlocked
                val isPro = BuildConfig.PRO_FEATURES || state.settings.proUnlocked || isPremium
                val pulseDisplayName = pulseBrandName(isPremium, isPro)
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var missingSmsPerms by remember { mutableStateOf(requiredSmsPermissions(context)) }
                val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                val ownerName = state.settings.ownerName
                var isPreparingCall by remember { mutableStateOf(false) }
                val activity = this@MainActivity
                var isCancelingEmergency by remember { mutableStateOf(false) }
                var isDefaultSms by remember { mutableStateOf(defaultSmsHelper.isDefaultSms()) }
                var isCheckingDefaultSms by remember { mutableStateOf(false) }
                var defaultSmsCheck by remember { mutableStateOf<Deferred<Boolean>?>(null) }
                var pendingInboxNav by remember { mutableStateOf(false) }
                var showBeaconAssist by remember { mutableStateOf(false) }
                var beaconAssistState by remember { mutableStateOf(BeaconAssistState(message = "")) }
                var beaconFlowStage by remember { mutableStateOf(BeaconFlowStage.Idle) }
                var beaconEnableAttempted by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current
                var lineSetupDismissed by remember { mutableStateOf(false) }
                var lineLimitDismissed by remember { mutableStateOf(false) }
                val orderedLines = remember(lines) {
                    lines.sortedWith(
                        compareBy<com.pulselink.domain.model.SmsLine> { it.phoneNumber.ifBlank { "~" } }
                            .thenBy { it.createdAt }
                    )
                }
                val maxLines = 2
                val showLineLimit = isPremium && orderedLines.size > maxLines && !lineLimitDismissed
                val showLineSetup = isPremium && orderedLines.size > 1 &&
                    !state.settings.lineInboxModeChosen && !lineSetupDismissed && !showLineLimit
                var lineSetupMode by remember { mutableStateOf(state.settings.lineInboxMode) }
                var lineSetupDefaultLineId by remember { mutableStateOf(state.settings.defaultSendLineId) }
                var lineSetupSendPreference by remember { mutableStateOf(state.settings.lineSendPreference) }
                var lineSetupPhone by remember { mutableStateOf(state.settings.devicePhoneNumber ?: "") }
                val refreshDefaultSms = remember(defaultSmsHelper) {
                    suspend refresh@{
                        val existing = defaultSmsCheck
                        if (existing != null && existing.isActive) {
                            return@refresh existing.await()
                        }
                        val deferred = scope.async {
                            isCheckingDefaultSms = true
                            try {
                                val latest = defaultSmsHelper.checkDefaultSmsWithRetry()
                                isDefaultSms = latest
                                latest
                            } finally {
                                isCheckingDefaultSms = false
                                defaultSmsCheck = null
                            }
                        }
                        defaultSmsCheck = deferred
                        deferred.await()
                    }
                }
                val smsPermLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    missingSmsPerms = requiredSmsPermissions(context)
                    if (showBeaconAssist) {
                        beaconAssistState = beaconAssistState.copy(
                            smsPermissionsGranted = missingSmsPerms.isEmpty(),
                            message = if (missingSmsPerms.isEmpty()) {
                                "SMS permissions granted."
                            } else {
                                context.getString(R.string.beacon_flow_permissions_needed)
                            }
                        )
                    }
                    if (pendingInboxNav) {
                        inboxShortcutFlow.tryEmit(Unit)
                    }
                }
                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    scope.launch {
                        if (showBeaconAssist) {
                            beaconAssistState = beaconAssistState.copy(
                                message = "Verifying default SMS status...",
                                error = null
                            )
                        }
                        val latest = refreshDefaultSms()
                        missingSmsPerms = requiredSmsPermissions(context)
                        if (showBeaconAssist) {
                            beaconAssistState = beaconAssistState.copy(
                                defaultSmsGranted = latest,
                                message = if (latest) {
                                    context.getString(R.string.settings_default_sms_ready)
                                } else {
                                    context.getString(R.string.settings_default_sms_required)
                                }
                            )
                        }
                        if (pendingInboxNav) {
                            inboxShortcutFlow.tryEmit(Unit)
                        }
                    }
                }
                val launchBeaconInbox: () -> Unit = {
                    pendingInboxNav = true
                    beaconFlowStage = BeaconFlowStage.Idle
                    showBeaconAssist = true
                    beaconAssistState = BeaconAssistState(
                        iconEnabled = state.settings.beaconLauncherEnabled,
                        defaultSmsGranted = isDefaultSms,
                        smsPermissionsGranted = missingSmsPerms.isEmpty(),
                        message = context.getString(R.string.settings_beacon_title)
                    )
                    if (!state.settings.beaconLauncherEnabled) {
                        viewModel.setBeaconLauncherEnabled(true)
                    }
                    inboxShortcutFlow.tryEmit(Unit)
                }
                val initialInboxShortcut = intent?.getBooleanExtra("open_sms_inbox", false) == true
                val requestDefaultSms = remember(defaultSmsLauncher) {
                    {
                        val intent = defaultSmsHelper.buildRoleRequestIntent()
                        if (intent != null) {
                            beaconFlowStage = BeaconFlowStage.RequestedDefault
                            defaultSmsLauncher.launch(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "Default SMS role not available on this device.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    refreshDefaultSms()
                    missingSmsPerms = requiredSmsPermissions(context)
                    if (initialInboxShortcut) inboxShortcutFlow.tryEmit(Unit)
                }

                LaunchedEffect(
                    showLineSetup,
                    orderedLines,
                    state.settings.lineInboxMode,
                    state.settings.defaultSendLineId,
                    state.settings.lineSendPreference,
                    state.settings.devicePhoneNumber
                ) {
                    if (showLineSetup) {
                        lineSetupMode = state.settings.lineInboxMode
                        val fallbackDefault = state.settings.defaultSendLineId
                            ?: orderedLines.firstOrNull()?.id
                        lineSetupDefaultLineId = fallbackDefault
                        lineSetupSendPreference = state.settings.lineSendPreference
                        lineSetupPhone = state.settings.devicePhoneNumber ?: ""
                    }
                }

                // Refresh default-SMS status when returning from system settings or role dialog.
                DisposableEffect(lifecycleOwner, activeLineId, deviceLineId, isPremium) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            scope.launch {
                                refreshDefaultSms()
                                missingSmsPerms = requiredSmsPermissions(context)
                                if (isPremium) {
                                    linesViewModel.touchPresence(activeLineId ?: deviceLineId)
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                LaunchedEffect(state.settings.beaconLauncherEnabled, state.settings.themePreferences.inboxIconVariant) {
                    updateBeaconLauncher(
                        state.settings.beaconLauncherEnabled,
                        state.settings.themePreferences.inboxIconVariant
                    )
                }
                LaunchedEffect(navController) {
                    inboxShortcutFlow.collectLatest {
                        pendingInboxNav = true
                        val isNowDefault = refreshDefaultSms()
                        val missingNow = requiredSmsPermissions(context)
                        missingSmsPerms = missingNow
                        val currentBeaconEnabled = viewModel.uiState.value.settings.beaconLauncherEnabled
                        if (showBeaconAssist) {
                            beaconAssistState = beaconAssistState.copy(
                                iconEnabled = currentBeaconEnabled,
                                defaultSmsGranted = isNowDefault,
                                smsPermissionsGranted = missingNow.isEmpty(),
                                message = context.getString(R.string.settings_beacon_subtitle),
                                error = null
                            )
                        }
                        val shouldEnable = currentBeaconEnabled
                        val iconVariant = viewModel.uiState.value.settings.themePreferences.inboxIconVariant
                        updateBeaconLauncher(shouldEnable, iconVariant)

                        // Policy: default-SMS prompt must precede runtime SMS permissions.
                        if (!isNowDefault) {
                            if (beaconFlowStage == BeaconFlowStage.RequestedDefault) {
                                // User declined or canceled; keep other features, hide Beacon launcher.
                                pendingInboxNav = false
                                beaconFlowStage = BeaconFlowStage.Idle
                                // Do not disable the launcher automatically, allowing retry.
                                if (showBeaconAssist) {
                                    beaconAssistState = beaconAssistState.copy(
                                        defaultSmsGranted = false,
                                        message = if (isCheckingDefaultSms) {
                                            "Verifying default SMS status..."
                                        } else {
                                            context.getString(R.string.settings_default_sms_required)
                                        },
                                        error = context.getString(R.string.beacon_flow_retry)
                                    )
                                }
                                return@collectLatest
                            }
                            beaconFlowStage = BeaconFlowStage.RequestedDefault
                            if (showBeaconAssist) {
                                beaconAssistState = beaconAssistState.copy(
                                    defaultSmsGranted = false,
                                    message = if (isCheckingDefaultSms) {
                                        "Verifying default SMS status..."
                                    } else {
                                        context.getString(R.string.settings_default_sms_required)
                                    },
                                    error = null
                                )
                            }
                            val intent = defaultSmsHelper.buildRoleRequestIntent()
                            if (intent != null) {
                                pendingInboxNav = true
                                defaultSmsLauncher.launch(intent)
                            } else {
                                pendingInboxNav = false
                                beaconFlowStage = BeaconFlowStage.Idle
                                if (showBeaconAssist) {
                                    beaconAssistState = beaconAssistState.copy(
                                        error = "Default SMS role not available on this device."
                                    )
                                }
                            }
                            return@collectLatest
                        }

                        if (!currentBeaconEnabled) {
                            if (showBeaconAssist) {
                                beaconAssistState = beaconAssistState.copy(
                                    message = context.getString(R.string.beacon_flow_enable_icon)
                                )
                            }
                            if (!beaconEnableAttempted) {
                                beaconEnableAttempted = true
                                viewModel.setBeaconLauncherEnabled(true)
                                inboxShortcutFlow.tryEmit(Unit)
                                return@collectLatest
                            } else {
                                pendingInboxNav = false
                                beaconFlowStage = BeaconFlowStage.Idle
                                if (showBeaconAssist) {
                                    beaconAssistState = beaconAssistState.copy(
                                        error = context.getString(R.string.beacon_flow_retry)
                                    )
                                }
                                return@collectLatest
                            }
                        }
                        beaconEnableAttempted = false

                        if (missingNow.isNotEmpty()) {
                            if (beaconFlowStage == BeaconFlowStage.RequestedPermissions) {
                                pendingInboxNav = false
                                beaconFlowStage = BeaconFlowStage.Idle
                                if (showBeaconAssist) {
                                    beaconAssistState = beaconAssistState.copy(
                                        error = context.getString(R.string.beacon_flow_permissions_needed)
                                    )
                                }
                                return@collectLatest
                            }
                            pendingInboxNav = true
                            beaconFlowStage = BeaconFlowStage.RequestedPermissions
                            if (showBeaconAssist) {
                                beaconAssistState = beaconAssistState.copy(
                                    smsPermissionsGranted = false,
                                    message = context.getString(R.string.beacon_flow_permissions_needed),
                                    error = null
                                )
                            }
                            smsPermLauncher.launch(missingNow.toTypedArray())
                            return@collectLatest
                        }

                        pendingInboxNav = false
                        beaconFlowStage = BeaconFlowStage.Idle
                        if (showBeaconAssist) {
                            beaconAssistState = beaconAssistState.copy(
                                iconEnabled = true,
                                smsPermissionsGranted = true,
                                message = context.getString(R.string.beacon_flow_ready),
                                error = null
                            )
                        }
                        navController.navigate("sms/inbox") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                        showBeaconAssist = false
                    }
                }
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
                var hasHandledOnboardingCompletionAd by rememberSaveable {
                    mutableStateOf(state.onboardingComplete)
                }

                LaunchedEffect(authState) {
                    if (authState is AuthState.Unauthenticated) {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
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
                        if (BuildConfig.PRO_FEATURES) {
                            add(Manifest.permission.SEND_SMS)
                            add(Manifest.permission.RECEIVE_SMS)
                            add(Manifest.permission.READ_SMS)
                        }
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
                var pendingUnusedRestrictionsCheck by remember { mutableStateOf(false) }
                var unusedAppRestrictionsStatus by rememberSaveable { mutableStateOf<Int?>(null) }
                // Unused-app restriction is now optional; treat as satisfied during onboarding.
                val unusedRestrictionsRequirementMet = true

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    pendingPermissionCheck = true
                }

                val callContactHandler: suspend (Contact) -> Unit = handler@ { contact ->
                    isPreparingCall = true
                    Toast.makeText(context, context.getString(R.string.call_preparing), Toast.LENGTH_SHORT).show()
                    val targetPhone = contact.primaryPhone()
                    if (targetPhone.isNullOrBlank()) {
                        isPreparingCall = false
                        Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
                        return@handler
                    }
                    val result = try {
                        viewModel.initiateCall(contact.id, targetPhone)
                    } finally {
                        isPreparingCall = false
                    }
                    when (result) {
                        CallInitiationResult.Ready -> {
                            Toast.makeText(context, context.getString(R.string.call_ready), Toast.LENGTH_SHORT).show()
                            val placed = placeCall(activity, contact, targetPhone, callStateMonitor) { duration ->
                                viewModel.notifyCallEnded(contact.id, duration)
                            }
                            if (!placed) {
                                Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        CallInitiationResult.Timeout -> {
                            Toast.makeText(context, context.getString(R.string.call_timeout), Toast.LENGTH_SHORT).show()
                            val placed = placeCall(activity, contact, targetPhone, callStateMonitor) { duration ->
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

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            pendingPermissionCheck = true
                            pendingUnusedRestrictionsCheck = true
                            // Refresh default-SMS status and auto-enable Beacon launcher if we just became default.
                            val wasDefault = isDefaultSms
                            scope.launch {
                                val isNowDefault = refreshDefaultSms()
                                // Only auto-enable if we transitioned from NOT default to DEFAULT.
                                // This respects the user's choice if they are already default but explicitly disabled the Beacon.
                                if (!wasDefault && isNowDefault && !viewModel.uiState.value.settings.beaconLauncherEnabled) {
                                    viewModel.setBeaconLauncherEnabled(true)
                                }
                            }
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
                        if (missing.isEmpty() && sanitizedName.isNotBlank() && dndGranted && unusedRestrictionsRequirementMet) {
                            onboardingNameDirty = false
                            if (ownerName != sanitizedName) {
                                viewModel.setOwnerName(sanitizedName)
                            }
                            viewModel.completeOnboarding()
                        }
                    }
                }

                LaunchedEffect(pendingUnusedRestrictionsCheck, context) {
                    if (pendingUnusedRestrictionsCheck) {
                        pendingUnusedRestrictionsCheck = false
                        val future = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
                        future.addListener(
                            {
                                val status = runCatching { future.get() }
                                    .getOrElse { UnusedAppRestrictionsConstants.ERROR }
                                unusedAppRestrictionsStatus = status
                            },
                            ContextCompat.getMainExecutor(context)
                        )
                    }
                }

                LaunchedEffect(state.onboardingComplete, currentRoute) {
                    val route = currentRoute.orEmpty()
                    val shouldNavigateHome = state.onboardingComplete &&
                        (route == "splash" || route.startsWith("onboarding_"))
                    if (shouldNavigateHome) {
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                LaunchedEffect(state.onboardingComplete, state.showAds) {
                    if (state.onboardingComplete && !hasHandledOnboardingCompletionAd) {
                        hasHandledOnboardingCompletionAd = true
                        if (state.showAds) {
                            appOpenAdController.maybeShow(activity)
                        }
                    } else if (!state.onboardingComplete) {
                        hasHandledOnboardingCompletionAd = false
                    }
                }

                LaunchedEffect(state.showAds) {
                    appOpenAdController.updateAvailability(state.showAds)
                }

                LaunchedEffect(state.settings.crashDetectionEnabled) {
                    val intent = Intent(context, com.pulselink.service.CrashDetectionService::class.java)
                    // Crash detection is temporarily disabled pending foreground service/location policy work.
                    context.stopService(intent)
                }

                val startDestination = remember(initialInboxShortcut) {
                    if (initialInboxShortcut) "sms/inbox" else "splash"
                }

                val bannerHeight = 50.dp
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if (state.showAds) bannerHeight else 0.dp)
                    ) {
                        NavHost(navController = navController, startDestination = startDestination) {
                    val premiumBranding = state.settings.premiumUnlocked ||
                        BuildConfig.PREMIUM_FEATURES ||
                        state.isProUser
                    composable("splash") {
                        val brandName = pulseBrandName(
                            isPremium = premiumBranding,
                            isPro = BuildConfig.PRO_FEATURES || state.settings.proUnlocked
                        )
                        val badgeText = when {
                            premiumBranding -> "Premium"
                            BuildConfig.PRO_FEATURES || state.settings.proUnlocked -> "Pro"
                            else -> null
                        }
                        SplashScreen(
                            usePremiumBranding = premiumBranding || BuildConfig.PRO_FEATURES || state.settings.proUnlocked,
                            brandName = brandName,
                            badgeText = badgeText
                        )
                        LaunchedEffect(authState, state.onboardingComplete) {
                            if (authState is AuthState.Loading) return@LaunchedEffect
                            delay(1200)
                            val destination = when (authState) {
                                is AuthState.Authenticated -> if (state.onboardingComplete) "home" else "onboarding_intro"
                                else -> "login"
                            }
                            navController.navigate(destination) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    composable("login") {
                        val loginViewModel: LoginViewModel = hiltViewModel()
                        val loginUiState by loginViewModel.uiState.collectAsStateWithLifecycle()
                        val activity = LocalContext.current as? MainActivity
                        val googleClient = remember {
                                GoogleSignIn.getClient(
                                    activity!!,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                )
                        }
                        val googleLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode != RESULT_OK) {
                                loginViewModel.reportExternalError()
                                return@rememberLauncherForActivityResult
                            }
                            try {
                                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                                val account = task.getResult(ApiException::class.java)
                                val idToken = account?.idToken
                                if (idToken != null) {
                                    loginViewModel.handleGoogleIdToken(idToken)
                                } else {
                                    loginViewModel.reportExternalError()
                                }
                            } catch (e: Exception) {
                                loginViewModel.reportExternalError()
                            }
                        }

                        LoginScreen(
                            state = loginUiState,
                            onEmailChange = loginViewModel::updateEmail,
                            onPasswordChange = loginViewModel::updatePassword,
                            onConfirmPasswordChange = loginViewModel::updateConfirmPassword,
                            onSubmit = loginViewModel::submit,
                            onToggleMode = loginViewModel::toggleMode,
                            onForgotPassword = loginViewModel::sendPasswordReset,
                            onSmsOnlyClick = loginViewModel::signInSmsOnly,
                            onGoogleClick = { googleLauncher.launch(googleClient.signInIntent) },
                            onMessageConsumed = loginViewModel::clearTransientMessages,
                            useProBranding = premiumBranding
                        )
                        LaunchedEffect(authState, state.onboardingComplete) {
                            val authenticated = authState as? AuthState.Authenticated
                            if (authenticated != null && !authenticated.user.isAnonymous) {
                                val destination = if (state.onboardingComplete) "home" else "onboarding_intro"
                                navController.navigate(destination) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                    composable("onboarding_intro") {
                        val requiresAgreement = viewModel.needsBetaAgreement(state.settings)
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
                                    val destination = if (requiresAgreement) {
                                        "onboarding_beta_agreement"
                                    } else {
                                        "onboarding_permissions"
                                    }
                                    navController.navigate(destination)
                                }
                            }
                        )
                    }
                    composable("onboarding_beta_agreement") {
                        val context = LocalContext.current
                        var submitting by remember { mutableStateOf(false) }
                        BetaAgreementScreen(
                            ownerName = onboardingName.ifBlank { ownerName },
                            agreementVersion = MainViewModel.BETA_AGREEMENT_VERSION,
                            isSubmitting = submitting,
                            onViewFullAgreement = { navController.navigate("onboarding_beta_agreement_full") },
                            onAgree = {
                                if (submitting) return@BetaAgreementScreen
                                submitting = true
                                viewModel.acceptBetaAgreement { success ->
                                    submitting = false
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.beta_agreement_agree_success),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        navController.navigate("onboarding_permissions") {
                                            popUpTo("onboarding_beta_agreement") { inclusive = true }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.beta_agreement_agree_error),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("onboarding_beta_agreement_full") {
                        BetaAgreementFullScreen(onBack = { navController.popBackStack() })
                    }
                    composable("onboarding_permissions") {
                        val missingPermissions = requiredPermissions.filter { perm ->
                            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
                        }

                        val hasDndAccess = notificationManager?.isNotificationPolicyAccessGranted == true

                LaunchedEffect(state.onboardingComplete, missingPermissions, onboardingName, hasDndAccess) {
                    val sanitized = onboardingName.trim()
                    if (
                        !state.onboardingComplete &&
                        missingPermissions.isEmpty() &&
                        sanitized.isNotBlank() &&
                        hasDndAccess &&
                        !viewModel.needsBetaAgreement(state.settings)
                    ) {
                                if (ownerName != sanitized) {
                                    viewModel.setOwnerName(sanitized)
                                }
                                onboardingNameDirty = false
                                viewModel.completeOnboarding()
                            }
                        }

                        val smsGranted = if (BuildConfig.PRO_FEATURES) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
                        } else true
                        val callPermissionGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                        val locationGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val contactsGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        val callLogGranted =
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

                        val permissionCards = buildList {
                            val title = if (BuildConfig.PRO_FEATURES) "SMS & Call" else "Call"
                            val description = if (BuildConfig.PRO_FEATURES)
                                "Allow PulseLink to send emergency messages and place calls."
                            else
                                "Allow PulseLink to place calls."

                            val manualHelp = if (!smsGranted || !callPermissionGranted) {
                                if (BuildConfig.PRO_FEATURES)
                                    "If SMS or Call stays disabled: open Settings -> Apps -> PulseLink -> Permissions, tap SMS and Phone, open the 3-dot menu, choose \"Allow disallowed permissions\", confirm with fingerprint or PIN, then switch both to Allow."
                                else
                                    "If Call stays disabled: open Settings -> Apps -> PulseLink -> Permissions, tap Phone, open the 3-dot menu, choose \"Allow disallowed permissions\", confirm with fingerprint or PIN, then switch to Allow."
                            } else null

                            OnboardingPermissionState(
                                icon = Icons.Filled.Call,
                                title = title,
                                description = description,
                                granted = smsGranted && callPermissionGranted,
                                manualHelp = manualHelp
                            ).also { add(it) }
                            OnboardingPermissionState(
                                icon = Icons.Filled.Lock,
                                title = "Override Silent / DND",
                                description = "Needed so critical alerts ring even when the phone is muted.",
                                granted = hasDndAccess,
                                actionLabel = if (hasDndAccess) "Manage" else "Allow",
                                onAction = { openDndSettings(context) }
                            ).also { add(it) }
                            OnboardingPermissionState(
                                icon = Icons.Filled.Person,
                                title = stringResource(R.string.permission_call_log_title),
                                description = stringResource(R.string.permission_call_log_description),
                                granted = callLogGranted,
                                manualHelp = if (!callLogGranted) {
                                    "Open Settings -> Apps -> PulseLink -> Permissions and allow Call logs so linked contacts can ring through."
                                } else null
                            ).also { add(it) }
                            OnboardingPermissionState(
                                icon = Icons.Filled.LocationOn,
                                title = "Location",
                                description = "Include precise location when you trigger an alert.",
                                granted = locationGranted
                            ).also { add(it) }
                            OnboardingPermissionState(
                                icon = Icons.Filled.Person,
                                title = "Contacts",
                                description = "Link trusted partners so they receive your alerts.",
                                granted = contactsGranted
                            ).also { add(it) }
                        }

                        val sanitizedOnboardingName = onboardingName.trim()
                        val canContinue = missingPermissions.isEmpty() &&
                            sanitizedOnboardingName.isNotBlank() &&
                            hasDndAccess &&
                            !viewModel.needsBetaAgreement(state.settings)

                        OnboardingScreen(
                            permissions = permissionCards,
                            focusedPermission = null,
                            isReadyToFinish = canContinue,
                            onGrantPermissions = {
                                if (missingPermissions.isEmpty()) {
                                    val currentName = onboardingName.trim()
                                    if (!hasDndAccess) {
                                        openDndSettings(context)
                                    } else if (!unusedRestrictionsRequirementMet) {
                                        pendingUnusedRestrictionsCheck = true
                                        openUnusedAppRestrictionsSettings(context)
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
                                    val missingSms = missingPermissions.any { it.contains("SMS") }
                                    if (missingSms && !defaultSmsHelper.isDefaultSms()) {
                                        requestDefaultSms()
                                    } else {
                                        permissionLauncher.launch(missingPermissions.toTypedArray())
                                    }
                                }
                            },
                            onOpenAppSettings = { openAppSettings(context) },
                            onBack = { navController.popBackStack() },
                            extraSection = {
                                OtpCleanupOnboardingCard(
                                    enabled = state.settings.otpCleanupEnabled,
                                    days = state.settings.otpCleanupDays,
                                    onToggle = viewModel::setOtpCleanupEnabled,
                                    onChangeDays = viewModel::setOtpCleanupDays
                                )
                            }
                        )
                    }
                    composable("home") {
                        val isSmsOnlyUser = (authState as? AuthState.Authenticated)?.user?.isAnonymous == true
                        HomeScreen(
                            state = state,
                            onDismissAssistantShortcuts = viewModel::dismissAssistantHint,
                            onTriggerEmergency = viewModel::triggerEmergency,
                            onSendCheckIn = viewModel::sendCheckIn,
                            onSettingsClick = { navController.navigate("settings") },
                            onFaqClick = { navController.navigate("faq") },
                            onBeaconClick = launchBeaconInbox,
                            showBeaconIcon = state.settings.beaconLauncherEnabled,
                            showBeaconHint = !state.settings.beaconHintDismissed,
                            onBeaconHintDismiss = { viewModel.setBeaconHintDismissed(true) },
                            onBeaconHintDisable = {
                                viewModel.setBeaconHintDismissed(true)
                                viewModel.setBeaconLauncherEnabled(false)
                            },
                            onBeaconHintUse = {
                                viewModel.setBeaconHintDismissed(true)
                                requestDefaultSms()
                            },
                            showWebAccessHint = !state.settings.webAccessHintDismissed && isPremium,
                            onWebAccessHintDismiss = { viewModel.setWebAccessHintDismissed(true) },
                            onWebAccessHintAction = {
                                viewModel.setWebAccessHintDismissed(true)
                                if (isPremium) {
                                    // Navigate to Beacon settings
                                    launchBeaconInbox()
                                } else {
                                    // Navigate to upgrade screen
                                    navController.navigate("account_settings")
                                }
                            },
                            onAddContact = viewModel::saveContact,
                            onContactSelected = { contactId -> navController.navigate("contact/$contactId") },
                            onContactSettings = { contactId -> navController.navigate("contact/$contactId/settings") },
                            onSendLink = { contactId ->
                                state.contacts.firstOrNull { it.id == contactId }?.let { sendLinkOrInvite(it) }
                            },
                            onApproveLink = viewModel::approveLink,
                            onCallContact = callContactHandler,
                            onReorderContacts = viewModel::reorderContacts,
                            onRequestCancelEmergency = cancelEmergencyHandler,
                            onViewEmergencyMap = { navController.navigate("emergency_map") },
                            isCancelingEmergency = isCancelingEmergency,
                            onAlertsClick = { navController.navigate("alerts_history") },
                            showAddLoginPrompt = isSmsOnlyUser,
                            onAddLoginClick = {
                                navController.navigate("login") {
                                    launchSingleTop = true
                                }
                            },
                            onUpgradeClick = {
                                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=com.pulselink.pro")
                                    setPackage("com.android.vending")
                                }
                                try {
                                    startActivity(playStoreIntent)
                                } catch (e: ActivityNotFoundException) {
                                    // Fallback to a browser if the Play Store app is not installed
                                    playStoreIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.pulselink.pro")
                                    playStoreIntent.setPackage(null)
                                    startActivity(playStoreIntent)
                                }
                            },
                            brandName = pulseDisplayName,
                            isPremium = isPremium,
                            isPro = isPro
                        )
                    }
                    composable("alerts_history") {
                        AlertHistoryScreen(
                            state = state,
                            onBackClick = { navController.popBackStack() },
                            onMarkAlertsAsRead = { ids -> viewModel.markAlertsAsRead(ids) },
                            onViewEmergencyMap = { navController.navigate("emergency_map") }
                        )
                    }
                    composable("emergency_map") {
                        EmergencyMapScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "contact/{contactId}",
                        arguments = listOf(navArgument("contactId") { type = NavType.LongType })
                    ) { entry ->
                        val contactId = entry.arguments?.getLong("contactId") ?: return@composable
                        val contact = state.contacts.firstOrNull { it.id == contactId }
                        val conversationViewModel: ContactConversationViewModel = hiltViewModel()
                        ContactConversationScreen(
                            contact = contact,
                            isProUser = state.isProUser,
                            showAds = state.showAds,
                            viewModel = conversationViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenSettings = { navController.navigate("contact/$contactId/settings") },
                            onCallContact = callContactHandler,
                            onPing = { viewModel.sendPing(contactId) },
                            onVoiceCommand = { query -> viewModel.processVoiceCommand(query) },
                            onUpgradeClick = {
                                val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("market://details?id=com.pulselink.pro")
                                    setPackage("com.android.vending")
                                }
                                try {
                                    startActivity(playStoreIntent)
                                } catch (e: ActivityNotFoundException) {
                                    // Fallback to a browser if the Play Store app is not installed
                                    playStoreIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.pulselink.pro")
                                    playStoreIntent.setPackage(null)
                                    startActivity(playStoreIntent)
                                }
                            },
                            brandName = pulseDisplayName,
                            isPremium = isPremium,
                            isPro = isPro
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
                            showAds = state.showAds,
                            onBack = { navController.popBackStack() },
                            onCallContact = callContactHandler,
                            onEditContact = { newName, newPhone, newEmail ->
                                contact?.let {
                                    val updated = it.copy(
                                        displayName = newName,
                                        phoneNumber = newPhone,
                                        email = newEmail
                                    )
                                    viewModel.saveContact(updated)
                                }
                            },
                            onEditEmergencyAlert = { navController.navigate("alerts/contact/$contactId/emergency") },
                            onEditCheckInAlert = { navController.navigate("alerts/contact/$contactId/checkin") },
                            onToggleLocation = { enabled -> contact?.let { viewModel.updateContact(it.copy(includeLocation = enabled)) } },
                            onToggleCamera = { enabled -> contact?.let { viewModel.updateContact(it.copy(cameraEnabled = enabled)) } },
                            onToggleAutoCall = { enabled -> contact?.let { viewModel.updateContact(it.copy(autoCall = enabled)) } },
                            onToggleFavorite = { enabled -> contact?.let { viewModel.updateContact(it.copy(isFavorite = enabled)) } },
                            onTogglePrivate = { enabled -> contact?.let { viewModel.updateContact(it.copy(isPrivate = enabled)) } },
                            onToggleRemoteOverride = { allow -> viewModel.setRemoteOverridePermission(contactId, allow) },
                            onToggleRemoteSound = { allow -> viewModel.setRemoteSoundPermission(contactId, allow) },
                            onSendLink = {
                                contact?.let { sendLinkOrInvite(it) }
                            },
                            onApproveLink = { viewModel.approveLink(contactId) },
                            onSetRemotePin = { pin -> viewModel.setRemotePin(contactId, pin) },
                            onPing = { viewModel.sendPing(contactId) },
                            onDelete = {
                                viewModel.deleteContact(contactId)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable(
                        route = "contact/new?phone={phone}&name={name}",
                        arguments = listOf(
                            navArgument("phone") { type = NavType.StringType; nullable = true; defaultValue = "" },
                            navArgument("name") { type = NavType.StringType; nullable = true; defaultValue = "" }
                        )
                    ) { entry ->
                        val phoneArg = entry.arguments?.getString("phone").orEmpty()
                        val nameArg = entry.arguments?.getString("name").orEmpty()
                        var publicProfile by remember { mutableStateOf<PublicProfile?>(null) }
                        var profileLoading by remember { mutableStateOf(false) }
                        var pendingNavigatePhone by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(phoneArg) {
                            if (phoneArg.isBlank()) return@LaunchedEffect
                            profileLoading = true
                            publicProfile = viewModel.lookupPublicProfileByPhone(phoneArg)
                            profileLoading = false
                        }

                        LaunchedEffect(state.contacts, pendingNavigatePhone) {
                            val pending = pendingNavigatePhone ?: return@LaunchedEffect
                            val resolved = state.contacts.firstOrNull { contact ->
                                normalizeSmsAddress(contact.phoneNumber) == pending ||
                                    contact.additionalPhones.any { normalizeSmsAddress(it) == pending }
                            }
                            if (resolved != null) {
                                pendingNavigatePhone = null
                                navController.navigate("contact/${resolved.id}/settings") {
                                    popUpTo("contact/new?phone={phone}&name={name}") { inclusive = true }
                                }
                            }
                        }

                        val suggestedName = publicProfile?.displayName?.takeIf { it.isNotBlank() } ?: nameArg
                        ContactCreateScreen(
                            initialName = suggestedName,
                            initialPhone = phoneArg,
                            initialEmail = publicProfile?.email,
                            initialAvatarUrl = publicProfile?.avatarUrl,
                            profileLoading = profileLoading,
                            onSave = { newName, newPhone, newEmail, avatarUrl ->
                                val contact = Contact(
                                    displayName = newName,
                                    phoneNumber = newPhone,
                                    email = newEmail,
                                    avatarUrl = avatarUrl,
                                    remoteDisplayName = publicProfile?.displayName
                                )
                                viewModel.saveContact(contact)
                                pendingNavigatePhone = normalizeSmsAddress(newPhone)
                            },
                            onBack = { navController.popBackStack() }
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
                    composable("alerts/default/emergency_vibration") {
                        VibrationPatternPickerScreen(
                            title = "Emergency vibration pattern",
                            subtitle = "Select the vibration style for emergency alerts.",
                            options = VibrationPatterns.alertOptions,
                            selectedKey = state.settings.emergencyProfile.vibrationPatternKey,
                            onSelect = { key ->
                                viewModel.updateEmergencyVibrationPattern(
                                    key ?: VibrationPatterns.ALERT_DEFAULT
                                )
                            },
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
                    composable("alerts/default/checkin_vibration") {
                        VibrationPatternPickerScreen(
                            title = "Check-in vibration pattern",
                            subtitle = "Select the vibration style for check-in alerts.",
                            options = VibrationPatterns.alertOptions,
                            selectedKey = state.settings.checkInProfile.vibrationPatternKey,
                            onSelect = { key ->
                                viewModel.updateCheckInVibrationPattern(
                                    key ?: VibrationPatterns.ALERT_DEFAULT
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("alerts/default/call") {
                        val callTitle = stringResource(id = R.string.settings_call_tone_title)
                        val callSubtitle = stringResource(id = R.string.settings_call_tone_subtitle)
                        AlertTonePickerScreen(
                            title = callTitle,
                            subtitle = callSubtitle,
                            options = state.callSoundOptions,
                            selectedKey = state.settings.callSoundKey,
                            onSelect = viewModel::updateCallSound,
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
                        val messageSoundLabel = if (state.settings.messageNotificationSoundUri.isNullOrBlank()) {
                            "Phone default notification"
                        } else {
                            "Custom audio"
                        }
                        val messageVibrationLabel = VibrationPatterns
                            .messageOption(state.settings.messageNotificationVibrationPattern)
                            .label
                        val emergencyVibrationLabel = VibrationPatterns
                            .alertOption(state.settings.emergencyProfile.vibrationPatternKey)
                            .label
                        val checkInVibrationLabel = VibrationPatterns
                            .alertOption(state.settings.checkInProfile.vibrationPatternKey)
                            .label
                        val isSmsOnlyUser = (authState as? AuthState.Authenticated)?.user?.isAnonymous == true
                        SettingsScreen(
                            settings = state.settings,
                            hasDndAccess = hasDndAccess,
                            showAds = state.showAds,
                            isProUser = state.isProUser,
                            isDefaultSmsApp = isDefaultSms,
                            defaultSmsSupported = defaultSmsHelper.buildRoleRequestIntent() != null || isDefaultSms,
                            beaconLauncherEnabled = state.settings.beaconLauncherEnabled,
                            onToggleIncludeLocation = viewModel::setIncludeLocation,
                            onToggleCrashDetection = viewModel::setCrashDetectionEnabled,
                            onRequestDndAccess = { openDndSettings(context) },
                            onRequestBatteryOpt = { openBatteryOptimizationSettings(context) },
                            onRequestUnusedApps = { openUnusedAppRestrictionsSettings(context) },
                            onToggleAutoAllowRemoteSoundChange = viewModel::setAutoAllowRemoteSoundChange,
                            onToggleAutoUpdateContactInfo = viewModel::setAutoUpdateContactInfo,
                            onToggleFirebaseMessaging = viewModel::setFirebaseMessagingEnabled,
                            onToggleEmailFallback = viewModel::setEmailFallbackEnabled,
                            onRequestDefaultSms = requestDefaultSms,
                            onToggleBeaconLauncher = { enabled -> viewModel.setBeaconLauncherEnabled(enabled) },
                            onSyncNow = viewModel::syncContactsNow,
                            profileUpdateState = state.profileUpdate,
                            onBroadcastProfileUpdate = viewModel::broadcastProfileToContacts,
                            onEditEmergencyTone = { navController.navigate("alerts/default/emergency") },
                            onEditCheckInTone = { navController.navigate("alerts/default/checkin") },
                            onEditCallTone = { navController.navigate("alerts/default/call") },
                            messageSoundLabel = messageSoundLabel,
                            messageVibrate = state.settings.messageNotificationVibrate,
                            onEditMessageSound = { navController.navigate("notifications/message_sound") },
                            messageVibrationLabel = messageVibrationLabel,
                            onEditMessageVibration = {
                                navController.navigate("notifications/message_vibration")
                            },
                            emergencyVibrationLabel = emergencyVibrationLabel,
                            onEditEmergencyVibration = {
                                navController.navigate("alerts/default/emergency_vibration")
                            },
                            checkInVibrationLabel = checkInVibrationLabel,
                            onEditCheckInVibration = {
                                navController.navigate("alerts/default/checkin_vibration")
                            },
                            onToggleMessageVibrate = viewModel::updateMessageNotificationVibrate,
                            onReportBug = { navController.navigate("bug_report") },
                            onBetaTesters = { navController.navigate("beta_testers") },
                            onOpenHelp = { navController.navigate("settings_help") },
                            onOpenBeacon = launchBeaconInbox,
                            onEditProfile = { navController.navigate("profile_settings") },
                            onOpenThemes = { navController.navigate("visual_settings") },
                            showAddLogin = isSmsOnlyUser,
                            onAddLogin = { navController.navigate("login") },
                            onSignOut = {
                                viewModel.signOut()
                            },
                            onOpenExtensionsStore = { navController.navigate("extensions_store") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("extensions_store") {
                        ExtensionsStoreScreen(
                            settings = state.settings,
                            onToggleBeaconLauncher = { enabled -> viewModel.setBeaconLauncherEnabled(enabled) },
                            onToggleFirebaseMessaging = viewModel::setFirebaseMessagingEnabled,
                            onToggleEmailFallback = viewModel::setEmailFallbackEnabled,
                            onToggleCrashDetection = viewModel::setCrashDetectionEnabled,
                            onToggleOtpCleanup = viewModel::setOtpCleanupEnabled,
                            onToggleRemoteWebAccess = viewModel::setRemoteWebAccess,
                            onToggleAiSummaries = viewModel::setAiSummariesEnabled,
                            onToggleThirdPartyExtensions = viewModel::setThirdPartyExtensionsEnabled,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "notifications/message_sound?address={address}",
                        arguments = listOf(navArgument("address") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { entry ->
                        val addressArg = entry.arguments?.getString("address")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { Uri.decode(it) }
                        val normalized = addressArg?.let { normalizeSmsAddress(it) }
                        val overrideUri = normalized?.let { state.settings.messageNotificationSoundOverrides[it] }
                        val isContact = addressArg != null
                        MessageNotificationSoundScreen(
                            title = if (isContact) "Notification sound" else "Message notification sound",
                            subtitle = if (isContact) {
                                "Overrides the global sound for this conversation."
                            } else {
                                "Choose the sound for incoming texts."
                            },
                            defaultLabel = if (isContact) "Use global message sound" else "Phone default notification",
                            currentSoundUri = if (isContact) overrideUri else state.settings.messageNotificationSoundUri,
                            onSelectDefault = {
                                if (addressArg != null) {
                                    viewModel.updateMessageNotificationOverride(addressArg, null)
                                } else {
                                    viewModel.updateMessageNotificationSound(null)
                                }
                            },
                            onSelectCustom = { uri ->
                                if (addressArg != null) {
                                    viewModel.updateMessageNotificationOverride(addressArg, uri.toString())
                                } else {
                                    viewModel.updateMessageNotificationSound(uri.toString())
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "notifications/message_vibration?address={address}",
                        arguments = listOf(navArgument("address") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { entry ->
                        val addressArg = entry.arguments?.getString("address")
                            ?.takeIf { it.isNotBlank() }
                            ?.let { Uri.decode(it) }
                        val normalized = addressArg?.let { normalizeSmsAddress(it) }
                        val overrideKey = normalized?.let { state.settings.messageNotificationVibrationOverrides[it] }
                        val isContact = addressArg != null
                        VibrationPatternPickerScreen(
                            title = if (isContact) "Notification vibration" else "Message vibration pattern",
                            subtitle = if (isContact) {
                                "Overrides the global pattern for this conversation."
                            } else {
                                "Choose the vibration style for incoming texts."
                            },
                            options = VibrationPatterns.messageOptions,
                            selectedKey = if (isContact) overrideKey else state.settings.messageNotificationVibrationPattern,
                            defaultLabel = if (isContact) "Use global message pattern" else null,
                            onSelect = { key ->
                                if (addressArg != null) {
                                    viewModel.updateMessageNotificationVibrationOverride(addressArg, key)
                                } else {
                                    viewModel.updateMessageNotificationVibrationPattern(
                                        key ?: VibrationPatterns.MESSAGE_DEFAULT
                                    )
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("profile_settings") {
                        val deleteAccountState by viewModel.deleteAccountState.collectAsStateWithLifecycle()
                        val authenticated = authState as? AuthState.Authenticated
                        val ownerEmail = authenticated?.user?.email
                        val ownerPhone = authenticated?.user?.phoneNumber ?: state.settings.devicePhoneNumber
                        ProfileSettingsScreen(
                            settings = state.settings,
                            ownerEmail = ownerEmail,
                            ownerPhone = ownerPhone,
                            deleteAccountState = deleteAccountState,
                            onSaveName = viewModel::setOwnerName,
                            onSaveAvatar = viewModel::setOwnerAvatarUrl,
                            onDeleteAccount = viewModel::deleteAccount,
                            onResetDeleteAccountState = viewModel::resetDeleteAccountState,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("sms/inbox") {
                        val smsInboxViewModel: SmsInboxViewModel = hiltViewModel()
                        val threads by smsInboxViewModel.threads.collectAsStateWithLifecycle()
                        val archivedThreads by smsInboxViewModel.archived.collectAsStateWithLifecycle()
                        val inboxBusy by smsInboxViewModel.isDatabaseBusy.collectAsStateWithLifecycle()
                        val contactsByNumber = remember(state.contacts) {
                            val map = mutableMapOf<String, Contact>()
                            state.contacts.forEach { contact ->
                                val numbers = listOf(contact.phoneNumber) + contact.additionalPhones
                                numbers.filter { it.isNotBlank() }.forEach { number ->
                                    map.putIfAbsent(normalizeSmsAddress(number), contact)
                                }
                            }
                            map.toMap()
                        }
                        val lifecycleOwner = LocalLifecycleOwner.current
                        var notificationsEnabled by remember {
                            mutableStateOf(MessageNotificationManager.areNotificationsEnabled(context))
                        }
                        var notificationsSilent by remember {
                            mutableStateOf(MessageNotificationManager.isMessageChannelSilent(context))
                        }
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    notificationsEnabled = MessageNotificationManager.areNotificationsEnabled(context)
                                    notificationsSilent = MessageNotificationManager.isMessageChannelSilent(context)
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }
                        LaunchedEffect(Unit) { smsInboxViewModel.refresh() }
                        LaunchedEffect(isDefaultSms, missingSmsPerms) {
                            if (isDefaultSms || missingSmsPerms.none { it == Manifest.permission.READ_SMS }) {
                                smsInboxViewModel.refresh()
                            }
                        }
                        SmsInboxScreen(
                            threads = threads,
                            archivedThreads = archivedThreads,
                            onOpenThread = { thread ->
                                val lineSuffix = thread.lineId?.let { Uri.encode(it) }.orEmpty()
                                navController.navigate(
                                    "sms/thread/${thread.threadId}/${Uri.encode(thread.address)}?lineId=$lineSuffix"
                                )
                            },
                            onOpenContactForThread = { thread ->
                                val contact = contactsByNumber[normalizeSmsAddress(thread.address)]
                                if (contact != null) {
                                    navController.navigate("contact/${contact.id}/settings")
                                } else {
                                    val (displayName, number) = splitSmsDisplayAddress(thread.address)
                                    val phone = (number ?: displayName).trim()
                                    val encodedPhone = Uri.encode(phone)
                                    val encodedName = displayName
                                        .takeIf { it.isNotBlank() && it != phone }
                                        ?.let { Uri.encode(it) }
                                        .orEmpty()
                                    navController.navigate("contact/new?phone=$encodedPhone&name=$encodedName")
                                }
                            },
                            onArchiveThread = { thread -> smsInboxViewModel.archive(thread.threadId) },
                            onUnarchiveThread = { thread -> smsInboxViewModel.unarchive(thread.threadId) },
                            onDeleteThread = { thread -> smsInboxViewModel.delete(thread.threadId) },
                            onBack = { navController.popBackStack() },
                            dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                            onOpenSettings = { navController.navigate("settings") },
                            lineOptions = if (isPremium) orderedLines else emptyList(),
                            deviceLineId = deviceLineId,
                            activeLineId = if (isPremium) activeLineId else null,
                            onSelectLine = { selected ->
                                linesViewModel.setActiveLineId(selected)
                                linesViewModel.touchPresence(selected)
                            },
                            showLinePicker = isPremium && state.settings.lineInboxMode == LineInboxMode.PER_LINE,
                            hideOtpInAll = true,
                            onImportAll = { smsInboxViewModel.importAllMessages() },
                            isDatabaseBusy = inboxBusy,
                            contactsByNumber = contactsByNumber,
                            isPremium = isPremium,
                            banner = {
                                if (!notificationsEnabled || notificationsSilent) {
                                    Surface(
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    text = if (!notificationsEnabled) {
                                                        "Message notifications are off"
                                                    } else {
                                                        "Message alerts are silent"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = if (!notificationsEnabled) {
                                                        "Turn on notifications so Beacon can alert you."
                                                    } else {
                                                        "Enable sound or vibration for incoming texts."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            OutlinedButton(onClick = {
                                                val intent = MessageNotificationManager.buildNotificationSettingsIntent(context)
                                                context.startActivity(intent)
                                            }) {
                                                Text("Open")
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    composable(
                        route = "sms/thread/{threadId}/{address}?lineId={lineId}",
                        arguments = listOf(
                            navArgument("threadId") { type = NavType.LongType },
                            navArgument("address") { type = NavType.StringType },
                            navArgument("lineId") {
                                type = NavType.StringType
                                defaultValue = ""
                            }
                        )
                    ) { entry ->
                        val threadId = entry.arguments?.getLong("threadId") ?: return@composable
                        val address = entry.arguments?.getString("address") ?: ""
                        val lineId = entry.arguments?.getString("lineId")?.takeIf { it.isNotBlank() }
                        val threadViewModel: SmsThreadViewModel = hiltViewModel()
                        val messages by threadViewModel.messages.collectAsStateWithLifecycle()
                        val contact by threadViewModel.contact.collectAsStateWithLifecycle()
                        val isArchived by threadViewModel.isArchived.collectAsStateWithLifecycle()
                        val threadBusy by threadViewModel.isDatabaseBusy.collectAsStateWithLifecycle()
                        val summaryState by threadViewModel.summaryState.collectAsStateWithLifecycle()
                        val composeState by threadViewModel.composeState.collectAsStateWithLifecycle()
                        val decodedAddress = Uri.decode(address)
                        val premiumActive = BuildConfig.PREMIUM_FEATURES || state.settings.premiumUnlocked
                        val threadLineId = lineId ?: deviceLineId
                        val threadKey = "${threadLineId ?: deviceLineId}:$threadId"
                        val fallbackLineId = defaultSendLineId ?: threadLineId ?: deviceLineId
                        val selectedLine = when (lineSendPreference) {
                            LineSendPreference.DEVICE_DEFAULT -> deviceLineId
                            LineSendPreference.LINE_DEFAULT -> fallbackLineId
                            LineSendPreference.LAST_USED -> threadLineOverrides[threadKey] ?: fallbackLineId
                        }
                        LaunchedEffect(threadId, decodedAddress, lineId) {
                            threadViewModel.load(threadId, decodedAddress, lineId)
                        }
                        SmsThreadScreen(
                            address = decodedAddress,
                            messages = messages,
                            contact = contact,
                            onBack = { navController.popBackStack() },
                            dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                            globalTheme = state.settings.themePreferences,
                            onUpdateContactTheme = { /* no-op for SMS inbox contacts */ },
                            onCustomizeTheme = { navController.navigate("visual_settings") },
                            onEditNotificationSound = {
                                navController.navigate("notifications/message_sound?address=${Uri.encode(decodedAddress)}")
                            },
                            onEditNotificationVibration = {
                                navController.navigate("notifications/message_vibration?address=${Uri.encode(decodedAddress)}")
                            },
                            onSendMessage = { body, sendLineId ->
                                threadViewModel.sendMessage(decodedAddress, body, sendLineId)
                            },
                            lineOptions = if (isPremium) orderedLines else emptyList(),
                            selectedLineId = if (isPremium) selectedLine else null,
                            deviceLineId = deviceLineId,
                            lineStatus = if (isPremium) {
                                val now = System.currentTimeMillis()
                                val status = lineDevices
                                    .groupBy { it.lineId }
                                    .mapValues { (_, devices) ->
                                        devices.any { device ->
                                            device.lastSeen?.let { last ->
                                                now - last < 5 * 60 * 1000  // 5 minute threshold (increased from 2 min)
                                            } == true
                                        }
                                    }
                                    .toMutableMap()
                                if (deviceLineId.isNotBlank()) {
                                    status[deviceLineId] = true
                                }
                                status
                            } else {
                                emptyMap()
                            },
                            onSelectLine = { selected ->
                                linesViewModel.setThreadLineOverride(threadKey, selected)
                            },
                            isArchived = isArchived,
                            onToggleArchive = { threadViewModel.toggleArchive() },
                            isDatabaseBusy = threadBusy,
                            aiSummaryState = summaryState,
                            onRequestSummary = { threadViewModel.requestSummary() },
                            onClearSummary = { threadViewModel.clearSummary() },
                            aiComposeState = composeState,
                            onRequestCompose = { action, draft, last ->
                                threadViewModel.requestCompose(action, draft, last)
                            },
                            onClearCompose = { threadViewModel.clearCompose() },
                            aiSummaryEnabled = premiumActive && state.settings.aiSummariesEnabled,
                            aiComposeEnabled = premiumActive && state.settings.aiComposeEnabled
                        )
                    }
                    composable("settings_help") {
                        SettingsHelpScreen(
                            onBack = { navController.popBackStack() },
                            onOpenFaq = { navController.navigate("faq") }
                        )
                    }
                    composable("faq") {
                        FaqScreen(onBack = { navController.popBackStack() })
                    }
                    composable("visual_settings") {
                        val currentTheme = state.settings.themePreferences
                        VisualSettingsScreen(
                            theme = currentTheme,
                            onSelectTheme = { newTheme -> viewModel.setThemePreferences(newTheme) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("private_pin") {
                        PrivatePinScreen(
                            hasPin = state.settings.privatePinHash != null,
                            onSavePin = { newPin ->
                                val hashed = newPin?.let { com.pulselink.util.hashPin(it) }
                                viewModel.setPrivatePinHash(hashed)
                                navController.popBackStack()
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("bug_report") {
                        BugReportWebScreen(
                            url = MainViewModel.BUG_REPORT_PAGE_URL,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("beta_testers") {
                        BetaTesterListScreen(
                            isBetaTester = state.settings.isBetaTester,
                            onToggleBetaTester = { enabled -> viewModel.setBetaTesterStatus(enabled) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                    }

                    BannerAdSlot(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        enabled = state.showAds
                    )
                }

                if (showBeaconAssist) {
                    BeaconAssistDialog(
                        state = beaconAssistState,
                        onDismiss = {
                            showBeaconAssist = false
                            pendingInboxNav = false
                            beaconFlowStage = BeaconFlowStage.Idle
                        },
                        onRetry = {
                            pendingInboxNav = true
                            beaconFlowStage = BeaconFlowStage.Idle
                            inboxShortcutFlow.tryEmit(Unit)
                        }
                    )
                }

                if (showLineLimit) {
                    LineLimitDialog(
                        lines = orderedLines,
                        onDisableLine = { lineId -> linesViewModel.disableLine(lineId) },
                        onDismiss = { lineLimitDismissed = true }
                    )
                }

                if (showLineSetup) {
                    MultiLineSetupDialog(
                        lines = orderedLines,
                        selectedMode = lineSetupMode,
                        onModeChange = { lineSetupMode = it },
                        selectedDefaultLineId = lineSetupDefaultLineId,
                        onDefaultLineChange = { lineSetupDefaultLineId = it },
                        lineSendPreference = lineSetupSendPreference,
                        onLineSendPreferenceChange = { lineSetupSendPreference = it },
                        devicePhoneInput = lineSetupPhone,
                        onDevicePhoneChange = { lineSetupPhone = it },
                        onConfirm = {
                            val resolvedDefault = lineSetupDefaultLineId
                                ?: orderedLines.firstOrNull()?.id
                                ?: deviceLineId
                            linesViewModel.setInboxMode(lineSetupMode)
                            linesViewModel.setLineSendPreference(lineSetupSendPreference)
                            linesViewModel.setDefaultSendLineId(resolvedDefault?.takeIf { it.isNotBlank() })
                            linesViewModel.setDevicePhoneNumber(lineSetupPhone.takeIf { it.isNotBlank() })
                            if (lineSetupMode == LineInboxMode.PER_LINE) {
                                resolvedDefault?.takeIf { it.isNotBlank() }?.let {
                                    linesViewModel.setActiveLineId(it)
                                    linesViewModel.touchPresence(it)
                                }
                            }
                            lineSetupDismissed = true
                        },
                        onDismiss = { lineSetupDismissed = true }
                    )
                }

                if (isPreparingCall) {
                    CallPreparationDialog()
                }
            }
        }
    }

    private fun sendLinkOrInvite(contact: Contact) {
        if (contact.phoneNumber.isNotBlank()) {
            viewModel.sendLinkRequest(contact.id)
            Toast.makeText(this, getString(R.string.link_request_sent_sms), Toast.LENGTH_SHORT).show()
            return
        }
        if (!contact.email.isNullOrBlank()) {
            viewModel.sendLinkRequest(contact.id)
            Toast.makeText(this, getString(R.string.link_request_sent_cloud), Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, getString(R.string.link_invite_missing_contact_info), Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.onboardingComplete) {
            appOpenAdController.maybeShow(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        callStateMonitor.cancel()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel.uiState.value
        if (state.isEmergencyActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder().build()
            enterPictureInPictureMode(params)
        }
    }
}

@Composable
private fun BeaconAssistDialog(
    state: BeaconAssistState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Beacon inbox setup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BeaconStepRow(label = "Set as Default SMS App", done = state.defaultSmsGranted)
                BeaconStepRow(label = "Enable Beacon icon", done = state.iconEnabled)
                BeaconStepRow(label = "Grant SMS permissions", done = state.smsPermissionsGranted)
                Text(text = state.error ?: state.message)
            }
        },
        confirmButton = {
            TextButton(onClick = if (state.ready) onDismiss else onRetry) {
                Text(text = if (state.ready) "Close" else "Retry")
            }
        },
        dismissButton = {
            if (!state.ready) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel")
                }
            }
        }
    )
}

@Composable
private fun BeaconStepRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun requiredSmsPermissions(context: android.content.Context): List<String> =
    buildList {
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.RECEIVE_MMS)
        add(Manifest.permission.RECEIVE_WAP_PUSH)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
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

private fun Contact.primaryPhone(): String? =
    (listOf(phoneNumber) + additionalPhones).firstOrNull { it.isNotBlank() }

private fun placeCall(
    activity: MainActivity,
    contact: Contact,
    phoneNumber: String,
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
    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
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
private const val NOTIFICATION_POLICY_DETAIL_ACTION =
    "android.settings.NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS"

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private fun openDndSettings(context: android.content.Context) {
    val packageName = context.packageName
    val detailIntent = Intent(NOTIFICATION_POLICY_DETAIL_ACTION).apply {
        data = Uri.fromParts("package", packageName, null)
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(detailIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // ignore and fall through
    } catch (_: SecurityException) {
        // ignore and fall through
    }

    val listIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(listIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // ignore and fall through
    } catch (_: SecurityException) {
        // ignore and fall through
    }

    openAppSettings(context)
}

private fun openUnusedAppRestrictionsSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        openAppSettings(context)
        return
    }
    val packageName = context.packageName
    val autoRevokeIntent = Intent(PackageManagerCompat.ACTION_PERMISSION_REVOCATION_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(autoRevokeIntent)
    } catch (_: ActivityNotFoundException) {
        openAppSettings(context)
    } catch (_: SecurityException) {
        openAppSettings(context)
    }
}

private fun openBatteryOptimizationSettings(context: android.content.Context) {
    val pkg = context.packageName
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        val req = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$pkg")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(req) }.onFailure {
            openAppSettings(context)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BugReportWebScreen(
    url: String,
    onBack: () -> Unit
) {
    val loading = remember { mutableStateOf(true) }
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_report_bug)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            if (loading.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading.value = false
                            }
                        }
                        loadUrl(url)
                    }
                },
                update = { webView ->
                    if (webView.url != url) {
                        loading.value = true
                        webView.loadUrl(url)
                    }
                }
            )
        }
    }
}
