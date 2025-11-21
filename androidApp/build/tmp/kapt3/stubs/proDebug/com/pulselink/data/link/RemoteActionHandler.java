package com.pulselink.data.link;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.provider.CallLog;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.pulselink.R;
import com.pulselink.data.alert.NotificationRegistrar;
import com.pulselink.data.alert.SoundCatalog;
import com.pulselink.data.sms.PulseLinkMessage;
import com.pulselink.data.sms.SmsCodec;
import com.pulselink.data.sms.SmsSender;
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.ContactMessage;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.model.LinkStatus;
import com.pulselink.domain.model.ManualMessageResult;
import com.pulselink.domain.model.MessageDirection;
import com.pulselink.domain.model.SoundCategory;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.BlockedContactRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.MessageRepository;
import com.pulselink.domain.repository.SettingsRepository;
import com.pulselink.service.AlertRouter;
import com.pulselink.util.AudioOverrideManager;
import com.pulselink.ui.EmergencyPopupActivity;
import com.pulselink.util.CallStateMonitor;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000x\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\"\n\u0002\b\u0005\b\u0007\u0018\u0000 12\u00020\u0001:\u00011B9\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u0016\u0010\u0016\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012H\u0086@\u00a2\u0006\u0002\u0010\u0017J\u001e\u0010\u0018\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001aH\u0086@\u00a2\u0006\u0002\u0010\u001bJJ\u0010\u001c\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001a2\u0006\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u001e2\u0006\u0010 \u001a\u00020!2\b\b\u0002\u0010\"\u001a\u00020#2\b\b\u0002\u0010$\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010%J \u0010&\u001a\u00020\'2\u0006\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010(\u001a\u00020)H\u0086@\u00a2\u0006\u0002\u0010*J.\u0010+\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001a2\u000e\b\u0002\u0010,\u001a\b\u0012\u0004\u0012\u00020\u00140-H\u0086@\u00a2\u0006\u0002\u0010.J\u001e\u0010/\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0019\u001a\u00020\u001aH\u0086@\u00a2\u0006\u0002\u0010\u001bJ\u0006\u00100\u001a\u00020\u0010R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00062"}, d2 = {"Lcom/pulselink/data/link/RemoteActionHandler;", "", "context", "Landroid/content/Context;", "alertRouter", "Lcom/pulselink/service/AlertRouter;", "audioOverrideManager", "Lcom/pulselink/util/AudioOverrideManager;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "notificationRegistrar", "Lcom/pulselink/data/alert/NotificationRegistrar;", "soundCatalog", "Lcom/pulselink/data/alert/SoundCatalog;", "(Landroid/content/Context;Lcom/pulselink/service/AlertRouter;Lcom/pulselink/util/AudioOverrideManager;Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/data/alert/NotificationRegistrar;Lcom/pulselink/data/alert/SoundCatalog;)V", "finishCall", "", "contact", "Lcom/pulselink/domain/model/Contact;", "callDuration", "", "(Lcom/pulselink/domain/model/Contact;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleIncomingCall", "(Lcom/pulselink/domain/model/Contact;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "notifyIncomingCall", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/domain/model/EscalationTier;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "playAttentionTone", "title", "", "body", "notificationId", "", "forceBypass", "", "overrideHoldMs", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/domain/model/EscalationTier;Ljava/lang/String;Ljava/lang/String;IZJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "prepareForAlert", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "reason", "Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepareReason;", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepareReason;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "routeRemoteAlert", "excludeContactIds", "", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/domain/model/EscalationTier;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showEmergencyPopup", "stopIncomingCallTone", "Companion", "androidApp_proDebug"})
public final class RemoteActionHandler {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.service.AlertRouter alertRouter = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.util.AudioOverrideManager audioOverrideManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.NotificationRegistrar notificationRegistrar = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.SoundCatalog soundCatalog = null;
    private static final long DEFAULT_OVERRIDE_HOLD_MS = 120000L;
    private static final long MESSAGE_OVERRIDE_HOLD_MS = 90000L;
    private static final long CALL_OVERRIDE_HOLD_MS = 180000L;
    private static final long AUDIO_PRIME_DELAY_MS = 90L;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "RemoteActionHandler";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.link.RemoteActionHandler.Companion Companion = null;
    
    @javax.inject.Inject()
    public RemoteActionHandler(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pulselink.service.AlertRouter alertRouter, @org.jetbrains.annotations.NotNull()
    com.pulselink.util.AudioOverrideManager audioOverrideManager, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.NotificationRegistrar notificationRegistrar, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.SoundCatalog soundCatalog) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object prepareForAlert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.PulseLinkMessage.AlertPrepareReason reason, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.util.AudioOverrideManager.OverrideResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object routeRemoteAlert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> excludeContactIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object playAttentionTone(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String body, int notificationId, boolean forceBypass, long overrideHoldMs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object handleIncomingCall(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void stopIncomingCallTone() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object notifyIncomingCall(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object showEmergencyPopup(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object finishCall(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, long callDuration, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/pulselink/data/link/RemoteActionHandler$Companion;", "", "()V", "AUDIO_PRIME_DELAY_MS", "", "CALL_OVERRIDE_HOLD_MS", "DEFAULT_OVERRIDE_HOLD_MS", "MESSAGE_OVERRIDE_HOLD_MS", "TAG", "", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}