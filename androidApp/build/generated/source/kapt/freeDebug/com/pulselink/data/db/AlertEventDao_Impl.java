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
import com.pulselink.domain.model.AlertEvent;
import com.pulselink.domain.model.EscalationTier;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Long;
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
public final class AlertEventDao_Impl implements AlertEventDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AlertEvent> __insertionAdapterOfAlertEvent;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  public AlertEventDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAlertEvent = new EntityInsertionAdapter<AlertEvent>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `alert_events` (`id`,`timestamp`,`triggeredBy`,`tier`,`contactCount`,`sentSms`,`sharedLocation`,`contactId`,`contactName`,`isIncoming`,`soundKey`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlertEvent entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        if (entity.getTriggeredBy() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTriggeredBy());
        }
        statement.bindString(4, __EscalationTier_enumToString(entity.getTier()));
        statement.bindLong(5, entity.getContactCount());
        final int _tmp = entity.getSentSms() ? 1 : 0;
        statement.bindLong(6, _tmp);
        final int _tmp_1 = entity.getSharedLocation() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        if (entity.getContactId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getContactId());
        }
        if (entity.getContactName() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getContactName());
        }
        final int _tmp_2 = entity.isIncoming() ? 1 : 0;
        statement.bindLong(10, _tmp_2);
        if (entity.getSoundKey() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getSoundKey());
        }
      }
    };
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM alert_events";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AlertEvent event, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAlertEvent.insert(event);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
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
  public Flow<List<AlertEvent>> observeRecent(final int limit) {
    final String _sql = "SELECT * FROM alert_events ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"alert_events"}, new Callable<List<AlertEvent>>() {
      @Override
      @NonNull
      public List<AlertEvent> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTriggeredBy = CursorUtil.getColumnIndexOrThrow(_cursor, "triggeredBy");
          final int _cursorIndexOfTier = CursorUtil.getColumnIndexOrThrow(_cursor, "tier");
          final int _cursorIndexOfContactCount = CursorUtil.getColumnIndexOrThrow(_cursor, "contactCount");
          final int _cursorIndexOfSentSms = CursorUtil.getColumnIndexOrThrow(_cursor, "sentSms");
          final int _cursorIndexOfSharedLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "sharedLocation");
          final int _cursorIndexOfContactId = CursorUtil.getColumnIndexOrThrow(_cursor, "contactId");
          final int _cursorIndexOfContactName = CursorUtil.getColumnIndexOrThrow(_cursor, "contactName");
          final int _cursorIndexOfIsIncoming = CursorUtil.getColumnIndexOrThrow(_cursor, "isIncoming");
          final int _cursorIndexOfSoundKey = CursorUtil.getColumnIndexOrThrow(_cursor, "soundKey");
          final List<AlertEvent> _result = new ArrayList<AlertEvent>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlertEvent _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpTriggeredBy;
            if (_cursor.isNull(_cursorIndexOfTriggeredBy)) {
              _tmpTriggeredBy = null;
            } else {
              _tmpTriggeredBy = _cursor.getString(_cursorIndexOfTriggeredBy);
            }
            final EscalationTier _tmpTier;
            _tmpTier = __EscalationTier_stringToEnum(_cursor.getString(_cursorIndexOfTier));
            final int _tmpContactCount;
            _tmpContactCount = _cursor.getInt(_cursorIndexOfContactCount);
            final boolean _tmpSentSms;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSentSms);
            _tmpSentSms = _tmp != 0;
            final boolean _tmpSharedLocation;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfSharedLocation);
            _tmpSharedLocation = _tmp_1 != 0;
            final Long _tmpContactId;
            if (_cursor.isNull(_cursorIndexOfContactId)) {
              _tmpContactId = null;
            } else {
              _tmpContactId = _cursor.getLong(_cursorIndexOfContactId);
            }
            final String _tmpContactName;
            if (_cursor.isNull(_cursorIndexOfContactName)) {
              _tmpContactName = null;
            } else {
              _tmpContactName = _cursor.getString(_cursorIndexOfContactName);
            }
            final boolean _tmpIsIncoming;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsIncoming);
            _tmpIsIncoming = _tmp_2 != 0;
            final String _tmpSoundKey;
            if (_cursor.isNull(_cursorIndexOfSoundKey)) {
              _tmpSoundKey = null;
            } else {
              _tmpSoundKey = _cursor.getString(_cursorIndexOfSoundKey);
            }
            _item = new AlertEvent(_tmpId,_tmpTimestamp,_tmpTriggeredBy,_tmpTier,_tmpContactCount,_tmpSentSms,_tmpSharedLocation,_tmpContactId,_tmpContactName,_tmpIsIncoming,_tmpSoundKey);
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

  private String __EscalationTier_enumToString(@NonNull final EscalationTier _value) {
    switch (_value) {
      case EMERGENCY: return "EMERGENCY";
      case CHECK_IN: return "CHECK_IN";
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
}
