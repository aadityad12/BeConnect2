package com.beconnect.data;

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
public final class AlertDao_Impl implements AlertDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AlertPacket> __insertionAdapterOfAlertPacket;

  private final SharedSQLiteStatement __preparedStmtOfPruneOldAlerts;

  public AlertDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAlertPacket = new EntityInsertionAdapter<AlertPacket>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `alerts` (`alertId`,`severity`,`headline`,`expires`,`instructions`,`sourceUrl`,`verified`,`fetchedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlertPacket entity) {
        statement.bindString(1, entity.getAlertId());
        statement.bindString(2, entity.getSeverity());
        statement.bindString(3, entity.getHeadline());
        statement.bindLong(4, entity.getExpires());
        statement.bindString(5, entity.getInstructions());
        statement.bindString(6, entity.getSourceUrl());
        final int _tmp = entity.getVerified() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getFetchedAt());
      }
    };
    this.__preparedStmtOfPruneOldAlerts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM alerts WHERE alertId NOT IN (SELECT alertId FROM alerts ORDER BY fetchedAt DESC LIMIT 20)";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AlertPacket alert, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAlertPacket.insert(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneOldAlerts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneOldAlerts.acquire();
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
          __preparedStmtOfPruneOldAlerts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AlertPacket>> getAllAlerts() {
    final String _sql = "SELECT * FROM alerts ORDER BY fetchedAt DESC LIMIT 20";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"alerts"}, new Callable<List<AlertPacket>>() {
      @Override
      @NonNull
      public List<AlertPacket> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAlertId = CursorUtil.getColumnIndexOrThrow(_cursor, "alertId");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final int _cursorIndexOfHeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "headline");
          final int _cursorIndexOfExpires = CursorUtil.getColumnIndexOrThrow(_cursor, "expires");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "verified");
          final int _cursorIndexOfFetchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "fetchedAt");
          final List<AlertPacket> _result = new ArrayList<AlertPacket>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlertPacket _item;
            final String _tmpAlertId;
            _tmpAlertId = _cursor.getString(_cursorIndexOfAlertId);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            final String _tmpHeadline;
            _tmpHeadline = _cursor.getString(_cursorIndexOfHeadline);
            final long _tmpExpires;
            _tmpExpires = _cursor.getLong(_cursorIndexOfExpires);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpSourceUrl;
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            final boolean _tmpVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfVerified);
            _tmpVerified = _tmp != 0;
            final long _tmpFetchedAt;
            _tmpFetchedAt = _cursor.getLong(_cursorIndexOfFetchedAt);
            _item = new AlertPacket(_tmpAlertId,_tmpSeverity,_tmpHeadline,_tmpExpires,_tmpInstructions,_tmpSourceUrl,_tmpVerified,_tmpFetchedAt);
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
  public Object getById(final String id, final Continuation<? super AlertPacket> $completion) {
    final String _sql = "SELECT * FROM alerts WHERE alertId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AlertPacket>() {
      @Override
      @Nullable
      public AlertPacket call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfAlertId = CursorUtil.getColumnIndexOrThrow(_cursor, "alertId");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final int _cursorIndexOfHeadline = CursorUtil.getColumnIndexOrThrow(_cursor, "headline");
          final int _cursorIndexOfExpires = CursorUtil.getColumnIndexOrThrow(_cursor, "expires");
          final int _cursorIndexOfInstructions = CursorUtil.getColumnIndexOrThrow(_cursor, "instructions");
          final int _cursorIndexOfSourceUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceUrl");
          final int _cursorIndexOfVerified = CursorUtil.getColumnIndexOrThrow(_cursor, "verified");
          final int _cursorIndexOfFetchedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "fetchedAt");
          final AlertPacket _result;
          if (_cursor.moveToFirst()) {
            final String _tmpAlertId;
            _tmpAlertId = _cursor.getString(_cursorIndexOfAlertId);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            final String _tmpHeadline;
            _tmpHeadline = _cursor.getString(_cursorIndexOfHeadline);
            final long _tmpExpires;
            _tmpExpires = _cursor.getLong(_cursorIndexOfExpires);
            final String _tmpInstructions;
            _tmpInstructions = _cursor.getString(_cursorIndexOfInstructions);
            final String _tmpSourceUrl;
            _tmpSourceUrl = _cursor.getString(_cursorIndexOfSourceUrl);
            final boolean _tmpVerified;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfVerified);
            _tmpVerified = _tmp != 0;
            final long _tmpFetchedAt;
            _tmpFetchedAt = _cursor.getLong(_cursorIndexOfFetchedAt);
            _result = new AlertPacket(_tmpAlertId,_tmpSeverity,_tmpHeadline,_tmpExpires,_tmpInstructions,_tmpSourceUrl,_tmpVerified,_tmpFetchedAt);
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
}
