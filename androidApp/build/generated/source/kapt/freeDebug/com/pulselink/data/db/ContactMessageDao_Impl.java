package com.pulselink.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.pulselink.domain.model.ContactMessage;
import com.pulselink.domain.model.MessageDirection;
import java.lang.Class;
import java.lang.Exception;
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
public final class ContactMessageDao_Impl implements ContactMessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ContactMessage> __insertionAdapterOfContactMessage;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public ContactMessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfContactMessage = new EntityInsertionAdapter<ContactMessage>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `contact_messages` (`id`,`contactId`,`body`,`direction`,`timestamp`,`overrideSucceeded`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ContactMessage entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getContactId());
        if (entity.getBody() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getBody());
        }
        final String _tmp = __converters.fromDirection(entity.getDirection());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        statement.bindLong(5, entity.getTimestamp());
        final int _tmp_1 = entity.getOverrideSucceeded() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM contact_messages WHERE contactId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ContactMessage message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfContactMessage.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final long contactId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
        int _argIndex = 1;
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
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ContactMessage>> observeForContact(final long contactId) {
    final String _sql = "SELECT * FROM contact_messages WHERE contactId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, contactId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"contact_messages"}, new Callable<List<ContactMessage>>() {
      @Override
      @NonNull
      public List<ContactMessage> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfContactId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactId");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfOverrideSucceeded = CursorUtil.getColumnIndexOrThrow(_cursor, "overrideSucceeded");
          final List<ContactMessage> _result = new ArrayList<ContactMessage>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ContactMessage _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpContactId;
            _tmpContactId = _cursor.getLong(_cursorIndexOfContactId);
            final String _tmpBody;
            if (_cursor.isNull(_cursorIndexOfBody)) {
              _tmpBody = null;
            } else {
              _tmpBody = _cursor.getString(_cursorIndexOfBody);
            }
            final MessageDirection _tmpDirection;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfDirection)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfDirection);
            }
            _tmpDirection = __converters.toDirection(_tmp);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpOverrideSucceeded;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfOverrideSucceeded);
            _tmpOverrideSucceeded = _tmp_1 != 0;
            _item = new ContactMessage(_tmpId,_tmpContactId,_tmpBody,_tmpDirection,_tmpTimestamp,_tmpOverrideSucceeded);
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
