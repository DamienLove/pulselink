package com.pulselink.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Log;
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.data.sms.SmsCodec;
import com.pulselink.service.AlertRouter;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u00152\u00020\u0001:\u0001\u0015B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000e\u00a8\u0006\u0016"}, d2 = {"Lcom/pulselink/receiver/PulseLinkSmsReceiver;", "Landroid/content/BroadcastReceiver;", "()V", "alertRouter", "Lcom/pulselink/service/AlertRouter;", "getAlertRouter", "()Lcom/pulselink/service/AlertRouter;", "setAlertRouter", "(Lcom/pulselink/service/AlertRouter;)V", "contactLinkManager", "Lcom/pulselink/data/link/ContactLinkManager;", "getContactLinkManager", "()Lcom/pulselink/data/link/ContactLinkManager;", "setContactLinkManager", "(Lcom/pulselink/data/link/ContactLinkManager;)V", "onReceive", "", "context", "Landroid/content/Context;", "intent", "Landroid/content/Intent;", "Companion", "androidApp_freeRelease"})
public final class PulseLinkSmsReceiver extends android.content.BroadcastReceiver {
    @javax.inject.Inject()
    public com.pulselink.service.AlertRouter alertRouter;
    @javax.inject.Inject()
    public com.pulselink.data.link.ContactLinkManager contactLinkManager;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "PulseLinkSmsReceiver";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.receiver.PulseLinkSmsReceiver.Companion Companion = null;
    
    public PulseLinkSmsReceiver() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.service.AlertRouter getAlertRouter() {
        return null;
    }
    
    public final void setAlertRouter(@org.jetbrains.annotations.NotNull()
    com.pulselink.service.AlertRouter p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.link.ContactLinkManager getContactLinkManager() {
        return null;
    }
    
    public final void setContactLinkManager(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.ContactLinkManager p0) {
    }
    
    @java.lang.Override()
    public void onReceive(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/receiver/PulseLinkSmsReceiver$Companion;", "", "()V", "TAG", "", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}