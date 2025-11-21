package com.pulselink.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import com.pulselink.R;
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor;
import com.pulselink.data.assistant.VoiceCommandResult;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.service.AlertRouter;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Locale;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000J\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 *2\u00020\u0001:\u0001*B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0014\u0010\u0015\u001a\u0004\u0018\u00010\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0002J\u0014\u0010\u0019\u001a\u0004\u0018\u00010\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0002J\b\u0010\u001a\u001a\u00020\u001bH\u0002J\u000e\u0010\u001c\u001a\u00020\u001bH\u0082@\u00a2\u0006\u0002\u0010\u001dJ\u0016\u0010\u001e\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020\u0016H\u0082@\u00a2\u0006\u0002\u0010 J\u0016\u0010!\u001a\u00020\u001b2\u0006\u0010\"\u001a\u00020\u0016H\u0082@\u00a2\u0006\u0002\u0010 J\u0012\u0010#\u001a\u00020\u001b2\b\u0010$\u001a\u0004\u0018\u00010%H\u0014J\u0016\u0010&\u001a\u00020\u001b2\u0006\u0010\'\u001a\u00020(H\u0082@\u00a2\u0006\u0002\u0010)R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001e\u0010\u000f\u001a\u00020\u00108\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014\u00a8\u0006+"}, d2 = {"Lcom/pulselink/ui/AssistantShortcutActivity;", "Landroidx/activity/ComponentActivity;", "()V", "alertRouter", "Lcom/pulselink/service/AlertRouter;", "getAlertRouter", "()Lcom/pulselink/service/AlertRouter;", "setAlertRouter", "(Lcom/pulselink/service/AlertRouter;)V", "contactLinkManager", "Lcom/pulselink/data/link/ContactLinkManager;", "getContactLinkManager", "()Lcom/pulselink/data/link/ContactLinkManager;", "setContactLinkManager", "(Lcom/pulselink/data/link/ContactLinkManager;)V", "naturalLanguageCommandProcessor", "Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;", "getNaturalLanguageCommandProcessor", "()Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;", "setNaturalLanguageCommandProcessor", "(Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;)V", "extractDeepLinkFeature", "", "intent", "Landroid/content/Intent;", "extractVoiceQuery", "finishSilently", "", "handleCancelEmergency", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleShortcutFeature", "feature", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleVoiceCommand", "query", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "triggerAlert", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "(Lcom/pulselink/domain/model/EscalationTier;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "androidApp_proDebug"})
public final class AssistantShortcutActivity extends androidx.activity.ComponentActivity {
    @javax.inject.Inject()
    public com.pulselink.service.AlertRouter alertRouter;
    @javax.inject.Inject()
    public com.pulselink.data.assistant.NaturalLanguageCommandProcessor naturalLanguageCommandProcessor;
    @javax.inject.Inject()
    public com.pulselink.data.link.ContactLinkManager contactLinkManager;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_ASSISTANT_EMERGENCY = "com.pulselink.intent.ASSISTANT_EMERGENCY";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_ASSISTANT_CHECK_IN = "com.pulselink.intent.ASSISTANT_CHECK_IN";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_ASSISTANT_VOICE = "com.pulselink.intent.ASSISTANT_VOICE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_VOICE_QUERY = "com.pulselink.extra.VOICE_QUERY";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FEATURE_EMERGENCY = "emergency";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String FEATURE_CANCEL = "cancel";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.ui.AssistantShortcutActivity.Companion Companion = null;
    
    public AssistantShortcutActivity() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.service.AlertRouter getAlertRouter() {
        return null;
    }
    
    public final void setAlertRouter(@org.jetbrains.annotations.NotNull()
    com.pulselink.service.AlertRouter p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.assistant.NaturalLanguageCommandProcessor getNaturalLanguageCommandProcessor() {
        return null;
    }
    
    public final void setNaturalLanguageCommandProcessor(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.assistant.NaturalLanguageCommandProcessor p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.link.ContactLinkManager getContactLinkManager() {
        return null;
    }
    
    public final void setContactLinkManager(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.ContactLinkManager p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    private final java.lang.Object handleVoiceCommand(java.lang.String query, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleShortcutFeature(java.lang.String feature, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.String extractVoiceQuery(android.content.Intent intent) {
        return null;
    }
    
    private final java.lang.String extractDeepLinkFeature(android.content.Intent intent) {
        return null;
    }
    
    private final java.lang.Object triggerAlert(com.pulselink.domain.model.EscalationTier tier, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleCancelEmergency(kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void finishSilently() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/pulselink/ui/AssistantShortcutActivity$Companion;", "", "()V", "ACTION_ASSISTANT_CHECK_IN", "", "ACTION_ASSISTANT_EMERGENCY", "ACTION_ASSISTANT_VOICE", "EXTRA_VOICE_QUERY", "FEATURE_CANCEL", "FEATURE_EMERGENCY", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}