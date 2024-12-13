package com.skedgo.tripkit.ui.core

import android.content.Context
import android.database.Cursor
import com.skedgo.sqlite.Cursors
import com.skedgo.tripkit.common.util.StringUtils
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.locations.LocationsResponse
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider
import io.reactivex.Observable

class CellsLoader(private val appContext: Context) : StopsFetcher.ICellsLoader {

    override fun loadSavedCellsAsync(cellIds: List<String>): Observable<List<LocationsResponse.Group>> {
        return querySavedCellsFromDatabaseAsync(appContext, cellIds)
            .flatMap { cursor ->
                toCellsAsync(cursor)
            }
            .toList()
            .toObservable()
    }

    private fun querySavedCellsFromDatabaseAsync(
        context: Context,
        cellIds: List<String>
    ): Observable<Cursor> {
        return Observable.create { emitter ->
            val cursor = querySavedCellsFromDatabaseSync(context, cellIds)
            if (cursor != null) {
                emitter.onNext(cursor)
            }
            emitter.onComplete()

            cursor?.close()
        }
    }

    private fun querySavedCellsFromDatabaseSync(
        context: Context,
        cellIds: List<String>
    ): Cursor? {
        val selection = "${DbFields.CELL_CODE} IN (${StringUtils.makeArgsString(cellIds.size)})"
        val selectionArgs = cellIds.toTypedArray()

        return context.contentResolver.query(
            ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
            null,
            selection,
            selectionArgs,
            null
        )
    }

    private fun toCellsAsync(cursor: Cursor): Observable<LocationsResponse.Group> {
        return Observable.just(cursor)
            .flatMap(Cursors.flattenCursor())
            .map { cursor ->
                val hashCode =
                    cursor.getLong(cursor.getColumnIndexOrThrow(DbFields.HASH_CODE_2.name))
                val cellId = cursor.getString(cursor.getColumnIndexOrThrow(DbFields.CELL_CODE.name))
                LocationsResponse.Group(hashCode, cellId)
            }
    }
}