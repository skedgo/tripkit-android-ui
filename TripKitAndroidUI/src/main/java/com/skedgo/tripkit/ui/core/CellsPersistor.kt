package com.skedgo.tripkit.ui.core

import android.content.ContentValues
import android.content.Context
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.locations.LocationsResponse.Group
import com.skedgo.tripkit.data.locations.StopsFetcher.ICellsPersistor
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider

class CellsPersistor(private val appContext: Context) : ICellsPersistor {
    override fun saveCellsSync(cells: List<Group>) {
        if (cells.isEmpty()) return

        // Persist metadata for ALL cells in the response (including those with no stops/POIs).
        // Skipping empty cells caused sparse regions (e.g. AU_NT_Darwin) to be re-fetched on
        // every viewport change because the cache never recorded that we already asked for them.
        // Cells without a key are still skipped because the cache is keyed by cellCode.
        val values = cells.mapNotNull { cell ->
            val key = cell.key ?: return@mapNotNull null
            ContentValues(3).apply {
                put(DbFields.CELL_CODE.name, key)
                put(DbFields.HASH_CODE_2.name, cell.hashCode)
                put(DbFields.DOWNLOAD_TIME.name, System.currentTimeMillis())
            }
        }
        if (values.isEmpty()) return

        appContext.contentResolver.bulkInsert(
            ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
            values.toTypedArray()
        )
    }
}