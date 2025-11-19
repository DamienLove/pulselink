package com.pulselink.domain.model;

import kotlinx.serialization.SerialName;
import kotlinx.serialization.Serializable;

@kotlinx.serialization.Serializable()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b)\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0087\b\u0018\u0000 M2\u00020\u0001:\u0002LMB\u00a9\u0001\b\u0011\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0006\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\b\u0012\u0006\u0010\n\u001a\u00020\b\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\f\u0012\b\u0010\r\u001a\u0004\u0018\u00010\f\u0012\b\u0010\u000e\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u000f\u001a\u00020\b\u0012\b\u0010\u0010\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0011\u001a\u00020\b\u0012\u0006\u0010\u0012\u001a\u00020\b\u0012\u0006\u0010\u0013\u001a\u00020\b\u0012\b\u0010\u0014\u001a\u0004\u0018\u00010\u0005\u0012\u0006\u0010\u0015\u001a\u00020\b\u0012\b\u0010\u0016\u001a\u0004\u0018\u00010\u0005\u0012\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018\u00a2\u0006\u0002\u0010\u0019B\u00a9\u0001\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\b\b\u0002\u0010\n\u001a\u00020\b\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\f\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u000f\u001a\u00020\b\u0012\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\u0011\u001a\u00020\b\u0012\b\b\u0002\u0010\u0012\u001a\u00020\b\u0012\b\b\u0002\u0010\u0013\u001a\u00020\b\u0012\b\b\u0002\u0010\u0014\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0015\u001a\u00020\b\u0012\b\b\u0002\u0010\u0016\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u001aJ\t\u0010-\u001a\u00020\u0005H\u00c6\u0003J\u000b\u0010.\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\t\u0010/\u001a\u00020\bH\u00c6\u0003J\t\u00100\u001a\u00020\bH\u00c6\u0003J\t\u00101\u001a\u00020\bH\u00c6\u0003J\t\u00102\u001a\u00020\u0005H\u00c6\u0003J\t\u00103\u001a\u00020\bH\u00c6\u0003J\t\u00104\u001a\u00020\u0005H\u00c6\u0003J\t\u00105\u001a\u00020\u0005H\u00c6\u0003J\t\u00106\u001a\u00020\bH\u00c6\u0003J\t\u00107\u001a\u00020\bH\u00c6\u0003J\t\u00108\u001a\u00020\bH\u00c6\u0003J\t\u00109\u001a\u00020\fH\u00c6\u0003J\t\u0010:\u001a\u00020\fH\u00c6\u0003J\u000b\u0010;\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\t\u0010<\u001a\u00020\bH\u00c6\u0003J\u00ad\u0001\u0010=\u001a\u00020\u00002\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\b2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\f2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\u000f\u001a\u00020\b2\n\b\u0002\u0010\u0010\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\u0011\u001a\u00020\b2\b\b\u0002\u0010\u0012\u001a\u00020\b2\b\b\u0002\u0010\u0013\u001a\u00020\b2\b\b\u0002\u0010\u0014\u001a\u00020\u00052\b\b\u0002\u0010\u0015\u001a\u00020\b2\b\b\u0002\u0010\u0016\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010>\u001a\u00020\b2\b\u0010?\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010@\u001a\u00020\u0003H\u00d6\u0001J\f\u0010A\u001a\b\u0012\u0004\u0012\u00020\u00050BJ\t\u0010C\u001a\u00020\u0005H\u00d6\u0001J&\u0010D\u001a\u00020E2\u0006\u0010F\u001a\u00020\u00002\u0006\u0010G\u001a\u00020H2\u0006\u0010I\u001a\u00020JH\u00c1\u0001\u00a2\u0006\u0002\bKR\u0011\u0010\n\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001cR\u0011\u0010\u0011\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001cR\u0011\u0010\u000f\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u001cR\u0013\u0010\u0010\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010!R\u0011\u0010\r\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010$R\u0011\u0010\u0014\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010!R\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010$R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001cR\u0011\u0010\u0015\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u001cR\u0011\u0010\u0013\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010\u001cR\u0011\u0010\u0016\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b)\u0010!R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010!R\u0011\u0010\u0012\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001cR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b,\u0010!\u00a8\u0006N"}, d2 = {"Lcom/pulselink/domain/model/PulseLinkSettings;", "", "seen1", "", "primaryPhrase", "", "secondaryPhrase", "includeLocation", "", "autoAllowRemoteSoundChange", "assistantShortcutsDismissed", "emergencyProfile", "Lcom/pulselink/domain/model/AlertProfile;", "checkInProfile", "callSoundKey", "betaAgreementAccepted", "betaAgreementVersion", "autoCallAfterAlert", "proUnlocked", "onboardingComplete", "deviceId", "isBetaTester", "ownerName", "serializationConstructorMarker", "Lkotlinx/serialization/internal/SerializationConstructorMarker;", "(ILjava/lang/String;Ljava/lang/String;ZZZLcom/pulselink/domain/model/AlertProfile;Lcom/pulselink/domain/model/AlertProfile;Ljava/lang/String;ZLjava/lang/String;ZZZLjava/lang/String;ZLjava/lang/String;Lkotlinx/serialization/internal/SerializationConstructorMarker;)V", "(Ljava/lang/String;Ljava/lang/String;ZZZLcom/pulselink/domain/model/AlertProfile;Lcom/pulselink/domain/model/AlertProfile;Ljava/lang/String;ZLjava/lang/String;ZZZLjava/lang/String;ZLjava/lang/String;)V", "getAssistantShortcutsDismissed", "()Z", "getAutoAllowRemoteSoundChange", "getAutoCallAfterAlert", "getBetaAgreementAccepted", "getBetaAgreementVersion", "()Ljava/lang/String;", "getCallSoundKey", "getCheckInProfile", "()Lcom/pulselink/domain/model/AlertProfile;", "getDeviceId", "getEmergencyProfile", "getIncludeLocation", "getOnboardingComplete", "getOwnerName", "getPrimaryPhrase", "getProUnlocked", "getSecondaryPhrase", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "phrases", "", "toString", "write$Self", "", "self", "output", "Lkotlinx/serialization/encoding/CompositeEncoder;", "serialDesc", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "write$Self$androidApp_freeRelease", "$serializer", "Companion", "androidApp_freeRelease"})
public final class PulseLinkSettings {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String primaryPhrase = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String secondaryPhrase = null;
    private final boolean includeLocation = false;
    private final boolean autoAllowRemoteSoundChange = false;
    private final boolean assistantShortcutsDismissed = false;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.model.AlertProfile emergencyProfile = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.model.AlertProfile checkInProfile = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String callSoundKey = null;
    private final boolean betaAgreementAccepted = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String betaAgreementVersion = null;
    private final boolean autoCallAfterAlert = false;
    private final boolean proUnlocked = false;
    private final boolean onboardingComplete = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String deviceId = null;
    private final boolean isBetaTester = false;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String ownerName = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.domain.model.PulseLinkSettings.Companion Companion = null;
    
    public PulseLinkSettings(@org.jetbrains.annotations.NotNull()
    java.lang.String primaryPhrase, @org.jetbrains.annotations.NotNull()
    java.lang.String secondaryPhrase, boolean includeLocation, boolean autoAllowRemoteSoundChange, boolean assistantShortcutsDismissed, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertProfile emergencyProfile, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertProfile checkInProfile, @org.jetbrains.annotations.Nullable()
    java.lang.String callSoundKey, boolean betaAgreementAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String betaAgreementVersion, boolean autoCallAfterAlert, boolean proUnlocked, boolean onboardingComplete, @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, boolean isBetaTester, @org.jetbrains.annotations.NotNull()
    java.lang.String ownerName) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPrimaryPhrase() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSecondaryPhrase() {
        return null;
    }
    
    public final boolean getIncludeLocation() {
        return false;
    }
    
    public final boolean getAutoAllowRemoteSoundChange() {
        return false;
    }
    
    public final boolean getAssistantShortcutsDismissed() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.AlertProfile getEmergencyProfile() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.AlertProfile getCheckInProfile() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getCallSoundKey() {
        return null;
    }
    
    public final boolean getBetaAgreementAccepted() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getBetaAgreementVersion() {
        return null;
    }
    
    public final boolean getAutoCallAfterAlert() {
        return false;
    }
    
    public final boolean getProUnlocked() {
        return false;
    }
    
    public final boolean getOnboardingComplete() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getDeviceId() {
        return null;
    }
    
    public final boolean isBetaTester() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getOwnerName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<java.lang.String> phrases() {
        return null;
    }
    
    public PulseLinkSettings() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component10() {
        return null;
    }
    
    public final boolean component11() {
        return false;
    }
    
    public final boolean component12() {
        return false;
    }
    
    public final boolean component13() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component14() {
        return null;
    }
    
    public final boolean component15() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component16() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final boolean component3() {
        return false;
    }
    
    public final boolean component4() {
        return false;
    }
    
    public final boolean component5() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.AlertProfile component6() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.AlertProfile component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component8() {
        return null;
    }
    
    public final boolean component9() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.PulseLinkSettings copy(@org.jetbrains.annotations.NotNull()
    java.lang.String primaryPhrase, @org.jetbrains.annotations.NotNull()
    java.lang.String secondaryPhrase, boolean includeLocation, boolean autoAllowRemoteSoundChange, boolean assistantShortcutsDismissed, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertProfile emergencyProfile, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertProfile checkInProfile, @org.jetbrains.annotations.Nullable()
    java.lang.String callSoundKey, boolean betaAgreementAccepted, @org.jetbrains.annotations.Nullable()
    java.lang.String betaAgreementVersion, boolean autoCallAfterAlert, boolean proUnlocked, boolean onboardingComplete, @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, boolean isBetaTester, @org.jetbrains.annotations.NotNull()
    java.lang.String ownerName) {
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
    
    @kotlin.jvm.JvmStatic()
    public static final void write$Self$androidApp_freeRelease(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.PulseLinkSettings self, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.encoding.CompositeEncoder output, @org.jetbrains.annotations.NotNull()
    kotlinx.serialization.descriptors.SerialDescriptor serialDesc) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c7\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\b\u001a\f\u0012\b\u0012\u0006\u0012\u0002\b\u00030\n0\tH\u00d6\u0001\u00a2\u0006\u0002\u0010\u000bJ\u0011\u0010\f\u001a\u00020\u00022\u0006\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\u0019\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0002H\u00d6\u0001R\u0014\u0010\u0004\u001a\u00020\u00058VX\u00d6\u0005\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006\u0014"}, d2 = {"com/pulselink/domain/model/PulseLinkSettings.$serializer", "Lkotlinx/serialization/internal/GeneratedSerializer;", "Lcom/pulselink/domain/model/PulseLinkSettings;", "()V", "descriptor", "Lkotlinx/serialization/descriptors/SerialDescriptor;", "getDescriptor", "()Lkotlinx/serialization/descriptors/SerialDescriptor;", "childSerializers", "", "Lkotlinx/serialization/KSerializer;", "()[Lkotlinx/serialization/KSerializer;", "deserialize", "decoder", "Lkotlinx/serialization/encoding/Decoder;", "serialize", "", "encoder", "Lkotlinx/serialization/encoding/Encoder;", "value", "androidApp_freeRelease"})
    @java.lang.Deprecated()
    public static final class $serializer implements kotlinx.serialization.internal.GeneratedSerializer<com.pulselink.domain.model.PulseLinkSettings> {
        @org.jetbrains.annotations.NotNull()
        public static final com.pulselink.domain.model.PulseLinkSettings.$serializer INSTANCE = null;
        
        private $serializer() {
            super();
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public kotlinx.serialization.KSerializer<?>[] childSerializers() {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public com.pulselink.domain.model.PulseLinkSettings deserialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Decoder decoder) {
            return null;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public kotlinx.serialization.descriptors.SerialDescriptor getDescriptor() {
            return null;
        }
        
        @java.lang.Override()
        public void serialize(@org.jetbrains.annotations.NotNull()
        kotlinx.serialization.encoding.Encoder encoder, @org.jetbrains.annotations.NotNull()
        com.pulselink.domain.model.PulseLinkSettings value) {
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public kotlinx.serialization.KSerializer<?>[] typeParametersSerializers() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000f\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u00c6\u0001\u00a8\u0006\u0006"}, d2 = {"Lcom/pulselink/domain/model/PulseLinkSettings$Companion;", "", "()V", "serializer", "Lkotlinx/serialization/KSerializer;", "Lcom/pulselink/domain/model/PulseLinkSettings;", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final kotlinx.serialization.KSerializer<com.pulselink.domain.model.PulseLinkSettings> serializer() {
            return null;
        }
    }
}