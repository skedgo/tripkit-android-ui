package com.skedgo.tripkit.ui.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import com.skedgo.sqlite.DatabaseField
import com.skedgo.sqlite.DatabaseTable
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.database.DbFields.ID
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.database.DbTables
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.utils.ProviderUtils
import timber.log.Timber
import javax.inject.Inject

class ServiceStopsProvider : ContentProvider() {
  companion object {

    val AUTHORITY = TripKitUI.AUTHORITY + ServiceStopsProvider::class.java.simpleName
    private val BASE_URI = Uri.parse("content://$AUTHORITY")

    /**
     * Supports insertion only - used for batch transactions when inserting stops
     */
    val STOPS_URI = Uri.withAppendedPath(BASE_URI, "stops")
    val SHAPES_URI = Uri.withAppendedPath(BASE_URI, "shapes")
    val LOCATIONS_URI = Uri.withAppendedPath(BASE_URI, "locations")
    val STOPS_BY_SERVICE_URI = Uri.withAppendedPath(BASE_URI, "stopsByServiceTripId")

    private val mURIMatcher = UriMatcher(UriMatcher.NO_MATCH)
    private val STOPS = 0x1
    private val SHAPES = 0x2
    private val LOCATIONS = 0x3
    private val STOPS_FOR_SERVICE_ID = 0x4

    init {
      mURIMatcher.addURI(AUTHORITY, "stops", STOPS)
      mURIMatcher.addURI(AUTHORITY, "locations", LOCATIONS)
      mURIMatcher.addURI(AUTHORITY, "shapes", SHAPES)
      mURIMatcher.addURI(AUTHORITY, "stopsByServiceTripId", STOPS_FOR_SERVICE_ID)
    }
  }

  @Inject
  internal lateinit var mDbHelper: DbHelper

  override fun onCreate(): Boolean {
    TripKitUI.getInstance().inject(this)
    return true
  }

  override fun getType(uri: Uri): String? {
    return if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
      uri.toString()
    } else {
      null
    }
  }

  override fun query(uri: Uri, proj: Array<String>?, sel: String?, selArgs: Array<String>?, sort: String?): Cursor {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }
      throw IllegalArgumentException("No match for URI: $uri")
    }

    val qb = SQLiteQueryBuilder()

    when (type) {
      STOPS_FOR_SERVICE_ID -> {
        val tablesBuilder = StringBuilder()
        tablesBuilder.append(DbTables.SERVICE_STOPS)
            .append(" INNER JOIN ").append(DbTables.SEGMENT_SHAPES)
            .append(" ON ")
            .append(DbTables.SERVICE_STOPS).append(".").append(DbFields.SERVICE_SHAPE_ID)
            .append(" = ")
            .append(DbTables.SEGMENT_SHAPES).append(".").append(DbFields.ID)

        tablesBuilder.append(" INNER JOIN ").append(DbTables.LOCATIONS)
            .append(" ON ")
            .append(DbTables.SERVICE_STOPS).append(".").append(DbFields.ID)
            .append(" = ")
            .append(DbTables.LOCATIONS).append(".").append(DbFields.SERVICE_STOP_ID)

        tablesBuilder.append(" LEFT JOIN ")
            .append(DbTables.SCHEDULED_SERVICES)
            .append(" ON ")
            .append(DbTables.SCHEDULED_SERVICES).append(".").append(DbFields.SERVICE_TRIP_ID)
            .append(" = ")
            .append(DbTables.SEGMENT_SHAPES).append(".").append(DbFields.SERVICE_TRIP_ID)

        /*
                     service_stops INNER JOIN segment_shapes ON segment_shapes._id = service_stops.service_shape_id
                     INNER JOIN locations ON locations.service_stop_id = service_stops._id
                     LEFT JOIN services ON services.service_trip_id = segment_shapes.service_trip_id
                     */
        qb.tables = tablesBuilder.toString()
      }
    }

    val cursor = qb.query(mDbHelper.readableDatabase, proj, sel, selArgs, null, null, sort)
    cursor.setNotificationUri(context!!.contentResolver, uri)
    return cursor
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }

      return null
    }

    try {
      val db = mDbHelper.writableDatabase

      var fields: Array<DatabaseField>
      var table: DatabaseTable
      when (type) {
        STOPS -> {
          table = DbTables.SERVICE_STOPS
          fields = arrayOf(DbFields.STOP_CODE, DbFields.DEPARTURE_TIME)
        }
        LOCATIONS -> {
          table = DbTables.LOCATIONS
          fields = arrayOf(ID)
        }
        SHAPES -> {
          table = DbTables.SEGMENT_SHAPES
          fields = arrayOf(ID)
        }
        else -> return null
      }

      val id = ProviderUtils.upsert(db, table, values, fields)
      if (id > 0) {
        val newUri = ContentUris.withAppendedId(uri, id)
        context!!.contentResolver.notifyChange(uri, null)
        return newUri
      } else {
        throw SQLException("Failed to insert row into $uri values=$values")
      }
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.e("Error inserting data", e)
      }

      return null
    }

  }

  override fun delete(uri: Uri, sel: String?, selArgs: Array<String>?): Int {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }

      return 0
    }

    try {
      val db = mDbHelper.writableDatabase
      var rowsAffected = 0
      when (type) {
        STOPS -> rowsAffected = db.delete(DbTables.SERVICE_STOPS.name, sel, selArgs)
        LOCATIONS -> rowsAffected = db.delete(DbTables.LOCATIONS.name, sel, selArgs)
        SHAPES -> rowsAffected = db.delete(DbTables.SEGMENT_SHAPES.name, sel, selArgs)
      }

      context!!.contentResolver.notifyChange(uri, null)
      return rowsAffected
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error deleting data", e)
      }

      return 0
    }

  }

  override fun update(uri: Uri, values: ContentValues?, sel: String?, selArgs: Array<String>?): Int {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }

      return 0
    }

    try {
      val db = mDbHelper.writableDatabase
      var rowsAffected = 0
      when (type) {
        STOPS -> rowsAffected = db.update(DbTables.SERVICE_STOPS.name, values, sel, selArgs)
        LOCATIONS -> rowsAffected = db.update(DbTables.LOCATIONS.name, values, sel, selArgs)
        SHAPES -> rowsAffected = db.update(DbTables.SEGMENT_SHAPES.name, values, sel, selArgs)
      }

      context!!.contentResolver.notifyChange(uri, null)
      return rowsAffected
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error deleting data", e)
      }

      return 0
    }

  }

  override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }

      return 0
    }

    try {
      val db = mDbHelper.writableDatabase

      var numInserted = 0
      try {
        db.beginTransaction()

        var fields: Array<DatabaseField>? = null
        var table: DatabaseTable? = null

        when (type) {
          STOPS -> {
            table = DbTables.SERVICE_STOPS
            fields = arrayOf(DbFields.STOP_CODE, DbFields.DEPARTURE_TIME)
          }
          LOCATIONS -> {
            table = DbTables.LOCATIONS
            fields = arrayOf(ID)
          }
          SHAPES -> {
            table = DbTables.SEGMENT_SHAPES
            fields = arrayOf(ID)
          }
        }

        for (value in values) {
          ProviderUtils.upsert(db, table, value, fields)
        }

        db.setTransactionSuccessful()

        context!!.contentResolver.notifyChange(uri, null)

        numInserted = values.size
      } finally {
        db.endTransaction()
      }

      return numInserted
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error inserting data", e)
      }

      return 0
    }

  }

}