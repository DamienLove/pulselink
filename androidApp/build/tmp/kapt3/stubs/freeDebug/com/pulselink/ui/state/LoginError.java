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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0002\b\n\b\u0082\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0011\b\u0002\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\f\u00a8\u0006\r"}, d2 = {"Lcom/pulselink/ui/state/LoginError;", "", "messageRes", "", "(Ljava/lang/String;II)V", "getMessageRes", "()I", "MISSING_EMAIL", "SHORT_PASSWORD", "PASSWORD_MISMATCH", "INVALID_CREDENTIALS", "EMAIL_IN_USE", "GENERIC", "androidApp_freeDebug"})
enum LoginError {
    /*public static final*/ MISSING_EMAIL /* = new MISSING_EMAIL(0) */,
    /*public static final*/ SHORT_PASSWORD /* = new SHORT_PASSWORD(0) */,
    /*public static final*/ PASSWORD_MISMATCH /* = new PASSWORD_MISMATCH(0) */,
    /*public static final*/ INVALID_CREDENTIALS /* = new INVALID_CREDENTIALS(0) */,
    /*public static final*/ EMAIL_IN_USE /* = new EMAIL_IN_USE(0) */,
    /*public static final*/ GENERIC /* = new GENERIC(0) */;
    private final int messageRes = 0;
    
    LoginError(@androidx.annotation.StringRes()
    int messageRes) {
    }
    
    public final int getMessageRes() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.pulselink.ui.state.LoginError> getEntries() {
        return null;
    }
}