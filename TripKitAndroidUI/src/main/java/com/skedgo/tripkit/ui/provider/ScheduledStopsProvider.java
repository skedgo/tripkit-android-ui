package com.skedgo.tripkit.ui.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import com.skedgo.sqlite.DatabaseField;
import com.skedgo.sqlite.DatabaseTable;
import com.skedgo.tripkit.data.database.DbFields;
import com.skedgo.tripkit.data.database.DbHelper;
import com.skedgo.tripkit.data.database.DbTables;
import com.skedgo.tripkit.ui.BuildConfig;
import com.skedgo.tripkit.ui.TripKitUI;
import com.skedgo.tripkit.ui.utils.ProviderUtils;

import javax.inject.Inject;
import java.sql.SQLException;

/**
 * @author Daniel Grech
 */
public class ScheduledStopsProvider extends ContentProvider {
  public static final String AUTHORITY = TripKitUI.AUTHORITY + ScheduledStopsProvider.class.getSimpleName();
  private static final String TAG = ScheduledStopsProvider.class
      .getSimpleName();
  private static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
  public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_URI,
                                                             "stopsOnly");
  /**
   * Supports insertion only - used for bulk transactions when inserting stops
   */
  public static final Uri LOCATIONS_URI = Uri.withAppendedPath(BASE_URI,
                                                               "locations");
  /**
   * Only valid whilst bulkInserting.
   * <p/>
   * Groups locations by their scheduled_stop_id rather than id
   */
  public static final Uri LOCATIONS_BY_SCHEDULED_STOP_URI = Uri
      .withAppendedPath(BASE_URI, "locationsByScheduledStopId");
  public static final Uri DOWNLOAD_HISTORY_URI = Uri.withAppendedPath(
      BASE_URI, "downloadHistory");
  private static final UriMatcher mURIMatcher = new UriMatcher(
      UriMatcher.NO_MATCH);
  private static final int DOWNLOAD_HISTORY = 0x2;
  private static final int LOCATIONS = 0x3;
  private static final int STOPS_ONLY = 0x4;
  private static final int LOCATIONS_BY_SCHEDULED_STOP = 0x5;
  @Inject
  DbHelper mDbHelper;

  @Override
  public boolean onCreate() {
    TripKitUI.getInstance().inject(this);
    return true;
  }

  @Override
  public String getType(final Uri uri) {
    if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
      return uri.toString();
    } else {
      return null;
    }
  }

  @Override
  public Cursor query(final Uri uri, String[] proj, final String sel,
                      final String[] selArgs, final String sort) {
    final int type = mURIMatcher.match(uri);
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Log.w(TAG, "No match for URI: " + uri);
      }

      return null;
    }

    try {
      SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

      switch (type) {
        case STOPS_ONLY: {
          qb.setTables(DbTables.SCHEDULED_STOPS.getName());
          break;
        }

        case LOCATIONS: {
          qb.setTables(DbTables.LOCATIONS.getName());
          break;
        }

        case DOWNLOAD_HISTORY: {
          qb.setTables(DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.getName());
          break;
        }
      }

      Cursor cursor = qb.query(mDbHelper.getReadableDatabase(), proj, sel,
                               selArgs, null, null, sort);
      cursor.setNotificationUri(getContext().getContentResolver(), uri);

      return cursor;
    } catch (Exception e) {
      if (BuildConfig.DEBUG) {
        Log.e(TAG, "Error querying data", e);
      }

      return null;
    }
  }

  @Override
  public Uri insert(final Uri uri, final ContentValues values) {
    final int type = mURIMatcher.match(uri);
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Log.w(TAG, "No match for URI: " + uri);
      }

      return null;
    }

    try {
      final SQLiteDatabase db = mDbHelper.getWritableDatabase();

      DatabaseField[] fields = null;
      DatabaseTable table = null;

      switch (type) {
        case LOCATIONS:
          table = DbTables.LOCATIONS;
          fields = new DatabaseField[] {DbFields.ID};
          break;
        case DOWNLOAD_HISTORY:
          table = DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY;
          fields = new DatabaseField[] {DbFields.CELL_CODE};
          break;
      }

      long id = ProviderUtils.upsert(db, table, values, fields);

      if (id > 0) {
        Uri newUri = ContentUris.withAppendedId(uri, id);
        getContext().getContentResolver().notifyChange(uri, null);
        return newUri;
      } else {
        throw new SQLException("Failed to insert row into " + uri
                                   + " values=" + values);
      }

    } catch (Exception e) {
      if (BuildConfig.DEBUG) {
        Log.e(TAG, "Error inserting data", e);
      }

      return null;
    }
  }

  @Override
  public int delete(final Uri uri, final String sel, final String[] selArgs) {
    final int type = mURIMatcher.match(uri);
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Log.w(TAG, "No match for URI: " + uri);
      }

      return 0;
    }

    try {
      final SQLiteDatabase db = mDbHelper.getWritableDatabase();
      int rowsAffected = 0;
      switch (type) {
        case DOWNLOAD_HISTORY:
          rowsAffected = db.delete(
              DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.getName(), sel,
              selArgs);
          break;
        case LOCATIONS_BY_SCHEDULED_STOP:
          rowsAffected = db.delete(DbTables.LOCATIONS.getName(), sel, selArgs);
          break;
      }

      getContext().getContentResolver().notifyChange(uri, null);
      return rowsAffected;
    } catch (Exception e) {
      if (BuildConfig.DEBUG) {
        Log.e(TAG, "Error deleting data", e);
      }
      return 0;
    }
  }

  @Override
  public int update(final Uri uri, final ContentValues values,
                    final String sel, final String[] selArgs) {
    final int type = mURIMatcher.match(uri);
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Log.w(TAG, "No match for URI: " + uri);
      }

      return 0;
    }

    try {
      final SQLiteDatabase db = mDbHelper.getWritableDatabase();
      int rowsAffected = 0;
      switch (type) {
        case DOWNLOAD_HISTORY:
          rowsAffected = db.update(
              DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.getName(),
              values, sel, selArgs);
          break;
      }

      getContext().getContentResolver().notifyChange(uri, null);
      return rowsAffected;
    } catch (Exception e) {
      if (BuildConfig.DEBUG) {
        Log.e(TAG, "Error deleting data", e);
      }
      return 0;
    }
  }

  @Override
  public int bulkInsert(final Uri uri, final ContentValues[] values) {
    final int type = mURIMatcher.match(uri);

    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Log.w(TAG, "No match for URI: " + uri);
      }
      return 0;
    }

    try {
      final SQLiteDatabase db = mDbHelper.getWritableDatabase();
      int numInserted = 0;
      try {
        db.beginTransaction();

        if (values != null) {
          for (ContentValues value : values) {
            switch (type) {
              case LOCATIONS:
                ProviderUtils.upsert(db, DbTables.LOCATIONS, value,
                                     DbFields.ID);
                break;
              case LOCATIONS_BY_SCHEDULED_STOP:
                ProviderUtils.upsert(db, DbTables.LOCATIONS, value,
                                     DbFields.SCHEDULED_STOP_CODE);
                break;
              case DOWNLOAD_HISTORY:
                ProviderUtils.upsert(db,
                                     DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY,
                                     value, DbFields.CELL_CODE);
                break;
            }
          }
        }

        db.setTransactionSuccessful();

        getContext().getContentResolver().notifyChange(uri, null);

        numInserted = values == null ? 0 : values.length;
      } finally {
        db.endTransaction();
      }
      return numInserted;

    } catch (Exception e) {
      if (BuildConfig.DEBUG) {
        Log.e(TAG, "Error inserting data", e);
      }
      return 0;
    }
  }

  static {
    mURIMatcher.addURI(AUTHORITY, "stopsOnly", STOPS_ONLY);
    mURIMatcher.addURI(AUTHORITY, "locations", LOCATIONS);
    mURIMatcher.addURI(AUTHORITY, "locationsByScheduledStopId",
                       LOCATIONS_BY_SCHEDULED_STOP);
    mURIMatcher.addURI(AUTHORITY, "downloadHistory", DOWNLOAD_HISTORY);
  }

}
