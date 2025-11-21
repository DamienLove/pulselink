package com.pulselink.auth;

import android.util.Log;
import androidx.activity.ComponentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.OAuthProvider;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.coroutines.flow.StateFlow;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0007\u0018\u0000 .2\u00020\u0001:\u0001.B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u000e\u001a\u0004\u0018\u00010\u000fJ\u000e\u0010\u0010\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J\u000e\u0010\u0013\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u0012J,\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00152\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0019\u0010\u001aJ$\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00110\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001c\u0010\u001dJ,\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00152\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u001f\u0010\u001aJ\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020\u000f0\u0015H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b!\u0010\u0012J$\u0010\"\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00152\u0006\u0010#\u001a\u00020$H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b%\u0010&J$\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u000f0\u00152\u0006\u0010(\u001a\u00020\u0017H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b)\u0010\u001dJ\u001c\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00110\u0015H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b+\u0010\u0012J\u0012\u0010,\u001a\u00020\u00112\b\u0010-\u001a\u0004\u0018\u00010\u000fH\u0002R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00070\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\r\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006/"}, d2 = {"Lcom/pulselink/auth/FirebaseAuthManager;", "", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "(Lcom/google/firebase/auth/FirebaseAuth;)V", "_authState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/pulselink/auth/AuthState;", "authListener", "Lcom/google/firebase/auth/FirebaseAuth$AuthStateListener;", "authState", "Lkotlinx/coroutines/flow/StateFlow;", "getAuthState", "()Lkotlinx/coroutines/flow/StateFlow;", "currentUser", "Lcom/google/firebase/auth/FirebaseUser;", "ensureSignedIn", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "refreshClaims", "register", "Lkotlin/Result;", "email", "", "password", "register-0E7RQCE", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendPasswordReset", "sendPasswordReset-gIAlu-s", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signIn", "signIn-0E7RQCE", "signInSmsOnly", "signInSmsOnly-IoAF18A", "signInWithApple", "activity", "Landroidx/activity/ComponentActivity;", "signInWithApple-gIAlu-s", "(Landroidx/activity/ComponentActivity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "signInWithGoogle", "idToken", "signInWithGoogle-gIAlu-s", "signOut", "signOut-IoAF18A", "updateAuthState", "user", "Companion", "androidApp_freeDebug"})
public final class FirebaseAuthManager {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth auth = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.pulselink.auth.AuthState> _authState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> authState = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth.AuthStateListener authListener = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "FirebaseAuthManager";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.auth.FirebaseAuthManager.Companion Companion = null;
    
    @javax.inject.Inject()
    public FirebaseAuthManager(@org.jetbrains.annotations.NotNull()
    com.google.firebase.auth.FirebaseAuth auth) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.pulselink.auth.AuthState> getAuthState() {
        return null;
    }
    
    private final void updateAuthState(com.google.firebase.auth.FirebaseUser user) {
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object ensureSignedIn(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final com.google.firebase.auth.FirebaseUser currentUser() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object refreshClaims(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/auth/FirebaseAuthManager$Companion;", "", "()V", "TAG", "", "androidApp_freeDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}