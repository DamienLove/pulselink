package com.pulselink.data.db;

import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.BlockedContact;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.ContactMessage;
import kotlinx.coroutines.flow.Flow;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\n\u001a\u0004\u0018\u00010\b2\u0006\u0010\u000b\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0018\u0010\u000e\u001a\u0004\u0018\u00010\b2\u0006\u0010\u000f\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0018\u0010\u0010\u001a\u0004\u0018\u00010\b2\u0006\u0010\u0011\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u001c\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u00132\u0006\u0010\u0014\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\rJ\u0014\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00130\u0016H\'J\u001e\u0010\u0017\u001a\u00020\u00032\u0006\u0010\t\u001a\u00020\u00052\u0006\u0010\u0018\u001a\u00020\u0019H\u00a7@\u00a2\u0006\u0002\u0010\u001aJ\u0016\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u001c\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u001d\u00a8\u0006\u001e"}, d2 = {"Lcom/pulselink/data/db/ContactDao;", "", "deleteById", "", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getById", "Lcom/pulselink/domain/model/Contact;", "contactId", "getByLinkCode", "code", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByPhone", "phone", "getByRemoteDeviceId", "deviceId", "getByTier", "", "tier", "observeContacts", "Lkotlinx/coroutines/flow/Flow;", "updateOrder", "order", "", "(JILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "upsert", "contact", "(Lcom/pulselink/domain/model/Contact;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "androidApp_proRelease"})
@androidx.room.Dao()
public abstract interface ContactDao {
    
    @androidx.room.Query(value = "SELECT * FROM contacts ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.Contact>> observeContacts();
    
    @androidx.room.Query(value = "SELECT * FROM contacts WHERE escalationTier = :tier ORDER BY contactOrder ASC, displayName COLLATE NOCASE")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByTier(@org.jetbrains.annotations.NotNull()
    java.lang.String tier, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.pulselink.domain.model.Contact>> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM contacts WHERE id = :contactId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getById(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM contacts WHERE linkCode = :code LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByLinkCode(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM contacts WHERE phoneNumber = :phone LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByPhone(@org.jetbrains.annotations.NotNull()
    java.lang.String phone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM contacts WHERE remoteDeviceId = :deviceId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByRemoteDeviceId(@org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.Contact> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.Contact contact, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM contacts WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteById(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE contacts SET contactOrder = :order WHERE id = :contactId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateOrder(long contactId, int order, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}