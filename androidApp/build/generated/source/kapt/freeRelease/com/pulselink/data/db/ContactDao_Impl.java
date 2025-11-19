package com.pulselink.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pulselink.domain.model.Contact;
import com.pulselink.domain.model.EscalationTier;
import com.pulselink.domain.model.LinkStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ContactDao_Impl implements ContactDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Contact> __insertionAdapterOfContact;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateOrder;

  public ContactDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfContact = new EntityInsertionAdapter<Contact>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `contacts` (`id`,`displayName`,`phoneNumber`,`escalationTier`,`includeLocation`,`autoCall`,`emergencySoundKey`,`checkInSoundKey`,`cameraEnabled`,`contactOrder`,`linkStatus`,`linkCode`,`remoteDeviceId`,`allowRemoteOverride`,`allowRemoteSoundChange`,`pendingApproval`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Contact entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getDisplayName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getDisplayName());
        }
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getPhoneNumber());
        }
        statement.bindString(4, __EscalationTier_enumToString(entity.getEscalationTier()));
        final int _tmp = entity.getIncludeLocation() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.getAutoCall() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        if (entity.getEmergencySoundKey() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEmergencySoundKey());
        }
        if (entity.getCheckInSoundKey() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCheckInSoundKey());
        }
        final int _tmp_2 = entity.getCameraEnabled() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindLong(10, entity.getContactOrder());
        statement.bindString(11, __LinkStatus_enumToString(entity.getLinkStatus()));
        if (entity.getLinkCode() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getLinkCode());
        }
        if (entity.getRemoteDeviceId() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getRemoteDeviceId());
        }
        final int _tmp_3 = entity.getAllowRemoteOverride() ? 1 : 0;
        statement.bindLong(14, _tmp_3);
        final int _tmp_4 = entity.getAllowRemoteSoundChange() ? 1 : 0;
        statement.bindLong(15, _tmp_4);
        final int _tmp_5 = entity.getPendingApproval() ? 1 : 0;
        statement.bindLong(16, _tmp_5);
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contacts WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateOrder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE contacts SET contactOrder = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final Contact contact, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfContact.insert(contact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateOrder(final long contactId, final int order,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateOrder.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, order);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, contactId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateOrder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Contact>> observeContacts() {
    final String _sql = "SELECT * FROM contacts ORDER BY contactOrder ASC, displayName COLLATE NOCASE";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contacts"}, new Callable<List<Contact>>() {
      @Override
      @NonNull
      public List<Contact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final List<Contact> _result = new ArrayList<Contact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Contact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _item = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getByTier(final String tier,
      final Continuation<? super List<Contact>> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE escalationTier = ? ORDER BY contactOrder ASC, displayName COLLATE NOCASE";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (tier == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, tier);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Contact>>() {
      @Override
      @NonNull
      public List<Contact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final List<Contact> _result = new ArrayList<Contact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Contact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _item = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final long contactId, final Continuation<? super Contact> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, contactId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Contact>() {
      @Override
      @Nullable
      public Contact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final Contact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _result = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByLinkCode(final String code, final Continuation<? super Contact> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE linkCode = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (code == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, code);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Contact>() {
      @Override
      @Nullable
      public Contact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final Contact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _result = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByPhone(final String phone, final Continuation<? super Contact> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE phoneNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (phone == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, phone);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Contact>() {
      @Override
      @Nullable
      public Contact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final Contact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _result = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByRemoteDeviceId(final String deviceId,
      final Continuation<? super Contact> $completion) {
    final String _sql = "SELECT * FROM contacts WHERE remoteDeviceId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (deviceId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, deviceId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Contact>() {
      @Override
      @Nullable
      public Contact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfEscalationTier = CursorUtil.getColumnIndexOrThrow(_cursor, "escalationTier");
          final int _cursorIndexOfIncludeLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "includeLocation");
          final int _cursorIndexOfAutoCall = CursorUtil.getColumnIndexOrThrow(_cursor, "autoCall");
          final int _cursorIndexOfEmergencySoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "emergencySoundKey");
          final int _cursorIndexOfCheckInSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "checkInSoundKey");
          final int _cursorIndexOfCameraEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "cameraEnabled");
          final int _cursorIndexOfContactOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "contactOrder");
          final int _cursorIndexOfLinkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "linkStatus");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfAllowRemoteOverride = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteOverride");
          final int _cursorIndexOfAllowRemoteSoundChange = CursorUtil.getColumnIndexOrThrow(_cursor, "allowRemoteSoundChange");
          final int _cursorIndexOfPendingApproval = CursorUtil.getColumnIndexOrThrow(_cursor, "pendingApproval");
          final Contact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
            final EscalationTier _tmpEscalationTier;
            _tmpEscalationTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfEscalationTier));
            final boolean _tmpIncludeLocation;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIncludeLocation);
            _tmpIncludeLocation = _tmp != 0;
            final boolean _tmpAutoCall;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfAutoCall);
            _tmpAutoCall = _tmp_1 != 0;
            final String _tmpEmergencySoundKey;
            if (_cursor.isNull(_cursorIndexOfEmergencySoundKey)) {
              _tmpEmergencySoundKey = null;
            } else {
              _tmpEmergencySoundKey = _cursor.getString(_cursorIndexOfEmergencySoundKey);
            }
            final String _tmpCheckInSoundKey;
            if (_cursor.isNull(_cursorIndexOfCheckInSoundKey)) {
              _tmpCheckInSoundKey = null;
            } else {
              _tmpCheckInSoundKey = _cursor.getString(_cursorIndexOfCheckInSoundKey);
            }
            final boolean _tmpCameraEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfCameraEnabled);
            _tmpCameraEnabled = _tmp_2 != 0;
            final int _tmpContactOrder;
            _tmpContactOrder = _cursor.getInt(_cursorIndexOfContactOrder);
            final LinkStatus _tmpLinkStatus;
            _tmpLinkStatus = __LinkStatus_stringToEnum(_cursor.getString(_cursorIndexOfLinkStatus));
            final String _tmpLinkCode;
            if (_cursor.isNull(_cursorIndexOfLinkCode)) {
              _tmpLinkCode = null;
            } else {
              _tmpLinkCode = _cursor.getString(_cursorIndexOfLinkCode);
            }
            final String _tmpRemoteDeviceId;
            if (_cursor.isNull(_cursorIndexOfRemoteDeviceId)) {
              _tmpRemoteDeviceId = null;
            } else {
              _tmpRemoteDeviceId = _cursor.getString(_cursorIndexOfRemoteDeviceId);
            }
            final boolean _tmpAllowRemoteOverride;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfAllowRemoteOverride);
            _tmpAllowRemoteOverride = _tmp_3 != 0;
            final boolean _tmpAllowRemoteSoundChange;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfAllowRemoteSoundChange);
            _tmpAllowRemoteSoundChange = _tmp_4 != 0;
            final boolean _tmpPendingApproval;
            final int _tmp_5;
            _tmp_5 = _cursor.getInt(_cursorIndexOfPendingApproval);
            _tmpPendingApproval = _tmp_5 != 0;
            _result = new Contact(_tmpId,_tmpDisplayName,_tmpPhoneNumber,_tmpEscalationTier,_tmpIncludeLocation,_tmpAutoCall,_tmpEmergencySoundKey,_tmpCheckInSoundKey,_tmpCameraEnabled,_tmpContactOrder,_tmpLinkStatus,_tmpLinkCode,_tmpRemoteDeviceId,_tmpAllowRemoteOverride,_tmpAllowRemoteSoundChange,_tmpPendingApproval);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __EscalationTier_enumToString(@NonNull final EscalationTier _value) {
    switch (_value) {
      case EMERGENCY: return "EMERGENCY";
      case CHECK_IN: return "CHECK_IN";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __LinkStatus_enumToString(@NonNull final LinkStatus _value) {
    switch (_value) {
      case NONE: return "NONE";
      case OUTBOUND_PENDING: return "OUTBOUND_PENDING";
      case INBOUND_REQUEST: return "INBOUND_REQUEST";
      case LINKED: return "LINKED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private EscalationTier __EscalationTier_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "EMERGENCY": return EscalationTier.EMERGENCY;
      case "CHECK_IN": return EscalationTier.CHECK_IN;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private LinkStatus __LinkStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "NONE": return LinkStatus.NONE;
      case "OUTBOUND_PENDING": return LinkStatus.OUTBOUND_PENDING;
      case "INBOUND_REQUEST": return LinkStatus.INBOUND_REQUEST;
      case "LINKED": return LinkStatus.LINKED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
