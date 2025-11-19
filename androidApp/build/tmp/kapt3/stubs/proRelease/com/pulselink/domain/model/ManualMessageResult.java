package com.pulselink.domain.model;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001:\u0002\u0003\u0004B\u0007\b\u0004\u00a2\u0006\u0002\u0010\u0002\u0082\u0001\u0002\u0005\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pulselink/domain/model/ManualMessageResult;", "", "()V", "Failure", "Success", "Lcom/pulselink/domain/model/ManualMessageResult$Failure;", "Lcom/pulselink/domain/model/ManualMessageResult$Success;", "androidApp_proRelease"})
public abstract class ManualMessageResult {
    
    private ManualMessageResult() {
        super();
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001:\u0001\u0011B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0012"}, d2 = {"Lcom/pulselink/domain/model/ManualMessageResult$Failure;", "Lcom/pulselink/domain/model/ManualMessageResult;", "reason", "Lcom/pulselink/domain/model/ManualMessageResult$Failure$Reason;", "(Lcom/pulselink/domain/model/ManualMessageResult$Failure$Reason;)V", "getReason", "()Lcom/pulselink/domain/model/ManualMessageResult$Failure$Reason;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "Reason", "androidApp_proRelease"})
    public static final class Failure extends com.pulselink.domain.model.ManualMessageResult {
        @org.jetbrains.annotations.NotNull()
        private final com.pulselink.domain.model.ManualMessageResult.Failure.Reason reason = null;
        
        public Failure(@org.jetbrains.annotations.NotNull()
        com.pulselink.domain.model.ManualMessageResult.Failure.Reason reason) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.domain.model.ManualMessageResult.Failure.Reason getReason() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.domain.model.ManualMessageResult.Failure.Reason component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.domain.model.ManualMessageResult.Failure copy(@org.jetbrains.annotations.NotNull()
        com.pulselink.domain.model.ManualMessageResult.Failure.Reason reason) {
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
        
        @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005j\u0002\b\u0006j\u0002\b\u0007\u00a8\u0006\b"}, d2 = {"Lcom/pulselink/domain/model/ManualMessageResult$Failure$Reason;", "", "(Ljava/lang/String;I)V", "CONTACT_MISSING", "NOT_LINKED", "SMS_FAILED", "PERMISSION_REQUIRED", "UNKNOWN", "androidApp_proRelease"})
        public static enum Reason {
            /*public static final*/ CONTACT_MISSING /* = new CONTACT_MISSING() */,
            /*public static final*/ NOT_LINKED /* = new NOT_LINKED() */,
            /*public static final*/ SMS_FAILED /* = new SMS_FAILED() */,
            /*public static final*/ PERMISSION_REQUIRED /* = new PERMISSION_REQUIRED() */,
            /*public static final*/ UNKNOWN /* = new UNKNOWN() */;
            
            Reason() {
            }
            
            @org.jetbrains.annotations.NotNull()
            public static kotlin.enums.EnumEntries<com.pulselink.domain.model.ManualMessageResult.Failure.Reason> getEntries() {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\n\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0005J\t\u0010\t\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u001d\u0010\u000b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\f\u001a\u00020\u00032\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u00d6\u0003J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0007\u00a8\u0006\u0013"}, d2 = {"Lcom/pulselink/domain/model/ManualMessageResult$Success;", "Lcom/pulselink/domain/model/ManualMessageResult;", "overrideApplied", "", "deliveryPending", "(ZZ)V", "getDeliveryPending", "()Z", "getOverrideApplied", "component1", "component2", "copy", "equals", "other", "", "hashCode", "", "toString", "", "androidApp_proRelease"})
    public static final class Success extends com.pulselink.domain.model.ManualMessageResult {
        private final boolean overrideApplied = false;
        private final boolean deliveryPending = false;
        
        public Success(boolean overrideApplied, boolean deliveryPending) {
        }
        
        public final boolean getOverrideApplied() {
            return false;
        }
        
        public final boolean getDeliveryPending() {
            return false;
        }
        
        public final boolean component1() {
            return false;
        }
        
        public final boolean component2() {
            return false;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.pulselink.domain.model.ManualMessageResult.Success copy(boolean overrideApplied, boolean deliveryPending) {
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
}