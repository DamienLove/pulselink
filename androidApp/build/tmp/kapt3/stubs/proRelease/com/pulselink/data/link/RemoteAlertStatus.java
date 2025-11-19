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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/pulselink/data/link/RemoteAlertStatus;", "", "(Ljava/lang/String;I)V", "SUCCESS", "NOT_LINKED", "SMS_FAILED", "androidApp_proRelease"})
public enum RemoteAlertStatus {
    /*public static final*/ SUCCESS /* = new SUCCESS() */,
    /*public static final*/ NOT_LINKED /* = new NOT_LINKED() */,
    /*public static final*/ SMS_FAILED /* = new SMS_FAILED() */;
    
    RemoteAlertStatus() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.pulselink.data.link.RemoteAlertStatus> getEntries() {
        return null;
    }
}