package com.pulselink.domain.repository;

import com.pulselink.domain.model.PulseLinkSettings;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u000e\u0010\u0007\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u000f\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u0011\u001a\u00020\u000b2\u0006\u0010\u0012\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0014\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\u000e\u0010\u0015\u001a\u00020\u000bH\u00a6@\u00a2\u0006\u0002\u0010\tJ\u0016\u0010\u0016\u001a\u00020\u000b2\u0006\u0010\u0017\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u0013J\u0016\u0010\u0018\u001a\u00020\u000b2\u0006\u0010\u0010\u001a\u00020\rH\u00a6@\u00a2\u0006\u0002\u0010\u000eJ\"\u0010\u0019\u001a\u00020\u000b2\u0012\u0010\u001a\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u001bH\u00a6@\u00a2\u0006\u0002\u0010\u001cR\u0018\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003X\u00a6\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u001d"}, d2 = {"Lcom/pulselink/domain/repository/SettingsRepository;", "", "settings", "Lkotlinx/coroutines/flow/Flow;", "Lcom/pulselink/domain/model/PulseLinkSettings;", "getSettings", "()Lkotlinx/coroutines/flow/Flow;", "ensureDeviceId", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setAssistantShortcutsDismissed", "", "dismissed", "", "(ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setAutoAllowRemoteSoundChange", "enabled", "setBetaAgreementAcceptance", "version", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "setBetaTesterStatus", "setOnboardingComplete", "setOwnerName", "name", "setProUnlocked", "update", "transform", "Lkotlin/Function1;", "(Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_proRelease"})
public abstract interface SettingsRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.pulselink.domain.model.PulseLinkSettings> getSettings();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object update(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super com.pulselink.domain.model.PulseLinkSettings, com.pulselink.domain.model.PulseLinkSettings> transform, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setProUnlocked(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setOnboardingComplete(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object ensureDeviceId(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setOwnerName(@org.jetbrains.annotations.NotNull()
    java.lang.String name, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setAutoAllowRemoteSoundChange(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setBetaTesterStatus(boolean enabled, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setAssistantShortcutsDismissed(boolean dismissed, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object setBetaAgreementAcceptance(@org.jetbrains.annotations.NotNull()
    java.lang.String version, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}