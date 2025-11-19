package com.pulselink.data.remoteconfig;

import android.util.Log;
import com.google.firebase.ktx.Firebase;
import com.pulselink.R;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u0007\u0018\u0000 \n2\u00020\u0001:\u0001\nB\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0005\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\u0007J\u0006\u0010\b\u001a\u00020\tR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/pulselink/data/remoteconfig/RemoteConfigService;", "", "()V", "remoteConfig", "Lcom/google/firebase/remoteconfig/FirebaseRemoteConfig;", "fetchAndActivate", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isExampleFeatureEnabled", "", "Companion", "androidApp_freeDebug"})
public final class RemoteConfigService {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.remoteconfig.FirebaseRemoteConfig remoteConfig = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "RemoteConfigService";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.remoteconfig.RemoteConfigService.Companion Companion = null;
    
    @javax.inject.Inject()
    public RemoteConfigService() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object fetchAndActivate(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final boolean isExampleFeatureEnabled() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/data/remoteconfig/RemoteConfigService$Companion;", "", "()V", "TAG", "", "androidApp_freeDebug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}