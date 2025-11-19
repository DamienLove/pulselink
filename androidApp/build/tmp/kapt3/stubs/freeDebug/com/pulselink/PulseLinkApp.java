package com.pulselink;

import android.app.Application;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.ads.AdConfig;
import com.pulselink.data.ads.AppOpenAdController;
import com.pulselink.assistant.AssistantShortcuts;
import com.pulselink.data.remoteconfig.RemoteConfigService;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;
import kotlinx.coroutines.Dispatchers;

@dagger.hilt.android.HiltAndroidApp()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u00012\u00020\u0002B\u0005\u00a2\u0006\u0002\u0010\u0003J\b\u0010\"\u001a\u00020#H\u0016R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001b\u0010\u0016\u001a\u00020\u00178VX\u0096\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001a\u0010\u001b\u001a\u0004\b\u0018\u0010\u0019R\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!\u00a8\u0006$"}, d2 = {"Lcom/pulselink/PulseLinkApp;", "Landroid/app/Application;", "Landroidx/work/Configuration$Provider;", "()V", "appOpenAdController", "Lcom/pulselink/data/ads/AppOpenAdController;", "getAppOpenAdController", "()Lcom/pulselink/data/ads/AppOpenAdController;", "setAppOpenAdController", "(Lcom/pulselink/data/ads/AppOpenAdController;)V", "firebaseAuthManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "getFirebaseAuthManager", "()Lcom/pulselink/auth/FirebaseAuthManager;", "setFirebaseAuthManager", "(Lcom/pulselink/auth/FirebaseAuthManager;)V", "remoteConfigService", "Lcom/pulselink/data/remoteconfig/RemoteConfigService;", "getRemoteConfigService", "()Lcom/pulselink/data/remoteconfig/RemoteConfigService;", "setRemoteConfigService", "(Lcom/pulselink/data/remoteconfig/RemoteConfigService;)V", "workManagerConfiguration", "Landroidx/work/Configuration;", "getWorkManagerConfiguration", "()Landroidx/work/Configuration;", "workManagerConfiguration$delegate", "Lkotlin/Lazy;", "workerFactory", "Landroidx/hilt/work/HiltWorkerFactory;", "getWorkerFactory", "()Landroidx/hilt/work/HiltWorkerFactory;", "setWorkerFactory", "(Landroidx/hilt/work/HiltWorkerFactory;)V", "onCreate", "", "androidApp_freeDebug"})
public final class PulseLinkApp extends android.app.Application implements androidx.work.Configuration.Provider {
    @javax.inject.Inject()
    public androidx.hilt.work.HiltWorkerFactory workerFactory;
    @javax.inject.Inject()
    public com.pulselink.data.ads.AppOpenAdController appOpenAdController;
    @javax.inject.Inject()
    public com.pulselink.auth.FirebaseAuthManager firebaseAuthManager;
    @javax.inject.Inject()
    public com.pulselink.data.remoteconfig.RemoteConfigService remoteConfigService;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy workManagerConfiguration$delegate = null;
    
    public PulseLinkApp() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final androidx.hilt.work.HiltWorkerFactory getWorkerFactory() {
        return null;
    }
    
    public final void setWorkerFactory(@org.jetbrains.annotations.NotNull()
    androidx.hilt.work.HiltWorkerFactory p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.ads.AppOpenAdController getAppOpenAdController() {
        return null;
    }
    
    public final void setAppOpenAdController(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.ads.AppOpenAdController p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.auth.FirebaseAuthManager getFirebaseAuthManager() {
        return null;
    }
    
    public final void setFirebaseAuthManager(@org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.remoteconfig.RemoteConfigService getRemoteConfigService() {
        return null;
    }
    
    public final void setRemoteConfigService(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.remoteconfig.RemoteConfigService p0) {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public androidx.work.Configuration getWorkManagerConfiguration() {
        return null;
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
}