package com.pulselink.ui.screens

import com.pulselink.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.R
import com.pulselink.domain.model.PulseLinkSettings

data class FeatureToggle(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onToggle: (Boolean) -> Unit,
    val isAvailable: Boolean = true,
    val requiresPremium: Boolean = false
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
    onToggleMergedExperience: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val premiumActive = remember(settings) {
        BuildConfig.PREMIUM_FEATURES || settings.premiumUnlocked
    }

    val features = remember(settings) {
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

    val applyEssentials = {
        features.forEach { feature ->
            if (feature.requiresPremium && !premiumActive) return@forEach
            when (feature.id) {
                "beacon", "relay", "email_backup", "otp" -> feature.onToggle(true)
                else -> feature.onToggle(false)
            }
        }
    }

    val applyPowerUser = {
        features.forEach { feature ->
            if (feature.requiresPremium && !premiumActive) return@forEach
            feature.onToggle(true)
        }
    }

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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.extensions_store_header),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.extensions_store_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Quick Setup Section
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.settings_extensions_store_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            // Feature List
            // Using Column here because we are already in a scrollable Column.
            // LazyVerticalGrid inside a verticalScroll Column causes crash or weird behavior unless fixed height.
            // Since the list is short, a loop is fine.
            features.forEach { feature ->
                FeatureCard(feature, premiumActive)
            }

            Spacer(modifier = Modifier.height(8.dp))

            ExtensionCTA()
        }
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
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
fun FeatureCard(feature: FeatureToggle, premiumActive: Boolean) {
    val locked = feature.requiresPremium && !premiumActive
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (feature.isEnabled && !locked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (feature.isEnabled && !locked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(feature.titleRes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (feature.isEnabled && !locked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (feature.requiresPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.extensions_store_premium_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (locked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Switch(
                    checked = feature.isEnabled && !locked,
                    onCheckedChange = {
                        if (!locked) feature.onToggle(it)
                    },
                    enabled = feature.isAvailable && !locked
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(feature.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = if (feature.isEnabled && !locked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!feature.isAvailable) {
                Text(
                    text = stringResource(R.string.extensions_store_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (locked) {
                Text(
                    text = stringResource(R.string.extensions_store_premium_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.extensions_store_external_action),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Placeholder for Developer Mode on Android
            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Developer Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To test 3rd party extensions on Android, use the Web Client or wait for the upcoming Android Dev Menu update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
