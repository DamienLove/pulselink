package com.pulselink.data.sms;

import android.net.Uri;
import com.pulselink.domain.model.EscalationTier;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0011\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001&B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J)\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0012\u0010\b\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u00040\t\"\u00020\u0004H\u0002\u00a2\u0006\u0002\u0010\nJ(\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u0011J\u001e\u0010\u0012\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u0013\u001a\u00020\u0014J\u001e\u0010\u0015\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u0016\u001a\u00020\u0017J&\u0010\u0018\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u0019\u001a\u00020\u00042\u0006\u0010\u001a\u001a\u00020\u0004J\u0016\u0010\u001b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u0004J\u001e\u0010\u001c\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u001d\u001a\u00020\u0004J\u001e\u0010\u001e\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u001f\u001a\u00020\u0004J\u0016\u0010 \u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u0004J\u001e\u0010!\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u000fJ(\u0010\"\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010#\u001a\u0004\u0018\u00010\u0004J\u0010\u0010$\u001a\u0004\u0018\u00010%2\u0006\u0010\u001f\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\'"}, d2 = {"Lcom/pulselink/data/sms/SmsCodec;", "", "()V", "PREFIX", "", "build", "type", "Lcom/pulselink/data/sms/SmsCodec$Type;", "parts", "", "(Lcom/pulselink/data/sms/SmsCodec$Type;[Ljava/lang/String;)Ljava/lang/String;", "encodeAlertPrepare", "senderId", "code", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "reason", "Lcom/pulselink/data/sms/PulseLinkMessage$AlertPrepareReason;", "encodeAlertReady", "ready", "", "encodeCallEnded", "callDuration", "", "encodeConfig", "key", "value", "encodeLinkAccept", "encodeLinkRequest", "senderName", "encodeManualMessage", "body", "encodePing", "encodeRemoteAlert", "encodeSoundOverride", "soundKey", "parse", "Lcom/pulselink/data/sms/PulseLinkMessage;", "Type", "androidApp_proRelease"})
public final class SmsCodec {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFIX = "PULSELINK";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.sms.SmsCodec INSTANCE = null;
    
    private SmsCodec() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeLinkRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    java.lang.String senderName) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeLinkAccept(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodePing(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeRemoteAlert(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeAlertPrepare(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.PulseLinkMessage.AlertPrepareReason reason) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeAlertReady(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, boolean ready) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeManualMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    java.lang.String body) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeSoundOverride(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.Nullable()
    java.lang.String soundKey) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeConfig(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    java.lang.String key, @org.jetbrains.annotations.NotNull()
    java.lang.String value) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String encodeCallEnded(@org.jetbrains.annotations.NotNull()
    java.lang.String senderId, @org.jetbrains.annotations.NotNull()
    java.lang.String code, long callDuration) {
        return null;
    }
    
    private final java.lang.String build(com.pulselink.data.sms.SmsCodec.Type type, java.lang.String... parts) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.pulselink.data.sms.PulseLinkMessage parse(@org.jetbrains.annotations.NotNull()
    java.lang.String body) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u000e\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u000f\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010\u00a8\u0006\u0011"}, d2 = {"Lcom/pulselink/data/sms/SmsCodec$Type;", "", "wire", "", "(Ljava/lang/String;ILjava/lang/String;)V", "getWire", "()Ljava/lang/String;", "LINK_REQUEST", "LINK_ACCEPT", "PING", "ALERT", "ALERT_PREPARE", "ALERT_READY", "MESSAGE", "SOUND_OVERRIDE", "CONFIG", "CALL_ENDED", "androidApp_proRelease"})
    public static enum Type {
        /*public static final*/ LINK_REQUEST /* = new LINK_REQUEST(null) */,
        /*public static final*/ LINK_ACCEPT /* = new LINK_ACCEPT(null) */,
        /*public static final*/ PING /* = new PING(null) */,
        /*public static final*/ ALERT /* = new ALERT(null) */,
        /*public static final*/ ALERT_PREPARE /* = new ALERT_PREPARE(null) */,
        /*public static final*/ ALERT_READY /* = new ALERT_READY(null) */,
        /*public static final*/ MESSAGE /* = new MESSAGE(null) */,
        /*public static final*/ SOUND_OVERRIDE /* = new SOUND_OVERRIDE(null) */,
        /*public static final*/ CONFIG /* = new CONFIG(null) */,
        /*public static final*/ CALL_ENDED /* = new CALL_ENDED(null) */;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String wire = null;
        
        Type(java.lang.String wire) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getWire() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.pulselink.data.sms.SmsCodec.Type> getEntries() {
            return null;
        }
    }
}