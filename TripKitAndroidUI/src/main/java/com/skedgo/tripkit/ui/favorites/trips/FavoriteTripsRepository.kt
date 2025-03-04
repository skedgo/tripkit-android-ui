package com.skedgo.tripkit.ui.favorites.trips

import kotlinx.coroutines.flow.Flow

@Deprecated("use [com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository]")
interface FavoriteTripsRepository {
    suspend fun saveFavoriteTrip(favoriteTrip: FavoriteTrip)
    suspend fun updateFavoriteTrip(favoriteTrip: FavoriteTrip)
    suspend fun deleteFavoriteTrip(favoriteTrip: FavoriteTrip)
    fun getAllFavoriteTrips(): Flow<List<FavoriteTrip>>
    suspend fun isFavoriteTrip(tripId: String): Boolean
    suspend fun isWayPointTrip(tripGroupId: String): Boolean
}