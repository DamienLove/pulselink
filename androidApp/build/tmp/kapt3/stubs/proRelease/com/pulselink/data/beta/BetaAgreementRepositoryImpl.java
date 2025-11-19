package com.pulselink.data.beta;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.pulselink.BuildConfig;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.domain.repository.BetaAgreementRepository;
import com.pulselink.domain.repository.SettingsRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0007\u0018\u0000 \u000f2\u00020\u0001:\u0001\u000fB\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u001e\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\fH\u0096@\u00a2\u0006\u0002\u0010\u000eR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0010"}, d2 = {"Lcom/pulselink/data/beta/BetaAgreementRepositoryImpl;", "Lcom/pulselink/domain/repository/BetaAgreementRepository;", "firestore", "Lcom/google/firebase/firestore/FirebaseFirestore;", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "authManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "(Lcom/google/firebase/firestore/FirebaseFirestore;Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/auth/FirebaseAuthManager;)V", "recordAgreement", "", "name", "", "agreementVersion", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "androidApp_proRelease"})
public final class BetaAgreementRepositoryImpl implements com.pulselink.domain.repository.BetaAgreementRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.firestore.FirebaseFirestore firestore = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.auth.FirebaseAuthManager authManager = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String COLLECTION = "betaAgreements";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.beta.BetaAgreementRepositoryImpl.Companion Companion = null;
    
    @javax.inject.Inject()
    public BetaAgreementRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.google.firebase.firestore.FirebaseFirestore firestore, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager authManager) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object recordAgreement(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    java.lang.String agreementVersion, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/data/beta/BetaAgreementRepositoryImpl$Companion;", "", "()V", "COLLECTION", "", "androidApp_proRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}