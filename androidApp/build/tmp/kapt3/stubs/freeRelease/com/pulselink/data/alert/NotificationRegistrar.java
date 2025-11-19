package com.pulselink.data.alert;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.util.Log;
import com.pulselink.R;
import com.pulselink.domain.model.AlertProfile;
import com.pulselink.domain.model.SoundCategory;
import com.pulselink.domain.model.SoundOption;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00132\u00020\u0001:\u0001\u0013B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u001a\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\nH\u0002J \u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\b\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\u000fJ\u0018\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\f\u001a\u00020\rH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/pulselink/data/alert/NotificationRegistrar;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "buildChannelId", "", "category", "Lcom/pulselink/domain/model/SoundCategory;", "soundOption", "Lcom/pulselink/domain/model/SoundOption;", "ensureAlertChannel", "profile", "Lcom/pulselink/domain/model/AlertProfile;", "ensureChannels", "", "validateChannel", "channel", "Landroid/app/NotificationChannel;", "Companion", "androidApp_freeRelease"})
public final class NotificationRegistrar {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "NotificationRegistrar";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LEGACY_ALERT_CHANNEL = "pulse_alerts_legacy";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LEGACY_CHECK_IN_CHANNEL = "pulse_checkins_legacy";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String LEGACY_CALL_CHANNEL = "pulse_call_legacy";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_BACKGROUND = "pulse_background";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String GROUP_ALERTS = "pulse_group_alerts";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.alert.NotificationRegistrar.Companion Companion = null;
    
    @javax.inject.Inject()
    public NotificationRegistrar(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final void ensureChannels() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String ensureAlertChannel(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.SoundCategory category, @org.jetbrains.annotations.Nullable()
    com.pulselink.domain.model.SoundOption soundOption, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertProfile profile) {
        return null;
    }
    
    private final java.lang.String buildChannelId(com.pulselink.domain.model.SoundCategory category, com.pulselink.domain.model.SoundOption soundOption) {
        return null;
    }
    
    private final void validateChannel(android.app.NotificationChannel channel, com.pulselink.domain.model.AlertProfile profile) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/pulselink/data/alert/NotificationRegistrar$Companion;", "", "()V", "CHANNEL_BACKGROUND", "", "GROUP_ALERTS", "LEGACY_ALERT_CHANNEL", "LEGACY_CALL_CHANNEL", "LEGACY_CHECK_IN_CHANNEL", "TAG", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}