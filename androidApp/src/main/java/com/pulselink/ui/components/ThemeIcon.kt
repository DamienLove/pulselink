package com.pulselink.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.pulselink.domain.model.ThemePreferences

object ThemeIconKey {
    const val BACK = "icon.back"
    const val SETTINGS = "icon.settings"
    const val LOCK = "icon.lock"
    const val SEARCH = "icon.search"
    const val CLOSE = "icon.close"
    const val INBOX = "icon.inbox"
    const val ARCHIVE = "icon.archive"
    const val UNARCHIVE = "icon.unarchive"
    const val DELETE = "icon.delete"
    const val EDIT = "icon.edit"
    const val REFRESH = "icon.refresh"
    const val NOTIFICATIONS = "icon.notifications"
    const val PALETTE = "icon.palette"
    const val SEND = "icon.send"
    const val AI = "icon.ai"
    const val ARROW_DOWN = "icon.arrow_down"
    const val ATTACH = "icon.attach"
}

@Composable
fun ThemeIcon(
    iconKey: String,
    theme: ThemePreferences,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val overrideUrl = theme.iconOverrides[iconKey]?.trim().orEmpty()
    if (overrideUrl.isBlank()) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(overrideUrl)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}
