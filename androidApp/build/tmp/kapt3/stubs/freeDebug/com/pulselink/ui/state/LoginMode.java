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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0004\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/ui/state/LoginMode;", "", "(Ljava/lang/String;I)V", "SIGN_IN", "CREATE_ACCOUNT", "androidApp_freeDebug"})
public enum LoginMode {
    /*public static final*/ SIGN_IN /* = new SIGN_IN() */,
    /*public static final*/ CREATE_ACCOUNT /* = new CREATE_ACCOUNT() */;
    
    LoginMode() {
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.pulselink.ui.state.LoginMode> getEntries() {
        return null;
    }
}