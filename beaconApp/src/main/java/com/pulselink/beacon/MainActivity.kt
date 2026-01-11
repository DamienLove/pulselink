package com.pulselink.beacon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.android.gms.ads.MobileAds
import com.pulselink.beacon.ui.ads.BannerAd
import com.pulselink.beacon.ui.InboxScreen
import com.pulselink.beacon.ui.NewMessageScreen
import com.pulselink.beacon.ui.SmsViewModel
import com.pulselink.beacon.ui.ThreadScreen
import com.pulselink.beacon.ui.ThemeViewModel
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.ui.customize.CustomizationScreen
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationTarget = mutableStateOf<NotificationTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationTarget.value = readNotificationTarget(intent)
        MobileAds.initialize(this)
        setContent {
            val vm: SmsViewModel = viewModel(factory = SmsViewModel.factory(application))
            val themeVm: ThemeViewModel = viewModel(factory = ThemeViewModel.factory(application))
            BeaconTheme(theme = themeVm.themeState.global) {
                BeaconNav(vm, themeVm, themeVm.themeState, notificationTarget)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationTarget.value = readNotificationTarget(intent)
    }

    private fun readNotificationTarget(intent: Intent?): NotificationTarget? {
        val address = intent?.getStringExtra(com.pulselink.beacon.notifications.MessageNotificationManager.EXTRA_ADDRESS)
            ?.takeIf { it.isNotBlank() } ?: return null
        val threadId = intent.getLongExtra(
            com.pulselink.beacon.notifications.MessageNotificationManager.EXTRA_THREAD_ID,
            0L
        )
        return NotificationTarget(threadId, address)
    }

}

private data class NotificationTarget(val threadId: Long, val address: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeaconNav(
    vm: SmsViewModel,
    themeVm: ThemeViewModel,
    themeState: ThemeState,
    notificationTarget: androidx.compose.runtime.MutableState<NotificationTarget?>
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPrefs = remember { com.pulselink.beacon.data.MessageNotificationPreferences(context.applicationContext) }
    val notificationSettings by notificationPrefs.flow.collectAsState(initial = com.pulselink.beacon.data.MessageNotificationPrefs())
    var isDefaultSms by remember {
        mutableStateOf(isDefaultSmsRoleHeld(context))
    }
    var isCheckingDefaultSms by remember { mutableStateOf(false) }
    var defaultSmsCheckJob by remember { mutableStateOf<Job?>(null) }
    var missingPerms by remember { mutableStateOf(requiredPermissions(context)) }
    var missingReadPerms by remember { mutableStateOf(requiredReadPermissions(context)) }
    var notificationsEnabled by remember {
        mutableStateOf(com.pulselink.beacon.notifications.MessageNotificationManager.areNotificationsEnabled(context))
    }
    var notificationsSilent by remember {
        mutableStateOf(com.pulselink.beacon.notifications.MessageNotificationManager.isMessageChannelSilent(context))
    }
    val refreshDefaultSms = remember {
        suspend {
            val latest = checkDefaultSmsWithRetry(context)
            isDefaultSms = latest
            latest
        }
    }
    val launchDefaultSmsCheck: () -> Unit = {
        if (defaultSmsCheckJob?.isActive == true) return@launchDefaultSmsCheck
        isCheckingDefaultSms = true
        defaultSmsCheckJob = scope.launch {
            try {
                refreshDefaultSms()
                missingPerms = requiredPermissions(context)
                missingReadPerms = requiredReadPermissions(context)
            } finally {
                isCheckingDefaultSms = false
                defaultSmsCheckJob = null
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        missingPerms = requiredPermissions(context)
        missingReadPerms = requiredReadPermissions(context)
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        launchDefaultSmsCheck()
    }

    LaunchedEffect(Unit) {
        launchDefaultSmsCheck()
    }

    LaunchedEffect(isDefaultSms, missingReadPerms) {
        if (isDefaultSms || missingReadPerms.isEmpty()) {
            vm.refreshThreads()
        }
    }

    val pendingNotification by notificationTarget
    LaunchedEffect(pendingNotification) {
        val target = pendingNotification ?: return@LaunchedEffect
        val encoded = Uri.encode(target.address)
        val threadId = target.threadId.takeIf { it > 0 } ?: 0L
        navController.navigate("thread/$threadId/$encoded")
        notificationTarget.value = null
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                launchDefaultSmsCheck()
                notificationsEnabled = com.pulselink.beacon.notifications.MessageNotificationManager.areNotificationsEnabled(context)
                notificationsSilent = com.pulselink.beacon.notifications.MessageNotificationManager.isMessageChannelSilent(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        bottomBar = { BannerAd(modifier = Modifier.navigationBarsPadding()) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "inbox",
            modifier = Modifier
                .imePadding()
                .padding(padding)
        ) {
            composable("inbox") {
                    InboxScreen(
                        threads = vm.threads,
                        theme = themeState.global,
                        searchState = vm.searchState,
                        isDefaultSms = isDefaultSms,
                        isCheckingDefaultSms = isCheckingDefaultSms,
                        missingPermissions = missingReadPerms,
                        notificationsEnabled = notificationsEnabled,
                        notificationsSilent = notificationsSilent,
                    onOpenNotificationSettings = {
                        val intent = com.pulselink.beacon.notifications.MessageNotificationManager
                            .buildNotificationSettingsIntent(context)
                        context.startActivity(intent)
                    },
                    onRequestPermissions = {
                        permissionLauncher.launch(missingPerms.toTypedArray())
                    },
                    onRequestDefault = {
                        buildDefaultSmsRequestIntent(context)?.let { intent ->
                            defaultSmsLauncher.launch(intent)
                        } ?: launchDefaultSmsCheck()
                    },
                    onRefreshDefaultStatus = launchDefaultSmsCheck,
                    onOpenThread = { id, address ->
                        vm.openThread(id, address)
                        navController.navigate("thread/$id/${Uri.encode(address)}")
                    },
                    onDeleteThread = { vm.deleteThread(it) },
                    onTogglePin = { vm.togglePin(it) },
                    onToggleArchive = { vm.toggleArchive(it) },
                    onRefresh = { vm.refreshThreads() },
                    onSearch = { vm.search(it) },
                    onClearSearch = { vm.clearSearch() },
                    onCustomize = { navController.navigate("customize?address=") },
                    onCompose = { navController.navigate("newMessage") },
                    onOpenNotifications = { navController.navigate("notifications") },
                    selectionMode = vm.selectionMode,
                    selectedThreadIds = vm.selectedThreadIds,
                    onToggleSelection = { vm.toggleSelection(it) },
                    onClearSelection = { vm.clearSelection() },
                    onArchiveSelected = { vm.archiveSelected() },
                    onDeleteSelected = { vm.deleteSelected() },
                    onMarkSelectedRead = { vm.markSelectedRead() },
                    onMarkSelectedUnread = { vm.markSelectedUnread() },
                    onPinSelected = { vm.pinSelected() },
                    onMarkAsUnread = { vm.markAsUnread(it) },
                    userMessage = vm.userMessage,
                    onClearUserMessage = { vm.clearUserMessage() }
                )
            }
            composable(
                route = "thread/{threadId}/{address}",
                arguments = listOf(
                    navArgument("threadId") { type = NavType.LongType },
                    navArgument("address") { type = NavType.StringType })
            ) { backStackEntry ->
                val threadId = backStackEntry.arguments?.getLong("threadId") ?: 0L
                val address = backStackEntry.arguments?.getString("address")?.let { Uri.decode(it) } ?: ""
                LaunchedEffect(threadId) {
                    vm.openThread(threadId, address)
                }
                val contactTheme = themeState.forAddress(address)
                BeaconTheme(theme = contactTheme) {
                    ThreadScreen(
                        address = address.ifBlank { "Unknown" },
                        uiItems = vm.uiMessages,
                        theme = contactTheme,
                        pendingMessage = vm.pendingMessage,
                        onBack = { navController.popBackStack() },
                        onSend = { vm.sendDelayedMessage(it) },
                        onCancelPending = { vm.cancelDelayedMessage() },
                        onSendNow = { vm.sendNow() },
                        onScheduleMessage = { body, time ->
                            vm.scheduleMessage(body, time)
                        },
                        onDeleteThread = {
                            vm.deleteThread(threadId)
                            navController.popBackStack()
                        },
                        onCustomize = { navController.navigate("customize?address=${Uri.encode(address)}") },
                        onEditNotificationSound = { navController.navigate("notifications?address=${Uri.encode(address)}") },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$address")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
            composable(
                route = "notifications?address={address}",
                arguments = listOf(
                    navArgument("address") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val addressArg = backStackEntry.arguments?.getString("address")
                    ?.takeIf { it.isNotBlank() }
                    ?.let { Uri.decode(it) }
                val normalized = addressArg?.let { com.pulselink.beacon.util.normalizeSmsAddress(it) }
                val key = normalized?.takeIf { it.isNotBlank() } ?: addressArg
                val overrideUri = key?.let { notificationSettings.contactOverrides[it] }
                val isContact = addressArg != null
                com.pulselink.beacon.ui.NotificationSoundScreen(
                    title = if (isContact) "Notification sound" else "Message notification sound",
                    subtitle = if (isContact) {
                        "Overrides the global sound for this conversation."
                    } else {
                        "Choose the sound for incoming texts."
                    },
                    defaultLabel = if (isContact) "Use global message sound" else "Phone default notification",
                    currentSoundUri = if (isContact) overrideUri else notificationSettings.soundUri,
                    vibrate = notificationSettings.vibrate,
                    showVibrateToggle = !isContact,
                    onToggleVibrate = { enabled ->
                        scope.launch { notificationPrefs.setVibrate(enabled) }
                    },
                    onSelectDefault = {
                        scope.launch {
                            if (addressArg != null) {
                                key?.let { notificationPrefs.setContactSound(it, null) }
                            } else {
                                notificationPrefs.setGlobalSound(null)
                            }
                        }
                    },
                    onSelectCustom = { uri ->
                        scope.launch {
                            if (addressArg != null) {
                                key?.let { notificationPrefs.setContactSound(it, uri.toString()) }
                            } else {
                                notificationPrefs.setGlobalSound(uri.toString())
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("newMessage") {
                NewMessageScreen(
                    theme = themeState.global,
                    onBack = { navController.popBackStack() },
                    onStartConversation = { address ->
                        navController.navigate("thread/0/${Uri.encode(address)}") {
                            popUpTo("inbox")
                        }
                    }
                )
            }
            composable(
                route = "customize?address={address}",
                arguments = listOf(
                    navArgument("address") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val addressArg = backStackEntry.arguments?.getString("address")?.takeIf { it.isNotBlank() }
                    ?.let { Uri.decode(it) }
                CustomizationScreen(
                    address = addressArg,
                    themeState = themeVm.themeState,
                    onBack = { navController.popBackStack() },
                    onColorChange = { target, color -> themeVm.applyColor(target, color, addressArg) },
                    onFontChange = { themeVm.applyFont(it, addressArg) },
                    onRadiusChange = { themeVm.applyRadius(it, addressArg) },
                    onPreset = { preset -> themeVm.applyPreset(preset, addressArg) },
                    onResetContact = { addressArg?.let { themeVm.resetContact(it) } },
                    onIconVariant = { variant -> themeVm.updateIconVariant(variant) }
                )
            }
        }
    }
}

private fun buildDefaultSmsRequestIntent(context: android.content.Context): Intent? {
    val packageName = context.packageName
    if (isDefaultSmsRoleHeld(context)) return null

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
            roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        } else {
            null
        }
    } else {
        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
            putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        }
    }
}

private suspend fun checkDefaultSmsWithRetry(
    context: android.content.Context,
    maxAttempts: Int = 7,
    initialDelayMs: Long = 300
): Boolean {
    var delayMs = initialDelayMs
    var latest = false
    repeat(maxAttempts) { attempt ->
        latest = isDefaultSmsRoleHeld(context)
        if (latest) return true
        if (attempt < maxAttempts - 1) {
            delay(delayMs)
            delayMs *= 2
        }
    }
    return latest
}

private fun isDefaultSmsRoleHeld(context: android.content.Context): Boolean {
    val packageName = context.packageName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) == true) {
            val holders = runCatching { roleManager.getRoleHolders(RoleManager.ROLE_SMS) }
                .getOrDefault(emptyList())
            if (holders.contains(packageName) ||
                runCatching { roleManager.isRoleHeld(RoleManager.ROLE_SMS) }.getOrDefault(false)
            ) {
                return true
            }
        }
    }
    return Telephony.Sms.getDefaultSmsPackage(context) == packageName
}

private fun requiredPermissions(context: android.content.Context): List<String> {
    val basePerms = listOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_MMS,
        android.Manifest.permission.RECEIVE_WAP_PUSH
    )
    val notif =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(android.Manifest.permission.POST_NOTIFICATIONS) else emptyList()
    val all = basePerms + notif
    return all.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredReadPermissions(context: android.content.Context): List<String> {
    val readPerms = listOf(android.Manifest.permission.READ_SMS)
    return readPerms.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
}
