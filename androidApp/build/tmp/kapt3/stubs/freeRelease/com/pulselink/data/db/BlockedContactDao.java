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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\u0004\u0018\u00010\b2\u0006\u0010\t\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u0018\u0010\f\u001a\u0004\u0018\u00010\b2\u0006\u0010\r\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJ\u0016\u0010\u000e\u001a\u00020\u00032\u0006\u0010\u000f\u001a\u00020\bH\u00a7@\u00a2\u0006\u0002\u0010\u0010J,\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\n2\b\u0010\u0014\u001a\u0004\u0018\u00010\n2\b\u0010\u0015\u001a\u0004\u0018\u00010\nH\u00a7@\u00a2\u0006\u0002\u0010\u0016J\u0014\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\b0\u00190\u0018H\'\u00a8\u0006\u001a"}, d2 = {"Lcom/pulselink/data/db/BlockedContactDao;", "", "delete", "", "id", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByLinkCode", "Lcom/pulselink/domain/model/BlockedContact;", "code", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getByPhone", "phone", "insert", "blockedContact", "(Lcom/pulselink/domain/model/BlockedContact;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "isBlocked", "", "phoneNumber", "linkCode", "remoteDeviceId", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeAll", "Lkotlinx/coroutines/flow/Flow;", "", "androidApp_freeRelease"})
@androidx.room.Dao()
public abstract interface BlockedContactDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.BlockedContact blockedContact, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        SELECT EXISTS(\n            SELECT 1 FROM blocked_contacts\n            WHERE (:phoneNumber IS NOT NULL AND phoneNumber = :phoneNumber)\n               OR (:linkCode IS NOT NULL AND linkCode = :linkCode)\n               OR (:remoteDeviceId IS NOT NULL AND remoteDeviceId = :remoteDeviceId)\n        )\n        ")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object isBlocked(@org.jetbrains.annotations.Nullable()
    java.lang.String phoneNumber, @org.jetbrains.annotations.Nullable()
    java.lang.String linkCode, @org.jetbrains.annotations.Nullable()
    java.lang.String remoteDeviceId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM blocked_contacts WHERE phoneNumber = :phone LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByPhone(@org.jetbrains.annotations.NotNull()
    java.lang.String phone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.BlockedContact> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM blocked_contacts WHERE linkCode = :code LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getByLinkCode(@org.jetbrains.annotations.NotNull()
    java.lang.String code, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.pulselink.domain.model.BlockedContact> $completion);
    
    @androidx.room.Query(value = "DELETE FROM blocked_contacts WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object delete(long id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM blocked_contacts ORDER BY blockedAt DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.BlockedContact>> observeAll();
}