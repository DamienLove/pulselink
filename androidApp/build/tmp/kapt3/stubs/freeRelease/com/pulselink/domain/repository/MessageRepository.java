package com.pulselink.domain.repository;

import com.pulselink.domain.model.ContactMessage;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b2\u0006\u0010\u0004\u001a\u00020\u0005H&J\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\r\u00a8\u0006\u000e"}, d2 = {"Lcom/pulselink/domain/repository/MessageRepository;", "", "clear", "", "contactId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeForContact", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/pulselink/domain/model/ContactMessage;", "record", "message", "(Lcom/pulselink/domain/model/ContactMessage;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_freeRelease"})
public abstract interface MessageRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.ContactMessage>> observeForContact(long contactId);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object record(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.ContactMessage message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clear(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}