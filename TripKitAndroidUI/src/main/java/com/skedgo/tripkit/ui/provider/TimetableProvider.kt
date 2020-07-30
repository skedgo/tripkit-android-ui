package com.skedgo.tripkit.ui.provider

import android.content.*
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import com.skedgo.sqlite.DatabaseField
import com.skedgo.sqlite.DatabaseTable
import com.skedgo.tripkit.data.database.*
import com.skedgo.tripkit.data.database.DbFields.*
import com.skedgo.tripkit.data.database.timetables.ScheduledServiceRealtimeInfoDao
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.module.TimetableModule
import com.skedgo.tripkit.ui.timetables.JoinedCursor
import com.skedgo.tripkit.ui.utils.ProviderUtils
import timber.log.Timber
import javax.inject.Inject

class TimetableProvider : ContentProvider() {
  lateinit var mDbHelper: DbHelper

  lateinit var scheduledServiceRealtimeInfoDao: ScheduledServiceRealtimeInfoDao


  override fun onCreate(): Boolean {
    val db = TripKitDatabase.getInstance(context!!)
    mDbHelper = DbHelper(context!!, "tripkit-legacy.db", DatabaseMigrator(db))
    scheduledServiceRealtimeInfoDao = db.scheduledServiceRealtimeInfoDao()
    setupConstants(context!!)
    mURIMatcher.addURI(AUTHORITY, "REMINDERS", REMINDERS)
    mURIMatcher.addURI(AUTHORITY, "SCHEDULED_SERVICES", SCHEDULED_SERVICES)
    mURIMatcher.addURI(AUTHORITY, "SERVICE_ALERTS", SERVICE_ALERT)

    return true
  }

  override fun getType(uri: Uri): String? {
    return if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
      uri.toString()
    } else {
      null
    }
  }

  override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                     sortOrder: String?): Cursor? {
    val type = mURIMatcher.match(uri)
    if (type == UriMatcher.NO_MATCH) {
      if (BuildConfig.DEBUG) {
        Timber.w("No match for URI: $uri")
      }

      return null
    }

    try {
      val qb = SQLiteQueryBuilder()

      when (type) {
        SCHEDULED_SERVICES -> {
          qb.tables = DbTables.SCHEDULED_SERVICES.name
        }

        SERVICE_ALERT -> {
          qb.tables = DbTables.SERVICE_ALERTS.name
        }

        REMINDERS -> {
          qb.tables = DbTables.SCHEDULED_SERVICES.name

          val sb = StringBuilder()
          sb.append(FAVOURITE).append(" > 0")

          qb.appendWhere(sb.toString())
        }
      }

      val cursor = qb.query(mDbHelper.readableDatabase, projection, selection, selectionArgs, null, null, sortOrder)
      cursor.setNotificationUri(context!!.contentResolver, uri)
      if (type == SCHEDULED_SERVICES) {
        val realtimeInfoCursor = (0 until cursor.count)
            .map {
              cursor.moveToPosition(it)
              cursor.getLong(cursor.getColumnIndex(DbFields.ID.name))
            }
            .let {
              scheduledServiceRealtimeInfoDao.getRealtimeInfoByService(it)
            }
        return JoinedCursor(cursor, realtimeInfoCursor, ID.name, "id")
      }

      return cursor
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error querying data", e)
      }

      return null
    }

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

      lateinit var table: DatabaseTable
      var fields: Array<DatabaseField>? = null
      when (type) {
        SCHEDULED_SERVICES -> {
          table = DbTables.SCHEDULED_SERVICES
          fields = arrayOf(SERVICE_TRIP_ID, STOP_CODE, JULIAN_DAY)
        }

        SERVICE_ALERT -> {
          table = DbTables.SERVICE_ALERTS
          fields = arrayOf(SCHEDULED_SERVICE_ID, HASH_CODE)
        }
      }

      val id = ProviderUtils.upsert(db, table, values, fields)

      if (id > 0) {
        val newUri = ContentUris.withAppendedId(uri, id)
        context!!.contentResolver.notifyChange(uri, null)
        context!!.contentResolver.notifyChange(REMINDERS_URI, null)
        return newUri
      } else {
        throw SQLException("Failed to insert row into $uri")
      }
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error inserting data", e)
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
      val db = mDbHelper!!.writableDatabase
      var rowsAffected = 0
      when (type) {
        SCHEDULED_SERVICES -> rowsAffected = db.delete(DbTables.SCHEDULED_SERVICES.name, sel, selArgs)
      }

      context!!.contentResolver.notifyChange(uri, null)
      context!!.contentResolver.notifyChange(REMINDERS_URI, null)
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
        SCHEDULED_SERVICES -> rowsAffected = db.update(DbTables.SCHEDULED_SERVICES.name, values, sel, selArgs)
        SERVICE_ALERT -> rowsAffected = db.update(DbTables.SERVICE_ALERTS.name, values, sel, selArgs)
      }

      context!!.contentResolver.notifyChange(uri, null)
      context!!.contentResolver.notifyChange(REMINDERS_URI, null)
      return rowsAffected
    } catch (e: Exception) {
      if (BuildConfig.DEBUG) {
        Timber.w("Error updating data", e)
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

        lateinit var table: DatabaseTable
        var fields: Array<DatabaseField>? = null

        when (type) {
          SCHEDULED_SERVICES -> table = DbTables.SCHEDULED_SERVICES
          SERVICE_ALERT -> {
            table = DbTables.SERVICE_ALERTS
            fields = arrayOf(SCHEDULED_SERVICE_ID, HASH_CODE)
          }
          else -> return 0
        }

        for (value in values) {
          ProviderUtils.upsert(db, table, value, fields)
        }

        db.setTransactionSuccessful()

        context!!.contentResolver.notifyChange(uri, null)

        numInserted = values?.size ?: 0
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

companion object {
  lateinit var AUTHORITY: String
  private lateinit var BASE_URI : Uri
  lateinit var REMINDERS_URI: Uri
  lateinit var SCHEDULED_SERVICES_URI : Uri
  lateinit var SERVICE_ALERT_URI : Uri

  private val mURIMatcher = UriMatcher(UriMatcher.NO_MATCH)
  private val REMINDERS = 0x1
  private val SCHEDULED_SERVICES = 0x2
  private val SERVICE_ALERT = 0x3
  fun setupConstants(context: Context) {
    AUTHORITY = context!!.packageName + TripKitUI.AUTHORITY_END + TimetableProvider::class.java.simpleName
    BASE_URI = Uri.parse("content://$AUTHORITY")
    REMINDERS_URI = Uri.withAppendedPath(BASE_URI, "REMINDERS")
    SCHEDULED_SERVICES_URI = Uri.withAppendedPath(BASE_URI, "SCHEDULED_SERVICES")
    SERVICE_ALERT_URI = Uri.withAppendedPath(BASE_URI, "SERVICE_ALERTS")
  }
}
}