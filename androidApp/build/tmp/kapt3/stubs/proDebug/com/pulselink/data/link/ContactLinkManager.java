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
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0086\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\b\b\u0007\u0018\u0000 \u008d\u00012\u00020\u0001:\u0004\u008c\u0001\u008d\u0001BY\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\u0006\u0010\u0012\u001a\u00020\u0013\u0012\u0006\u0010\u0014\u001a\u00020\u0015\u00a2\u0006\u0002\u0010\u0016J\u0016\u0010\'\u001a\u00020(2\u0006\u0010)\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010*J\u000e\u0010+\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0002\u0010,J&\u0010-\u001a\u00020(2\u0006\u0010.\u001a\u00020/2\u0006\u00100\u001a\u00020\u00192\u0006\u00101\u001a\u00020\u001bH\u0082@\u00a2\u0006\u0002\u00102J\b\u00103\u001a\u00020(H\u0002J\u0018\u00104\u001a\u0004\u0018\u00010/2\u0006\u00105\u001a\u00020\u0019H\u0082@\u00a2\u0006\u0002\u00106J\u0016\u00107\u001a\u00020(2\u0006\u00108\u001a\u000209H\u0082@\u00a2\u0006\u0002\u0010:J\u0010\u0010;\u001a\u00020(2\u0006\u00108\u001a\u00020<H\u0002J\u0016\u0010=\u001a\u00020(2\u0006\u00108\u001a\u00020>H\u0082@\u00a2\u0006\u0002\u0010?J\u0016\u0010@\u001a\u00020(2\u0006\u00108\u001a\u00020AH\u0082@\u00a2\u0006\u0002\u0010BJ\u001e\u0010C\u001a\u00020(2\u0006\u00108\u001a\u00020D2\u0006\u0010E\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010FJ\u0018\u0010G\u001a\u00020(2\b\u00105\u001a\u0004\u0018\u00010\u0019H\u0082@\u00a2\u0006\u0002\u00106J\u001e\u0010H\u001a\u00020(2\u0006\u00108\u001a\u00020I2\u0006\u0010E\u001a\u00020\u0019H\u0082@\u00a2\u0006\u0002\u0010JJ\u001e\u0010K\u001a\u00020(2\u0006\u00108\u001a\u00020L2\u0006\u0010E\u001a\u00020\u0019H\u0082@\u00a2\u0006\u0002\u0010MJ\u001e\u0010N\u001a\u00020(2\u0006\u00108\u001a\u00020O2\u0006\u0010E\u001a\u00020\u0019H\u0082@\u00a2\u0006\u0002\u0010PJ\u0016\u0010Q\u001a\u00020(2\u0006\u00108\u001a\u00020RH\u0082@\u00a2\u0006\u0002\u0010SJ\u0016\u0010T\u001a\u00020(2\u0006\u0010U\u001a\u00020VH\u0082@\u00a2\u0006\u0002\u0010WJ\u0016\u0010X\u001a\u00020(2\u0006\u00108\u001a\u00020YH\u0082@\u00a2\u0006\u0002\u0010ZJ\u0016\u0010[\u001a\u00020(2\u0006\u00108\u001a\u00020\\H\u0082@\u00a2\u0006\u0002\u0010]J\u0010\u0010^\u001a\u00020\u001b2\u0006\u0010_\u001a\u00020\u0019H\u0002J,\u0010`\u001a\u00020\u001b2\b\u0010a\u001a\u0004\u0018\u00010\u00192\b\u0010b\u001a\u0004\u0018\u00010\u00192\b\u0010c\u001a\u0004\u0018\u00010\u0019H\u0082@\u00a2\u0006\u0002\u0010dJ\n\u0010e\u001a\u0004\u0018\u00010\u0019H\u0002J\u0010\u0010f\u001a\u00020(2\u0006\u0010g\u001a\u00020\u0019H\u0002J\u0010\u0010h\u001a\u00020(2\u0006\u0010.\u001a\u00020/H\u0002J\u0010\u0010i\u001a\u00020(2\u0006\u0010.\u001a\u00020/H\u0002J\u0016\u0010j\u001a\u00020k2\u0006\u0010)\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010*J\u001e\u0010l\u001a\u00020\u001b2\u0006\u0010)\u001a\u00020&2\u0006\u0010m\u001a\u00020nH\u0086@\u00a2\u0006\u0002\u0010oJ(\u0010p\u001a\u00020\u001b2\u0006\u0010.\u001a\u00020/2\u0006\u0010m\u001a\u00020n2\b\b\u0002\u0010q\u001a\u00020rH\u0082@\u00a2\u0006\u0002\u0010sJ \u0010t\u001a\u0004\u0018\u00010/2\u0006\u00108\u001a\u00020O2\u0006\u0010E\u001a\u00020\u0019H\u0082@\u00a2\u0006\u0002\u0010PJ\u001e\u0010u\u001a\u00020(2\u0006\u0010)\u001a\u00020&2\u0006\u0010v\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010wJ\u0016\u0010x\u001a\u00020(2\u0006\u0010)\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010*J\u001e\u0010y\u001a\u00020z2\u0006\u0010)\u001a\u00020&2\u0006\u00108\u001a\u00020\u0019H\u0086@\u00a2\u0006\u0002\u0010{J\u0016\u0010|\u001a\u00020\u001b2\u0006\u0010)\u001a\u00020&H\u0086@\u00a2\u0006\u0002\u0010*J(\u0010}\u001a\u00020(2\u0006\u0010)\u001a\u00020&2\u0006\u0010m\u001a\u00020n2\b\u0010~\u001a\u0004\u0018\u00010\u0019H\u0086@\u00a2\u0006\u0002\u0010\u007fJ\u001b\u0010\u0080\u0001\u001a\u00020\u001b2\u0007\u0010\u0081\u0001\u001a\u00020\u00192\u0007\u0010\u0082\u0001\u001a\u00020\u0019H\u0002J\u0007\u0010\u0083\u0001\u001a\u00020(J\u0007\u0010\u0084\u0001\u001a\u00020(J!\u0010\u0085\u0001\u001a\u00030\u0086\u00012\u0006\u0010.\u001a\u00020/2\u0006\u0010m\u001a\u00020nH\u0086@\u00a2\u0006\u0003\u0010\u0087\u0001J!\u0010\u0088\u0001\u001a\u00020(2\u0006\u0010)\u001a\u00020&2\u0007\u0010\u0089\u0001\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0003\u0010\u008a\u0001J!\u0010\u008b\u0001\u001a\u00020(2\u0006\u0010)\u001a\u00020&2\u0007\u0010\u0089\u0001\u001a\u00020\u001bH\u0086@\u00a2\u0006\u0003\u0010\u008a\u0001R \u0010\u0017\u001a\u0014\u0012\u0004\u0012\u00020\u0019\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001b0\u001a0\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u001f\u001a\u00020 8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b#\u0010$\u001a\u0004\b!\u0010\"R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010%\u001a\u000e\u0012\u0004\u0012\u00020\u0019\u0012\u0004\u0012\u00020&0\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u008e\u0001"}, d2 = {"Lcom/pulselink/data/link/ContactLinkManager;", "", "context", "Landroid/content/Context;", "smsSender", "Lcom/pulselink/data/sms/SmsSender;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "alertRepository", "Lcom/pulselink/domain/repository/AlertRepository;", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "blockedContactRepository", "Lcom/pulselink/domain/repository/BlockedContactRepository;", "remoteActionHandler", "Lcom/pulselink/data/link/RemoteActionHandler;", "messageRepository", "Lcom/pulselink/domain/repository/MessageRepository;", "callStateMonitor", "Lcom/pulselink/util/CallStateMonitor;", "linkChannelService", "Lcom/pulselink/data/link/LinkChannelService;", "(Landroid/content/Context;Lcom/pulselink/data/sms/SmsSender;Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/domain/repository/AlertRepository;Lcom/pulselink/domain/repository/ContactRepository;Lcom/pulselink/domain/repository/BlockedContactRepository;Lcom/pulselink/data/link/RemoteActionHandler;Lcom/pulselink/domain/repository/MessageRepository;Lcom/pulselink/util/CallStateMonitor;Lcom/pulselink/data/link/LinkChannelService;)V", "alertHandshake", "Ljava/util/concurrent/ConcurrentHashMap;", "", "Lkotlinx/coroutines/CompletableDeferred;", "", "incomingMonitorActive", "monitorScope", "Lkotlinx/coroutines/CoroutineScope;", "notificationManager", "Landroidx/core/app/NotificationManagerCompat;", "getNotificationManager", "()Landroidx/core/app/NotificationManagerCompat;", "notificationManager$delegate", "Lkotlin/Lazy;", "remoteAlertDedup", "", "approveLink", "", "contactId", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "cancelActiveEmergency", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deliverManualMessage", "contact", "Lcom/pulselink/domain/model/Contact;", "rawBody", "overrideApplied", "(Lcom/pulselink/domain/model/Contact;Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "ensureChannel", "findContactByPhoneFlexible", "phone", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleAlertPrepare", "message", "Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepare;", "(Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepare;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleAlertReady", "Lcom/pulselink/data/sms/PulseLinkMessage$AlertReady;", "handleCallEnded", "Lcom/pulselink/data/sms/PulseLinkMessage$CallEnded;", "(Lcom/pulselink/data/sms/PulseLinkMessage$CallEnded;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleConfigUpdate", "Lcom/pulselink/data/sms/PulseLinkMessage$ConfigUpdate;", "(Lcom/pulselink/data/sms/PulseLinkMessage$ConfigUpdate;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleInbound", "Lcom/pulselink/data/sms/PulseLinkMessage;", "fromPhone", "(Lcom/pulselink/data/sms/PulseLinkMessage;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleIncomingRinging", "handleLinkAccept", "Lcom/pulselink/data/sms/PulseLinkMessage$LinkAccept;", "(Lcom/pulselink/data/sms/PulseLinkMessage$LinkAccept;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleLinkRequest", "Lcom/pulselink/data/sms/PulseLinkMessage$LinkRequest;", "(Lcom/pulselink/data/sms/PulseLinkMessage$LinkRequest;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleManualMessage", "Lcom/pulselink/data/sms/PulseLinkMessage$ManualMessage;", "(Lcom/pulselink/data/sms/PulseLinkMessage$ManualMessage;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handlePing", "Lcom/pulselink/data/sms/PulseLinkMessage$Ping;", "(Lcom/pulselink/data/sms/PulseLinkMessage$Ping;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleRealtimeManualMessage", "payload", "Lcom/pulselink/data/link/LinkChannelPayload;", "(Lcom/pulselink/data/link/LinkChannelPayload;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleRemoteAlert", "Lcom/pulselink/data/sms/PulseLinkMessage$RemoteAlert;", "(Lcom/pulselink/data/sms/PulseLinkMessage$RemoteAlert;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleSoundOverride", "Lcom/pulselink/data/sms/PulseLinkMessage$SoundOverride;", "(Lcom/pulselink/data/sms/PulseLinkMessage$SoundOverride;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isAutoAlertBody", "body", "isSenderBlocked", "phoneNumber", "linkCode", "remoteDeviceId", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "latestIncomingNumber", "notifyBlockedAttempt", "name", "notifyLinkRequest", "notifyLinked", "prepareRemoteCall", "Lcom/pulselink/data/link/ContactLinkManager$CallPreparationResult;", "prepareRemoteOverride", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "(JLcom/pulselink/domain/model/EscalationTier;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "requestRemotePrepare", "reason", "Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepareReason;", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/domain/model/EscalationTier;Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepareReason;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resolveContactForManualMessage", "sendCallEndedNotification", "callDuration", "(JJLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendLinkRequest", "sendManualMessage", "Lcom/pulselink/domain/model/ManualMessageResult;", "(JLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendPing", "sendSoundOverride", "soundKey", "(JLcom/pulselink/domain/model/EscalationTier;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "shouldProcessRemoteAlert", "senderId", "code", "startIncomingMonitoring", "stopIncomingMonitoring", "triggerRemoteAlert", "Lcom/pulselink/data/link/RemoteAlertResult;", "(Lcom/pulselink/domain/model/Contact;Lcom/pulselink/domain/model/EscalationTier;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateRemoteOverridePermission", "allow", "(JZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateRemoteSoundPermission", "CallPreparationResult", "Companion", "androidApp_proDebug"})
public final class ContactLinkManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.sms.SmsSender smsSender = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.AlertRepository alertRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.ContactRepository contactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.BlockedContactRepository blockedContactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.link.RemoteActionHandler remoteActionHandler = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.MessageRepository messageRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.util.CallStateMonitor callStateMonitor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.link.LinkChannelService linkChannelService = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy notificationManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, kotlinx.coroutines.CompletableDeferred<java.lang.Boolean>> alertHandshake = null;
    @kotlin.jvm.Volatile()
    private volatile boolean incomingMonitorActive = false;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope monitorScope = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, java.lang.Long> remoteAlertDedup = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "ContactLinkManager";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHANNEL_ID = "pulselink_link_channel";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CONFIG_REMOTE_SOUND = "ALLOW_SOUND";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CONFIG_REMOTE_OVERRIDE = "ALLOW_OVERRIDE";
    private static final long PREPARE_TIMEOUT_MS = 10000L;
    private static final long REMOTE_ALERT_DEDUP_WINDOW_MS = 120000L;
    private static final int REMOTE_ALERT_DEDUP_MAX = 64;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.link.ContactLinkManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public ContactLinkManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.SmsSender smsSender, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.AlertRepository alertRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.ContactRepository contactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.BlockedContactRepository blockedContactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.RemoteActionHandler remoteActionHandler, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.MessageRepository messageRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.util.CallStateMonitor callStateMonitor, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.LinkChannelService linkChannelService) {
        super();
    }
    
    private final androidx.core.app.NotificationManagerCompat getNotificationManager() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendLinkRequest(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object approveLink(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendPing(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object prepareRemoteCall(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.link.ContactLinkManager.CallPreparationResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendCallEndedNotification(long contactId, long callDuration, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendSoundOverride(long contactId, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.Nullable()
    java.lang.String soundKey, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object handleInbound(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.PulseLinkMessage message, @org.jetbrains.annotations.NotNull()
    java.lang.String fromPhone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object isSenderBlocked(java.lang.String phoneNumber, java.lang.String linkCode, java.lang.String remoteDeviceId, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.Object handleLinkRequest(com.pulselink.data.sms.PulseLinkMessage.LinkRequest message, java.lang.String fromPhone, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleLinkAccept(com.pulselink.data.sms.PulseLinkMessage.LinkAccept message, java.lang.String fromPhone, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handlePing(com.pulselink.data.sms.PulseLinkMessage.Ping message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleRemoteAlert(com.pulselink.data.sms.PulseLinkMessage.RemoteAlert message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleAlertPrepare(com.pulselink.data.sms.PulseLinkMessage.AlertPrepare message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void handleAlertReady(com.pulselink.data.sms.PulseLinkMessage.AlertReady message) {
    }
    
    private final boolean shouldProcessRemoteAlert(java.lang.String senderId, java.lang.String code) {
        return false;
    }
    
    private final java.lang.Object handleSoundOverride(com.pulselink.data.sms.PulseLinkMessage.SoundOverride message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleManualMessage(com.pulselink.data.sms.PulseLinkMessage.ManualMessage message, java.lang.String fromPhone, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object handleRealtimeManualMessage(com.pulselink.data.link.LinkChannelPayload payload, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object deliverManualMessage(com.pulselink.domain.model.Contact contact, java.lang.String rawBody, boolean overrideApplied, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final boolean isAutoAlertBody(java.lang.String body) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object cancelActiveEmergency(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.Object handleCallEnded(com.pulselink.data.sms.PulseLinkMessage.CallEnded message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object resolveContactForManualMessage(com.pulselink.data.sms.PulseLinkMessage.ManualMessage message, java.lang.String fromPhone, kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion) {
        return null;
    }
    
    private final java.lang.Object handleConfigUpdate(com.pulselink.data.sms.PulseLinkMessage.ConfigUpdate message, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void notifyLinkRequest(com.pulselink.domain.model.Contact contact) {
    }
    
    private final void notifyLinked(com.pulselink.domain.model.Contact contact) {
    }
    
    private final void notifyBlockedAttempt(java.lang.String name) {
    }
    
    private final void ensureChannel() {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateRemoteSoundPermission(long contactId, boolean allow, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object updateRemoteOverridePermission(long contactId, boolean allow, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object prepareRemoteOverride(long contactId, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendManualMessage(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.ManualMessageResult> $completion) {
        return null;
    }
    
    private final java.lang.Object requestRemotePrepare(com.pulselink.domain.model.Contact contact, com.pulselink.domain.model.EscalationTier tier, com.pulselink.data.sms.PulseLinkMessage.AlertPrepareReason reason, kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    public final void startIncomingMonitoring() {
    }
    
    public final void stopIncomingMonitoring() {
    }
    
    private final java.lang.Object handleIncomingRinging(java.lang.String phone, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.String latestIncomingNumber() {
        return null;
    }
    
    private final java.lang.Object findContactByPhoneFlexible(java.lang.String phone, kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object triggerRemoteAlert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.link.RemoteAlertResult> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/pulselink/data/link/ContactLinkManager$CallPreparationResult;", "", "(Ljava/lang/String;I)V", "READY", "TIMEOUT", "FAILED", "androidApp_proDebug"})
    public static enum CallPreparationResult {
        /*public static final*/ READY /* = new READY() */,
        /*public static final*/ TIMEOUT /* = new TIMEOUT() */,
        /*public static final*/ FAILED /* = new FAILED() */;
        
        CallPreparationResult() {
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.pulselink.data.link.ContactLinkManager.CallPreparationResult> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/pulselink/data/link/ContactLinkManager$Companion;", "", "()V", "CHANNEL_ID", "", "CONFIG_REMOTE_OVERRIDE", "CONFIG_REMOTE_SOUND", "PREPARE_TIMEOUT_MS", "", "REMOTE_ALERT_DEDUP_MAX", "", "REMOTE_ALERT_DEDUP_WINDOW_MS", "TAG", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}