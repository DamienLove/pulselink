package com.pulselink.ui.screens

import com.pulselink.BuildConfig
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings

data class ExtensionCategory(
    val id: String,
    val title: String,
    val items: List<FeatureToggle>
)

data class FeatureToggle(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val isAvailable: Boolean = true,
    val requiresPremium: Boolean = false,
    val isExternal: Boolean = false // For things like RingerSong/Themes that are external modules but managed here
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsStoreScreen(
    settings: PulseLinkSettings,
    onToggleBeaconLauncher: (Boolean) -> Unit,
    onToggleFirebaseMessaging: (Boolean) -> Unit,
    onToggleEmailFallback: (Boolean) -> Unit,
    onToggleCrashDetection: (Boolean) -> Unit,
    onToggleOtpCleanup: (Boolean) -> Unit,
    onToggleRemoteWebAccess: (Boolean) -> Unit,
    onToggleAiSummaries: (Boolean) -> Unit,
    onToggleThirdPartyExtensions: (Boolean) -> Unit,
    onToggleTruecaller: (Boolean) -> Unit,
    onToggleMergedExperience: (Boolean) -> Unit,
    onTogglePrivateSafe: (Boolean) -> Unit,
    onToggleSmartReplies: (Boolean) -> Unit,
    onOpenThemes: () -> Unit,
    onOpenRingerSong: () -> Unit,
    onBack: () -> Unit
) {
    val premiumActive = remember(settings) {
        BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
    }

    // Define all features
    val allFeatures = remember(settings) {
        listOf(
            FeatureToggle(
                id = "beacon",
                titleRes = R.string.extension_beacon_title,
                descriptionRes = R.string.extension_beacon_desc,
                icon = Icons.Filled.WifiTethering,
                isEnabled = settings.beaconLauncherEnabled,
                onToggle = onToggleBeaconLauncher
            ),
            FeatureToggle(
                id = "relay",
                titleRes = R.string.extension_relay_title,
                descriptionRes = R.string.extension_relay_desc,
                icon = Icons.Filled.CloudSync,
                isEnabled = settings.firebaseMessagingEnabled,
                onToggle = onToggleFirebaseMessaging
            ),
            FeatureToggle(
                id = "email_backup",
                titleRes = R.string.extension_email_backup_title,
                descriptionRes = R.string.extension_email_backup_desc,
                icon = Icons.Filled.Email,
                isEnabled = settings.emailFallbackEnabled,
                onToggle = onToggleEmailFallback
            ),
            FeatureToggle(
                id = "private_safe",
                titleRes = R.string.extension_private_safe_title,
                descriptionRes = R.string.extension_private_safe_desc,
                icon = Icons.Filled.Lock,
                isEnabled = settings.privateSafeEnabled,
                onToggle = onTogglePrivateSafe
            ),
            FeatureToggle(
                id = "smart_replies",
                titleRes = R.string.extension_smart_replies_title,
                descriptionRes = R.string.extension_smart_replies_desc,
                icon = Icons.Filled.Message,
                isEnabled = settings.smartRepliesEnabled,
                onToggle = onToggleSmartReplies
            ),
            FeatureToggle(
                id = "web",
                titleRes = R.string.extension_web_title,
                descriptionRes = R.string.extension_web_desc,
                icon = Icons.Filled.Laptop,
                isEnabled = settings.remoteWebAccessEnabled,
                onToggle = onToggleRemoteWebAccess,
                requiresPremium = true
            ),
            FeatureToggle(
                id = "otp",
                titleRes = R.string.extension_otp_title,
                descriptionRes = R.string.extension_otp_desc,
                icon = Icons.Filled.DeleteSweep,
                isEnabled = settings.otpCleanupEnabled,
                onToggle = onToggleOtpCleanup
            ),
            FeatureToggle(
                id = "ai",
                titleRes = R.string.extension_ai_title,
                descriptionRes = R.string.extension_ai_desc,
                icon = Icons.Filled.SmartToy,
                isEnabled = settings.aiSummariesEnabled,
                onToggle = onToggleAiSummaries,
                requiresPremium = true
            ),
            FeatureToggle(
                id = "unified",
                titleRes = R.string.extension_merged_title,
                descriptionRes = R.string.extension_merged_desc,
                icon = Icons.Filled.Layers,
                isEnabled = settings.mergedExperienceEnabled,
                onToggle = onToggleMergedExperience
            ),
            FeatureToggle(
                id = "third_party_extensions",
                titleRes = R.string.extension_third_party_title,
                descriptionRes = R.string.extension_third_party_desc,
                icon = Icons.Filled.Extension,
                isEnabled = settings.thirdPartyExtensionsEnabled,
                onToggle = onToggleThirdPartyExtensions,
                requiresPremium = true
            ),
            FeatureToggle(
                id = "truecaller",
                titleRes = R.string.extension_truecaller_title,
                descriptionRes = R.string.extension_truecaller_desc,
                icon = Icons.Filled.Search,
                isEnabled = settings.truecallerEnabled,
                onToggle = onToggleTruecaller
            ),
            FeatureToggle(
                id = "crash",
                titleRes = R.string.extension_crash_title,
                descriptionRes = R.string.extension_crash_desc,
                icon = Icons.Filled.CarCrash,
                isEnabled = settings.crashDetectionEnabled,
                onToggle = onToggleCrashDetection,
                isAvailable = BuildConfig.CRASH_DETECTION_ENABLED,
                requiresPremium = true
            )
        )
    }

    // Group features into categories
    val categories = remember(allFeatures) {
        listOf(
            ExtensionCategory(
                id = "core",
                title = "Core",
                items = allFeatures.filter { it.id in listOf("beacon", "relay") }
            ),
            ExtensionCategory(
                id = "safety",
                title = "Safety & Security",
                items = allFeatures.filter { it.id in listOf("email_backup", "crash", "private_safe") }
            ),
            ExtensionCategory(
                id = "smart",
                title = "Smart Features",
                items = allFeatures.filter { it.id in listOf("smart_replies", "otp", "ai") }
            ),
            ExtensionCategory(
                id = "integrations",
                title = "Integrations",
                items = allFeatures.filter { it.id in listOf("web", "unified", "third_party_extensions", "truecaller") }
            )
        )
    }

    val applyEssentials = {
        allFeatures.forEach { feature ->
            if (feature.requiresPremium && !premiumActive) return@forEach
            when (feature.id) {
                "beacon", "relay", "email_backup", "otp", "smart_replies" -> feature.onToggle(true)
                else -> feature.onToggle(false)
            }
        }
    }

    val applyPowerUser = {
        allFeatures.forEach { feature ->
            if (feature.requiresPremium && !premiumActive) return@forEach
            feature.onToggle(true)
        }
    }

    var selectedFeature by remember { mutableStateOf<FeatureToggle?>(null) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.extensions_store_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                StoreHeader()
            }

            item {
                QuickSetupSection(
                    applyEssentials = applyEssentials,
                    applyPowerUser = applyPowerUser
                )
            }

            // External Modules Section
            item {
                Text(
                    text = "Modules",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModuleCard(
                        title = "Theme Gallery",
                        description = "Customize the look and feel of your app",
                        icon = Icons.Filled.Palette,
                        onClick = onOpenThemes
                    )
                    ModuleCard(
                        title = "RingerSong",
                        description = "Ringtone progressions & streaming",
                        icon = Icons.Filled.MusicNote,
                        onClick = onOpenRingerSong
                    )
                }
            }

            categories.forEach { category ->
                item {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(category.items) { feature ->
                    FeatureStoreItem(
                        feature = feature,
                        premiumActive = premiumActive,
                        onClick = { selectedFeature = feature }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                ExtensionCTA()
            }
        }
    }

    if (selectedFeature != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedFeature = null },
            sheetState = sheetState
        ) {
            FeatureDetailSheet(
                feature = selectedFeature!!,
                premiumActive = premiumActive,
                onClose = { selectedFeature = null }
            )
        }
    }
}

@Composable
fun StoreHeader() {
    Column {
        Text(
            text = stringResource(R.string.extensions_store_header),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.extensions_store_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickSetupSection(
    applyEssentials: () -> Unit,
    applyPowerUser: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.extensions_store_quick_setup_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickSetupCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.extensions_store_quick_setup_essentials),
                description = stringResource(R.string.extensions_store_quick_setup_essentials_desc),
                icon = Icons.Filled.Bolt,
                onClick = applyEssentials
            )
            QuickSetupCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.extensions_store_quick_setup_power),
                description = stringResource(R.string.extensions_store_quick_setup_power_desc),
                icon = Icons.Filled.Star,
                onClick = applyPowerUser
            )
        }
    }
}

@Composable
fun ModuleCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Using Back arrow rotated 180 or ChevronRight would be better
                contentDescription = null,
                modifier = Modifier.rotate(180f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FeatureStoreItem(
    feature: FeatureToggle,
    premiumActive: Boolean,
    onClick: () -> Unit
) {
    val locked = feature.requiresPremium && !premiumActive

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = if (feature.isEnabled) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (feature.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (feature.isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(feature.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (feature.requiresPremium) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PREMIUM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(feature.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (feature.isEnabled) {
                 Icon(
                     imageVector = Icons.Filled.Check,
                     contentDescription = "Installed",
                     tint = MaterialTheme.colorScheme.primary
                 )
            } else {
                 Icon(
                     imageVector = Icons.Filled.Add,
                     contentDescription = "Install",
                     tint = MaterialTheme.colorScheme.onSurfaceVariant
                 )
            }
        }
    }
}

@Composable
fun FeatureDetailSheet(
    feature: FeatureToggle,
    premiumActive: Boolean,
    onClose: () -> Unit
) {
    val locked = feature.requiresPremium && !premiumActive
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text(
            text = stringResource(feature.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(feature.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (locked) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Requires PulseLink Premium",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (feature.isEnabled) {
                 Button(
                     onClick = {
                         feature.onToggle(false)
                         onClose()
                     },
                     colors = ButtonDefaults.buttonColors(
                         containerColor = MaterialTheme.colorScheme.errorContainer,
                         contentColor = MaterialTheme.colorScheme.onErrorContainer
                     ),
                     modifier = Modifier.weight(1f)
                 ) {
                     Text("Remove")
                 }
            } else {
                 Button(
                     onClick = {
                         feature.onToggle(true)
                         onClose()
                     },
                     enabled = !locked && feature.isAvailable,
                     modifier = Modifier.weight(1f)
                 ) {
                     Text(if (locked) "Locked" else "Install")
                 }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuickSetupCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                minLines = 2
            )
        }
    }
}

@Composable
private fun ExtensionCTA() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.extensions_store_external_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.extensions_store_external_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.extensions_store_external_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
