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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\b\n\u0002\b\u0006\b\u0082\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0011\b\u0002\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006j\u0002\b\u0007j\u0002\b\b\u00a8\u0006\t"}, d2 = {"Lcom/pulselink/ui/state/LoginMessage;", "", "messageRes", "", "(Ljava/lang/String;II)V", "getMessageRes", "()I", "RESET_SENT", "ACCOUNT_CREATED", "androidApp_freeDebug"})
enum LoginMessage {
    /*public static final*/ RESET_SENT /* = new RESET_SENT(0) */,
    /*public static final*/ ACCOUNT_CREATED /* = new ACCOUNT_CREATED(0) */;
    private final int messageRes = 0;
    
    LoginMessage(@androidx.annotation.StringRes()
    int messageRes) {
    }
    
    public final int getMessageRes() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public static kotlin.enums.EnumEntries<com.pulselink.ui.state.LoginMessage> getEntries() {
        return null;
    }
}