package com.skedgo.tripkit.ui.favorites.trips

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map


class FavoriteTripsRepositoryImpl(private val db: FavoriteTripsDataBase) : FavoriteTripsRepository {
    override suspend fun saveFavoriteTrip(favoriteTrip: FavoriteTrip) {
        db.runInTransaction {
            db.favoriteTripsDao().insert(favoriteTrip.toFavoriteTripEntity())
            favoriteTrip.waypoints.forEachIndexed { index, waypoint ->
                db.waypointsDao().insert(waypoint.toWaypointEntity(favoriteTrip.uuid, index))
            }

        }
    }

    override suspend fun updateFavoriteTrip(favoriteTrip: FavoriteTrip) {
        db.favoriteTripsDao().update(favoriteTrip.toFavoriteTripEntity())
    }

    override suspend fun deleteFavoriteTrip(favoriteTrip: FavoriteTrip) {
        db.favoriteTripsDao().delete(favoriteTrip.toFavoriteTripEntity())
    }

    override fun getAllFavoriteTrips(): Flow<List<FavoriteTrip>> {
        return db.favoriteTripsDao().getAll()
                .distinctUntilChanged()
                .map {
                    it.map {
                        it.toFavoriteTrip(
                                db.waypointsDao().getWaypointsByFavoriteTrip(it.uuid)
                                        .map { it.toWaypoint() })
                    }
        }
    }

    override suspend fun isFavoriteTrip(tripId: String): Boolean {
        return db.favoriteTripsDao().countFavoriteTripById(tripId)
    }

    override suspend fun isWayPointTrip(tripGroupId: String): Boolean {
        return db.favoriteTripsDao().countWaypointTripGroupsById(tripGroupId) > 0
    }
}