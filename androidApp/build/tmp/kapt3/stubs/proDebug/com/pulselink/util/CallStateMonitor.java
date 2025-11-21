package com.pulselink.util;

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import androidx.annotation.RequiresPermission;
import androidx.core.content.ContextCompat;
import dagger.hilt.android.qualifiers.ApplicationContext;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\t\b\u0007\u0018\u00002\u00020\u0001B\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u001e\u001a\u00020\rJ\b\u0010\u001f\u001a\u00020\rH\u0002J\b\u0010 \u001a\u00020\rH\u0002J\u001a\u0010!\u001a\u00020\r2\u0006\u0010\"\u001a\u00020#2\b\u0010$\u001a\u0004\u0018\u00010\u000fH\u0002J.\u0010%\u001a\u00020\r2\u0014\u0010&\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u000f\u0012\u0004\u0012\u00020\r0\u000b2\u000e\b\u0002\u0010\'\u001a\b\u0012\u0004\u0012\u00020\r0\u0011H\u0007J\u001c\u0010(\u001a\u00020\r2\u0012\u0010)\u001a\u000e\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r0\u000bH\u0007J\b\u0010*\u001a\u00020\rH\u0002J\u0006\u0010+\u001a\u00020\rR\u000e\u0010\u0007\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\n\u001a\u0010\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u000e\u001a\u0012\u0012\u0006\u0012\u0004\u0018\u00010\u000f\u0012\u0004\u0012\u00020\r\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n\u0012\u0004\u0012\u00020\r\u0018\u00010\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0012\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006,"}, d2 = {"Lcom/pulselink/util/CallStateMonitor;", "", "context", "Landroid/content/Context;", "telephonyManager", "Landroid/telephony/TelephonyManager;", "(Landroid/content/Context;Landroid/telephony/TelephonyManager;)V", "active", "", "awaitingResult", "completion", "Lkotlin/Function1;", "", "", "incomingCallback", "", "incomingFinishedCallback", "Lkotlin/Function0;", "lastIncomingNumber", "legacyListener", "Landroid/telephony/PhoneStateListener;", "lock", "mainExecutor", "Ljava/util/concurrent/Executor;", "monitoringIncoming", "monitoringOutgoing", "ringingActive", "startTimestamp", "telephonyCallback", "Landroid/telephony/TelephonyCallback;", "cancel", "dispatchFailure", "ensureRegisteredLocked", "handleState", "state", "", "phoneNumber", "monitorIncomingCalls", "onRinging", "onCallFinished", "monitorOutgoingCall", "onCompleted", "releaseIfPossibleLocked", "stopIncomingMonitoring", "androidApp_proDebug"})
public final class CallStateMonitor {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final android.telephony.TelephonyManager telephonyManager = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Object lock = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.Executor mainExecutor = null;
    @kotlin.jvm.Volatile()
    private volatile boolean active = false;
    @kotlin.jvm.Volatile()
    private volatile long startTimestamp = 0L;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> completion;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> incomingCallback;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile kotlin.jvm.functions.Function0<kotlin.Unit> incomingFinishedCallback;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile android.telephony.TelephonyCallback telephonyCallback;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile android.telephony.PhoneStateListener legacyListener;
    private boolean awaitingResult = false;
    private boolean monitoringOutgoing = false;
    private boolean monitoringIncoming = false;
    private boolean ringingActive = false;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String lastIncomingNumber;
    
    @javax.inject.Inject()
    public CallStateMonitor(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.telephony.TelephonyManager telephonyManager) {
        super();
    }
    
    @androidx.annotation.RequiresPermission(value = "android.permission.READ_PHONE_STATE")
    public final void monitorOutgoingCall(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onCompleted) {
    }
    
    public final void cancel() {
    }
    
    @androidx.annotation.RequiresPermission(allOf = {"android.permission.READ_PHONE_STATE", "android.permission.READ_CALL_LOG"})
    public final void monitorIncomingCalls(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onRinging, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onCallFinished) {
    }
    
    public final void stopIncomingMonitoring() {
    }
    
    private final void ensureRegisteredLocked() {
    }
    
    private final void releaseIfPossibleLocked() {
    }
    
    private final void handleState(int state, java.lang.String phoneNumber) {
    }
    
    private final void dispatchFailure() {
    }
}