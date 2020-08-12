package com.skedgo.tripkit.ui.provider

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.util.Log
import com.skedgo.sqlite.DatabaseField
import com.skedgo.sqlite.DatabaseTable
import com.skedgo.tripkit.data.database.DatabaseMigrator
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.database.DbTables
import com.skedgo.tripkit.data.database.TripKitDatabase.Companion.getInstance
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.utils.ProviderUtils
import timber.log.Timber
import java.sql.SQLException
import javax.inject.Inject

/**
 * @author Daniel Grech
 */
class ScheduledStopsProvider : ContentProvider() {

    lateinit var mDbHelper: DbHelper
    override fun onCreate(): Boolean {
        mDbHelper = DbHelper(context!!, "tripkit-legacy.db", DatabaseMigrator(getInstance(context!!)))
        setupConstants(context!!)
        mURIMatcher.addURI(AUTHORITY, "stopsOnly", STOPS_ONLY)
        mURIMatcher.addURI(AUTHORITY, "locations", LOCATIONS)
        mURIMatcher.addURI(AUTHORITY, "locationsByScheduledStopId",
                LOCATIONS_BY_SCHEDULED_STOP)
        mURIMatcher.addURI(AUTHORITY, "downloadHistory", DOWNLOAD_HISTORY)

        return true
    }

    override fun getType(uri: Uri): String? {
        return if (mURIMatcher.match(uri) != UriMatcher.NO_MATCH) {
            uri.toString()
        } else {
            null
        }
    }

    override fun query(uri: Uri, proj: Array<String>?, sel: String?,
                       selArgs: Array<String>?, sort: String?): Cursor? {
        val type = mURIMatcher.match(uri)
        if (type == UriMatcher.NO_MATCH) {
            if (BuildConfig.DEBUG) {
                Timber.w("No match for URI: $uri")
            }
            return null
        }
        return try {
            val qb = SQLiteQueryBuilder()
            when (type) {
                STOPS_ONLY -> {
                    qb.tables = DbTables.SCHEDULED_STOPS.name
                }
                LOCATIONS -> {
                    qb.tables = DbTables.LOCATIONS.name
                }
                DOWNLOAD_HISTORY -> {
                    qb.tables = DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.name
                }
            }
            val cursor = qb.query(mDbHelper!!.readableDatabase, proj, sel,
                    selArgs, null, null, sort)
            cursor.setNotificationUri(context!!.contentResolver, uri)
            cursor
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e("Error querying data", e)
            }
            null
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
        return try {
            val db = mDbHelper!!.writableDatabase
            var fields: Array<DatabaseField?>? = null
            var table: DatabaseTable? = null
            when (type) {
                LOCATIONS -> {
                    table = DbTables.LOCATIONS
                    fields = arrayOf(DbFields.ID)
                }
                DOWNLOAD_HISTORY -> {
                    table = DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY
                    fields = arrayOf(DbFields.CELL_CODE)
                }
            }
            val id = ProviderUtils.upsert(db, table, values, fields)
            if (id > 0) {
                val newUri = ContentUris.withAppendedId(uri, id)
                context!!.contentResolver.notifyChange(uri, null)
                newUri
            } else {
                throw SQLException("Failed to insert row into " + uri
                        + " values=" + values)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e("Error inserting data", e)
            }
            null
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
        return try {
            val db = mDbHelper!!.writableDatabase
            var rowsAffected = 0
            when (type) {
                DOWNLOAD_HISTORY -> rowsAffected = db.delete(
                        DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.name, sel,
                        selArgs)
                LOCATIONS_BY_SCHEDULED_STOP -> rowsAffected = db.delete(DbTables.LOCATIONS.name, sel, selArgs)
            }
            context!!.contentResolver.notifyChange(uri, null)
            rowsAffected
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e("Error deleting data", e)
            }
            0
        }
    }

    override fun update(uri: Uri, values: ContentValues?,
                        sel: String?, selArgs: Array<String>?): Int {
        val type = mURIMatcher.match(uri)
        if (type == UriMatcher.NO_MATCH) {
            if (BuildConfig.DEBUG) {
                Timber.w("No match for URI: $uri")
            }
            return 0
        }
        return try {
            val db = mDbHelper!!.writableDatabase
            var rowsAffected = 0
            when (type) {
                DOWNLOAD_HISTORY -> rowsAffected = db.update(
                        DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY.name,
                        values, sel, selArgs)
            }
            context!!.contentResolver.notifyChange(uri, null)
            rowsAffected
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e("Error updating data", e)
            }
            0
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
        return try {
            val db = mDbHelper!!.writableDatabase
            var numInserted = 0
            numInserted = try {
                db.beginTransaction()
                if (values != null) {
                    for (value in values) {
                        when (type) {
                            LOCATIONS -> ProviderUtils.upsert(db, DbTables.LOCATIONS, value,
                                    DbFields.ID)
                            LOCATIONS_BY_SCHEDULED_STOP -> ProviderUtils.upsert(db, DbTables.LOCATIONS, value,
                                    DbFields.SCHEDULED_STOP_CODE)
                            DOWNLOAD_HISTORY -> ProviderUtils.upsert(db,
                                    DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY,
                                    value, DbFields.CELL_CODE)
                        }
                    }
                }
                db.setTransactionSuccessful()
                context!!.contentResolver.notifyChange(uri, null)
                values?.size ?: 0
            } finally {
                db.endTransaction()
            }
            numInserted
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Timber.e("Error inserting data", e)
            }
            0
        }
    }

    companion object {
        lateinit var AUTHORITY : String

        private lateinit var BASE_URI: Uri
        lateinit var CONTENT_URI : Uri

        /**
         * Supports insertion only - used for bulk transactions when inserting stops
         */
        lateinit var LOCATIONS_URI : Uri

        /**
         * Only valid whilst bulkInserting.
         *
         *
         * Groups locations by their scheduled_stop_id rather than id
         */
        lateinit var LOCATIONS_BY_SCHEDULED_STOP_URI : Uri
        lateinit var DOWNLOAD_HISTORY_URI : Uri

        private val mURIMatcher = UriMatcher(
                UriMatcher.NO_MATCH)
        private const val DOWNLOAD_HISTORY = 0x2
        private const val LOCATIONS = 0x3
        private const val STOPS_ONLY = 0x4
        private const val LOCATIONS_BY_SCHEDULED_STOP = 0x5

        fun setupConstants(context: Context) {
            AUTHORITY =  context.packageName + TripKitUI.AUTHORITY_END + ScheduledStopsProvider::class.java.simpleName
            BASE_URI = Uri.parse("content://$AUTHORITY")
            CONTENT_URI = Uri.withAppendedPath(BASE_URI, "stopsOnly")
            LOCATIONS_URI = Uri.withAppendedPath(BASE_URI, "locations")
            LOCATIONS_BY_SCHEDULED_STOP_URI = Uri.withAppendedPath(BASE_URI, "locationsByScheduledStopId")
            DOWNLOAD_HISTORY_URI = Uri.withAppendedPath(BASE_URI, "downloadHistory")
        }
    }
}