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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulselink.BuildConfig
import com.pulselink.billing.SubscriptionManager
import com.pulselink.ui.ads.BannerAdSlot
import com.pulselink.ui.screens.BeaconNavBar
import com.pulselink.ui.screens.BeaconNavRoute
import com.pulselink.ui.screens.BeaconSettingsScreen
import com.pulselink.ui.screens.PrivatePinScreen
import com.pulselink.ui.screens.NewMessageScreen
import com.pulselink.ui.screens.MessageNotificationSoundScreen
import com.pulselink.ui.screens.VibrationPatternPickerScreen
import com.pulselink.ui.screens.ProfileSettingsScreen
import com.pulselink.ui.screens.MultiLineSetupDialog
import com.pulselink.ui.screens.LineLimitDialog
import com.pulselink.ui.screens.SmsInboxScreen
import com.pulselink.ui.screens.SmsThreadScreen
import com.pulselink.ui.screens.VisualSettingsScreen
import com.pulselink.ui.model.MessageRecipient
import com.pulselink.ui.state.DeviceContactsViewModel
import com.pulselink.ui.state.MainViewModel
import com.pulselink.ui.state.SmsInboxViewModel
import com.pulselink.ui.state.SmsLinesViewModel
import com.pulselink.ui.state.SmsThreadViewModel
import com.pulselink.domain.model.LineInboxMode
import com.pulselink.domain.model.LineSendPreference
import com.pulselink.ui.theme.PulseLinkTheme
import com.pulselink.util.DefaultSmsHelper
import com.pulselink.util.VibrationPatterns
import com.pulselink.util.formatTimestamp
import com.pulselink.util.hashPin
import com.pulselink.util.parseColorOr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BeaconInboxActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    @Inject lateinit var defaultSmsHelper: DefaultSmsHelper
    @Inject lateinit var subscriptionManager: SubscriptionManager
    private val notificationTarget = mutableStateOf<NotificationTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationTarget.value = readNotificationTarget(intent)
        enableEdgeToEdge()
        setContent {
            PulseLinkTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val linesViewModel: SmsLinesViewModel = hiltViewModel()
                val lines by linesViewModel.lines.collectAsStateWithLifecycle()
                val lineDevices by linesViewModel.devices.collectAsStateWithLifecycle()
                val activeLineId by linesViewModel.activeLineId.collectAsStateWithLifecycle()
                val deviceLineId by linesViewModel.deviceLineId.collectAsStateWithLifecycle()
                val defaultSendLineId by linesViewModel.defaultSendLineId.collectAsStateWithLifecycle()
                val lineSendPreference by linesViewModel.lineSendPreference.collectAsStateWithLifecycle()
                val threadLineOverrides by linesViewModel.threadLineOverrides.collectAsStateWithLifecycle()
                val subscriptionUiState by subscriptionManager.subscriptionState.collectAsStateWithLifecycle()
                val isPremium = subscriptionUiState.isPremiumActive ||
                    state.settings.premiumUnlocked ||
                    BuildConfig.PREMIUM_FEATURES
                val deleteAccountState by viewModel.deleteAccountState.collectAsStateWithLifecycle()
                var isDefaultSms by remember { mutableStateOf(defaultSmsHelper.isDefaultSms()) }
                var isCheckingDefaultSms by remember { mutableStateOf(false) }
                var notificationsEnabled by remember {
                    mutableStateOf(com.pulselink.data.sms.MessageNotificationManager.areNotificationsEnabled(context))
                }
                var notificationsSilent by remember {
                    mutableStateOf(com.pulselink.data.sms.MessageNotificationManager.isMessageChannelSilent(context))
                }
                val privateThreads = state.settings.privateThreadIds.toSet()
                var showPrivate by remember { mutableStateOf(false) }
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
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
                val defaultSmsSupported =
                    defaultSmsHelper.buildRoleRequestIntent() != null || isDefaultSms
                val lifecycleOwner = LocalLifecycleOwner.current
                var defaultSmsCheckJob by remember { mutableStateOf<Job?>(null) }
                val launchDefaultSmsCheck: () -> Unit = launchDefaultSmsCheck@{
                    if (defaultSmsCheckJob?.isActive == true) return@launchDefaultSmsCheck
                    isCheckingDefaultSms = true
                    defaultSmsCheckJob = scope.launch {
                        try {
                            val latest = defaultSmsHelper.checkDefaultSmsWithRetry()
                            isDefaultSms = latest
                        } finally {
                            isCheckingDefaultSms = false
                            defaultSmsCheckJob = null
                        }
                    }
                }

                val defaultSmsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    launchDefaultSmsCheck()
                }

                // Permission handling
                var hasSmsPermissions by remember {
                    mutableStateOf(checkSmsPermissions(context))
                }
                var requestedDefaultOnce by remember { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    hasSmsPermissions = checkSmsPermissions(context)
                }

                // Refresh default-SMS status whenever the activity resumes.
                DisposableEffect(lifecycleOwner, activeLineId, deviceLineId, isPremium) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            launchDefaultSmsCheck()
                            notificationsEnabled = com.pulselink.data.sms.MessageNotificationManager.areNotificationsEnabled(context)
                            notificationsSilent = com.pulselink.data.sms.MessageNotificationManager.isMessageChannelSilent(context)
                            if (isPremium) {
                                linesViewModel.touchPresence(activeLineId ?: deviceLineId)
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                LaunchedEffect(isDefaultSms, hasSmsPermissions, requestedDefaultOnce) {
                    if (!isDefaultSms && !requestedDefaultOnce && !isCheckingDefaultSms) {
                        // Attempt to request role once automatically
                        requestedDefaultOnce = true
                        defaultSmsHelper.buildRoleRequestIntent()?.let { intent ->
                            defaultSmsLauncher.launch(intent)
                        }
                    }
                    if (isDefaultSms && !hasSmsPermissions) {
                        permissionLauncher.launch(requiredSmsPermissions())
                    }
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

                val pendingNotification by notificationTarget
                LaunchedEffect(pendingNotification) {
                    val target = pendingNotification ?: return@LaunchedEffect
                    val encoded = Uri.encode(target.address)
                    val threadId = target.threadId.takeIf { it > 0 } ?: 0L
                    navController.navigate("sms/thread/$threadId/$encoded")
                    notificationTarget.value = null
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    val bannerHeight = 50.dp
                    Box(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (state.showAds) bannerHeight else 0.dp)
                        ) {
                            NavHost(navController = navController, startDestination = "sms/inbox") {
                                composable("sms/inbox") {
                                    val smsInboxViewModel: SmsInboxViewModel = hiltViewModel()
                                    val threads by smsInboxViewModel.threads.collectAsStateWithLifecycle()
                                    val archivedThreads by smsInboxViewModel.archived.collectAsStateWithLifecycle()
                                    val searchState by smsInboxViewModel.searchState.collectAsStateWithLifecycle()
                                    val contactsByNumber = remember(state.contacts) {
                                        val map = mutableMapOf<String, com.pulselink.domain.model.Contact>()
                                        state.contacts.forEach { contact ->
                                            val numbers = listOf(contact.phoneNumber) + contact.additionalPhones
                                            numbers.filter { it.isNotBlank() }.forEach { number ->
                                                val normalized = com.pulselink.util.normalizeSmsAddress(number)
                                                if (normalized.isNotBlank()) {
                                                    map.putIfAbsent(normalized, contact)
                                                }
                                            }
                                        }
                                        map.toMap()
                                    }
                                    LaunchedEffect(Unit) { smsInboxViewModel.refresh() }
                                    LaunchedEffect(isDefaultSms, hasSmsPermissions) {
                                        if (isDefaultSms || hasSmsPermissions) {
                                            smsInboxViewModel.refresh()
                                        }
                                    }

                                    var currentRoute by remember { mutableStateOf(BeaconNavRoute.Inbox) }

                                    val displayedThreads = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> threads
                                        BeaconNavRoute.Otp -> threads.filter { it.isOtp }
                                        BeaconNavRoute.Trusted -> threads.filter { it.isTrusted }
                                        BeaconNavRoute.Favorites -> threads.filter { it.isFavorite }
                                        BeaconNavRoute.Private -> threads
                                        BeaconNavRoute.Contacts -> threads
                                        BeaconNavRoute.Archived -> emptyList()
                                    }

                                    val displayedArchived = when (currentRoute) {
                                        BeaconNavRoute.Inbox -> archivedThreads
                                        BeaconNavRoute.Otp -> archivedThreads.filter { it.isOtp }
                                        BeaconNavRoute.Trusted -> archivedThreads.filter { it.isTrusted }
                                        BeaconNavRoute.Favorites -> archivedThreads.filter { it.isFavorite }
                                        BeaconNavRoute.Private -> archivedThreads
                                        BeaconNavRoute.Contacts -> archivedThreads
                                        BeaconNavRoute.Archived -> archivedThreads
                                    }

                                    key(currentRoute) {
                                        SmsInboxScreen(
                                            threads = displayedThreads,
                                            archivedThreads = displayedArchived,
                                            onOpenThread = { thread ->
                                                val lineSuffix = thread.lineId?.let { Uri.encode(it) }.orEmpty()
                                                navController.navigate(
                                                    "sms/thread/${thread.threadId}/${Uri.encode(thread.address)}?lineId=$lineSuffix"
                                                )
                                            },
                                            onOpenThreadById = { threadId, address ->
                                                navController.navigate("sms/thread/$threadId/${Uri.encode(address)}")
                                            },
                                            onArchiveThread = { thread -> smsInboxViewModel.archive(thread.threadId) },
                                            onUnarchiveThread = { thread -> smsInboxViewModel.unarchive(thread.threadId) },
                                            onDeleteThread = { thread -> smsInboxViewModel.delete(thread.threadId) },
                                            onBack = { finish() },
                                            dateFormatter = { ts -> formatTimestamp(context, ts, state.settings.timeFormat) },
                                            lineOptions = if (isPremium) orderedLines else emptyList(),
                                            deviceLineId = deviceLineId,
                                            activeLineId = if (isPremium) activeLineId else null,
                                            onSelectLine = { selected ->
                                                linesViewModel.setActiveLineId(selected)
                                                linesViewModel.touchPresence(selected)
                                            },
                                            showLinePicker = isPremium && state.settings.lineInboxMode == LineInboxMode.PER_LINE && currentRoute == BeaconNavRoute.Inbox,
                                            isBeaconMode = true,
                                            onOpenSettings = { navController.navigate("beacon_settings") },
                                            onOpenPrivate = {},
                                            privateThreadIds = privateThreads,
                                            showPrivateOnly = currentRoute == BeaconNavRoute.Private,
                                            hideOtpInAll = currentRoute == BeaconNavRoute.Inbox,
                                            onTogglePrivate = { thread, makePrivate ->
                                                viewModel.setThreadPrivacy(thread.threadId, thread.address, makePrivate)
                                            },
                                            theme = state.settings.themePreferences,
                                            onImportAll = { smsInboxViewModel.importAllMessages() },
                                            onLoadMore = { smsInboxViewModel.loadMoreThreads() },
                                            hasMoreToLoad = smsInboxViewModel.hasMoreThreads.collectAsStateWithLifecycle().value,
                                                sectionTitle = when (currentRoute) {
                                                    BeaconNavRoute.Inbox -> "All messages"
                                                    BeaconNavRoute.Otp -> "2-step codes"
                                                    BeaconNavRoute.Trusted -> "Trusted contacts"
                                                    BeaconNavRoute.Favorites -> "Favorites"
                                                    BeaconNavRoute.Private -> "Private"
                                                    BeaconNavRoute.Contacts -> "Contacts"
                                                    BeaconNavRoute.Archived -> "Archived"
                                                },
                                            showFilterTabs = currentRoute == BeaconNavRoute.Inbox,
                                            showSearchBar = currentRoute == BeaconNavRoute.Inbox,
                                            searchState = searchState,
                                            onSearch = { smsInboxViewModel.search(it) },
                                            onClearSearch = { smsInboxViewModel.clearSearch() },
                                            archivedOnly = currentRoute == BeaconNavRoute.Archived,
                                            contactsByNumber = contactsByNumber,
                                            banner = {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                                Icon(
                                                                    Icons.Filled.NotificationsActive,
                                                                    contentDescription = null
                                                                )
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
                                                                    val intent = com.pulselink.data.sms.MessageNotificationManager
                                                                        .buildNotificationSettingsIntent(context)
                                                                    context.startActivity(intent)
                                                                }) {
                                                                    Text("Open")
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!hasSmsPermissions) {
                                                        Surface(
                                                            tonalElevation = 2.dp,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Filled.Settings, contentDescription = null)
                                                                Column(Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = "Permissions needed",
                                                                        style = MaterialTheme.typography.titleSmall
                                                                    )
                                                                    Text(
                                                                        text = "Grant SMS permission so Beacon can read and show messages.",
                                                                        style = MaterialTheme.typography.bodySmall
                                                                    )
                                                                }
                                                                OutlinedButton(onClick = {
                                                                    permissionLauncher.launch(requiredSmsPermissions())
                                                                }) {
                                                                    Text("Grant")
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!isDefaultSms || isCheckingDefaultSms) {
                                                        Surface(
                                                            tonalElevation = 2.dp,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(Icons.Filled.Sms, contentDescription = null)
                                                                Column(Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = if (isCheckingDefaultSms) {
                                                                            "Checking default SMS status..."
                                                                        } else {
                                                                            "Set as default SMS"
                                                                        },
                                                                        style = MaterialTheme.typography.titleSmall
                                                                    )
                                                                    Text(
                                                                        text = if (defaultSmsSupported) {
                                                                            "Required for receiving texts and showing notifications."
                                                                        } else {
                                                                            "Default SMS role unavailable on this device."
                                                                        },
                                                                        style = MaterialTheme.typography.bodySmall
                                                                    )
                                                                }
                                                                if (isCheckingDefaultSms) {
                                                                    CircularProgressIndicator(
                                                                        modifier = Modifier.padding(end = 8.dp),
                                                                        strokeWidth = 2.dp
                                                                    )
                                                                } else if (defaultSmsSupported) {
                                                                    OutlinedButton(onClick = {
                                                                        defaultSmsHelper.buildRoleRequestIntent()?.let { intent ->
                                                                            defaultSmsLauncher.launch(intent)
                                                                        }
                                                                    }) {
                                                                        Text("Set")
                                                                    }
                                                                }
                                                                OutlinedButton(
                                                                    onClick = launchDefaultSmsCheck,
                                                                    enabled = !isCheckingDefaultSms
                                                                ) {
                                                                    Icon(Icons.Filled.Refresh, contentDescription = null)
                                                                    Text("Refresh", modifier = Modifier.padding(start = 6.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            floatingActionButton = {
                                                if (currentRoute == BeaconNavRoute.Inbox) {
                                                    FloatingActionButton(
                                                        onClick = { navController.navigate("sms/new") },
                                                        containerColor = parseColorOr(
                                                            MaterialTheme.colorScheme.primary,
                                                            state.settings.themePreferences.primaryColor
                                                        ),
                                                        contentColor = parseColorOr(
                                                            MaterialTheme.colorScheme.onPrimary,
                                                            state.settings.themePreferences.onBubbleOutgoing
                                                        )
                                                    ) {
                                                        Icon(Icons.Filled.Edit, contentDescription = "New message")
                                                    }
                                                }
                                            },
                                            bottomBar = {
                                                BeaconNavBar(
                                                    currentRoute = currentRoute,
                                                    onNavigate = { route ->
                                                        if (route == BeaconNavRoute.Private && currentRoute != BeaconNavRoute.Private) {
                                                            if (state.settings.privatePinHash.isNullOrBlank()) {
                                                                navController.navigate("private_pin")
                                                            } else {
                                                                pinInput = ""
                                                                showPinDialog = true
                                                            }
                                                        } else {
                                                            if (route != BeaconNavRoute.Inbox) {
                                                                smsInboxViewModel.clearSearch()
                                                            }
                                                            currentRoute = route
                                                        }
                                                    },
                                                    theme = state.settings.themePreferences
                                                )
                                            }
                                        )
                                    }

                                    if (showPrivate && currentRoute != BeaconNavRoute.Private) {
                                         currentRoute = BeaconNavRoute.Private
                                         showPrivate = false
                                    }
                                }
                                composable("sms/new") {
                                    val deviceContactsViewModel: DeviceContactsViewModel = hiltViewModel()
                                    val deviceContacts by deviceContactsViewModel.contacts.collectAsStateWithLifecycle()
                                    var hasContactsPermission by remember {
                                        mutableStateOf(checkContactsPermission(context))
                                    }
                                    val contactsPermissionLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.RequestPermission()
                                    ) { granted ->
                                        hasContactsPermission = granted
                                        if (granted) {
                                            deviceContactsViewModel.refresh()
                                        }
                                    }
                                    LaunchedEffect(hasContactsPermission) {
                                        if (hasContactsPermission) {
                                            deviceContactsViewModel.refresh()
                                        }
                                    }
                                    val recipients = remember(state.contacts, deviceContacts) {
                                        val trustedRecipients = state.contacts.flatMap { contact ->
                                            val phones = (listOf(contact.phoneNumber) + contact.additionalPhones)
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            phones.mapIndexed { index, phone ->
                                                MessageRecipient(
                                                    id = (contact.id * 10_000L) + index,
                                                    displayName = contact.displayName,
                                                    phoneNumber = phone,
                                                    isTrusted = true
                                                )
                                            }
                                        }
                                        val trustedNumbers = trustedRecipients
                                            .map { normalizePhone(it.phoneNumber) }
                                            .toSet()
                                        val deviceRecipients = deviceContacts.map { device ->
                                            MessageRecipient(
                                                id = -device.id,
                                                displayName = device.displayName.ifBlank { device.phoneNumber },
                                                phoneNumber = device.phoneNumber,
                                                isTrusted = false
                                            )
                                        }.filter { candidate ->
                                            normalizePhone(candidate.phoneNumber) !in trustedNumbers
                                        }
                                        (trustedRecipients + deviceRecipients)
                                            .distinctBy { normalizePhone(it.phoneNumber) }
                                            .sortedWith(
                                                compareByDescending<MessageRecipient> { it.isTrusted }
                                                    .thenBy { it.displayName.lowercase() }
                                            )
                                    }
                                    NewMessageScreen(
                                        contacts = recipients,
                                        onBack = { navController.popBackStack() },
                                        onCreateConversation = { selectedNumbers ->
                                            val numbers = selectedNumbers
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                            if (numbers.isNotEmpty()) {
                                                val address = numbers.joinToString(";")
                                                val lineSuffix = Uri.encode(activeLineId ?: "")
                                                navController.navigate("sms/thread/0/${Uri.encode(address)}?lineId=$lineSuffix")
                                            }
                                        },
                                        onManualInput = { raw ->
                                            val address = raw.trim()
                                            if (address.isNotBlank()) {
                                                val lineSuffix = Uri.encode(activeLineId ?: "")
                                                navController.navigate("sms/thread/0/${Uri.encode(address)}?lineId=$lineSuffix")
                                            }
                                        },
                                        hasContactsPermission = hasContactsPermission,
                                        onRequestContactsPermission = {
                                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        },
                                        theme = state.settings.themePreferences
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
                                    val summaryState by threadViewModel.summaryState.collectAsStateWithLifecycle()
                                    val composeState by threadViewModel.composeState.collectAsStateWithLifecycle()
                                    val decodedAddress = Uri.decode(address)
                                    val premiumActive = subscriptionUiState.isPremiumActive ||
                                        state.settings.premiumUnlocked ||
                                        BuildConfig.PREMIUM_FEATURES
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
                                            onUpdateContactTheme = { theme -> threadViewModel.setContactTheme(theme) },
                                            onCustomizeTheme = {
                                                val contactId = contact?.id ?: -1L
                                                if (contactId != -1L) {
                                                    navController.navigate("visual_settings?contactId=$contactId")
                                                }
                                            },
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
                                            aiSummaryState = summaryState,
                                        onRequestSummary = { threadViewModel.requestSummary() },
                                        onClearSummary = { threadViewModel.clearSummary() },
                                        aiComposeState = composeState,
                                        onRequestCompose = { action, draft, last ->
                                            threadViewModel.requestCompose(action, draft, last)
                                        },
                                        onClearCompose = { threadViewModel.clearCompose() },
                                        aiSummaryEnabled = premiumActive && state.settings.aiSummariesEnabled,
                                        aiComposeEnabled = premiumActive && state.settings.aiComposeEnabled,
                                        onLoadMore = { threadViewModel.loadMoreMessages() },
                                        hasMoreToLoad = threadViewModel.hasMoreMessages.collectAsStateWithLifecycle().value
                                    )
                                }
                                composable("beacon_settings") {
                                    val premiumActive = subscriptionUiState.isPremiumActive ||
                                        state.settings.premiumUnlocked ||
                                        BuildConfig.PREMIUM_FEATURES
                                    val messageSoundLabel = if (state.settings.messageNotificationSoundUri.isNullOrBlank()) {
                                        "Phone default notification"
                                    } else {
                                        "Custom audio"
                                    }
                                    val messageVibrationLabel = VibrationPatterns
                                        .messageOption(state.settings.messageNotificationVibrationPattern)
                                        .label
                                    BeaconSettingsScreen(
                                        settings = state.settings,
                                        onBack = { navController.popBackStack() },
                                        onTimeFormatChange = { viewModel.setTimeFormat(it) },
                                        onOpenVisualSettings = { navController.navigate("visual_settings") },
                                        onOpenProfileSettings = { navController.navigate("profile_settings") },
                                        messageSoundLabel = messageSoundLabel,
                                        messageVibrate = state.settings.messageNotificationVibrate,
                                        onEditMessageSound = { navController.navigate("notifications/message_sound") },
                                        messageVibrationLabel = messageVibrationLabel,
                                        onEditMessageVibration = {
                                            navController.navigate("notifications/message_vibration")
                                        },
                                        onToggleMessageVibrate = viewModel::updateMessageNotificationVibrate,
                                        isDefaultSmsApp = isDefaultSms,
                                        defaultSmsSupported = defaultSmsSupported,
                                        onRequestDefaultSms = {
                                            val intent = defaultSmsHelper.buildRoleRequestIntent()
                                            if (intent != null) {
                                                defaultSmsLauncher.launch(intent)
                                            }
                                        },
                                        remoteWebAccessEnabled = state.settings.remoteWebAccessEnabled,
                                        isPremiumActive = premiumActive,
                                        onToggleRemoteWebAccess = { enabled -> viewModel.setRemoteWebAccess(enabled) },
                                        otpCleanupEnabled = state.settings.otpCleanupEnabled,
                                        otpCleanupDays = state.settings.otpCleanupDays,
                                        onToggleOtpCleanup = { enabled -> viewModel.setOtpCleanupEnabled(enabled) },
                                        onChangeOtpCleanupDays = { days -> viewModel.setOtpCleanupDays(days) },
                                        onSetPrivatePin = { navController.navigate("private_pin") },
                                        onPurchasePremium = { subscriptionManager.launchSubscribe(this@BeaconInboxActivity) },
                                        beaconLauncherEnabled = state.settings.beaconLauncherEnabled,
                                        onToggleBeaconLauncher = { enabled -> viewModel.setBeaconLauncherEnabled(enabled) },
                                        aiSummariesEnabled = state.settings.aiSummariesEnabled,
                                        aiComposeEnabled = state.settings.aiComposeEnabled,
                                        aiUrgencyEnabled = state.settings.aiUrgencyEnabled,
                                        aiUrgencyBypassDnd = state.settings.aiUrgencyBypassDnd,
                                        aiUrgencyIncludeUnknown = state.settings.aiUrgencyIncludeUnknown,
                                        onToggleAiSummaries = { enabled -> viewModel.setAiSummariesEnabled(enabled) },
                                        onToggleAiCompose = { enabled -> viewModel.setAiComposeEnabled(enabled) },
                                        onToggleAiUrgency = { enabled -> viewModel.setAiUrgencyEnabled(enabled) },
                                        onToggleAiUrgencyBypass = { enabled -> viewModel.setAiUrgencyBypassDnd(enabled) },
                                        onToggleAiUrgencyIncludeUnknown = { enabled ->
                                            viewModel.setAiUrgencyIncludeUnknown(enabled)
                                        }
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
                                    val normalized = addressArg?.let { com.pulselink.util.normalizeSmsAddress(it) }
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
                                    val normalized = addressArg?.let { com.pulselink.util.normalizeSmsAddress(it) }
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
                                composable(
                                    route = "visual_settings?contactId={contactId}",
                                    arguments = listOf(navArgument("contactId") {
                                        type = NavType.LongType
                                        defaultValue = -1L
                                    })
                                ) { entry ->
                                    val contactId = entry.arguments?.getLong("contactId") ?: -1L
                                    if (contactId != -1L) {
                                        val contact = state.contacts.find { it.id == contactId }
                                        val theme = contact?.themeOverride ?: state.settings.themePreferences
                                        VisualSettingsScreen(
                                            theme = theme,
                                            onSelectTheme = { newTheme -> viewModel.updateContactTheme(contactId, newTheme) },
                                            onBack = { navController.popBackStack() },
                                            isGlobal = false
                                        )
                                    } else {
                                        VisualSettingsScreen(
                                            theme = state.settings.themePreferences,
                                            onSelectTheme = { newTheme -> viewModel.setThemePreferences(newTheme) },
                                            onBack = { navController.popBackStack() },
                                            isGlobal = true
                                        )
                                    }
                                }
                                composable("profile_settings") {
                                    ProfileSettingsScreen(
                                        settings = state.settings,
                                        deleteAccountState = deleteAccountState,
                                        onSaveName = { viewModel.setOwnerName(it) },
                                        onSaveAvatar = { viewModel.setOwnerAvatarUrl(it) },
                                        onDeleteAccount = { viewModel.deleteAccount() },
                                        onResetDeleteAccountState = { viewModel.resetDeleteAccountState() },
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                                composable("private_pin") {
                                    PrivatePinScreen(
                                        hasPin = state.settings.privatePinHash != null,
                                        onSavePin = { newPin ->
                                            val hashed = newPin?.let { hashPin(it) }
                                            viewModel.setPrivatePinHash(hashed)
                                            navController.popBackStack()
                                        },
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

                    if (showPinDialog) {
                        AlertDialog(
                            onDismissRequest = { showPinDialog = false },
                            title = { Text("Enter private PIN") },
                            text = {
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    label = { Text("PIN") },
                                    visualTransformation = PasswordVisualTransformation()
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val hashed = hashPin(pinInput)
                                    if (hashed == state.settings.privatePinHash) {
                                        showPinDialog = false
                                        showPrivate = true
                                    } else {
                                        pinInput = ""
                                    }
                                }) {
                                    Text("Unlock")
                                }
                            },
                            dismissButton = {
                                Button(onClick = { showPinDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationTarget.value = readNotificationTarget(intent)
    }

    private fun readNotificationTarget(intent: Intent?): NotificationTarget? {
        val address = intent?.getStringExtra(com.pulselink.data.sms.MessageNotificationManager.EXTRA_ADDRESS)
            ?.takeIf { it.isNotBlank() } ?: return null
        val threadId = intent.getLongExtra(com.pulselink.data.sms.MessageNotificationManager.EXTRA_THREAD_ID, 0L)
        return NotificationTarget(threadId, address)
    }

    private fun checkSmsPermissions(context: android.content.Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredSmsPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    private fun checkContactsPermission(context: android.content.Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    private fun normalizePhone(input: String): String {
        if (input.isBlank()) return ""
        val digits = buildString {
            input.forEach { ch ->
                if (ch.isDigit()) append(ch)
            }
        }
        return if (input.trim().startsWith("+")) "+$digits" else digits
    }

    private data class NotificationTarget(val threadId: Long, val address: String)
}
