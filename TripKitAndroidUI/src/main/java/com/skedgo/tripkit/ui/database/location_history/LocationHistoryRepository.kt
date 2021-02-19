package com.skedgo.tripkit.ui.database.location_history

import com.skedgo.tripkit.common.model.Location
import io.reactivex.Completable
import io.reactivex.Single

import javax.inject.Inject

open class LocationHistoryRepository @Inject constructor(
        private val locationHistoryDao: LocationHistoryDao,
        private val mapper: LocationHistoryMapper
) {
    fun saveLocationsToHistory(locations: List<Location>): Completable {
        val entities = mapper.toEntity(locations)
        return locationHistoryDao.insert(entities)
    }

    fun getLocationHistory(): Single<List<Location>> {
        return locationHistoryDao.getAllLocationInHistory()
                .map { mapper.toLocation(it) }
    }

    fun getLatestLocationHistory(startTimestamp: Long): Single<List<Location>> {
        return locationHistoryDao.deleteOldHistory(startTimestamp)
                .andThen(locationHistoryDao.getLocationInHistory(startTimestamp))
                .map { mapper.toLocation(it) }
    }
}
