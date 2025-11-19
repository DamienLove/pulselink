package com.pulselink.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PulseLinkDatabase_Impl extends PulseLinkDatabase {
  private volatile ContactDao _contactDao;

  private volatile AlertEventDao _alertEventDao;

  private volatile ContactMessageDao _contactMessageDao;

  private volatile BlockedContactDao _blockedContactDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, `phoneNumber` TEXT NOT NULL, `escalationTier` TEXT NOT NULL, `includeLocation` INTEGER NOT NULL, `autoCall` INTEGER NOT NULL, `emergencySoundKey` TEXT, `checkInSoundKey` TEXT, `cameraEnabled` INTEGER NOT NULL, `contactOrder` INTEGER NOT NULL, `linkStatus` TEXT NOT NULL, `linkCode` TEXT, `remoteDeviceId` TEXT, `allowRemoteOverride` INTEGER NOT NULL, `allowRemoteSoundChange` INTEGER NOT NULL, `pendingApproval` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `alert_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `triggeredBy` TEXT NOT NULL, `tier` TEXT NOT NULL, `contactCount` INTEGER NOT NULL, `sentSms` INTEGER NOT NULL, `sharedLocation` INTEGER NOT NULL, `contactId` INTEGER, `contactName` TEXT, `isIncoming` INTEGER NOT NULL, `soundKey` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `contact_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactId` INTEGER NOT NULL, `body` TEXT NOT NULL, `direction` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `overrideSucceeded` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `blocked_contacts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `phoneNumber` TEXT, `linkCode` TEXT, `remoteDeviceId` TEXT, `displayName` TEXT NOT NULL, `blockedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'dd2057a05d5a43058955b31f43f94ed5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `contacts`");
        db.execSQL("DROP TABLE IF EXISTS `alert_events`");
        db.execSQL("DROP TABLE IF EXISTS `contact_messages`");
        db.execSQL("DROP TABLE IF EXISTS `blocked_contacts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsContacts = new HashMap<String, TableInfo.Column>(16);
        _columnsContacts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("escalationTier", new TableInfo.Column("escalationTier", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("includeLocation", new TableInfo.Column("includeLocation", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("autoCall", new TableInfo.Column("autoCall", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("emergencySoundKey", new TableInfo.Column("emergencySoundKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("checkInSoundKey", new TableInfo.Column("checkInSoundKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("cameraEnabled", new TableInfo.Column("cameraEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("contactOrder", new TableInfo.Column("contactOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("linkStatus", new TableInfo.Column("linkStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("linkCode", new TableInfo.Column("linkCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("remoteDeviceId", new TableInfo.Column("remoteDeviceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("allowRemoteOverride", new TableInfo.Column("allowRemoteOverride", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("allowRemoteSoundChange", new TableInfo.Column("allowRemoteSoundChange", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContacts.put("pendingApproval", new TableInfo.Column("pendingApproval", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContacts = new TableInfo("contacts", _columnsContacts, _foreignKeysContacts, _indicesContacts);
        final TableInfo _existingContacts = TableInfo.read(db, "contacts");
        if (!_infoContacts.equals(_existingContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "contacts(com.pulselink.domain.model.Contact).\n"
                  + " Expected:\n" + _infoContacts + "\n"
                  + " Found:\n" + _existingContacts);
        }
        final HashMap<String, TableInfo.Column> _columnsAlertEvents = new HashMap<String, TableInfo.Column>(11);
        _columnsAlertEvents.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("triggeredBy", new TableInfo.Column("triggeredBy", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("tier", new TableInfo.Column("tier", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("contactCount", new TableInfo.Column("contactCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("sentSms", new TableInfo.Column("sentSms", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("sharedLocation", new TableInfo.Column("sharedLocation", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("contactId", new TableInfo.Column("contactId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("contactName", new TableInfo.Column("contactName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("isIncoming", new TableInfo.Column("isIncoming", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlertEvents.put("soundKey", new TableInfo.Column("soundKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAlertEvents = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAlertEvents = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAlertEvents = new TableInfo("alert_events", _columnsAlertEvents, _foreignKeysAlertEvents, _indicesAlertEvents);
        final TableInfo _existingAlertEvents = TableInfo.read(db, "alert_events");
        if (!_infoAlertEvents.equals(_existingAlertEvents)) {
          return new RoomOpenHelper.ValidationResult(false, "alert_events(com.pulselink.domain.model.AlertEvent).\n"
                  + " Expected:\n" + _infoAlertEvents + "\n"
                  + " Found:\n" + _existingAlertEvents);
        }
        final HashMap<String, TableInfo.Column> _columnsContactMessages = new HashMap<String, TableInfo.Column>(6);
        _columnsContactMessages.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContactMessages.put("contactId", new TableInfo.Column("contactId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContactMessages.put("body", new TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContactMessages.put("direction", new TableInfo.Column("direction", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContactMessages.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContactMessages.put("overrideSucceeded", new TableInfo.Column("overrideSucceeded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContactMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContactMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoContactMessages = new TableInfo("contact_messages", _columnsContactMessages, _foreignKeysContactMessages, _indicesContactMessages);
        final TableInfo _existingContactMessages = TableInfo.read(db, "contact_messages");
        if (!_infoContactMessages.equals(_existingContactMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "contact_messages(com.pulselink.domain.model.ContactMessage).\n"
                  + " Expected:\n" + _infoContactMessages + "\n"
                  + " Found:\n" + _existingContactMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsBlockedContacts = new HashMap<String, TableInfo.Column>(6);
        _columnsBlockedContacts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedContacts.put("phoneNumber", new TableInfo.Column("phoneNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedContacts.put("linkCode", new TableInfo.Column("linkCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedContacts.put("remoteDeviceId", new TableInfo.Column("remoteDeviceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedContacts.put("displayName", new TableInfo.Column("displayName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBlockedContacts.put("blockedAt", new TableInfo.Column("blockedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBlockedContacts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBlockedContacts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBlockedContacts = new TableInfo("blocked_contacts", _columnsBlockedContacts, _foreignKeysBlockedContacts, _indicesBlockedContacts);
        final TableInfo _existingBlockedContacts = TableInfo.read(db, "blocked_contacts");
        if (!_infoBlockedContacts.equals(_existingBlockedContacts)) {
          return new RoomOpenHelper.ValidationResult(false, "blocked_contacts(com.pulselink.domain.model.BlockedContact).\n"
                  + " Expected:\n" + _infoBlockedContacts + "\n"
                  + " Found:\n" + _existingBlockedContacts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "dd2057a05d5a43058955b31f43f94ed5", "fdfc9b46d792bf6e33f746483c4db132");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "contacts","alert_events","contact_messages","blocked_contacts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `contacts`");
      _db.execSQL("DELETE FROM `alert_events`");
      _db.execSQL("DELETE FROM `contact_messages`");
      _db.execSQL("DELETE FROM `blocked_contacts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ContactDao.class, ContactDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AlertEventDao.class, AlertEventDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContactMessageDao.class, ContactMessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BlockedContactDao.class, BlockedContactDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ContactDao contactDao() {
    if (_contactDao != null) {
      return _contactDao;
    } else {
      synchronized(this) {
        if(_contactDao == null) {
          _contactDao = new ContactDao_Impl(this);
        }
        return _contactDao;
      }
    }
  }

  @Override
  public AlertEventDao alertEventDao() {
    if (_alertEventDao != null) {
      return _alertEventDao;
    } else {
      synchronized(this) {
        if(_alertEventDao == null) {
          _alertEventDao = new AlertEventDao_Impl(this);
        }
        return _alertEventDao;
      }
    }
  }

  @Override
  public ContactMessageDao contactMessageDao() {
    if (_contactMessageDao != null) {
      return _contactMessageDao;
    } else {
      synchronized(this) {
        if(_contactMessageDao == null) {
          _contactMessageDao = new ContactMessageDao_Impl(this);
        }
        return _contactMessageDao;
      }
    }
  }

  @Override
  public BlockedContactDao blockedContactDao() {
    if (_blockedContactDao != null) {
      return _blockedContactDao;
    } else {
      synchronized(this) {
        if(_blockedContactDao == null) {
          _blockedContactDao = new BlockedContactDao_Impl(this);
        }
        return _blockedContactDao;
      }
    }
  }
}
