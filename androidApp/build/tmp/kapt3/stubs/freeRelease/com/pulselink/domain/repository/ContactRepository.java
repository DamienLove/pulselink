package com.pulselink.domain.repository;

import com.pulselink.domain.model.Contact;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0007\bf\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0018\u0010\f\u001a\u0004\u0018\u00010\b2\u0006\u0010\r\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0018\u0010\u000e\u001a\u0004\u0018\u00010\b2\u0006\u0010\u000f\u001a\u00020\nH\u00a6@\u00a2\u0006\u0002\u0010\u000bJ\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\b0\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u0012J\u0018\u0010\u0013\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0004\u001a\u00020\u0005H\u00a6@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\b0\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u0012J\u0014\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00110\u0016H&J\u001c\u0010\u0017\u001a\u00020\u00032\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00050\u0011H\u00a6@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\u00032\u0006\u0010\u001b\u001a\u00020\bH\u00a6@\u00a2\u0006\u0002\u0010\u001c\u00a8\u0006\u001d"}, d2 = {"Lcom/pulselink/domain/repository/ContactRepository;", "", "delete", "", "contactId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByLinkCode", "Lcom/pulselink/domain/model/Contact;", "code", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByPhone", "phone", "getByRemoteDeviceId", "deviceId", "getCheckInContacts", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getContact", "getEmergencyContacts", "observeContacts", "Lkotlinx/coroutines/flow/Flow;", "updateOrder", "contactIds", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "upsert", "contact", "(Lcom/pulselink/domain/model/Contact;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_freeRelease"})
public abstract interface ContactRepository {
    
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.Contact>> observeContacts();
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getContact(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getEmergencyContacts(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pulselink.domain.model.Contact>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCheckInContacts(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pulselink.domain.model.Contact>> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByLinkCode(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByPhone(@org.jetbrains.annotations.NotNull()
    java.lang.String phone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByRemoteDeviceId(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateOrder(@org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.Long> contactIds, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}