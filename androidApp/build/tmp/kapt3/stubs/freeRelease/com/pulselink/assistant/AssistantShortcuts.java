package com.pulselink.assistant;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import com.pulselink.R;
import com.pulselink.ui.AssistantShortcutActivity;
import kotlinx.coroutines.Dispatchers;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0004H\u0002J\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u000e\u001a\u00020\u000fJ\u000e\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u000e\u001a\u00020\u000fJ\u001c\u0010\u0014\u001a\u0004\u0018\u00010\u00152\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0016\u001a\u0004\u0018\u00010\u0004H\u0002J\u0012\u0010\u0017\u001a\u0004\u0018\u00010\u00042\u0006\u0010\u000e\u001a\u00020\u000fH\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lcom/pulselink/assistant/AssistantShortcuts;", "", "()V", "PACKAGE_GEMINI", "", "PACKAGE_GOOGLE_APP", "PACKAGE_GOOGLE_ASSISTANT", "SHORTCUT_CHECK_IN", "SHORTCUT_EMERGENCY", "TAG", "shortcutScope", "Lkotlinx/coroutines/CoroutineScope;", "isInstalled", "", "context", "Landroid/content/Context;", "packageName", "launchShortcutSettings", "", "publish", "resolveAssistantEntryIntent", "Landroid/content/Intent;", "assistantPackage", "resolveAssistantPackage", "androidApp_freeRelease"})
public final class AssistantShortcuts {
    @org.jetbrains.annotations.NotNull()
    private static final kotlinx.coroutines.CoroutineScope shortcutScope = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHORTCUT_EMERGENCY = "assistant_emergency";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SHORTCUT_CHECK_IN = "assistant_check_in";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AssistantShortcuts";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PACKAGE_GEMINI = "com.google.android.apps.bard";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PACKAGE_GOOGLE_APP = "com.google.android.googlequicksearchbox";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PACKAGE_GOOGLE_ASSISTANT = "com.google.android.apps.googleassistant";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.assistant.AssistantShortcuts INSTANCE = null;
    
    private AssistantShortcuts() {
        super();
    }
    
    public final void publish(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void launchShortcutSettings(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    private final android.content.Intent resolveAssistantEntryIntent(android.content.Context context, java.lang.String assistantPackage) {
        return null;
    }
    
    private final java.lang.String resolveAssistantPackage(android.content.Context context) {
        return null;
    }
    
    private final boolean isInstalled(android.content.Context context, java.lang.String packageName) {
        return false;
    }
}