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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0016\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a\u0010\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u0001H\u0002\u001a\u0014\u0010\u0003\u001a\u00020\u0004*\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0002\u00a8\u0006\u0007"}, d2 = {"normalizePhone", "", "input", "resolveLinkState", "Lcom/pulselink/domain/model/Contact;", "message", "Lcom/pulselink/data/sms/PulseLinkMessage$ManualMessage;", "androidApp_freeRelease"})
public final class ContactLinkManagerKt {
    
    private static final com.pulselink.domain.model.Contact resolveLinkState(com.pulselink.domain.model.Contact $this$resolveLinkState, com.pulselink.data.sms.PulseLinkMessage.ManualMessage message) {
        return null;
    }
    
    private static final java.lang.String normalizePhone(java.lang.String input) {
        return null;
    }
}