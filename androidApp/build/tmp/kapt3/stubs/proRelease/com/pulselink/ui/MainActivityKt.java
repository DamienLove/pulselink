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

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000T\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\r\n\u0000\u001a\b\u0010\u0005\u001a\u00020\u0006H\u0003\u001a\u0010\u0010\u0007\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0002\u001a\u0010\u0010\n\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0002\u001a\u0010\u0010\u000b\u001a\u00020\u00062\u0006\u0010\b\u001a\u00020\tH\u0002\u001a4\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0016\u0012\u0004\u0012\u00020\u00060\u0015H\u0002\u001a:\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00060\u00182\u0006\u0010\u000e\u001a\u00020\u00192\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00060\u00182\u0014\u0010\u001b\u001a\u0010\u0012\u0006\u0012\u0004\u0018\u00010\u001c\u0012\u0004\u0012\u00020\u00060\u0015H\u0003\"\u000e\u0010\u0000\u001a\u00020\u0001X\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010\u0002\u001a\u00020\u0003X\u0082T\u00a2\u0006\u0002\n\u0000\"\u000e\u0010\u0004\u001a\u00020\u0001X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001d"}, d2 = {"CANCEL_EMERGENCY_AUTHENTICATORS", "", "NOTIFICATION_POLICY_DETAIL_ACTION", "", "REQUEST_CALL_PERMISSIONS", "CallPreparationDialog", "", "openAppSettings", "context", "Landroid/content/Context;", "openDndSettings", "openUnusedAppRestrictionsSettings", "placeCall", "", "activity", "Lcom/pulselink/ui/MainActivity;", "contact", "Lcom/pulselink/domain/model/Contact;", "monitor", "Lcom/pulselink/util/CallStateMonitor;", "onCallEnded", "Lkotlin/Function1;", "", "rememberCancelEmergencyLauncher", "Lkotlin/Function0;", "Landroidx/appcompat/app/AppCompatActivity;", "onAuthenticated", "onError", "", "androidApp_proRelease"})
public final class MainActivityKt {
    private static final int CANCEL_EMERGENCY_AUTHENTICATORS = 32783;
    private static final int REQUEST_CALL_PERMISSIONS = 2001;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String NOTIFICATION_POLICY_DETAIL_ACTION = "android.settings.NOTIFICATION_POLICY_ACCESS_DETAIL_SETTINGS";
    
    @androidx.compose.runtime.Composable()
    private static final kotlin.jvm.functions.Function0<kotlin.Unit> rememberCancelEmergencyLauncher(androidx.appcompat.app.AppCompatActivity activity, kotlin.jvm.functions.Function0<kotlin.Unit> onAuthenticated, kotlin.jvm.functions.Function1<? super java.lang.CharSequence, kotlin.Unit> onError) {
        return null;
    }
    
    private static final boolean placeCall(com.pulselink.ui.MainActivity activity, com.pulselink.domain.model.Contact contact, com.pulselink.util.CallStateMonitor monitor, kotlin.jvm.functions.Function1<? super java.lang.Long, kotlin.Unit> onCallEnded) {
        return false;
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CallPreparationDialog() {
    }
    
    private static final void openAppSettings(android.content.Context context) {
    }
    
    private static final void openDndSettings(android.content.Context context) {
    }
    
    private static final void openUnusedAppRestrictionsSettings(android.content.Context context) {
    }
}