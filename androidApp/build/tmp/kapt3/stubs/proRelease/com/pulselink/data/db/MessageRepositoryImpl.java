package com.pulselink.data.db;

import com.pulselink.domain.model.ContactMessage;
import com.pulselink.domain.repository.MessageRepository;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlinx.coroutines.flow.Flow;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0016\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0096@\u00a2\u0006\u0002\u0010\tJ\u001c\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\f0\u000b2\u0006\u0010\u0007\u001a\u00020\bH\u0016J\u0016\u0010\u000e\u001a\u00020\u00062\u0006\u0010\u000f\u001a\u00020\rH\u0096@\u00a2\u0006\u0002\u0010\u0010R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/pulselink/data/db/MessageRepositoryImpl;", "Lcom/pulselink/domain/repository/MessageRepository;", "dao", "Lcom/pulselink/data/db/ContactMessageDao;", "(Lcom/pulselink/data/db/ContactMessageDao;)V", "clear", "", "contactId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeForContact", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/pulselink/domain/model/ContactMessage;", "record", "message", "(Lcom/pulselink/domain/model/ContactMessage;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_proRelease"})
public final class MessageRepositoryImpl implements com.pulselink.domain.repository.MessageRepository {
    @org.jetbrains.annotations.NotNull()
    private final com.pulselink.data.db.ContactMessageDao dao = null;
    
    @javax.inject.Inject()
    public MessageRepositoryImpl(@org.jetbrains.annotations.NotNull()
    com.pulselink.data.db.ContactMessageDao dao) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.ContactMessage>> observeForContact(long contactId) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.ContactMessage message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object clear(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}