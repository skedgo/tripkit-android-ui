package com.skedgo.tripkit.ui.core

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.google.android.gms.common.util.CollectionUtils
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.database.DbFields
import com.skedgo.tripkit.data.locations.LocationsResponse
import com.skedgo.tripkit.data.locations.LocationsResponse.Group
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.locations.StopsFetcher.IStopsPersistor
import com.skedgo.tripkit.ui.map.ScheduledStopRepository
import com.skedgo.tripkit.ui.provider.ScheduledStopsProvider
import timber.log.Timber
import java.util.Arrays
import java.util.Random

class StopsPersistor(
    private val appContext: Context,
    private val gson: Gson,
    private val scheduledStopRepository: ScheduledStopRepository
) : StopsFetcher.IStopsPersistor {

    companion object {
        private const val INSERT_BATCH_SIZE = 300
    }

    override fun saveStopsSync(cells: List<LocationsResponse.Group>) {
        val stopValuesList = mutableListOf<ContentValues>()
        val locationValuesList = mutableListOf<ContentValues>()

        for (cell in cells) {
            val cellId = cell.key
            val stops = cell.stops
            if (cellId.isNullOrEmpty() || stops.isNullOrEmpty()) {
                continue
            }

            createContentValues(
                cellId,
                getCodeToIdMapping(cellId),
                stops,
                stopValuesList,
                locationValuesList
            )
        }

        val coreCount = Runtime.getRuntime().availableProcessors()
        val sleepTime = when {
            coreCount >= 4 -> 0L
            coreCount >= 2 -> 200L
            else -> 600L
        }

        if (stopValuesList.size == locationValuesList.size) {
            insertInBatches(stopValuesList, locationValuesList, sleepTime)
            stopValuesList.clear()
            locationValuesList.clear()
        }
    }

    private fun createContentValues(
        cellCode: String?,
        codeToIdMap: Map<String, Int>?,
        stops: MutableList<ScheduledStop>,
        scheduledStopValues: MutableList<ContentValues>,
        locationValues: MutableList<ContentValues>
    ) {
        if (cellCode != null && stops.isNotEmpty()) {
            val random = Random(System.currentTimeMillis())
            val iterator = stops.iterator()

            while (iterator.hasNext()) {
                val stop = iterator.next()
                val existingId = codeToIdMap?.get(stop.code)
                val parentStopId = existingId ?: random.nextInt(Int.MAX_VALUE)

                val parentStopValues = ContentValues(8).apply {
                    put(DbFields.ID.name, parentStopId)
                    put(DbFields.STOP_TYPE.name, stop.type?.toString())
                    put(DbFields.CELL_CODE.name, cellCode)
                    put(DbFields.CODE.name, stop.code)
                    put(DbFields.SHORT_NAME.name, stop.shortName)
                    put(DbFields.SERVICES.name, stop.services)
                    put(DbFields.MODE_INFO.name, gson.toJson(stop.modeInfo))
                    put(DbFields.IS_PARENT.name, if (stop.hasChildren()) 1 else 0)
                }
                scheduledStopValues.add(parentStopValues)

                val parentLocationValues = ContentValues(9).apply {
                    put(DbFields.SCHEDULED_STOP_CODE.name, stop.code)
                    put(DbFields.NAME.name, stop.name)
                    put(DbFields.ADDRESS.name, stop.address)
                    put(DbFields.LAT.name, stop.lat)
                    put(DbFields.LON.name, stop.lon)
                    put(DbFields.BEARING.name, stop.bearing)
                    put(DbFields.LOCATION_TYPE.name, Location.TYPE_SCHEDULED_STOP)
                    put(DbFields.EXACT.name, 1)
                    put(DbFields.IS_DYNAMIC.name, 0)
                }
                locationValues.add(parentLocationValues)

                if (stop.hasChildren()) {
                    for (child in stop.children.orEmpty()) {
                        val childExistingId = codeToIdMap?.get(child.code)
                        val childStopId = childExistingId ?: random.nextInt(Int.MAX_VALUE)

                        val childStopValues = ContentValues(9).apply {
                            put(DbFields.ID.name, childStopId)
                            put(DbFields.PARENT_ID.name, parentStopId)
                            put(DbFields.IS_PARENT.name, 0)
                            put(DbFields.STOP_TYPE.name, child.type?.toString())
                            put(DbFields.CELL_CODE.name, cellCode)
                            put(DbFields.CODE.name, child.code)
                            put(DbFields.SHORT_NAME.name, child.shortName)
                            put(DbFields.SERVICES.name, child.services)
                        }
                        scheduledStopValues.add(childStopValues)

                        val childLocationValues = ContentValues(9).apply {
                            put(DbFields.SCHEDULED_STOP_CODE.name, child.code)
                            put(DbFields.NAME.name, child.name)
                            put(DbFields.ADDRESS.name, child.address)
                            put(DbFields.LAT.name, child.lat)
                            put(DbFields.LON.name, child.lon)
                            put(DbFields.BEARING.name, child.bearing)
                            put(DbFields.LOCATION_TYPE.name, Location.TYPE_SCHEDULED_STOP)
                            put(DbFields.EXACT.name, 1)
                            put(DbFields.IS_DYNAMIC.name, 0)
                        }
                        locationValues.add(childLocationValues)
                    }
                }

                iterator.remove()
            }
        }
    }

    private fun insertInBatches(
        scheduledStopValues: List<ContentValues>,
        locationValues: List<ContentValues>,
        sleep: Long
    ) {
        var counter = 0
        var continueLoop = true

        do {
            val startIndex = counter * INSERT_BATCH_SIZE
            var endIndex = (++counter) * INSERT_BATCH_SIZE
            if (endIndex > scheduledStopValues.size) {
                continueLoop = false
                endIndex = scheduledStopValues.size
            }

            val stopSubList = scheduledStopValues.subList(startIndex, endIndex).toTypedArray()
            scheduledStopRepository.bulkInsert(stopSubList)

            val locationSubList = locationValues.subList(startIndex, endIndex).toTypedArray()
            appContext.contentResolver.bulkInsert(
                ScheduledStopsProvider.LOCATIONS_BY_SCHEDULED_STOP_URI,
                locationSubList
            )

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep)
                } catch (e: InterruptedException) {
                    Timber.e("Error while sleeping for batch insert")
                }
            }
        } while (continueLoop)
    }

    private fun getCodeToIdMapping(cellCode: String): Map<String, Int> {
        val resultMap = mutableMapOf<String, Int>()
        val cursor = appContext.contentResolver.query(
            ScheduledStopsProvider.CONTENT_URI,
            arrayOf(DbFields.CODE.name, DbFields.ID.name),
            "${DbFields.CELL_CODE} = ? AND ${DbFields.CODE} IS NOT NULL",
            arrayOf(cellCode),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    resultMap[it.getString(0)] = it.getInt(1)
                } while (it.moveToNext())
            }
        }

        return resultMap
    }
}