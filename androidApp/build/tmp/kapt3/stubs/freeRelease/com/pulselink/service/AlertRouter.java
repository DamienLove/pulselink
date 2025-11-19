package com.pulselink.service;

import android.util.Log;
import com.pulselink.data.alert.AlertDispatcher;
import com.pulselink.data.alert.AlertDispatcher.AlertResult;
import com.pulselink.data.link.ContactLinkManager;
import com.pulselink.data.link.RemoteAlertResult;
import com.pulselink.data.link.RemoteAlertStatus;
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.repository.AlertRepository;
import com.pulselink.domain.repository.ContactRepository;
import com.pulselink.domain.repository.SettingsRepository;
import dagger.Lazy;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\"\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 $2\u00020\u0001:\u0001$B5\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000b\u00a2\u0006\u0002\u0010\rJ0\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u000e\b\u0002\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00180\u0017H\u0086@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u001dJ\u0016\u0010\u001e\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020\u0015H\u0086@\u00a2\u0006\u0002\u0010\u001dJ6\u0010 \u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010!\u001a\u00020\"2\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00180\u0017H\u0082@\u00a2\u0006\u0002\u0010#R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006%"}, d2 = {"Lcom/pulselink/service/AlertRouter;", "", "settingsRepository", "Lcom/pulselink/domain/repository/SettingsRepository;", "contactRepository", "Lcom/pulselink/domain/repository/ContactRepository;", "alertRepository", "Lcom/pulselink/domain/repository/AlertRepository;", "dispatcher", "Lcom/pulselink/data/alert/AlertDispatcher;", "contactLinkManager", "Ldagger/Lazy;", "Lcom/pulselink/data/link/ContactLinkManager;", "(Lcom/pulselink/domain/repository/SettingsRepository;Lcom/pulselink/domain/repository/ContactRepository;Lcom/pulselink/domain/repository/AlertRepository;Lcom/pulselink/data/alert/AlertDispatcher;Ldagger/Lazy;)V", "mutex", "Lkotlinx/coroutines/sync/Mutex;", "dispatchManual", "Lcom/pulselink/data/alert/AlertDispatcher$AlertResult;", "tier", "Lcom/pulselink/domain/model/EscalationTier;", "trigger", "", "excludeContactIds", "", "", "(Lcom/pulselink/domain/model/EscalationTier;Ljava/lang/String;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "onInboundMessage", "", "body", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "onPhraseDetected", "phrase", "route", "settings", "Lcom/pulselink/domain/model/PulseLinkSettings;", "(Lcom/pulselink/domain/model/EscalationTier;Ljava/lang/String;Lcom/pulselink/domain/model/PulseLinkSettings;Ljava/util/Set;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "androidApp_freeRelease"})
public final class AlertRouter {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.SettingsRepository settingsRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.ContactRepository contactRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.domain.repository.AlertRepository alertRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.alert.AlertDispatcher dispatcher = null;
    @org.jetbrains.annotations.NotNull()
    private final dagger.Lazy<com.pulselink.data.link.ContactLinkManager> contactLinkManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.sync.Mutex mutex = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "AlertRouter";
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.service.AlertRouter.Companion Companion = null;
    
    @javax.inject.Inject()
    public AlertRouter(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.SettingsRepository settingsRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.ContactRepository contactRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.domain.repository.AlertRepository alertRepository, @org.jetbrains.annotations.NotNull()
    com.pulselink.data.alert.AlertDispatcher dispatcher, @org.jetbrains.annotations.NotNull()
    dagger.Lazy<com.pulselink.data.link.ContactLinkManager> contactLinkManager) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object onPhraseDetected(@org.jetbrains.annotations.NotNull()
    java.lang.String phrase, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object dispatchManual(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.EscalationTier tier, @org.jetbrains.annotations.NotNull()
    java.lang.String trigger, @org.jetbrains.annotations.NotNull()
    java.util.Set<java.lang.Long> excludeContactIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.data.alert.AlertDispatcher.AlertResult> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object onInboundMessage(@org.jetbrains.annotations.NotNull()
    java.lang.String body, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final java.lang.Object route(com.pulselink.domain.model.EscalationTier tier, java.lang.String trigger, com.pulselink.domain.model.PulseLinkSettings settings, java.util.Set<java.lang.Long> excludeContactIds, kotlin.coroutines.Continuation<? super com.pulselink.data.alert.AlertDispatcher.AlertResult> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/pulselink/service/AlertRouter$Companion;", "", "()V", "TAG", "", "androidApp_freeRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}