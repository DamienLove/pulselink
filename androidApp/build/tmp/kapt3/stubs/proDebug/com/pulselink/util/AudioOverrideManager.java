package com.pulselink.util;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.coroutines.Dispatchers;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\f\b\u0007\u0018\u0000 .2\u00020\u0001:\u0003./0B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\b\u0010\u0017\u001a\u00020\u0018H\u0002J\u0006\u0010\u0019\u001a\u00020\u0016J\u0006\u0010\u001a\u001a\u00020\u0016J\u000e\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001eJ\u001a\u0010\u001f\u001a\u00020\u00162\b\u0010 \u001a\u0004\u0018\u00010!2\b\b\u0002\u0010\"\u001a\u00020#J\u0010\u0010$\u001a\u00020%2\u0006\u0010&\u001a\u00020%H\u0002J\u0010\u0010\'\u001a\u00020\u00162\u0006\u0010\"\u001a\u00020#H\u0002J\u0006\u0010(\u001a\u00020\u0016J\u0010\u0010)\u001a\u00020\u00162\b\b\u0002\u0010*\u001a\u00020%J\u0006\u0010+\u001a\u00020\u0016J\b\u0010,\u001a\u00020\u0016H\u0002J\b\u0010-\u001a\u00020\u0016H\u0002R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000e\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n \u0012*\u0004\u0018\u00010\u00110\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u00061"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "audioManager", "Landroid/media/AudioManager;", "currentFocusRequest", "Landroid/media/AudioFocusRequest;", "mediaLock", "mediaPlayer", "Landroid/media/MediaPlayer;", "notificationManager", "Landroid/app/NotificationManager;", "pendingRestoreJob", "Lkotlinx/coroutines/Job;", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "scope", "Lkotlinx/coroutines/CoroutineScope;", "abandonAudioFocus", "", "buildToneAudioAttributes", "Landroid/media/AudioAttributes;", "cancelScheduledRestore", "clear", "overrideForAlert", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "requestDndBypass", "", "playTone", "soundUri", "Landroid/net/Uri;", "profile", "Lcom/pulselink/util/AudioOverrideManager$ToneProfile;", "preferredRestoreDelay", "", "requestedDelay", "requestAudioFocus", "restoreIfNeeded", "scheduleRestore", "delayMillis", "stopTone", "stopToneLocked", "waitForVolumePropagation", "Companion", "OverrideResult", "ToneProfile", "androidApp_proDebug"})
public final class AudioOverrideManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private final android.media.AudioManager audioManager = null;
    @org.jetbrains.annotations.Nullable()
    private final android.app.NotificationManager notificationManager = null;
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile kotlinx.coroutines.Job pendingRestoreJob;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile android.media.MediaPlayer mediaPlayer;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Object mediaLock = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile android.media.AudioFocusRequest currentFocusRequest;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AudioOverrideManager";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_NAME = "pulselink_audio_override";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ACTIVE = "active";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_RING_VOLUME = "ring_volume";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_NOTIFICATION_VOLUME = "notification_volume";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ALARM_VOLUME = "alarm_volume";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_RING_MODE = "ring_mode";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_INTERRUPTION_FILTER = "interruption_filter";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_TIMESTAMP = "timestamp";
    private static final long DEFAULT_RESTORE_DELAY_MS = 120000L;
    private static final long PRIORITY_RESTORE_DELAY_MS = 25000L;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.util.AudioOverrideManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public AudioOverrideManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.util.AudioOverrideManager.OverrideResult overrideForAlert(boolean requestDndBypass) {
        return null;
    }
    
    public final void scheduleRestore(long delayMillis) {
    }
    
    public final void cancelScheduledRestore() {
    }
    
    public final void restoreIfNeeded() {
    }
    
    public final void clear() {
    }
    
    public final void playTone(@org.jetbrains.annotations.Nullable()
    android.net.Uri soundUri, @org.jetbrains.annotations.NotNull()
    com.pulselink.util.AudioOverrideManager.ToneProfile profile) {
    }
    
    public final void stopTone() {
    }
    
    private final void stopToneLocked() {
    }
    
    private final void requestAudioFocus(com.pulselink.util.AudioOverrideManager.ToneProfile profile) {
    }
    
    private final void abandonAudioFocus() {
    }
    
    private final void waitForVolumePropagation() {
    }
    
    private final android.media.AudioAttributes buildToneAudioAttributes() {
        return null;
    }
    
    private final long preferredRestoreDelay(long requestedDelay) {
        return 0L;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$Companion;", "", "()V", "DEFAULT_RESTORE_DELAY_MS", "", "KEY_ACTIVE", "", "KEY_ALARM_VOLUME", "KEY_INTERRUPTION_FILTER", "KEY_NOTIFICATION_VOLUME", "KEY_RING_MODE", "KEY_RING_VOLUME", "KEY_TIMESTAMP", "PREF_NAME", "PRIORITY_RESTORE_DELAY_MS", "TAG", "androidApp_proDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0015\n\u0002\u0010\b\n\u0002\b\u0005\b\u0086\b\u0018\u0000 \"2\u00020\u0001:\u0003\"#$B9\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0017\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0019\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010\u001a\u001a\u0004\u0018\u00010\bH\u00c6\u0003J\u000b\u0010\u001b\u001a\u0004\u0018\u00010\nH\u00c6\u0003J?\u0010\u001c\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\nH\u00c6\u0001J\u0013\u0010\u001d\u001a\u00020\u00052\b\u0010\u001e\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001f\u001a\u00020 H\u00d6\u0001J\t\u0010!\u001a\u00020\nH\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0013\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0015\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\r\u00a8\u0006%"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "", "state", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult$State;", "dndApplied", "", "limitedByPolicy", "reason", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult$FailureReason;", "message", "", "(Lcom/pulselink/util/AudioOverrideManager$OverrideResult$State;ZZLcom/pulselink/util/AudioOverrideManager$OverrideResult$FailureReason;Ljava/lang/String;)V", "getDndApplied", "()Z", "getLimitedByPolicy", "getMessage", "()Ljava/lang/String;", "getReason", "()Lcom/pulselink/util/AudioOverrideManager$OverrideResult$FailureReason;", "getState", "()Lcom/pulselink/util/AudioOverrideManager$OverrideResult$State;", "success", "getSuccess", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "", "toString", "Companion", "FailureReason", "State", "androidApp_proDebug"})
    public static final class OverrideResult {
        @org.jetbrains.annotations.NotNull()
        private final com.pulselink.util.AudioOverrideManager.OverrideResult.State state = null;
        private final boolean dndApplied = false;
        private final boolean limitedByPolicy = false;
        @org.jetbrains.annotations.Nullable()
        private final com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason reason = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String message = null;
        @org.jetbrains.annotations.NotNull()
        public static final com.pulselink.util.AudioOverrideManager.OverrideResult.Companion Companion = null;
        
        public OverrideResult(@org.jetbrains.annotations.NotNull()
        com.pulselink.util.AudioOverrideManager.OverrideResult.State state, boolean dndApplied, boolean limitedByPolicy, @org.jetbrains.annotations.Nullable()
        com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason reason, @org.jetbrains.annotations.Nullable()
        java.lang.String message) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult.State getState() {
            return null;
        }
        
        public final boolean getDndApplied() {
            return false;
        }
        
        public final boolean getLimitedByPolicy() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason getReason() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getMessage() {
            return null;
        }
        
        public final boolean getSuccess() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult.State component1() {
            return null;
        }
        
        public final boolean component2() {
            return false;
        }
        
        public final boolean component3() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason component4() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.util.AudioOverrideManager.OverrideResult copy(@org.jetbrains.annotations.NotNull()
        com.pulselink.util.AudioOverrideManager.OverrideResult.State state, boolean dndApplied, boolean limitedByPolicy, @org.jetbrains.annotations.Nullable()
        com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason reason, @org.jetbrains.annotations.Nullable()
        java.lang.String message) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\b\u0010\u0007\u001a\u0004\u0018\u00010\bJ(\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000b2\b\u0010\u0007\u001a\u0004\u0018\u00010\bJ\u0006\u0010\r\u001a\u00020\u0004J \u0010\u000e\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000b2\b\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a8\u0006\u000f"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$OverrideResult$Companion;", "", "()V", "failure", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult;", "reason", "Lcom/pulselink/util/AudioOverrideManager$OverrideResult$FailureReason;", "message", "", "partial", "dndApplied", "", "limited", "skipped", "success", "androidApp_proDebug"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.OverrideResult success(boolean dndApplied, boolean limited, @org.jetbrains.annotations.Nullable()
            java.lang.String message) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.OverrideResult partial(@org.jetbrains.annotations.NotNull()
            com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason reason, boolean dndApplied, boolean limited, @org.jetbrains.annotations.Nullable()
            java.lang.String message) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.OverrideResult failure(@org.jetbrains.annotations.NotNull()
            com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason reason, @org.jetbrains.annotations.Nullable()
            java.lang.String message) {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.OverrideResult skipped() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$OverrideResult$FailureReason;", "", "(Ljava/lang/String;I)V", "AUDIO_SERVICE_UNAVAILABLE", "POLICY_PERMISSION_MISSING", "POLICY_LIMITED", "UNKNOWN", "androidApp_proDebug"})
        public static enum FailureReason {
            /*public static final*/ AUDIO_SERVICE_UNAVAILABLE /* = new AUDIO_SERVICE_UNAVAILABLE() */,
            /*public static final*/ POLICY_PERMISSION_MISSING /* = new POLICY_PERMISSION_MISSING() */,
            /*public static final*/ POLICY_LIMITED /* = new POLICY_LIMITED() */,
            /*public static final*/ UNKNOWN /* = new UNKNOWN() */;
            
            FailureReason() {
            }
            
            @org.jetbrains.annotations.NotNull()
            public static kotlin.enums.EnumEntries<com.pulselink.util.AudioOverrideManager.OverrideResult.FailureReason> getEntries() {
                return null;
            }
        }
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0006\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$OverrideResult$State;", "", "(Ljava/lang/String;I)V", "SUCCESS", "PARTIAL", "FAILURE", "SKIPPED", "androidApp_proDebug"})
        public static enum State {
            /*public static final*/ SUCCESS /* = new SUCCESS() */,
            /*public static final*/ PARTIAL /* = new PARTIAL() */,
            /*public static final*/ FAILURE /* = new FAILURE() */,
            /*public static final*/ SKIPPED /* = new SKIPPED() */;
            
            State() {
            }
            
            @org.jetbrains.annotations.NotNull()
            public static kotlin.enums.EnumEntries<com.pulselink.util.AudioOverrideManager.OverrideResult.State> getEntries() {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\f\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\b\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u00032\b\u0010\u000f\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0010\u001a\u00020\u0005H\u00d6\u0001J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$ToneProfile;", "", "looping", "", "focusGain", "", "(ZI)V", "getFocusGain", "()I", "getLooping", "()Z", "component1", "component2", "copy", "equals", "other", "hashCode", "toString", "", "Companion", "androidApp_proDebug"})
    public static final class ToneProfile {
        private final boolean looping = false;
        private final int focusGain = 0;
        @org.jetbrains.annotations.NotNull()
        private static final com.pulselink.util.AudioOverrideManager.ToneProfile Emergency = null;
        @org.jetbrains.annotations.NotNull()
        private static final com.pulselink.util.AudioOverrideManager.ToneProfile CheckIn = null;
        @org.jetbrains.annotations.NotNull()
        private static final com.pulselink.util.AudioOverrideManager.ToneProfile IncomingCall = null;
        @org.jetbrains.annotations.NotNull()
        public static final com.pulselink.util.AudioOverrideManager.ToneProfile.Companion Companion = null;
        
        public ToneProfile(boolean looping, int focusGain) {
            super();
        }
        
        public final boolean getLooping() {
            return false;
        }
        
        public final int getFocusGain() {
            return 0;
        }
        
        public final boolean component1() {
            return false;
        }
        
        public final int component2() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.util.AudioOverrideManager.ToneProfile copy(boolean looping, int focusGain) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0006R\u0011\u0010\t\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u0006\u00a8\u0006\u000b"}, d2 = {"Lcom/pulselink/util/AudioOverrideManager$ToneProfile$Companion;", "", "()V", "CheckIn", "Lcom/pulselink/util/AudioOverrideManager$ToneProfile;", "getCheckIn", "()Lcom/pulselink/util/AudioOverrideManager$ToneProfile;", "Emergency", "getEmergency", "IncomingCall", "getIncomingCall", "androidApp_proDebug"})
        public static final class Companion {
            
            private Companion() {
                super();
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.ToneProfile getEmergency() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.ToneProfile getCheckIn() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull()
            public final com.pulselink.util.AudioOverrideManager.ToneProfile getIncomingCall() {
                return null;
            }
        }
    }
}