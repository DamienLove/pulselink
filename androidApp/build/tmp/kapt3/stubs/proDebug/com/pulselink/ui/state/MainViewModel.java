package com.pulselink.ui.state;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.os.Build;
import androidx.lifecycle.ViewModel;
import com.pulselink.BuildConfig;
import com.pulselink.R;
import com.pulselink.auth.AuthState;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.alert.AlertDispatcher.AlertResult;
import com.pulselink.data.assistant.NaturalLanguageCommandProcessor;
import com.pulselink.data.assistant.VoiceCommandResult;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.ContactMessage;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.model.ManualMessageResult;
import com.pulselink.domain.model.SoundCategory;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.BetaAgreementRepository;
import com.pulselink.domain.repository.BlockedContactRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.service.AlertRouter;
import com.pulselink.ui.screens.BugReportData;
import com.pulselink.ui.state.DndStatusMessage;
import com.pulselink.util.AudioOverrideManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u00f0\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u001a\b\u0007\u0018\u0000 |2\u00020\u0001:\u0002{|B_\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\u0006\u0010\u0012\u001a\u00020\u0013\u0012\u0006\u0010\u0014\u001a\u00020\u0015\u0012\u0006\u0010\u0016\u001a\u00020\u0017\u00a2\u0006\u0002\u0010\u0018J\u001c\u0010/\u001a\u0002002\u0014\b\u0002\u00101\u001a\u000e\u0012\u0004\u0012\u00020&\u0012\u0004\u0012\u00020002J\u000e\u00103\u001a\u0002002\u0006\u00104\u001a\u000205J\u0016\u00106\u001a\u0002072\u0006\u00108\u001a\u0002092\u0006\u0010:\u001a\u00020;J\u001a\u0010<\u001a\u0002002\u0012\u0010=\u001a\u000e\u0012\u0004\u0012\u00020&\u0012\u0004\u0012\u00020002J\u0006\u0010>\u001a\u000200J\u0006\u0010?\u001a\u000200J\u000e\u0010@\u001a\u0002002\u0006\u0010A\u001a\u000205J\u0006\u0010B\u001a\u000200J\u0018\u0010C\u001a\u0002002\u0006\u0010D\u001a\u00020E2\u0006\u0010F\u001a\u00020,H\u0002J\u0012\u0010G\u001a\u0002002\b\u0010H\u001a\u0004\u0018\u00010IH\u0002J\u0010\u0010J\u001a\u00020K2\u0006\u0010L\u001a\u00020KH\u0002J\u001e\u0010M\u001a\u00020N2\u0006\u00104\u001a\u0002052\u0006\u0010O\u001a\u00020,H\u0086@\u00a2\u0006\u0002\u0010PJ\u001a\u0010Q\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020S0\"0R2\u0006\u00104\u001a\u000205J\u000e\u0010T\u001a\u00020&2\u0006\u0010L\u001a\u00020KJ\u0016\u0010U\u001a\u0002002\u0006\u00104\u001a\u0002052\u0006\u0010V\u001a\u000205J\u0016\u0010W\u001a\u00020X2\u0006\u0010Y\u001a\u00020,H\u0086@\u00a2\u0006\u0002\u0010ZJ\u0014\u0010[\u001a\u0002002\f\u0010\\\u001a\b\u0012\u0004\u0012\u0002050\"J\u000e\u0010]\u001a\u0002002\u0006\u0010^\u001a\u00020_J\u0006\u0010`\u001a\u000200J\u000e\u0010a\u001a\u0002002\u0006\u00104\u001a\u000205J\u001e\u0010b\u001a\u00020c2\u0006\u00104\u001a\u0002052\u0006\u0010d\u001a\u00020,H\u0086@\u00a2\u0006\u0002\u0010PJ\u0016\u0010e\u001a\u00020&2\u0006\u00104\u001a\u000205H\u0086@\u00a2\u0006\u0002\u0010fJ\u000e\u0010g\u001a\u0002002\u0006\u0010h\u001a\u00020&J\u000e\u0010i\u001a\u0002002\u0006\u0010h\u001a\u00020&J\u000e\u0010j\u001a\u0002002\u0006\u0010h\u001a\u00020&J\u000e\u0010k\u001a\u0002002\u0006\u0010l\u001a\u00020,J\u000e\u0010m\u001a\u0002002\u0006\u0010h\u001a\u00020&J\u0016\u0010n\u001a\u0002002\u0006\u00104\u001a\u0002052\u0006\u0010o\u001a\u00020&J\u0016\u0010p\u001a\u0002002\u0006\u00104\u001a\u0002052\u0006\u0010o\u001a\u00020&J\u0006\u0010q\u001a\u000200J\u0006\u0010r\u001a\u000200J\u000e\u0010s\u001a\u0002002\u0006\u0010t\u001a\u00020,J\u000e\u0010u\u001a\u0002002\u0006\u0010t\u001a\u00020,J\u000e\u0010v\u001a\u0002002\u0006\u0010^\u001a\u00020_J\"\u0010w\u001a\u0002002\u0006\u00104\u001a\u0002052\b\u0010x\u001a\u0004\u0018\u00010,2\b\u0010y\u001a\u0004\u0018\u00010,J\u000e\u0010z\u001a\u0002002\u0006\u0010t\u001a\u00020,R\u0014\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010!\u001a\b\u0012\u0004\u0012\u00020#0\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010$\u001a\b\u0012\u0004\u0012\u00020#0\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010%\u001a\b\u0012\u0004\u0012\u00020&0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\'\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010(0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010)\u001a\b\u0012\u0004\u0012\u00020&0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010*\u001a\b\u0012\u0004\u0012\u00020#0\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010+\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010,0\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010-\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001d\u00a2\u0006\b\n\u0000\u001a\u0004\b.\u0010 \u00a8\u0006}"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel;", "Landroidx/lifecycle/ViewModel;", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "alertRepository", "Lcom/pulselink/domain/repository/AlertRepository;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "alertRouter", "Lcom/pulselink/service/AlertRouter;", "soundCatalog", "Lcom/pulselink/data/alert/SoundCatalog;", "linkManager", "Lcom/pulselink/data/link/ContactLinkManager;", "messageRepository", "Lcom/pulselink/domain/repository/MessageRepository;", "blockedContactRepository", "Lcom/pulselink/domain/repository/BlockedContactRepository;", "betaAgreementRepository", "Lcom/pulselink/domain/repository/BetaAgreementRepository;", "naturalLanguageCommandProcessor", "Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;", "firebaseAuthManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "(Lcom/pulselink/domain/repository/ContactRepository;Lcom/pulselink/domain/repository/AlertRepository;Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/service/AlertRouter;Lcom/pulselink/data/alert/SoundCatalog;Lcom/pulselink/data/link/ContactLinkManager;Lcom/pulselink/domain/repository/MessageRepository;Lcom/pulselink/domain/repository/BlockedContactRepository;Lcom/pulselink/domain/repository/BetaAgreementRepository;Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;Lcom/pulselink/auth/FirebaseAuthManager;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pulselink/ui/state/PulseLinkUiState;", "authState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/pulselink/auth/AuthState;", "getAuthState", "()Lkotlinx/coroutines/flow/StateFlow;", "callSounds", "", "Lcom/pulselink/domain/model/SoundOption;", "checkInSounds", "dispatching", "", "dndStatus", "Lcom/pulselink/ui/state/DndStatusMessage;", "emergencyActive", "emergencySounds", "lastMessage", "", "uiState", "getUiState", "acceptBetaAgreement", "", "onResult", "Lkotlin/Function1;", "approveLink", "contactId", "", "buildBugReportUri", "Landroid/net/Uri;", "context", "Landroid/content/Context;", "bugReportData", "Lcom/pulselink/ui/screens/BugReportData;", "cancelEmergency", "onComplete", "clearDndStatusMessage", "completeOnboarding", "deleteContact", "id", "dismissAssistantHint", "dispatch", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "phrase", "emitDndStatus", "result", "Lcom/pulselink/data/alert/AlertDispatcher$AlertResult;", "ensureSoundDefaults", "Lcom/pulselink/domain/model/PulseLinkSettings;", "settings", "initiateCall", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult;", "phoneNumber", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "messagesForContact", "Lkotlinx/coroutines/flow/Flow;", "Lcom/pulselink/domain/model/ContactMessage;", "needsBetaAgreement", "notifyCallEnded", "callDuration", "processVoiceCommand", "Lcom/pulselink/data/assistant/VoiceCommandResult;", "query", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "reorderContacts", "contactIds", "saveContact", "contact", "Lcom/pulselink/domain/model/Contact;", "sendCheckIn", "sendLinkRequest", "sendManualMessage", "Lcom/pulselink/domain/model/ManualMessageResult;", "message", "sendPing", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setAutoAllowRemoteSoundChange", "enabled", "setBetaTesterStatus", "setIncludeLocation", "setOwnerName", "name", "setProUnlocked", "setRemoteOverridePermission", "allow", "setRemoteSoundPermission", "signOut", "triggerEmergency", "updateCallSound", "key", "updateCheckInSound", "updateContact", "updateContactSounds", "emergencyKey", "checkInKey", "updateEmergencySound", "CallInitiationResult", "Companion", "androidApp_proDebug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class MainViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.ContactRepository contactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.AlertRepository alertRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.service.AlertRouter alertRouter = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.SoundCatalog soundCatalog = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.link.ContactLinkManager linkManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.MessageRepository messageRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.BlockedContactRepository blockedContactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.BetaAgreementRepository betaAgreementRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.assistant.NaturalLanguageCommandProcessor naturalLanguageCommandProcessor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.auth.FirebaseAuthManager firebaseAuthManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> dispatching = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> lastMessage = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pulselink.ui.state.DndStatusMessage> dndStatus = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> emergencyActive = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> emergencySounds = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> checkInSounds = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> callSounds = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pulselink.ui.state.PulseLinkUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pulselink.ui.state.PulseLinkUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> authState = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "MainViewModel";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BUG_REPORT_PAGE_URL = "https://DamienLove.github.io/PulseLink/bug-report/";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String BETA_AGREEMENT_VERSION = "2025-11-13";
    private static final long REMOTE_BETA_AGREEMENT_TIMEOUT_MS = 10000L;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.ui.state.MainViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public MainViewModel(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.ContactRepository contactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.AlertRepository alertRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.service.AlertRouter alertRouter, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.SoundCatalog soundCatalog, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.ContactLinkManager linkManager, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.MessageRepository messageRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.BlockedContactRepository blockedContactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.BetaAgreementRepository betaAgreementRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.assistant.NaturalLanguageCommandProcessor naturalLanguageCommandProcessor, @org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager firebaseAuthManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pulselink.ui.state.PulseLinkUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> getAuthState() {
        return null;
    }
    
    public final void saveContact(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact) {
    }
    
    public final void deleteContact(long id) {
    }
    
    public final void triggerEmergency() {
    }
    
    public final void sendCheckIn() {
    }
    
    public final void signOut() {
    }
    
    public final void reorderContacts(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> contactIds) {
    }
    
    public final void setIncludeLocation(boolean enabled) {
    }
    
    public final void setOwnerName(@org.jetbrains.annotations.NotNull()
    java.lang.String name) {
    }
    
    public final void setAutoAllowRemoteSoundChange(boolean enabled) {
    }
    
    public final void acceptBetaAgreement(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onResult) {
    }
    
    public final void dismissAssistantHint() {
    }
    
    public final void updateEmergencySound(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
    }
    
    public final void updateCheckInSound(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
    }
    
    public final void updateCallSound(@org.jetbrains.annotations.NotNull()
    java.lang.String key) {
    }
    
    public final void updateContact(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact) {
    }
    
    public final void updateContactSounds(long contactId, @org.jetbrains.annotations.Nullable()
    java.lang.String emergencyKey, @org.jetbrains.annotations.Nullable()
    java.lang.String checkInKey) {
    }
    
    public final void sendLinkRequest(long contactId) {
    }
    
    public final void approveLink(long contactId) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendPing(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    public final void setRemoteSoundPermission(long contactId, boolean allow) {
    }
    
    public final void setRemoteOverridePermission(long contactId, boolean allow) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendManualMessage(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.ManualMessageResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.ContactMessage>> messagesForContact(long contactId) {
        return null;
    }
    
    public final void setProUnlocked(boolean enabled) {
    }
    
    public final void setBetaTesterStatus(boolean enabled) {
    }
    
    public final void completeOnboarding() {
    }
    
    @kotlin.Suppress(names = {"UNUSED_PARAMETER"})
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object initiateCall(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.ui.state.MainViewModel.CallInitiationResult> $completion) {
        return null;
    }
    
    public final void notifyCallEnded(long contactId, long callDuration) {
    }
    
    public final boolean needsBetaAgreement(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings settings) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object processVoiceCommand(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.assistant.VoiceCommandResult> $completion) {
        return null;
    }
    
    public final void cancelEmergency(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onComplete) {
    }
    
    private final void dispatch(com.pulselink.domain.model.EscalationTier tier, java.lang.String phrase) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final android.net.Uri buildBugReportUri(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pulselink.ui.screens.BugReportData bugReportData) {
        return null;
    }
    
    private final com.pulselink.domain.model.PulseLinkSettings ensureSoundDefaults(com.pulselink.domain.model.PulseLinkSettings settings) {
        return null;
    }
    
    public final void clearDndStatusMessage() {
    }
    
    private final void emitDndStatus(com.pulselink.data.alert.AlertDispatcher.AlertResult result) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0003\u0003\u0004\u0005B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0003\u0006\u0007\b\u00a8\u0006\t"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult;", "", "()V", "Failure", "Ready", "Timeout", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Failure;", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Ready;", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Timeout;", "androidApp_proDebug"})
    public static abstract class CallInitiationResult {
        
        private CallInitiationResult() {
            super();
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Failure;", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult;", "()V", "androidApp_proDebug"})
        public static final class Failure extends com.pulselink.ui.state.MainViewModel.CallInitiationResult {
            @org.jetbrains.annotations.NotNull()
            public static final com.pulselink.ui.state.MainViewModel.CallInitiationResult.Failure INSTANCE = null;
            
            private Failure() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Ready;", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult;", "()V", "androidApp_proDebug"})
        public static final class Ready extends com.pulselink.ui.state.MainViewModel.CallInitiationResult {
            @org.jetbrains.annotations.NotNull()
            public static final com.pulselink.ui.state.MainViewModel.CallInitiationResult.Ready INSTANCE = null;
            
            private Ready() {
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult$Timeout;", "Lcom/pulselink/ui/state/MainViewModel$CallInitiationResult;", "()V", "androidApp_proDebug"})
        public static final class Timeout extends com.pulselink.ui.state.MainViewModel.CallInitiationResult {
            @org.jetbrains.annotations.NotNull()
            public static final com.pulselink.ui.state.MainViewModel.CallInitiationResult.Timeout INSTANCE = null;
            
            private Timeout() {
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/pulselink/ui/state/MainViewModel$Companion;", "", "()V", "BETA_AGREEMENT_VERSION", "", "BUG_REPORT_PAGE_URL", "REMOTE_BETA_AGREEMENT_TIMEOUT_MS", "", "TAG", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}