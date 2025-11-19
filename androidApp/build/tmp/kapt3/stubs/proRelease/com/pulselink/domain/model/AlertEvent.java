package com.pulselink.domain.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000b\n\u0002\b)\b\u0087\b\u0018\u00002\u00020\u0001Bm\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\f\u0012\u0006\u0010\r\u001a\u00020\f\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u0012\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0006\u0012\b\b\u0002\u0010\u0010\u001a\u00020\f\u0012\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\u0002\u0010\u0012J\t\u0010$\u001a\u00020\u0003H\u00c6\u0003J\t\u0010%\u001a\u00020\fH\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\t\u0010\'\u001a\u00020\u0003H\u00c6\u0003J\t\u0010(\u001a\u00020\u0006H\u00c6\u0003J\t\u0010)\u001a\u00020\bH\u00c6\u0003J\t\u0010*\u001a\u00020\nH\u00c6\u0003J\t\u0010+\u001a\u00020\fH\u00c6\u0003J\t\u0010,\u001a\u00020\fH\u00c6\u0003J\u0010\u0010-\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0016J\u000b\u0010.\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003J\u0082\u0001\u0010/\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\f2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u00032\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u00062\b\b\u0002\u0010\u0010\u001a\u00020\f2\n\b\u0002\u0010\u0011\u001a\u0004\u0018\u00010\u0006H\u00c6\u0001\u00a2\u0006\u0002\u00100J\u0013\u00101\u001a\u00020\f2\b\u00102\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u00103\u001a\u00020\nH\u00d6\u0001J\t\u00104\u001a\u00020\u0006H\u00d6\u0001R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0015\u0010\u000e\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\u0017\u001a\u0004\b\u0015\u0010\u0016R\u0013\u0010\u000f\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0011\u0010\u0010\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u001cR\u0011\u0010\u000b\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001cR\u0011\u0010\r\u001a\u00020\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001cR\u0013\u0010\u0011\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0019R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010!R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001bR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0019\u00a8\u00065"}, d2 = {"Lcom/pulselink/domain/model/AlertEvent;", "", "id", "", "timestamp", "triggeredBy", "", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "contactCount", "", "sentSms", "", "sharedLocation", "contactId", "contactName", "isIncoming", "soundKey", "(JJLjava/lang/String;Lcom/pulselink/domain/model/EscalationTier;IZZLjava/lang/Long;Ljava/lang/String;ZLjava/lang/String;)V", "getContactCount", "()I", "getContactId", "()Ljava/lang/Long;", "Ljava/lang/Long;", "getContactName", "()Ljava/lang/String;", "getId", "()J", "()Z", "getSentSms", "getSharedLocation", "getSoundKey", "getTier", "()Lcom/pulselink/domain/model/EscalationTier;", "getTimestamp", "getTriggeredBy", "component1", "component10", "component11", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "(JJLjava/lang/String;Lcom/pulselink/domain/model/EscalationTier;IZZLjava/lang/Long;Ljava/lang/String;ZLjava/lang/String;)Lcom/pulselink/domain/model/AlertEvent;", "equals", "other", "hashCode", "toString", "androidApp_proRelease"})
@androidx.room.Entity(tableName = "alert_events")
public final class AlertEvent {
    @androidx.room.PrimaryKey(autoGenerate = true)
    private final long id = 0L;
    private final long timestamp = 0L;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String triggeredBy = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.model.EscalationTier tier = null;
    private final int contactCount = 0;
    private final boolean sentSms = false;
    private final boolean sharedLocation = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Long contactId = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String contactName = null;
    private final boolean isIncoming = false;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String soundKey = null;
    
    public AlertEvent(long id, long timestamp, @org.jetbrains.annotations.NotNull()
    java.lang.String triggeredBy, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, int contactCount, boolean sentSms, boolean sharedLocation, @org.jetbrains.annotations.Nullable()
    java.lang.Long contactId, @org.jetbrains.annotations.Nullable()
    java.lang.String contactName, boolean isIncoming, @org.jetbrains.annotations.Nullable()
    java.lang.String soundKey) {
        super();
    }
    
    public final long getId() {
        return 0L;
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTriggeredBy() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.EscalationTier getTier() {
        return null;
    }
    
    public final int getContactCount() {
        return 0;
    }
    
    public final boolean getSentSms() {
        return false;
    }
    
    public final boolean getSharedLocation() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long getContactId() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getContactName() {
        return null;
    }
    
    public final boolean isIncoming() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getSoundKey() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    public final boolean component10() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component11() {
        return null;
    }
    
    public final long component2() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.EscalationTier component4() {
        return null;
    }
    
    public final int component5() {
        return 0;
    }
    
    public final boolean component6() {
        return false;
    }
    
    public final boolean component7() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Long component8() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component9() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.domain.model.AlertEvent copy(long id, long timestamp, @org.jetbrains.annotations.NotNull()
    java.lang.String triggeredBy, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, int contactCount, boolean sentSms, boolean sharedLocation, @org.jetbrains.annotations.Nullable()
    java.lang.Long contactId, @org.jetbrains.annotations.Nullable()
    java.lang.String contactName, boolean isIncoming, @org.jetbrains.annotations.Nullable()
    java.lang.String soundKey) {
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