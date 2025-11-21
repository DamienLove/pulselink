package com.pulselink.data.assistant;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import com.pulselink.BuildConfig;
import com.pulselink.R;
import com.pulselink.auth.FirebaseAuthManager;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.model.ManualMessageResult;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.service.AlertRouter;
import javax.inject.Inject;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000T\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001BA\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u00a2\u0006\u0002\u0010\u0010J\u000e\u0010\u0011\u001a\u00020\u0012H\u0082@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0017H\u0086@\u00a2\u0006\u0002\u0010\u0018J\u001a\u0010\u0019\u001a\u0004\u0018\u00010\u001a2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0017H\u0082@\u00a2\u0006\u0002\u0010\u0018R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001c"}, d2 = {"Lcom/pulselink/data/assistant/NaturalLanguageCommandProcessor;", "", "context", "Landroid/content/Context;", "functions", "Lcom/google/firebase/functions/FirebaseFunctions;", "auth", "Lcom/google/firebase/auth/FirebaseAuth;", "alertRouter", "Lcom/pulselink/service/AlertRouter;", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "linkManager", "Lcom/pulselink/data/link/ContactLinkManager;", "firebaseAuthManager", "Lcom/pulselink/auth/FirebaseAuthManager;", "(Landroid/content/Context;Lcom/google/firebase/functions/FirebaseFunctions;Lcom/google/firebase/auth/FirebaseAuth;Lcom/pulselink/service/AlertRouter;Lcom/pulselink/domain/repository/ContactRepository;Lcom/pulselink/data/link/ContactLinkManager;Lcom/pulselink/auth/FirebaseAuthManager;)V", "ensureProAccess", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleCommand", "Lcom/pulselink/data/assistant/VoiceCommandResult;", "query", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "resolveContact", "Lcom/pulselink/domain/model/Contact;", "name", "androidApp_proDebug"})
public final class NaturalLanguageCommandProcessor {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.functions.FirebaseFunctions functions = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.firebase.auth.FirebaseAuth auth = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.service.AlertRouter alertRouter = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.ContactRepository contactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.link.ContactLinkManager linkManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.auth.FirebaseAuthManager firebaseAuthManager = null;
    
    @javax.inject.Inject()
    public NaturalLanguageCommandProcessor(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.google.firebase.functions.FirebaseFunctions functions, @org.jetbrains.annotations.NotNull()
    com.google.firebase.auth.FirebaseAuth auth, @org.jetbrains.annotations.NotNull()
    com.pulselink.service.AlertRouter alertRouter, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.ContactRepository contactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.link.ContactLinkManager linkManager, @org.jetbrains.annotations.NotNull()
    com.pulselink.auth.FirebaseAuthManager firebaseAuthManager) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object handleCommand(@org.jetbrains.annotations.NotNull()
    java.lang.String query, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.assistant.VoiceCommandResult> $completion) {
        return null;
    }
    
    private final java.lang.Object ensureProAccess(kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    private final java.lang.Object resolveContact(java.lang.String name, kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion) {
        return null;
    }
}