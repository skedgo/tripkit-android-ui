package com.skedgo.tripkit.ui.map

import android.content.ContentValues
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopDatabase
import com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopMapper
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ScheduledStopRepository @Inject constructor(
    private val scheduledStopDatabase: ScheduledStopDatabase,
    private val scheduledStopMapper: ScheduledStopMapper
) {

    val changes: PublishRelay<Unit> = PublishRelay.create()

    fun queryStopsSync(
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        order: String?
    ): List<ScheduledStop> {
        val cellCodes = extractCellCodesFromSelection(selection, selectionArgs)
        if (cellCodes.isEmpty()) {
            Timber.i("DEBUG: No cell codes found, returning empty list")
            return emptyList()
        }
        
        val bounds = extractBoundsFromSelection(selectionArgs)

        Timber.i("DEBUG: Querying with cellCodes: $cellCodes")
        Timber.i("DEBUG: Bounds: $bounds")
        
        // First, let's check if we have any data in the database at all
        val allStops = scheduledStopDatabase.scheduledStopDao().getAllScheduledStopsSync()
        val allLocations = scheduledStopDatabase.scheduledStopDao().getAllLocationsSync()
        Timber.i("DEBUG: Total stops in DB: ${allStops.size}")
        Timber.i("DEBUG: Total locations in DB: ${allLocations.size}")
        
        val entities = if (bounds != null) {
            Timber.i("DEBUG: Querying with bounds: $bounds")
            Timber.i("DEBUG: Bounds details:")
            Timber.i("  - southWestLat: ${bounds.southWestLat}")
            Timber.i("  - southWestLon: ${bounds.southWestLon}")
            Timber.i("  - northEastLat: ${bounds.northEastLat}")
            Timber.i("  - northEastLon: ${bounds.northEastLon}")
            
            val result = scheduledStopDatabase.scheduledStopDao().getScheduledStopsWithLocationInBoundsNoHistory(
                cellCodes,
                bounds.southWestLat,
                bounds.southWestLon,
                bounds.northEastLat,
                bounds.northEastLon
            )
            Timber.i("DEBUG: Query with bounds returned: ${result.size} entities")
            
            // Debug: Show first few results to see what coordinates we got
            if (result.isNotEmpty()) {
                Timber.i("DEBUG: First 3 results coordinates:")
                result.take(3).forEach { entity ->
                    Timber.i("  - Stop ${entity.scheduledStop.code}: lat=${entity.location?.lat}, lon=${entity.location?.lon}")
                }
            }
            
            result
        } else {
            Timber.i("DEBUG: Querying without bounds")
            val result = scheduledStopDatabase.scheduledStopDao().getScheduledStopsWithLocationNoHistory(cellCodes)
            Timber.i("DEBUG: Query without bounds returned: ${result.size} entities")
            result
        }

        Timber.i("DEBUG: Final entities count: ${entities.size}")
        return scheduledStopMapper.mapToDomainList(entities)
    }

    fun queryStops(
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        order: String?
    ): Observable<List<ScheduledStop>> {
        return Observable.fromCallable {
            queryStopsSync(projection, selection, selectionArgs, order)
        }.subscribeOn(Schedulers.io())
    }

    fun insertStops(contentValues: ContentValues): Completable {
        return Completable
            .fromAction {
                // Parse ContentValues and convert to entities
                val scheduledStop = parseContentValuesToScheduledStopEntity(contentValues)
                val location = parseContentValuesToLocationEntity(contentValues)
                
                // Insert with proper error handling
                try {
                    val stopId = scheduledStopDatabase.scheduledStopDao().insertScheduledStop(scheduledStop)
                    val locationId = scheduledStopDatabase.scheduledStopDao().insertLocation(location)

                    // Log for debugging
                    Timber.i("Inserted scheduled stop with ID: $stopId, location with ID: $locationId")
                    Timber.i("Stop data: code=${scheduledStop.code}, cellCode=${scheduledStop.cellCode}")
                    Timber.i("Location data: lat=${location.lat}, lon=${location.lon}, name=${location.name}")
                } catch (e: Exception) {
                    Timber.i("Error inserting data: ${e.message}")
                    throw e
                }
            }
            .andThen(Completable.fromAction {
                notifyChange()
            })
            .subscribeOn(Schedulers.io())
    }

    fun delete(selection: String?, selectionArgs: Array<String>?): Completable {
        return Completable
            .fromAction {
                val cellCodes = extractCellCodesFromSelection(selection, selectionArgs)
                if (cellCodes.isNotEmpty()) {
                    val deletedStops = scheduledStopDatabase.scheduledStopDao().deleteScheduledStopsByCellCodes(cellCodes)
                    val deletedLocations = scheduledStopDatabase.scheduledStopDao().deleteLocationsByCellCodes(cellCodes)
                    val deletedHistory = scheduledStopDatabase.scheduledStopDao().deleteDownloadHistoriesByCellCodes(cellCodes)
                    
                    Timber.i("Deleted: $deletedStops stops, $deletedLocations locations, $deletedHistory history records")
                }
            }
            .andThen(Completable.fromAction {
                notifyChange()
            })
            .subscribeOn(Schedulers.io())
    }

    fun update(
        selection: String?,
        selectionArgs: Array<String>?,
        contentValues: ContentValues
    ): Completable {
        return Completable
            .fromAction {
                // For updates, we need to find existing entities and update them
                val cellCodes = extractCellCodesFromSelection(selection, selectionArgs)
                if (cellCodes.isNotEmpty()) {
                    // Implementation depends on what fields are being updated
                    // For now, we'll just notify changes
                }
            }
            .andThen(Completable.fromAction {
                notifyChange()
            })
            .subscribeOn(Schedulers.io())
    }

    fun bulkInsert(contentValues: Array<ContentValues>): Int {
        val scheduledStops = mutableListOf<com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity>()
        val locations = mutableListOf<com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity>()
        
        contentValues.forEach { contentValue ->
            val scheduledStop = parseContentValuesToScheduledStopEntity(contentValue)
            val location = parseContentValuesToLocationEntity(contentValue)
            
            scheduledStops.add(scheduledStop)
            locations.add(location)
        }
        
        try {
            val insertedStops = scheduledStopDatabase.scheduledStopDao().insertScheduledStops(scheduledStops)
            val insertedLocations = scheduledStopDatabase.scheduledStopDao().insertLocations(locations)
            
            Timber.i("Bulk inserted: ${insertedStops.size} stops, ${insertedLocations.size} locations")
        } catch (e: Exception) {
            Timber.i("Error in bulk insert: ${e.message}")
            throw e
        }
        
        notifyChange()
        return contentValues.size
    }

    fun bulkInsertLocations(contentValues: Array<ContentValues>): Int {
        val locations = mutableListOf<com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity>()
        
        contentValues.forEach { contentValue ->
            // Debug: Print all keys and values
            Timber.i("=== ContentValues Debug ===")
            contentValue.keySet().forEach { key ->
                Timber.i("Key: '$key' = ${contentValue.get(key)}")
            }
            Timber.i("==========================")
            
            val location = parseContentValuesToLocationEntity(contentValue)
            locations.add(location)
        }
        
        try {
            val insertedLocations = scheduledStopDatabase.scheduledStopDao().insertLocations(locations)
            Timber.i("Bulk inserted locations: ${insertedLocations.size} locations")
        } catch (e: Exception) {
            Timber.i("Error in bulk insert locations: ${e.message}")
            throw e
        }
        
        notifyChange()
        return contentValues.size
    }

    // New methods for direct Room entity insertion
    fun bulkInsertEntities(entities: List<com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity>): Int {
        try {
            val insertedStops = scheduledStopDatabase.scheduledStopDao().insertScheduledStops(entities)
            Timber.i("Bulk inserted scheduled stops: ${insertedStops.size} stops")
            notifyChange()
            return insertedStops.size
        } catch (e: Exception) {
            Timber.i("Error in bulk insert entities: ${e.message}")
            throw e
        }
    }

    fun bulkInsertLocationEntities(entities: List<com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity>): Int {
        try {
            val insertedLocations = scheduledStopDatabase.scheduledStopDao().insertLocations(entities)
            Timber.i("Bulk inserted location entities: ${insertedLocations.size} locations")
            notifyChange()
            return insertedLocations.size
        } catch (e: Exception) {
            Timber.i("Error in bulk insert location entities: ${e.message}")
            throw e
        }
    }

    fun notifyChange() {
        changes.accept(Unit)
    }
    
    // Test method to verify Room is working
    fun insertTestData(): Completable {
        return Completable.fromAction {
            try {
                // Insert a test scheduled stop
                val testStop = com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity(
                    code = "TEST_STOP_001",
                    cellCode = "TEST_CELL_001",
                    stopType = "bus",
                    shortName = "Test Stop",
                    services = "1,2,3",
                    parentId = null,
                    isParent = 0,
                    modeInfo = "{\"type\":\"bus\"}",
                    filter = null
                )
                
                val testLocation = com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity(
                    name = "Test Bus Stop",
                    address = "123 Test Street",
                    lat = -33.8688, // Sydney coordinates
                    lon = 151.2093,
                    exact = 1,
                    bearing = 0,
                    favourite = 0,
                    favouriteSortOrderPosition = 0,
                    hasCar = 0,
                    hasMotorbike = 0,
                    hasTaxi = 0,
                    hasBicycle = 0,
                    hasPubTrans = 1,
                    scheduledStopCode = "TEST_STOP_001",
                    locationType = 1, // TYPE_SCHEDULED_STOP
                    isDynamic = 0
                )
                
                val stopId = scheduledStopDatabase.scheduledStopDao().insertScheduledStop(testStop)
                val locationId = scheduledStopDatabase.scheduledStopDao().insertLocation(testLocation)
                
                Timber.i("Test data inserted - Stop ID: $stopId, Location ID: $locationId")
                Timber.i("Test location: lat=${testLocation.lat}, lon=${testLocation.lon}, name='${testLocation.name}'")
                
            } catch (e: Exception) {
                Timber.i("Error inserting test data: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }.subscribeOn(Schedulers.io())
    }
    
    // Helper methods for parsing ContentValues and selection arguments (same logic as before)
    private fun extractCellCodesFromSelection(selection: String?, selectionArgs: Array<String>?): List<String> {
        if (selection == null || selectionArgs == null) {
            return emptyList()
        }
        
        // The selection pattern is: cell_code IN (?,?,...) AND lat >= ? AND lat <= ? AND lon >= ? AND lon <= ?
        // The first N arguments are cell codes, followed by 4 bounds arguments
        if (selection.contains("cell_code") && selectionArgs.size > 4) {
            val cellCodeCount = selectionArgs.size - 4
            return selectionArgs.take(cellCodeCount).toList()
        }
        
        return emptyList()
    }
    
    private fun extractBoundsFromSelection(selectionArgs: Array<String>?): Bounds? {
        if (selectionArgs == null || selectionArgs.size < 4) {
            return null
        }
        
        // The last 4 arguments are bounds: lat >= ?, lat <= ?, lon >= ?, lon <= ?
        val boundsStartIndex = selectionArgs.size - 4
        return try {
            Bounds(
                southWestLat = selectionArgs[boundsStartIndex].toDouble(),
                northEastLat = selectionArgs[boundsStartIndex + 1].toDouble(),
                southWestLon = selectionArgs[boundsStartIndex + 2].toDouble(),
                northEastLon = selectionArgs[boundsStartIndex + 3].toDouble()
            )
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    private data class Bounds(
        val southWestLat: Double,
        val northEastLat: Double,
        val southWestLon: Double,
        val northEastLon: Double
    )
    
    private fun parseContentValuesToScheduledStopEntity(contentValues: ContentValues): com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity {
        return com.skedgo.tripkit.ui.database.scheduled_stops.ScheduledStopEntity(
            code = contentValues.getAsString("code") ?: "",
            cellCode = contentValues.getAsString("cell_code") ?: "",
            stopType = contentValues.getAsString("stop_type"),
            shortName = contentValues.getAsString("short_name"),
            services = contentValues.getAsString("services"),
            parentId = contentValues.getAsString("parent_id"),
            isParent = contentValues.getAsInteger("is_parent") ?: 0,
            modeInfo = contentValues.getAsString("mode_info"),
            filter = contentValues.getAsString("filter")
        )
    }
    
    private fun parseContentValuesToLocationEntity(contentValues: ContentValues): com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity {
        // Handle both prefixed and non-prefixed field names
        val name = contentValues.getAsString("name") ?: contentValues.getAsString("locations.name")
        val address = contentValues.getAsString("address") ?: contentValues.getAsString("locations.address")
        val lat = contentValues.getAsDouble("lat") ?: contentValues.getAsDouble("locations.lat") ?: 0.0
        val lon = contentValues.getAsDouble("lon") ?: contentValues.getAsDouble("locations.lon") ?: 0.0
        val exact = contentValues.getAsInteger("exact") ?: contentValues.getAsInteger("locations.exact") ?: 0
        val bearing = contentValues.getAsInteger("bearing") ?: contentValues.getAsInteger("locations.bearing") ?: 0
        val favourite = contentValues.getAsInteger("favourite") ?: contentValues.getAsInteger("locations.favourite") ?: 0
        val favouriteSortOrderPosition = contentValues.getAsInteger("favourite_sort_order_position") ?: contentValues.getAsInteger("locations.favourite_sort_order_position") ?: 0
        val hasCar = contentValues.getAsInteger("has_car") ?: contentValues.getAsInteger("locations.has_car") ?: 0
        val hasMotorbike = contentValues.getAsInteger("has_motorbike") ?: contentValues.getAsInteger("locations.has_motorbike") ?: 0
        val hasTaxi = contentValues.getAsInteger("has_taxi") ?: contentValues.getAsInteger("locations.has_taxi") ?: 0
        val hasBicycle = contentValues.getAsInteger("has_bicycle") ?: contentValues.getAsInteger("locations.has_bicycle") ?: 0
        val hasPubTrans = contentValues.getAsInteger("has_pub_trans") ?: contentValues.getAsInteger("locations.has_pub_trans") ?: 0
        val scheduledStopCode = contentValues.getAsString("scheduled_stop_code") ?: contentValues.getAsString("locations.scheduled_stop_code")
        val locationType = contentValues.getAsInteger("location_type") ?: contentValues.getAsInteger("locations.location_type") ?: 0
        val isDynamic = contentValues.getAsInteger("is_dynamic") ?: contentValues.getAsInteger("locations.is_dynamic") ?: 0
        
        // Debug: Print extracted values
        Timber.i("=== Parsed Location Values ===")
        Timber.i("name: '$name'")
        Timber.i("address: '$address'")
        Timber.i("lat: $lat")
        Timber.i("lon: $lon")
        Timber.i("exact: $exact")
        Timber.i("bearing: $bearing")
        Timber.i("scheduledStopCode: '$scheduledStopCode'")
        Timber.i("locationType: $locationType")
        Timber.i("isDynamic: $isDynamic")
        Timber.i("=============================")
        
        return com.skedgo.tripkit.ui.database.scheduled_stops.LocationEntity(
            name = name,
            address = address,
            lat = lat,
            lon = lon,
            exact = exact,
            bearing = bearing,
            favourite = favourite,
            favouriteSortOrderPosition = favouriteSortOrderPosition,
            hasCar = hasCar,
            hasMotorbike = hasMotorbike,
            hasTaxi = hasTaxi,
            hasBicycle = hasBicycle,
            hasPubTrans = hasPubTrans,
            scheduledStopCode = scheduledStopCode,
            locationType = locationType,
            isDynamic = isDynamic
        )
    }
}