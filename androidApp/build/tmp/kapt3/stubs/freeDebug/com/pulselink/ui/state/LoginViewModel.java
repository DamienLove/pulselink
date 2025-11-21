package com.pulselink.ui.state;

import androidx.activity.ComponentActivity;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModel;
import com.pulselink.R;
import com.pulselink.auth.AuthState;
import com.pulselink.auth.FirebaseAuthManager;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;
import kotlinx.coroutines.flow.StateFlow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\u0010\u0003\n\u0002\b\u0002\b\u0007\u0018\u0000 #2\u00020\u0001:\u0001#B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\u00102\u0006\u0010\u0012\u001a\u00020\u0013J\u0006\u0010\u0014\u001a\u00020\u0010J\u0006\u0010\u0015\u001a\u00020\u0010J\u0006\u0010\u0016\u001a\u00020\u0010J\u000e\u0010\u0017\u001a\u00020\u00102\u0006\u0010\u0018\u001a\u00020\u0019J\u0006\u0010\u001a\u001a\u00020\u0010J\u0006\u0010\u001b\u001a\u00020\u0010J\u000e\u0010\u001c\u001a\u00020\u00102\u0006\u0010\u001d\u001a\u00020\u0013J\u000e\u0010\u001e\u001a\u00020\u00102\u0006\u0010\u001d\u001a\u00020\u0013J\u000e\u0010\u001f\u001a\u00020\u00102\u0006\u0010\u001d\u001a\u00020\u0013J\f\u0010 \u001a\u00020!*\u00020\"H\u0002R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\f\u00a8\u0006$"}, d2 = {"Lcom/pulselink/ui/state/LoginViewModel;", "Landroidx/lifecycle/ViewModel;", "authManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "(Lcom/pulselink/auth/FirebaseAuthManager;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pulselink/ui/state/LoginUiState;", "authState", "Lkotlinx/coroutines/flow/StateFlow;", "Lcom/pulselink/auth/AuthState;", "getAuthState", "()Lkotlinx/coroutines/flow/StateFlow;", "uiState", "getUiState", "clearTransientMessages", "", "handleGoogleIdToken", "idToken", "", "reportExternalError", "sendPasswordReset", "signInSmsOnly", "signInWithApple", "activity", "Landroidx/activity/ComponentActivity;", "submit", "toggleMode", "updateConfirmPassword", "value", "updateEmail", "updatePassword", "toLoginError", "Lcom/pulselink/ui/state/LoginError;", "", "Companion", "androidApp_freeDebug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class LoginViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.auth.FirebaseAuthManager authManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pulselink.ui.state.LoginUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pulselink.ui.state.LoginUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> authState = null;
    private static final int MIN_PASSWORD_LENGTH = 8;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.ui.state.LoginViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public LoginViewModel(@org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager authManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pulselink.ui.state.LoginUiState> getUiState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> getAuthState() {
        return null;
    }
    
    public final void updateEmail(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updatePassword(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void updateConfirmPassword(@org.jetbrains.annotations.NotNull()
    java.lang.String value) {
    }
    
    public final void toggleMode() {
    }
    
    public final void submit() {
    }
    
    public final void sendPasswordReset() {
    }
    
    public final void clearTransientMessages() {
    }
    
    public final void handleGoogleIdToken(@org.jetbrains.annotations.NotNull()
    java.lang.String idToken) {
    }
    
    public final void signInWithApple(@org.jetbrains.annotations.NotNull()
    androidx.activity.ComponentActivity activity) {
    }
    
    public final void reportExternalError() {
    }
    
    public final void signInSmsOnly() {
    }
    
    private final com.pulselink.ui.state.LoginError toLoginError(java.lang.Throwable $this$toLoginError) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/ui/state/LoginViewModel$Companion;", "", "()V", "MIN_PASSWORD_LENGTH", "", "androidApp_freeDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}