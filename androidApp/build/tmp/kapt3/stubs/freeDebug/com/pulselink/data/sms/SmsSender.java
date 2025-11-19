package com.pulselink.data.sms;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.RequiresPermission;
import com.pulselink.domain.model.Contact;
import com.pulselink.receiver.SmsSendReceiver;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\b\b\u0007\u0018\u0000 \u001e2\u00020\u0001:\u0001\u001eB\u0019\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\t2\u0006\u0010\u000f\u001a\u00020\u000bJ.\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\t2\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u00142\b\b\u0002\u0010\u0016\u001a\u00020\u0017H\u0087@\u00a2\u0006\u0002\u0010\u0018J2\u0010\u0019\u001a\u00020\u000b2\u0006\u0010\u001a\u001a\u00020\t2\u0006\u0010\u0012\u001a\u00020\t2\b\b\u0002\u0010\u001b\u001a\u00020\u00172\b\b\u0002\u0010\u001c\u001a\u00020\u000bH\u0087@\u00a2\u0006\u0002\u0010\u001dR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\u0007\u001a\u0014\u0012\u0004\u0012\u00020\t\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/pulselink/data/sms/SmsSender;", "", "context", "Landroid/content/Context;", "smsManager", "Landroid/telephony/SmsManager;", "(Landroid/content/Context;Landroid/telephony/SmsManager;)V", "pendingRequests", "Ljava/util/concurrent/ConcurrentHashMap;", "", "Lkotlinx/coroutines/CompletableDeferred;", "", "completeSmsRequest", "", "requestId", "success", "sendAlert", "", "message", "contacts", "", "Lcom/pulselink/domain/model/Contact;", "delayBetweenMillis", "", "(Ljava/lang/String;Ljava/util/List;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendSms", "phoneNumber", "timeoutMillis", "awaitResult", "(Ljava/lang/String;Ljava/lang/String;JZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "androidApp_freeDebug"})
public final class SmsSender {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final android.telephony.SmsManager smsManager = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, kotlinx.coroutines.CompletableDeferred<java.lang.Boolean>> pendingRequests = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PulseLinkSmsSender";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_SMS_SENT = "com.pulselink.SMS_SENT";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_SMS_DELIVERED = "com.pulselink.SMS_DELIVERED";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_REQUEST_ID = "request_id";
    private static final long DEFAULT_TIMEOUT_MS = 30000L;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.sms.SmsSender.Companion Companion = null;
    
    @javax.inject.Inject()
    public SmsSender(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.telephony.SmsManager smsManager) {
        super();
    }
    
    @androidx.annotation.RequiresPermission(allOf = {"android.permission.SEND_SMS"})
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendAlert(@org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.NotNull()
    java.util.List<com.pulselink.domain.model.Contact> contacts, long delayBetweenMillis, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @androidx.annotation.RequiresPermission(value = "android.permission.SEND_SMS")
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object sendSms(@org.jetbrains.annotations.NotNull()
    java.lang.String phoneNumber, @org.jetbrains.annotations.NotNull()
    java.lang.String message, long timeoutMillis, boolean awaitResult, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    public final void completeSmsRequest(@org.jetbrains.annotations.NotNull()
    java.lang.String requestId, boolean success) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/pulselink/data/sms/SmsSender$Companion;", "", "()V", "ACTION_SMS_DELIVERED", "", "ACTION_SMS_SENT", "DEFAULT_TIMEOUT_MS", "", "EXTRA_REQUEST_ID", "TAG", "androidApp_freeDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}