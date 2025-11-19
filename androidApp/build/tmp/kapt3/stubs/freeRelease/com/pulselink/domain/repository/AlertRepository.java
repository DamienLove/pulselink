package com.pulselink.domain.repository;

import com.pulselink.domain.model.AlertEvent;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J\u001e\u0010\u0002\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\u00040\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0007H&J\u0016\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u000b\u00a8\u0006\f"}, d2 = {"Lcom/pulselink/domain/repository/AlertRepository;", "", "observeRecent", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/pulselink/domain/model/AlertEvent;", "limit", "", "record", "", "event", "(Lcom/pulselink/domain/model/AlertEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_freeRelease"})
public abstract interface AlertRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.AlertEvent>> observeRecent(int limit);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}