package com.skedgo.tripkit.ui.core

import android.content.Context
import com.google.android.gms.common.util.CollectionUtils
import com.google.gson.Gson
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.locations.LocationsResponse
import com.skedgo.tripkit.data.locations.StopsFetcher
import com.skedgo.tripkit.data.locations.StopsFetcher.IStopsPersistor
import com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity
import com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity
import com.skedgo.tripkit.ui.map.ScheduledStopRepository
import timber.log.Timber
import java.util.Random
import javax.inject.Inject

class StopsPersistor @Inject constructor(
    private val appContext: Context,
    private val scheduledStopRepository: ScheduledStopRepository,
    private val gson: Gson
) : IStopsPersistor {

    companion object {
        private const val INSERT_BATCH_SIZE = 100
    }

    override fun saveStopsSync(cells: List<LocationsResponse.Group>) {
        println("DEBUG: StopsPersistor.saveStopsSync called with ${cells.size} cells")
        
        val scheduledStops = mutableListOf<ScheduledStopEntity>()
        val locations = mutableListOf<LocationEntity>()

        for (cell in cells) {
            val cellId = cell.key
            val stops = cell.stops
            println("DEBUG: Processing cell: $cellId with ${stops?.size ?: 0} stops")
            
            if (cellId.isNullOrEmpty() || stops.isNullOrEmpty()) {
                println("DEBUG: Skipping cell $cellId - empty or null")
                continue
            }

            createEntities(
                cellId,
                getCodeToIdMapping(cellId),
                stops,
                scheduledStops,
                locations
            )
        }

        println("DEBUG: Created ${scheduledStops.size} scheduled stops and ${locations.size} locations")

        val coreCount = Runtime.getRuntime().availableProcessors()
        val sleepTime = when {
            coreCount >= 4 -> 0L
            coreCount >= 2 -> 200L
            else -> 600L
        }

        if (scheduledStops.isNotEmpty() && locations.isNotEmpty()) {
            println("DEBUG: Starting batch insertion")
            insertInBatches(scheduledStops, locations, sleepTime)
            println("DEBUG: Batch insertion completed")
        } else {
            println("DEBUG: No data to insert - scheduledStops: ${scheduledStops.size}, locations: ${locations.size}")
        }
    }

    private fun createEntities(
        cellCode: String?,
        codeToIdMap: Map<String, Int>?,
        stops: MutableList<ScheduledStop>,
        scheduledStops: MutableList<ScheduledStopEntity>,
        locations: MutableList<LocationEntity>
    ) {
        if (cellCode != null && stops.isNotEmpty()) {
            val random = Random(System.currentTimeMillis())
            val iterator = stops.iterator()
            while (iterator.hasNext()) {
                val stop = iterator.next()
                val stopCode = stop.code
                if (stopCode.isNullOrEmpty()) {
                    iterator.remove()
                    continue
                }
                
                val existingId = codeToIdMap?.get(stopCode)
                val parentStopId = existingId ?: random.nextInt(Int.MAX_VALUE)

                val parentStopEntity = ScheduledStopEntity(
                    code = stopCode,
                    cellCode = cellCode,
                    stopType = stop.type?.toString(),
                    shortName = stop.shortName,
                    services = stop.services,
                    parentId = null,
                    isParent = if (stop.hasChildren()) 1 else 0,
                    modeInfo = gson.toJson(stop.modeInfo),
                    filter = null
                )
                scheduledStops.add(parentStopEntity)

                val parentLocationEntity = LocationEntity(
                    name = stop.name,
                    address = stop.address,
                    lat = stop.lat,
                    lon = stop.lon,
                    scheduledStopCode = stopCode,
                    exact = if (stop.exact) 1 else 0,
                    bearing = stop.bearing,
                    favourite = if (stop.isFavourite) 1 else 0,
                    favouriteSortOrderPosition = stop.favouriteSortOrderIndex,
                    hasCar = 0, // Not available on Location class
                    hasMotorbike = 0, // Not available on Location class
                    hasTaxi = 0, // Not available on Location class
                    hasBicycle = 0, // Not available on Location class
                    hasPubTrans = 1, // Default value
                    locationType = stop.locationType,
                    isDynamic = 0 // Not available on Location class
                )
                
                // Debug: Print the actual coordinate values being stored
                println("DEBUG: Creating LocationEntity for stop $stopCode:")
                println("  - lat: ${stop.lat}")
                println("  - lon: ${stop.lon}")
                println("  - name: ${stop.name}")
                println("  - address: ${stop.address}")
                
                locations.add(parentLocationEntity)

                if (stop.hasChildren()) {
                    for (child in stop.children.orEmpty()) {
                        val childCode = child.code
                        if (childCode.isNullOrEmpty()) {
                            continue
                        }
                        
                        val childExistingId = codeToIdMap?.get(childCode)
                        val childStopId = childExistingId ?: random.nextInt(Int.MAX_VALUE)

                        val childStopEntity = ScheduledStopEntity(
                            code = childCode,
                            cellCode = cellCode,
                            stopType = child.type?.toString(),
                            shortName = child.shortName,
                            services = child.services,
                            parentId = parentStopId.toString(),
                            isParent = 0,
                            modeInfo = null,
                            filter = null
                        )
                        scheduledStops.add(childStopEntity)

                        val childLocationEntity = LocationEntity(
                            name = child.name,
                            address = child.address,
                            lat = child.lat,
                            lon = child.lon,
                            exact = 1,
                            bearing = child.bearing,
                            favourite = 0,
                            favouriteSortOrderPosition = 0,
                            hasCar = 0,
                            hasMotorbike = 0,
                            hasTaxi = 0,
                            hasBicycle = 0,
                            hasPubTrans = 1,
                            scheduledStopCode = childCode,
                            locationType = Location.TYPE_SCHEDULED_STOP,
                            isDynamic = 0
                        )
                        locations.add(childLocationEntity)
                    }
                }
            }
        }
    }

    private fun insertInBatches(
        scheduledStops: List<ScheduledStopEntity>,
        locations: List<LocationEntity>,
        sleep: Long
    ) {
        var counter = 0
        var continueLoop = true

        while (continueLoop) {
            val startIndex = counter * INSERT_BATCH_SIZE
            var endIndex = (++counter) * INSERT_BATCH_SIZE
            if (endIndex > scheduledStops.size) {
                continueLoop = false
                endIndex = scheduledStops.size
            }

            val stopSubList = scheduledStops.subList(startIndex, endIndex)
            val locationSubList = locations.subList(startIndex, endIndex)
            
            try {
                scheduledStopRepository.bulkInsertEntities(stopSubList)
                scheduledStopRepository.bulkInsertLocationEntities(locationSubList)
                
                Timber.d("Inserted batch: ${stopSubList.size} stops, ${locationSubList.size} locations")
            } catch (e: Exception) {
                Timber.e(e, "Error inserting batch")
                throw e
            }

            if (sleep > 0) {
                try {
                    Thread.sleep(sleep)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun getCodeToIdMapping(cellCode: String): Map<String, Int> {
        // Since we're now using Room, we should query the Room database instead of ContentProvider
        // For now, return empty map to avoid breaking existing logic
        // TODO: Update this to use Room database query
        return emptyMap()
    }
}