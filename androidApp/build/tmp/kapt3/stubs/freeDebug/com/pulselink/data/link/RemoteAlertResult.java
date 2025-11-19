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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\tH\u00c6\u0003J1\u0010\u0017\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u00c6\u0001J\u0013\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001J\t\u0010\u001d\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001e"}, d2 = {"Lcom/pulselink/data/link/RemoteAlertResult;", "", "contactId", "", "contactName", "", "status", "Lcom/pulselink/data/link/RemoteAlertStatus;", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "(JLjava/lang/String;Lcom/pulselink/data/link/RemoteAlertStatus;Lcom/pulselink/domain/model/EscalationTier;)V", "getContactId", "()J", "getContactName", "()Ljava/lang/String;", "getStatus", "()Lcom/pulselink/data/link/RemoteAlertStatus;", "getTier", "()Lcom/pulselink/domain/model/EscalationTier;", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "androidApp_freeDebug"})
public final class RemoteAlertResult {
    private final long contactId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String contactName = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.link.RemoteAlertStatus status = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.model.EscalationTier tier = null;
    
    public RemoteAlertResult(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactName, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.RemoteAlertStatus status, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier) {
        super();
    }
    
    public final long getContactId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getContactName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.link.RemoteAlertStatus getStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.EscalationTier getTier() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.link.RemoteAlertStatus component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.EscalationTier component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.link.RemoteAlertResult copy(long contactId, @org.jetbrains.annotations.NotNull()
    java.lang.String contactName, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.RemoteAlertStatus status, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}