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

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\'\u0018\u0000 \u000b2\u00020\u0001:\u0001\u000bB\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H&J\b\u0010\u0005\u001a\u00020\u0006H&J\b\u0010\u0007\u001a\u00020\bH&J\b\u0010\t\u001a\u00020\nH&\u00a8\u0006\f"}, d2 = {"Lcom/pulselink/data/db/PulseLinkDatabase;", "Landroidx/room/RoomDatabase;", "()V", "alertEventDao", "Lcom/pulselink/data/db/AlertEventDao;", "blockedContactDao", "Lcom/pulselink/data/db/BlockedContactDao;", "contactDao", "Lcom/pulselink/data/db/ContactDao;", "contactMessageDao", "Lcom/pulselink/data/db/ContactMessageDao;", "Companion", "androidApp_proRelease"})
@androidx.room.Database(entities = {com.pulselink.domain.model.Contact.class, com.pulselink.domain.model.AlertEvent.class, com.pulselink.domain.model.ContactMessage.class, com.pulselink.domain.model.BlockedContact.class}, version = 4, exportSchema = true)
@androidx.room.TypeConverters(value = {com.pulselink.data.db.Converters.class})
public abstract class PulseLinkDatabase extends androidx.room.RoomDatabase {
    @org.jetbrains.annotations.NotNull()
    private static final androidx.room.migration.Migration MIGRATION_3_4 = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.pulselink.data.db.PulseLinkDatabase.Companion Companion = null;
    
    public PulseLinkDatabase() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.data.db.ContactDao contactDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.data.db.AlertEventDao alertEventDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.data.db.ContactMessageDao contactMessageDao();
    
    @org.jetbrains.annotations.NotNull()
    public abstract com.pulselink.data.db.BlockedContactDao blockedContactDao();
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0007"}, d2 = {"Lcom/pulselink/data/db/PulseLinkDatabase$Companion;", "", "()V", "MIGRATION_3_4", "Landroidx/room/migration/Migration;", "getMIGRATION_3_4", "()Landroidx/room/migration/Migration;", "androidApp_proRelease"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final androidx.room.migration.Migration getMIGRATION_3_4() {
            return null;
        }
    }
}