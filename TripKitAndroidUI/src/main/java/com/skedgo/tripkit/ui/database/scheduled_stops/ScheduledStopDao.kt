package com.skedgo.tripkit.ui.database.scheduled_stops

import androidx.room.*
import io.reactivex.Flowable

@Dao
interface ScheduledStopDao {
    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        INNER JOIN scheduled_stops_download_history ON scheduled_stops_download_history.cellCode = scheduled_stops.cellCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
    """)
    fun getScheduledStopsWithLocation(cellCodes: List<String>): Flowable<List<ScheduledStopWithLocation>>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        INNER JOIN scheduled_stops_download_history ON scheduled_stops_download_history.cellCode = scheduled_stops.cellCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
    """)
    fun getScheduledStopsWithLocationSync(cellCodes: List<String>): List<ScheduledStopWithLocation>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        INNER JOIN scheduled_stops_download_history ON scheduled_stops_download_history.cellCode = scheduled_stops.cellCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        AND locations.lat BETWEEN :southWestLat AND :northEastLat
        AND locations.lon BETWEEN :southWestLon AND :northEastLon
    """)
    fun getScheduledStopsWithLocationInBounds(
        cellCodes: List<String>,
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ): Flowable<List<ScheduledStopWithLocation>>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        INNER JOIN scheduled_stops_download_history ON scheduled_stops_download_history.cellCode = scheduled_stops.cellCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        AND locations.lat BETWEEN :southWestLat AND :northEastLat
        AND locations.lon BETWEEN :southWestLon AND :northEastLon
    """)
    fun getScheduledStopsWithLocationInBoundsSync(
        cellCodes: List<String>,
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ): List<ScheduledStopWithLocation>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        AND locations.lat BETWEEN :southWestLat AND :northEastLat
        AND locations.lon BETWEEN :southWestLon AND :northEastLon
    """)
    fun getScheduledStopsWithLocationInBoundsNoHistory(
        cellCodes: List<String>,
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ): List<ScheduledStopWithLocation>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
    """)
    fun getScheduledStopsWithLocationNoHistory(cellCodes: List<String>): List<ScheduledStopWithLocation>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        LIMIT :limit OFFSET :offset
    """)
    fun getScheduledStopsWithLocationNoHistoryPaginated(
        cellCodes: List<String>,
        limit: Int,
        offset: Int
    ): List<ScheduledStopWithLocation>

    @Transaction
    @Query("""
        SELECT * FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        AND locations.lat BETWEEN :southWestLat AND :northEastLat
        AND locations.lon BETWEEN :southWestLon AND :northEastLon
        LIMIT :limit OFFSET :offset
    """)
    fun getScheduledStopsWithLocationInBoundsNoHistoryPaginated(
        cellCodes: List<String>,
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double,
        limit: Int,
        offset: Int
    ): List<ScheduledStopWithLocation>

    @Query("""
        SELECT COUNT(*) FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
    """)
    fun getScheduledStopsCount(cellCodes: List<String>): Int

    @Query("""
        SELECT COUNT(*) FROM scheduled_stops 
        INNER JOIN locations ON scheduled_stops.code = locations.scheduledStopCode
        WHERE scheduled_stops.cellCode IN (:cellCodes)
        AND scheduled_stops.parentId IS NULL
        AND locations.lat BETWEEN :southWestLat AND :northEastLat
        AND locations.lon BETWEEN :southWestLon AND :northEastLon
    """)
    fun getScheduledStopsWithBoundsCount(
        cellCodes: List<String>,
        southWestLat: Double,
        southWestLon: Double,
        northEastLat: Double,
        northEastLon: Double
    ): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScheduledStop(scheduledStop: ScheduledStopEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocation(location: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDownloadHistory(downloadHistory: ScheduledStopDownloadHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScheduledStops(scheduledStops: List<ScheduledStopEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocations(locations: List<LocationEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDownloadHistories(downloadHistories: List<ScheduledStopDownloadHistoryEntity>): List<Long>

    @Update
    fun updateScheduledStop(scheduledStop: ScheduledStopEntity)

    @Update
    fun updateLocation(location: LocationEntity)

    @Update
    fun updateDownloadHistory(downloadHistory: ScheduledStopDownloadHistoryEntity)

    @Delete
    fun deleteScheduledStop(scheduledStop: ScheduledStopEntity)

    @Delete
    fun deleteLocation(location: LocationEntity)

    @Delete
    fun deleteDownloadHistory(downloadHistory: ScheduledStopDownloadHistoryEntity)

    @Query("DELETE FROM scheduled_stops WHERE cellCode IN (:cellCodes)")
    fun deleteScheduledStopsByCellCodes(cellCodes: List<String>): Int

    @Query("DELETE FROM locations WHERE scheduledStopCode IN (SELECT code FROM scheduled_stops WHERE cellCode IN (:cellCodes))")
    fun deleteLocationsByCellCodes(cellCodes: List<String>): Int

    @Query("DELETE FROM scheduled_stops_download_history WHERE cellCode IN (:cellCodes)")
    fun deleteDownloadHistoriesByCellCodes(cellCodes: List<String>): Int

    @Query("SELECT id FROM scheduled_stops WHERE code = :code")
    fun getStopIdByCode(code: String): Long?

    @Query("SELECT * FROM scheduled_stops")
    fun getAllScheduledStopsSync(): List<ScheduledStopEntity>

    @Query("SELECT * FROM locations")
    fun getAllLocationsSync(): List<LocationEntity>

    @Query("SELECT * FROM scheduled_stops_download_history")
    fun getAllDownloadHistorySync(): List<ScheduledStopDownloadHistoryEntity>
}
