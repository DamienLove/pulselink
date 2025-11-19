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
import com.pulselink.domain.model.BlockedContact;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class BlockedContactDao_Impl implements BlockedContactDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BlockedContact> __insertionAdapterOfBlockedContact;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public BlockedContactDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBlockedContact = new EntityInsertionAdapter<BlockedContact>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `blocked_contacts` (`id`,`phoneNumber`,`linkCode`,`remoteDeviceId`,`displayName`,`blockedAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BlockedContact entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getPhoneNumber() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPhoneNumber());
        }
        if (entity.getLinkCode() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getLinkCode());
        }
        if (entity.getRemoteDeviceId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getRemoteDeviceId());
        }
        if (entity.getDisplayName() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDisplayName());
        }
        statement.bindLong(6, entity.getBlockedAt());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM blocked_contacts WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final BlockedContact blockedContact,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBlockedContact.insert(blockedContact);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object isBlocked(final String phoneNumber, final String linkCode,
      final String remoteDeviceId, final Continuation<? super Boolean> $completion) {
    final String _sql = "\n"
            + "        SELECT EXISTS(\n"
            + "            SELECT 1 FROM blocked_contacts\n"
            + "            WHERE (? IS NOT NULL AND phoneNumber = ?)\n"
            + "               OR (? IS NOT NULL AND linkCode = ?)\n"
            + "               OR (? IS NOT NULL AND remoteDeviceId = ?)\n"
            + "        )\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 6);
    int _argIndex = 1;
    if (phoneNumber == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, phoneNumber);
    }
    _argIndex = 2;
    if (phoneNumber == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, phoneNumber);
    }
    _argIndex = 3;
    if (linkCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, linkCode);
    }
    _argIndex = 4;
    if (linkCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, linkCode);
    }
    _argIndex = 5;
    if (remoteDeviceId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, remoteDeviceId);
    }
    _argIndex = 6;
    if (remoteDeviceId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, remoteDeviceId);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Boolean>() {
      @Override
      @NonNull
      public Boolean call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Boolean _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp == null ? null : _tmp != 0;
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
  public Object getByPhone(final String phone,
      final Continuation<? super BlockedContact> $completion) {
    final String _sql = "SELECT * FROM blocked_contacts WHERE phoneNumber = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (phone == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, phone);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BlockedContact>() {
      @Override
      @Nullable
      public BlockedContact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfBlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "blockedAt");
          final BlockedContact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
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
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final long _tmpBlockedAt;
            _tmpBlockedAt = _cursor.getLong(_cursorIndexOfBlockedAt);
            _result = new BlockedContact(_tmpId,_tmpPhoneNumber,_tmpLinkCode,_tmpRemoteDeviceId,_tmpDisplayName,_tmpBlockedAt);
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
  public Object getByLinkCode(final String code,
      final Continuation<? super BlockedContact> $completion) {
    final String _sql = "SELECT * FROM blocked_contacts WHERE linkCode = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (code == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, code);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BlockedContact>() {
      @Override
      @Nullable
      public BlockedContact call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfBlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "blockedAt");
          final BlockedContact _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
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
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final long _tmpBlockedAt;
            _tmpBlockedAt = _cursor.getLong(_cursorIndexOfBlockedAt);
            _result = new BlockedContact(_tmpId,_tmpPhoneNumber,_tmpLinkCode,_tmpRemoteDeviceId,_tmpDisplayName,_tmpBlockedAt);
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
  public Flow<List<BlockedContact>> observeAll() {
    final String _sql = "SELECT * FROM blocked_contacts ORDER BY blockedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blocked_contacts"}, new Callable<List<BlockedContact>>() {
      @Override
      @NonNull
      public List<BlockedContact> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfLinkCode = CursorUtil.getColumnIndexOrThrow(_cursor, "linkCode");
          final int _cursorIndexOfRemoteDeviceId = CursorUtil.getColumnIndexOrThrow(_cursor, "remoteDeviceId");
          final int _cursorIndexOfDisplayName = CursorUtil.getColumnIndexOrThrow(_cursor, "displayName");
          final int _cursorIndexOfBlockedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "blockedAt");
          final List<BlockedContact> _result = new ArrayList<BlockedContact>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BlockedContact _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpPhoneNumber;
            if (_cursor.isNull(_cursorIndexOfPhoneNumber)) {
              _tmpPhoneNumber = null;
            } else {
              _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            }
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
            final String _tmpDisplayName;
            if (_cursor.isNull(_cursorIndexOfDisplayName)) {
              _tmpDisplayName = null;
            } else {
              _tmpDisplayName = _cursor.getString(_cursorIndexOfDisplayName);
            }
            final long _tmpBlockedAt;
            _tmpBlockedAt = _cursor.getLong(_cursorIndexOfBlockedAt);
            _item = new BlockedContact(_tmpId,_tmpPhoneNumber,_tmpLinkCode,_tmpRemoteDeviceId,_tmpDisplayName,_tmpBlockedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
