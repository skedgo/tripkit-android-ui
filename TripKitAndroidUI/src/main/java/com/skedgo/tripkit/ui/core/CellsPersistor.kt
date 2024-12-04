package com.skedgo.tripkit.ui.core

import android.content.ContentValues
import android.content.Context
import com.google.android.gms.common.util.CollectionUtils
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.locations.LocationsResponse.Group
import com.skedgo.tripkit.data.locations.StopsFetcher.ICellsPersistor
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider

class CellsPersistor(private val appContext: Context) : ICellsPersistor {
    override fun saveCellsSync(cells: List<Group>) {
        val valuesArray = arrayOfNulls<ContentValues>(cells.size)

        var i = 0
        for (cell in cells) {
            if (CollectionUtils.isEmpty(cell.stops)) {
                continue
            }

            val values = ContentValues(3)
            values.put(DbFields.CELL_CODE.name, cell.key)
            values.put(DbFields.HASH_CODE_2.name, cell.hashCode)
            values.put(DbFields.DOWNLOAD_TIME.name, System.currentTimeMillis())

            valuesArray[i++] = values
        }

        appContext.contentResolver.bulkInsert(
            ScheduledStopsProvider.DOWNLOAD_HISTORY_URI,
            valuesArray
        )
    }
}