package com.skedgo.tripkit.ui.map

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.sqlite.Cursors
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.database.DbHelper
import com.skedgo.tripkit.data.database.DbTables
import com.skedgo.tripkit.ui.data.CursorToStopConverter
import com.skedgo.tripkit.ui.utils.ProviderUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ScheduledStopRepository @Inject constructor(
        val dbHelper: DbHelper,
        val cursorToStopConverter: CursorToStopConverter) {

    val changes: PublishRelay<Unit> = PublishRelay.create()

    fun queryStopsSync(projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, order: String?): Cursor {
        val qb = SQLiteQueryBuilder()
        val b = StringBuilder()
        b.append(DbTables.SCHEDULED_STOPS).append(" INNER JOIN ")
                .append(DbTables.LOCATIONS).append(" ON ")
                .append(DbTables.SCHEDULED_STOPS).append(".")
                .append(DbFields.CODE).append(" = ")
                .append(DbTables.LOCATIONS).append(".")
                .append(DbFields.SCHEDULED_STOP_CODE)
        b.append(" INNER JOIN ")
                .append(DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY)
                .append(" ON ")
                .append(DbTables.SCHEDULED_STOP_DOWNLOAD_HISTORY)
                .append(".").append(DbFields.CELL_CODE).append(" = ")
                .append(DbTables.SCHEDULED_STOPS).append(".")
                .append(DbFields.CELL_CODE)

        qb.tables = b.toString()
        return qb.query(dbHelper.readableDatabase, projection, selection, selectionArgs, null, null, order)
    }

    fun queryStops(projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, order: String?): Observable<List<ScheduledStop>> {
        return Observable
                .fromCallable {
                    queryStopsSync(projection, selection, selectionArgs, order)
                }
                .flatMap(Cursors.flattenCursor())
                .map { cursorToStopConverter.apply(it) }
                .toList().toObservable()
                .subscribeOn(Schedulers.io())
    }

    fun insertStops(contentValues: ContentValues): Completable {
        return Completable
                .fromAction {
                    ProviderUtils.upsert(dbHelper.writableDatabase, DbTables.SCHEDULED_STOPS, contentValues, DbFields.CODE)
                }
                .andThen(Completable.fromAction {
                    notifyChange()
                })
                .subscribeOn(Schedulers.io())

    }

    fun delete(selection: String?, selectionArgs: Array<String>?): Completable {
        return Completable
                .fromAction {
                    dbHelper.writableDatabase.delete(DbTables.SCHEDULED_STOPS.name, selection, selectionArgs)
                }
                .andThen(Completable.fromAction {
                    notifyChange()
                })
                .subscribeOn(Schedulers.io())

    }

    fun update(selection: String?, selectionArgs: Array<String>?, contentValues: ContentValues): Completable {
        return Completable
                .fromAction {
                    dbHelper.writableDatabase.update(DbTables.SCHEDULED_STOPS.name, contentValues, selection, selectionArgs);
                }
                .andThen(Completable.fromAction {
                    notifyChange()
                })
                .subscribeOn(Schedulers.io())
    }

    fun bulkInsert(contentValues: Array<ContentValues>): Int {
        val database = dbHelper.writableDatabase
        database.beginTransaction()
        var rowCount = 0
        try {
            contentValues.forEach {
                ProviderUtils.upsert(database, DbTables.SCHEDULED_STOPS, it, DbFields.CODE)
                rowCount += 1
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        notifyChange()
        return rowCount
    }

    fun notifyChange() {
        changes.accept(Unit)
    }
}