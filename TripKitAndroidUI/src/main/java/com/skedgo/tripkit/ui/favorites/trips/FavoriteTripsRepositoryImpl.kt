package com.skedgo.tripkit.ui.favorites.trips

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class FavoriteTripsRepositoryImpl(private val db: FavoriteTripsDataBase) : FavoriteTripsRepository {
    override suspend fun saveFavoriteTrip(favoriteTrip: FavoriteTrip) {
    }

    override suspend fun updateFavoriteTrip(favoriteTrip: FavoriteTrip) {
        db.favoriteTripsDao().update(favoriteTrip.toFavoriteTripEntity())
    }

    override suspend fun deleteFavoriteTrip(favoriteTrip: FavoriteTrip) {
        db.favoriteTripsDao().delete(favoriteTrip.toFavoriteTripEntity())
    }

    override fun getAllFavoriteTrips(): Flow<List<FavoriteTrip>> {
        return flow { emit(emptyList()) }
    }

    override suspend fun isFavoriteTrip(tripId: String): Boolean {
        return db.favoriteTripsDao().countFavoriteTripById(tripId)
    }

    override suspend fun isWayPointTrip(tripGroupId: String): Boolean {
        return db.favoriteTripsDao().countWaypointTripGroupsById(tripGroupId) > 0
    }
}