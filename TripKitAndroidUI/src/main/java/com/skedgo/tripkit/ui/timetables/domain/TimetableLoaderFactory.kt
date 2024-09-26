package com.skedgo.tripkit.ui.timetables.domain

import android.content.Context
import android.database.Cursor
import android.text.TextUtils
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.ui.provider.TimetableProvider

class TimetableLoaderFactory {

    /**
     * Creates a loader which is used to load A2B timetable from database.
     *
     * @param segment Should be a public transport segment (aka scheduled segment)
     */
    fun createForSegment(
        context: Context,
        pairIdentifier: String,
        sinceSecs: Long
    ): CursorLoader {
        val selection =
            String.format("%s = ? AND %s > ?", DbFields.PAIR_IDENTIFIER, DbFields.START_TIME)
        val selectionArgs = arrayOf(pairIdentifier, java.lang.Long.toString(sinceSecs))
        val sortOrder = DbFields.START_TIME.toString() + " ASC"
        return CursorLoader(
            context,
            TimetableProvider.SCHEDULED_SERVICES_URI,
            null,
            selection,
            selectionArgs,
            sortOrder
        )
    }

    companion object {

        fun buildQueryParams(stopCodes: List<String>, sinceSecs: Long): TimetableQueryParams {
            val selectionStringForStopCodes = stopCodes.map { "?" }.joinToString(separator = ",")
            return TimetableQueryParams(
                selection = "${DbFields.START_TIME} > ? AND ${DbFields.STOP_CODE} IN ($selectionStringForStopCodes)",
                selectionArgs = (listOf(sinceSecs.toString()) + stopCodes).toTypedArray(),
                sortOrder = DbFields.START_TIME.toString() + " ASC"
            )
        }

        fun buildQueryParams(
            stop: ScheduledStop,
            filter: String?,
            sinceSecs: Long
        ): TimetableQueryParams {

            val selectionBuilder = StringBuilder()
            selectionBuilder.append(DbFields.START_TIME).append(" > ?")

            if (stop.isParent) {
                if (!stop.hasChildren()) {
                    // We show nothing
                    selectionBuilder.append(" AND 0=1")
                } else {
                    val stopCodes = stop.children.map { it.code }.plus(stop.code).map { "\'$it\'" }
                        .joinToString(separator = ",")
                    selectionBuilder.append(" AND ").append(DbFields.STOP_CODE)
                        .append(" IN ($stopCodes)")
                }
            } else {
                selectionBuilder.append(" AND ").append(DbFields.STOP_CODE).append(" = ?")
            }

            val args: Array<String>
            if (!TextUtils.isEmpty(filter)) {
                selectionBuilder.append(" AND (").append(DbFields.SERVICE_NAME).append(" LIKE ?")
                selectionBuilder.append(" OR ").append(DbFields.SERVICE_NUMBER).append(" LIKE ?")
                selectionBuilder.append(" OR ").append(DbFields.SEARCH_STRING).append(" LIKE ?")
                selectionBuilder.append(" OR ").append(DbFields.STOP_CODE).append(" LIKE ?)")

                val wildcardStop = "$filter%"
                val wildcard = "%$filter%"
                if (stop.isParent) {
                    args = arrayOf(sinceSecs.toString(), wildcard, wildcard, wildcard, wildcardStop)
                } else {
                    args = arrayOf(
                        sinceSecs.toString(),
                        stop.code,
                        wildcard,
                        wildcard,
                        wildcard,
                        wildcard
                    )
                }
            } else if (stop.isParent) {
                args = arrayOf(sinceSecs.toString())
            } else {
                args = arrayOf(sinceSecs.toString(), stop.code)
            }

            val selection = selectionBuilder.toString()

            return TimetableQueryParams(
                selection = selection,
                selectionArgs = args,
                sortOrder = DbFields.START_TIME.toString() + " ASC"
            )
        }

        fun createForStop(
            appContext: Context,
            stop: ScheduledStop,
            filter: String?,
            sinceSecs: Long
        ): Loader<Cursor> {
            val params = buildQueryParams(stop, filter, sinceSecs)
            return CursorLoader(
                appContext,
                TimetableProvider.SCHEDULED_SERVICES_URI, null,
                params.selection,
                params.selectionArgs,
                params.sortOrder
            )
        }
    }
}