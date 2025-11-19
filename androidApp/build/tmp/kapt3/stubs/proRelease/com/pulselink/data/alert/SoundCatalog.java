package com.pulselink.data.alert;

import android.content.Context;
import com.pulselink.R;
import com.pulselink.domain.model.SoundCategory;
import com.pulselink.domain.model.SoundOption;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010 \n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\n\b\u0007\u0018\u0000 #2\u00020\u0001:\u0001#B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\b0\u000eH\u0002J\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\b0\u000eJ\f\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\b0\u000eJ\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u0019\u001a\u00020\u001aJ\u000e\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\b0\u000eH\u0002J\f\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\b0\u000eJ\u001a\u0010\u001d\u001a\u0004\u0018\u00010\b2\b\u0010\u001e\u001a\u0004\u0018\u00010\u00072\u0006\u0010\u001f\u001a\u00020\u001aJ\u0018\u0010 \u001a\u00020\u00072\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\"\u001a\u00020\u0007H\u0002R\'\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\b0\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000b\u0010\f\u001a\u0004\b\t\u0010\nR!\u0010\r\u001a\b\u0012\u0004\u0012\u00020\b0\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\f\u001a\u0004\b\u000f\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R!\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\f\u001a\u0004\b\u0013\u0010\u0010\u00a8\u0006$"}, d2 = {"Lcom/pulselink/data/alert/SoundCatalog;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "cachedOptions", "", "", "Lcom/pulselink/domain/model/SoundOption;", "getCachedOptions", "()Ljava/util/Map;", "cachedOptions$delegate", "Lkotlin/Lazy;", "callSoundOptions", "", "getCallSoundOptions", "()Ljava/util/List;", "callSoundOptions$delegate", "soundOptions", "getSoundOptions", "soundOptions$delegate", "buildCallOptions", "callOptions", "checkInOptions", "defaultKeyFor", "category", "Lcom/pulselink/domain/model/SoundCategory;", "discoverSounds", "emergencyOptions", "resolve", "key", "fallbackCategory", "toLabel", "raw", "fallback", "Companion", "androidApp_proRelease"})
public final class SoundCatalog {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy soundOptions$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy callSoundOptions$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy cachedOptions$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String SIREN_PREFIX = "alert_siren";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String CHIME_PREFIX = "alert_chime";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SYSTEM_NOTIFICATION_DEFAULT = "system_default_notification";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SYSTEM_RINGTONE_DEFAULT = "system_default_ringtone";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.alert.SoundCatalog.Companion Companion = null;
    
    @javax.inject.Inject()
    public SoundCatalog(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final java.util.List<com.pulselink.domain.model.SoundOption> getSoundOptions() {
        return null;
    }
    
    private final java.util.List<com.pulselink.domain.model.SoundOption> getCallSoundOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> emergencyOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> checkInOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> callOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pulselink.domain.model.SoundOption resolve(@org.jetbrains.annotations.Nullable()
    java.lang.String key, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.SoundCategory fallbackCategory) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String defaultKeyFor(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.SoundCategory category) {
        return null;
    }
    
    private final java.util.Map<java.lang.String, com.pulselink.domain.model.SoundOption> getCachedOptions() {
        return null;
    }
    
    private final java.util.List<com.pulselink.domain.model.SoundOption> discoverSounds() {
        return null;
    }
    
    private final java.util.List<com.pulselink.domain.model.SoundOption> buildCallOptions() {
        return null;
    }
    
    private final java.lang.String toLabel(java.lang.String raw, java.lang.String fallback) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/pulselink/data/alert/SoundCatalog$Companion;", "", "()V", "CHIME_PREFIX", "", "KEY_SYSTEM_NOTIFICATION_DEFAULT", "KEY_SYSTEM_RINGTONE_DEFAULT", "SIREN_PREFIX", "androidApp_proRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}