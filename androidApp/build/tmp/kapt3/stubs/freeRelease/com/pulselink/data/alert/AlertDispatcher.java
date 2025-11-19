package com.pulselink.data.alert;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.PendingIntentCompat;
import com.pulselink.R;
import com.pulselink.data.location.LocationProvider;
import com.pulselink.data.sms.SmsSender;
import com.pulselink.domain.model.AlertProfile;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.model.SoundCategory;
import com.pulselink.domain.model.SoundOption;
import com.pulselink.domain.model.PulseLinkSettings;
import com.pulselink.util.AudioOverrideManager;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\b\u0007\u0018\u0000 .2\u00020\u0001:\u0002-.B9\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u00a2\u0006\u0002\u0010\u000eJ\u0010\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0082@\u00a2\u0006\u0002\u0010\u0011J\"\u0010\u0012\u001a\u00020\u00102\u0006\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u0010H\u0002J@\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0013\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u00152\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001a2\u0006\u0010\u001c\u001a\u00020\u001d2\n\b\u0002\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0086@\u00a2\u0006\u0002\u0010 J<\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020\u00102\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010$\u001a\u00020\u00102\u0006\u0010%\u001a\u00020&2\b\u0010\'\u001a\u0004\u0018\u00010(2\b\u0010)\u001a\u0004\u0018\u00010\u001bH\u0002J,\u0010*\u001a\u00020+2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010$\u001a\u00020\u00102\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aH\u0083@\u00a2\u0006\u0002\u0010,R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006/"}, d2 = {"Lcom/pulselink/data/alert/AlertDispatcher;", "", "context", "Landroid/content/Context;", "smsSender", "Lcom/pulselink/data/sms/SmsSender;", "locationProvider", "Lcom/pulselink/data/location/LocationProvider;", "registrar", "Lcom/pulselink/data/alert/NotificationRegistrar;", "soundCatalog", "Lcom/pulselink/data/alert/SoundCatalog;", "audioOverrideManager", "Lcom/pulselink/util/AudioOverrideManager;", "(Landroid/content/Context;Lcom/pulselink/data/sms/SmsSender;Lcom/pulselink/data/location/LocationProvider;Lcom/pulselink/data/alert/NotificationRegistrar;Lcom/pulselink/data/alert/SoundCatalog;Lcom/pulselink/util/AudioOverrideManager;)V", "buildLocationText", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "buildMessage", "phrase", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "locationText", "dispatch", "Lcom/pulselink/data/alert/AlertDispatcher$AlertResult;", "contacts", "", "Lcom/pulselink/domain/model/Contact;", "settings", "Lcom/pulselink/domain/model/PulseLinkSettings;", "contactId", "", "(Ljava/lang/String;Lcom/pulselink/domain/model/EscalationTier;Ljava/util/List;Lcom/pulselink/domain/model/PulseLinkSettings;Ljava/lang/Long;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendNotification", "", "channel", "message", "profile", "Lcom/pulselink/domain/model/AlertProfile;", "soundOption", "Lcom/pulselink/domain/model/SoundOption;", "primaryContact", "sendSms", "", "(Lcom/pulselink/domain/model/EscalationTier;Ljava/lang/String;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "AlertResult", "Companion", "androidApp_freeRelease"})
public final class AlertDispatcher {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.sms.SmsSender smsSender = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.location.LocationProvider locationProvider = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.NotificationRegistrar registrar = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.SoundCatalog soundCatalog = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.util.AudioOverrideManager audioOverrideManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AlertDispatcher";
    private static final long SEQUENTIAL_CONTACT_DELAY_MS = 1500L;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.alert.AlertDispatcher.Companion Companion = null;
    
    @javax.inject.Inject()
    public AlertDispatcher(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.SmsSender smsSender, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.location.LocationProvider locationProvider, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.NotificationRegistrar registrar, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.SoundCatalog soundCatalog, @org.jetbrains.annotations.NotNull()
    com.pulselink.util.AudioOverrideManager audioOverrideManager) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object dispatch(@org.jetbrains.annotations.NotNull()
    java.lang.String phrase, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.Contact> contacts, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings settings, @org.jetbrains.annotations.Nullable()
    java.lang.Long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.alert.AlertDispatcher.AlertResult> $completion) {
        return null;
    }
    
    private final java.lang.Object buildLocationText(kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    @androidx.annotation.RequiresPermission(value = "android.permission.SEND_SMS")
    private final java.lang.Object sendSms(com.pulselink.domain.model.EscalationTier tier, java.lang.String message, java.util.List<com.pulselink.domain.model.Contact> contacts, kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    private final java.lang.String buildMessage(java.lang.String phrase, com.pulselink.domain.model.EscalationTier tier, java.lang.String locationText) {
        return null;
    }
    
    private final void sendNotification(java.lang.String channel, com.pulselink.domain.model.EscalationTier tier, java.lang.String message, com.pulselink.domain.model.AlertProfile profile, com.pulselink.domain.model.SoundOption soundOption, com.pulselink.domain.model.Contact primaryContact) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u001a\b\u0086\b\u0018\u00002\u00020\u0001BA\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\t\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\u0002\u0010\rJ\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u001d\u001a\u0004\u0018\u00010\tH\u00c6\u0003J\u000b\u0010\u001e\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\u0010\u0010\u001f\u001a\u0004\u0018\u00010\fH\u00c6\u0003\u00a2\u0006\u0002\u0010\u000fJP\u0010 \u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\t2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00c6\u0001\u00a2\u0006\u0002\u0010!J\u0013\u0010\"\u001a\u00020\u00072\b\u0010#\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010$\u001a\u00020\u0005H\u00d6\u0001J\t\u0010%\u001a\u00020\u0003H\u00d6\u0001R\u0015\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\n\n\u0002\u0010\u0010\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\b\u001a\u0004\u0018\u00010\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0013\u0010\n\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0012\u00a8\u0006&"}, d2 = {"Lcom/pulselink/data/alert/AlertDispatcher$AlertResult;", "", "message", "", "notifiedContacts", "", "sharedLocation", "", "overrideResult", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "soundKey", "contactId", "", "(Ljava/lang/String;IZLcom/pulselink/util/AudioOverrideManager$OverrideResult;Ljava/lang/String;Ljava/lang/Long;)V", "getContactId", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getMessage", "()Ljava/lang/String;", "getNotifiedContacts", "()I", "getOverrideResult", "()Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "getSharedLocation", "()Z", "getSoundKey", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "(Ljava/lang/String;IZLcom/pulselink/util/AudioOverrideManager$OverrideResult;Ljava/lang/String;Ljava/lang/Long;)Lcom/pulselink/data/alert/AlertDispatcher$AlertResult;", "equals", "other", "hashCode", "toString", "androidApp_freeRelease"})
    public static final class AlertResult {
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String message = null;
        private final int notifiedContacts = 0;
        private final boolean sharedLocation = false;
        @org.jetbrains.annotations.Nullable()
        private final com.pulselink.util.AudioOverrideManager.OverrideResult overrideResult = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String soundKey = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.Long contactId = null;
        
        public AlertResult(@org.jetbrains.annotations.NotNull()
        java.lang.String message, int notifiedContacts, boolean sharedLocation, @org.jetbrains.annotations.Nullable()
        com.pulselink.util.AudioOverrideManager.OverrideResult overrideResult, @org.jetbrains.annotations.Nullable()
        java.lang.String soundKey, @org.jetbrains.annotations.Nullable()
        java.lang.Long contactId) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMessage() {
            return null;
        }
        
        public final int getNotifiedContacts() {
            return 0;
        }
        
        public final boolean getSharedLocation() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult getOverrideResult() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getSoundKey() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Long getContactId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component1() {
            return null;
        }
        
        public final int component2() {
            return 0;
        }
        
        public final boolean component3() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult component4() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Long component6() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.data.alert.AlertDispatcher.AlertResult copy(@org.jetbrains.annotations.NotNull()
        java.lang.String message, int notifiedContacts, boolean sharedLocation, @org.jetbrains.annotations.Nullable()
        com.pulselink.util.AudioOverrideManager.OverrideResult overrideResult, @org.jetbrains.annotations.Nullable()
        java.lang.String soundKey, @org.jetbrains.annotations.Nullable()
        java.lang.Long contactId) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/pulselink/data/alert/AlertDispatcher$Companion;", "", "()V", "SEQUENTIAL_CONTACT_DELAY_MS", "", "TAG", "", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}