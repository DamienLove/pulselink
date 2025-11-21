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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0000\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\t0\r0\f2\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006\u000e"}, d2 = {"Lcom/pulselink/data/db/ContactMessageDao;", "", "clear", "", "contactId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "insert", "message", "Lcom/pulselink/domain/model/ContactMessage;", "(Lcom/pulselink/domain/model/ContactMessage;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeForContact", "Lkotlinx/coroutines/flow/Flow;", "", "androidApp_proDebug"})
@androidx.room.Dao()
public abstract interface ContactMessageDao {
    
    @androidx.room.Query(value = "SELECT * FROM contact_messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.pulselink.domain.model.ContactMessage>> observeForContact(long contactId);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insert(@org.jetbrains.annotations.NotNull()
    com.pulselink.domain.model.ContactMessage message, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM contact_messages WHERE contactId = :contactId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clear(long contactId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}