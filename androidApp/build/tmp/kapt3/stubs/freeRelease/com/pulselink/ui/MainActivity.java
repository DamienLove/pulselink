package com.pulselink.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.compose.runtime.Composable;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.navigation.NavType;
import com.google.firebase.ktx.Firebase;
import com.pulselink.data.ads.AppOpenAdController;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.ManualMessageResult;
import com.pulselink.R;
import com.pulselink.ui.screens.BugReportData;
import com.pulselink.ui.screens.OnboardingPermissionState;
import com.pulselink.ui.state.MainViewModel;
import com.pulselink.ui.state.MainViewModel.CallInitiationResult;
import kotlinx.coroutines.Dispatchers;
import androidx.compose.material.icons.Icons;
import dagger.hilt.android.AndroidEntryPoint;
import com.pulselink.util.CallStateMonitor;
import javax.inject.Inject;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0018H\u0014J\b\u0010\u0019\u001a\u00020\u0016H\u0014J\b\u0010\u001a\u001a\u00020\u0016H\u0014R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001e\u0010\t\u001a\u00020\n8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u001b\u0010\u000f\u001a\u00020\u00108BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\u0014\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001b"}, d2 = {"Lcom/pulselink/ui/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "appOpenAdController", "Lcom/pulselink/data/ads/AppOpenAdController;", "getAppOpenAdController", "()Lcom/pulselink/data/ads/AppOpenAdController;", "setAppOpenAdController", "(Lcom/pulselink/data/ads/AppOpenAdController;)V", "callStateMonitor", "Lcom/pulselink/util/CallStateMonitor;", "getCallStateMonitor", "()Lcom/pulselink/util/CallStateMonitor;", "setCallStateMonitor", "(Lcom/pulselink/util/CallStateMonitor;)V", "viewModel", "Lcom/pulselink/ui/state/MainViewModel;", "getViewModel", "()Lcom/pulselink/ui/state/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "onResume", "androidApp_freeRelease"})
public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy viewModel$delegate = null;
    @javax.inject.Inject()
    public com.pulselink.data.ads.AppOpenAdController appOpenAdController;
    @javax.inject.Inject()
    public com.pulselink.util.CallStateMonitor callStateMonitor;
    
    public MainActivity() {
        super();
    }
    
    private final com.pulselink.ui.state.MainViewModel getViewModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.data.ads.AppOpenAdController getAppOpenAdController() {
        return null;
    }
    
    public final void setAppOpenAdController(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.ads.AppOpenAdController p0) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.pulselink.util.CallStateMonitor getCallStateMonitor() {
        return null;
    }
    
    public final void setCallStateMonitor(@org.jetbrains.annotations.NotNull()
    com.pulselink.util.CallStateMonitor p0) {
    }
    
    @java.lang.Override()
    protected void onCreate(@org.jetbrains.annotations.Nullable()
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override()
    protected void onResume() {
    }
    
    @java.lang.Override()
    protected void onDestroy() {
    }
}