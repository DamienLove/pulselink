package com.pulselink.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import com.pulselink.data.sms.SmsSender;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u000f2\u00020\u0001:\u0001\u000fB\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eH\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u0010"}, d2 = {"Lcom/pulselink/receiver/SmsSendReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "smsSender", "Lcom/pulselink/data/sms/SmsSender;", "getSmsSender", "()Lcom/pulselink/data/sms/SmsSender;", "setSmsSender", "(Lcom/pulselink/data/sms/SmsSender;)V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "Companion", "androidApp_freeRelease"})
public final class SmsSendReceiver extends android.content.BroadcastReceiver {
    @javax.inject.Inject()
    public com.pulselink.data.sms.SmsSender smsSender;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "SmsSendReceiver";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.receiver.SmsSendReceiver.Companion Companion = null;
    
    public SmsSendReceiver() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.sms.SmsSender getSmsSender() {
        return null;
    }
    
    public final void setSmsSender(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.sms.SmsSender p0) {
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/receiver/SmsSendReceiver$Companion;", "", "()V", "TAG", "", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}