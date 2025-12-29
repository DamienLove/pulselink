package com.pulselink.beacon.ui.customize

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulselink.beacon.data.InboxIconVariant
import com.pulselink.beacon.data.ThemeFont
import com.pulselink.beacon.data.ThemePalette
import com.pulselink.beacon.data.ThemeState
import com.pulselink.beacon.data.ThemeTarget
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    address: String?,
    themeState: ThemeState,
    onBack: () -> Unit,
    onColorChange: (ThemeTarget, Color) -> Unit,
    onFontChange: (ThemeFont) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onPreset: (ThemePalette) -> Unit,
    onResetContact: () -> Unit,
    onIconVariant: (InboxIconVariant) -> Unit
) {
    val scopeLabel = address?.takeIf { it.isNotBlank() } ?: "All new chats"
    val currentTheme = themeState.forAddress(address)
    var selectedTarget by remember { mutableStateOf(ThemeTarget.OutgoingBubble) }
    val iconTint = currentTheme.accentColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize - $scopeLabel") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iconTint)
                    }
                },
                actions = {
                    IconButton(onClick = { onColorChange(ThemeTarget.Accent, currentTheme.accentColor) }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Accent", tint = iconTint)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = currentTheme.threadBackgroundColor,
                    titleContentColor = currentTheme.frameColor,
                    navigationIconContentColor = iconTint,
                    actionIconContentColor = iconTint
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScopeBanner(
                address = address,
                onResetContact = onResetContact
            )

            PreviewSample(
                theme = currentTheme,
                selected = selectedTarget,
                onSelect = { selectedTarget = it }
            )

            ThemesRow(onPreset = { onPreset(it) })

            PopularColorsRow(
                onSelect = { color -> onColorChange(selectedTarget, color) },
                iconTint = iconTint
            )

            ColorWheel(
                selectedColor = currentTheme.accentColor,
                onSelect = { onColorChange(selectedTarget, it) }
            )

            FontRow(currentTheme.font, onFontChange, iconTint)

            RadiusSlider(currentTheme.bubbleRadius, onRadiusChange, iconTint)

            IconRow(currentTheme.iconVariant, onIconVariant, iconTint)
        }
    }
}

@Composable
private fun ScopeBanner(address: String?, onResetContact: () -> Unit) {
    val isContact = !address.isNullOrBlank()
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isContact) "Contact-specific" else "Global default",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isContact) "Changes override only this contact. Reset to fall back to global." else "Applies to all new chats and inbox.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (isContact) {
                OutlinedButton(onClick = onResetContact) {
                    Text("Use global")
                }
            }
        }
    }
}

@Composable
private fun PreviewSample(
    theme: ThemePalette,
    selected: ThemeTarget,
    onSelect: (ThemeTarget) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.threadBackgroundColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Tap a part to adjust",
                style = MaterialTheme.typography.labelLarge,
                color = theme.frameColor
            )
            BubblePreview(
                text = "Hey! Want to grab coffee later?",
                isOutgoing = false,
                theme = theme,
                selected = selected == ThemeTarget.IncomingBubble,
                onClick = { onSelect(ThemeTarget.IncomingBubble) }
            )
            BubblePreview(
                text = "Sure! 5pm works. Also look at this new inbox icon.",
                isOutgoing = true,
                theme = theme,
                selected = selected == ThemeTarget.OutgoingBubble,
                onClick = { onSelect(ThemeTarget.OutgoingBubble) }
            )
            Surface(
                shape = RoundedCornerShape(theme.bubbleRadius.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor),
                color = theme.inboxBackgroundColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(ThemeTarget.InboxBackground) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Inbox preview", fontWeight = FontWeight.SemiBold, color = theme.frameColor)
                    Icon(iconForVariant(theme.iconVariant), contentDescription = null, tint = theme.accentColor)
                }
            }
        }
    }
}

@Composable
private fun BubblePreview(
    text: String,
    isOutgoing: Boolean,
    theme: ThemePalette,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(theme.bubbleRadius.dp)
    val background = if (isOutgoing) theme.outgoingColor else theme.incomingColor
    val textColor = preferredTextColor(background)
    val secondary = textColor.copy(alpha = 0.7f)
    Surface(
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.frameColor.copy(alpha = 0.6f)),
        shape = shape,
        tonalElevation = if (selected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor)
            Text(
                if (isOutgoing) "You - 5:00 PM" else "Alex - 5:00 PM",
                style = MaterialTheme.typography.labelSmall,
                color = secondary
            )
        }
    }
}

@Composable
private fun PopularColorsRow(onSelect: (Color) -> Unit, iconTint: Color) {
    val popular = listOf(
        Color(0xFF1B6EF3),
        Color(0xFF10B981),
        Color(0xFFFFB020),
        Color(0xFFEF4444),
        Color(0xFF8B5CF6),
        Color(0xFF0EA5E9),
        Color(0xFF111827)
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ColorLens, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Popular quick-picks")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(popular.size) { idx ->
                val color = popular[idx]
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelect(color) }
                )
            }
        }
    }
}

@Composable
private fun ColorWheel(selectedColor: Color, onSelect: (Color) -> Unit) {
    val gradient = listOf(
        Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val radius = min(size.width, size.height) / 2f
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx.toDouble(), dy.toDouble())
                        if (dist <= radius.toDouble()) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            val color = Color.hsv(angle, 0.8f, 1f)
                            onSelect(color)
                        }
                    }
                }
        ) {
            drawCircle(brush = Brush.sweepGradient(gradient))
            drawCircle(color = Color.White.copy(alpha = 0.12f), radius = min(size.width, size.height) / 2.6f)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(selectedColor)
        )
    }
}

@Composable
private fun FontRow(current: ThemeFont, onFontChange: (ThemeFont) -> Unit, iconTint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TextFields, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Font style")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeFont.values().forEach { font ->
                FilterChip(
                    selected = current == font,
                    onClick = { onFontChange(font) },
                    label = { Text(font.label) }
                )
            }
        }
    }
}

@Composable
private fun RadiusSlider(current: Float, onRadiusChange: (Float) -> Unit, iconTint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Bubble corners")
        }
        Slider(
            value = current,
            onValueChange = { onRadiusChange(it) },
            valueRange = 6f..28f,
            steps = 8
        )
    }
}

private data class ThemePreset(
    val name: String,
    val palette: ThemePalette
)

@Composable
private fun ThemesRow(onPreset: (ThemePalette) -> Unit) {
    val themes = listOf(
        ThemePreset(
            "Clear Sky",
            ThemePalette.default().copy(
                incoming = Color(0xFFE8F4FF).toArgb().toLong(),
                outgoing = Color(0xFFD0E8FF).toArgb().toLong(),
                frame = Color(0xFF1B6EF3).toArgb().toLong(),
                accent = Color(0xFF1B6EF3).toArgb().toLong(),
                threadBackground = Color(0xFFF5F8FF).toArgb().toLong(),
                inboxBackground = Color(0xFFFFFFFF).toArgb().toLong(),
                font = ThemeFont.System,
                iconVariant = InboxIconVariant.Beacon
            )
        ),
        ThemePreset(
            "Midnight OLED",
            ThemePalette.default().copy(
                incoming = Color(0xFF111827).toArgb().toLong(),
                outgoing = Color(0xFF0B1220).toArgb().toLong(),
                frame = Color(0xFF10B981).toArgb().toLong(),
                accent = Color(0xFF10B981).toArgb().toLong(),
                threadBackground = Color(0xFF000000).toArgb().toLong(),
                inboxBackground = Color(0xFF050505).toArgb().toLong(),
                font = ThemeFont.Mono,
                bubbleRadius = 16f,
                iconVariant = InboxIconVariant.Shield
            )
        ),
        ThemePreset(
            "Sunrise",
            ThemePalette.default().copy(
                incoming = Color(0xFFFFF3E0).toArgb().toLong(),
                outgoing = Color(0xFFFFE0B2).toArgb().toLong(),
                frame = Color(0xFFFF9800).toArgb().toLong(),
                accent = Color(0xFFFF9800).toArgb().toLong(),
                threadBackground = Color(0xFFFFFBF2).toArgb().toLong(),
                inboxBackground = Color(0xFFFFF7ED).toArgb().toLong(),
                font = ThemeFont.Rounded,
                bubbleRadius = 20f,
                iconVariant = InboxIconVariant.Bubble
            )
        ),
        ThemePreset(
            "Forest Trail",
            ThemePalette.default().copy(
                incoming = Color(0xFFD1FAE5).toArgb().toLong(),
                outgoing = Color(0xFFBBF7D0).toArgb().toLong(),
                frame = Color(0xFF047857).toArgb().toLong(),
                accent = Color(0xFF10B981).toArgb().toLong(),
                threadBackground = Color(0xFFECFDF5).toArgb().toLong(),
                inboxBackground = Color(0xFFF0FDF4).toArgb().toLong(),
                font = ThemeFont.Serif,
                bubbleRadius = 18f,
                iconVariant = InboxIconVariant.Beacon
            )
        ),
        ThemePreset(
            "Lavender Haze",
            ThemePalette.default().copy(
                incoming = Color(0xFFEDE9FE).toArgb().toLong(),
                outgoing = Color(0xFFC4B5FD).toArgb().toLong(),
                frame = Color(0xFF6D28D9).toArgb().toLong(),
                accent = Color(0xFF7C3AED).toArgb().toLong(),
                threadBackground = Color(0xFFF5F3FF).toArgb().toLong(),
                inboxBackground = Color(0xFFF8FAFF).toArgb().toLong(),
                font = ThemeFont.Rounded,
                bubbleRadius = 22f,
                iconVariant = InboxIconVariant.Shield
            )
        ),
        ThemePreset(
            "Citrus Pop",
            ThemePalette.default().copy(
                incoming = Color(0xFFF7FEE7).toArgb().toLong(),
                outgoing = Color(0xFFDCFCE7).toArgb().toLong(),
                frame = Color(0xFF65A30D).toArgb().toLong(),
                accent = Color(0xFF84CC16).toArgb().toLong(),
                threadBackground = Color(0xFFF7FEE7).toArgb().toLong(),
                inboxBackground = Color(0xFFFFFFFF).toArgb().toLong(),
                font = ThemeFont.System,
                bubbleRadius = 16f,
                iconVariant = InboxIconVariant.Minimal
            )
        ),
        ThemePreset(
            "Ember Glow",
            ThemePalette.default().copy(
                incoming = Color(0xFFFFEDD5).toArgb().toLong(),
                outgoing = Color(0xFFFED7AA).toArgb().toLong(),
                frame = Color(0xFFEA580C).toArgb().toLong(),
                accent = Color(0xFFF97316).toArgb().toLong(),
                threadBackground = Color(0xFFFFF7ED).toArgb().toLong(),
                inboxBackground = Color(0xFFFFFBF5).toArgb().toLong(),
                font = ThemeFont.Serif,
                bubbleRadius = 18f,
                iconVariant = InboxIconVariant.Bubble
            )
        ),
        ThemePreset(
            "Slate Mono",
            ThemePalette.default().copy(
                incoming = Color(0xFFE2E8F0).toArgb().toLong(),
                outgoing = Color(0xFFCBD5E1).toArgb().toLong(),
                frame = Color(0xFF334155).toArgb().toLong(),
                accent = Color(0xFF475569).toArgb().toLong(),
                threadBackground = Color(0xFFF8FAFC).toArgb().toLong(),
                inboxBackground = Color(0xFFF1F5F9).toArgb().toLong(),
                font = ThemeFont.Mono,
                bubbleRadius = 12f,
                iconVariant = InboxIconVariant.Minimal
            )
        ),
        ThemePreset(
            "Cyber Mist",
            ThemePalette.default().copy(
                incoming = Color(0xFF4C1D95).toArgb().toLong(),
                outgoing = Color(0xFFD946EF).toArgb().toLong(),
                frame = Color(0xFFD946EF).toArgb().toLong(),
                accent = Color(0xFFD946EF).toArgb().toLong(),
                threadBackground = Color(0xFF1E1B4B).toArgb().toLong(),
                inboxBackground = Color(0xFF2E1065).toArgb().toLong(),
                font = ThemeFont.Mono,
                bubbleRadius = 18f,
                iconVariant = InboxIconVariant.Shield
            )
        ),
        ThemePreset(
            "Deep Ocean",
            ThemePalette.default().copy(
                incoming = Color(0xFF172554).toArgb().toLong(),
                outgoing = Color(0xFF0EA5E9).toArgb().toLong(),
                frame = Color(0xFF1E3A8A).toArgb().toLong(),
                accent = Color(0xFF0EA5E9).toArgb().toLong(),
                threadBackground = Color(0xFF0F172A).toArgb().toLong(),
                inboxBackground = Color(0xFF1E3A8A).toArgb().toLong(),
                font = ThemeFont.System,
                bubbleRadius = 24f,
                iconVariant = InboxIconVariant.Beacon
            )
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Themes", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(themes) { preset ->
                ThemePreviewCard(preset = preset, onSelect = { onPreset(preset.palette) })
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(preset: ThemePreset, onSelect: () -> Unit) {
    val palette = preset.palette
    val titleColor = if (palette.threadBackgroundColor.luminance() < 0.4f) Color.White else palette.frameColor
    val incomingText = preferredTextColor(palette.incomingColor)
    val outgoingText = preferredTextColor(palette.outgoingColor)
    val bubbleShape = RoundedCornerShape(palette.bubbleRadius.dp)
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(containerColor = palette.threadBackgroundColor),
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconForVariant(palette.iconVariant),
                    contentDescription = null,
                    tint = palette.accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Beacon", style = MaterialTheme.typography.labelSmall, color = titleColor)
            }
            Surface(
                shape = bubbleShape,
                border = BorderStroke(1.dp, palette.frameColor.copy(alpha = 0.5f)),
                color = palette.incomingColor
            ) {
                Text(
                    "Incoming check-in.",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = incomingText
                )
            }
            Surface(
                shape = bubbleShape,
                border = BorderStroke(1.dp, palette.frameColor.copy(alpha = 0.5f)),
                color = palette.outgoingColor
            ) {
                Text(
                    "Reply sent.",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = outgoingText
                )
            }
            Text(preset.name, style = MaterialTheme.typography.labelMedium, color = titleColor)
        }
    }
}

@Composable
private fun IconRow(current: InboxIconVariant, onIconVariant: (InboxIconVariant) -> Unit, iconTint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Inbox icon")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InboxIconVariant.values().forEach { variant ->
                FilterChip(
                    selected = current == variant,
                    onClick = { onIconVariant(variant) },
                    label = { Text(variant.label) }
                )
            }
        }
    }
}

private fun preferredTextColor(background: Color): Color =
    if (background.luminance() < 0.5f) Color.White else Color.Black

private fun iconForVariant(variant: InboxIconVariant) = when (variant) {
    InboxIconVariant.Beacon -> Icons.Default.WaterDrop
    InboxIconVariant.Bubble -> Icons.Default.Chat
    InboxIconVariant.Shield -> Icons.Default.Shield
    InboxIconVariant.Minimal -> Icons.Default.Inbox
}
