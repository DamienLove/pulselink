package com.pulselink.data.db;

import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.repository.AlertRepository;
import kotlinx.coroutines.flow.Flow;
import javax.inject.Inject;
import javax.inject.Singleton;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u001c\u0010\u0005\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00070\u00062\u0006\u0010\t\u001a\u00020\nH\u0016J\u0016\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\u000eR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/pulselink/data/db/AlertRepositoryImpl;", "Lcom/pulselink/domain/repository/AlertRepository;", "alertEventDao", "Lcom/pulselink/data/db/AlertEventDao;", "(Lcom/pulselink/data/db/AlertEventDao;)V", "observeRecent", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/pulselink/domain/model/AlertEvent;", "limit", "", "record", "", "event", "(Lcom/pulselink/domain/model/AlertEvent;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_proDebug"})
public final class AlertRepositoryImpl implements com.pulselink.domain.repository.AlertRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.db.AlertEventDao alertEventDao = null;
    
    @javax.inject.Inject()
    public AlertRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.AlertEventDao alertEventDao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.AlertEvent>> observeRecent(int limit) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.AlertEvent event, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}