package com.pulselink.ui.state;

import androidx.annotation.StringRes;
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.PulseLinkSettings;
import com.pulselink.domain.model.SoundOption;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b$\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u00b3\u0001\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0005\u0012\b\b\u0002\u0010\t\u001a\u00020\n\u0012\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f\u0012\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u0012\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u0012\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u0012\b\b\u0002\u0010\u0011\u001a\u00020\n\u0012\b\b\u0002\u0010\u0012\u001a\u00020\n\u0012\b\b\u0002\u0010\u0013\u001a\u00020\n\u0012\b\b\u0002\u0010\u0014\u001a\u00020\n\u0012\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u0016\u0012\b\b\u0002\u0010\u0017\u001a\u00020\n\u00a2\u0006\u0002\u0010\u0018J\t\u0010)\u001a\u00020\u0003H\u00c6\u0003J\t\u0010*\u001a\u00020\nH\u00c6\u0003J\t\u0010+\u001a\u00020\nH\u00c6\u0003J\t\u0010,\u001a\u00020\nH\u00c6\u0003J\u000b\u0010-\u001a\u0004\u0018\u00010\u0016H\u00c6\u0003J\t\u0010.\u001a\u00020\nH\u00c6\u0003J\u000f\u0010/\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005H\u00c6\u0003J\u000f\u00100\u001a\b\u0012\u0004\u0012\u00020\b0\u0005H\u00c6\u0003J\t\u00101\u001a\u00020\nH\u00c6\u0003J\u000b\u00102\u001a\u0004\u0018\u00010\fH\u00c6\u0003J\u000f\u00103\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005H\u00c6\u0003J\u000f\u00104\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005H\u00c6\u0003J\u000f\u00105\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005H\u00c6\u0003J\t\u00106\u001a\u00020\nH\u00c6\u0003J\u00b7\u0001\u00107\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\u000e\b\u0002\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00052\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00052\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f2\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00052\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00052\u000e\b\u0002\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000e0\u00052\b\b\u0002\u0010\u0011\u001a\u00020\n2\b\b\u0002\u0010\u0012\u001a\u00020\n2\b\b\u0002\u0010\u0013\u001a\u00020\n2\b\b\u0002\u0010\u0014\u001a\u00020\n2\n\b\u0002\u0010\u0015\u001a\u0004\u0018\u00010\u00162\b\b\u0002\u0010\u0017\u001a\u00020\nH\u00c6\u0001J\u0013\u00108\u001a\u00020\n2\b\u00109\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010:\u001a\u00020;H\u00d6\u0001J\t\u0010<\u001a\u00020\fH\u00d6\u0001R\u0011\u0010\u0013\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0017\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001cR\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001cR\u0013\u0010\u0015\u001a\u0004\u0018\u00010\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\u001cR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u001aR\u0011\u0010\u0017\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u001aR\u0011\u0010\u0012\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u001aR\u0013\u0010\u000b\u001a\u0004\u0018\u00010\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010#R\u0011\u0010\u0014\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u001aR\u0017\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u001cR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0011\u0010\u0011\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u001a\u00a8\u0006="}, d2 = {"Lcom/pulselink/ui/state/PulseLinkUiState;", "", "settings", "Lcom/pulselink/domain/model/PulseLinkSettings;", "contacts", "", "Lcom/pulselink/domain/model/Contact;", "recentEvents", "Lcom/pulselink/domain/model/AlertEvent;", "isDispatching", "", "lastMessagePreview", "", "emergencySoundOptions", "Lcom/pulselink/domain/model/SoundOption;", "checkInSoundOptions", "callSoundOptions", "showAds", "isProUser", "adsAvailable", "onboardingComplete", "dndStatus", "Lcom/pulselink/ui/state/DndStatusMessage;", "isEmergencyActive", "(Lcom/pulselink/domain/model/PulseLinkSettings;Ljava/util/List;Ljava/util/List;ZLjava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/util/List;ZZZZLcom/pulselink/ui/state/DndStatusMessage;Z)V", "getAdsAvailable", "()Z", "getCallSoundOptions", "()Ljava/util/List;", "getCheckInSoundOptions", "getContacts", "getDndStatus", "()Lcom/pulselink/ui/state/DndStatusMessage;", "getEmergencySoundOptions", "getLastMessagePreview", "()Ljava/lang/String;", "getOnboardingComplete", "getRecentEvents", "getSettings", "()Lcom/pulselink/domain/model/PulseLinkSettings;", "getShowAds", "component1", "component10", "component11", "component12", "component13", "component14", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "", "toString", "androidApp_freeDebug"})
public final class PulseLinkUiState {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.model.PulseLinkSettings settings = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.Contact> contacts = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.AlertEvent> recentEvents = null;
    private final boolean isDispatching = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String lastMessagePreview = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> emergencySoundOptions = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> checkInSoundOptions = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<com.pulselink.domain.model.SoundOption> callSoundOptions = null;
    private final boolean showAds = false;
    private final boolean isProUser = false;
    private final boolean adsAvailable = false;
    private final boolean onboardingComplete = false;
    @org.jetbrains.annotations.Nullable()
    private final com.pulselink.ui.state.DndStatusMessage dndStatus = null;
    private final boolean isEmergencyActive = false;
    
    public PulseLinkUiState(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings settings, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.Contact> contacts, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.AlertEvent> recentEvents, boolean isDispatching, @org.jetbrains.annotations.Nullable()
    java.lang.String lastMessagePreview, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> emergencySoundOptions, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> checkInSoundOptions, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> callSoundOptions, boolean showAds, boolean isProUser, boolean adsAvailable, boolean onboardingComplete, @org.jetbrains.annotations.Nullable()
    com.pulselink.ui.state.DndStatusMessage dndStatus, boolean isEmergencyActive) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.PulseLinkSettings getSettings() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.Contact> getContacts() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.AlertEvent> getRecentEvents() {
        return null;
    }
    
    public final boolean isDispatching() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getLastMessagePreview() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> getEmergencySoundOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> getCheckInSoundOptions() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> getCallSoundOptions() {
        return null;
    }
    
    public final boolean getShowAds() {
        return false;
    }
    
    public final boolean isProUser() {
        return false;
    }
    
    public final boolean getAdsAvailable() {
        return false;
    }
    
    public final boolean getOnboardingComplete() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pulselink.ui.state.DndStatusMessage getDndStatus() {
        return null;
    }
    
    public final boolean isEmergencyActive() {
        return false;
    }
    
    public PulseLinkUiState() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.PulseLinkSettings component1() {
        return null;
    }
    
    public final boolean component10() {
        return false;
    }
    
    public final boolean component11() {
        return false;
    }
    
    public final boolean component12() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pulselink.ui.state.DndStatusMessage component13() {
        return null;
    }
    
    public final boolean component14() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.Contact> component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.AlertEvent> component3() {
        return null;
    }
    
    public final boolean component4() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.pulselink.domain.model.SoundOption> component8() {
        return null;
    }
    
    public final boolean component9() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.ui.state.PulseLinkUiState copy(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings settings, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.Contact> contacts, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.AlertEvent> recentEvents, boolean isDispatching, @org.jetbrains.annotations.Nullable()
    java.lang.String lastMessagePreview, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> emergencySoundOptions, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> checkInSoundOptions, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.SoundOption> callSoundOptions, boolean showAds, boolean isProUser, boolean adsAvailable, boolean onboardingComplete, @org.jetbrains.annotations.Nullable()
    com.pulselink.ui.state.DndStatusMessage dndStatus, boolean isEmergencyActive) {
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