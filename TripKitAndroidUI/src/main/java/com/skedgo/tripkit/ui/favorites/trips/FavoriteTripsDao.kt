package com.skedgo.tripkit.ui.favorites.trips

import androidx.room.*
import androidx.room.OnConflictStrategy.FAIL
import androidx.room.OnConflictStrategy.REPLACE
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTripsDao {
  @Query("SELECT * from favoriteTrips")
  fun getAll(): Flow<List<FavoriteTripEntity>>

  @Insert(onConflict = REPLACE)
  fun insert(favoriteTrip: FavoriteTripEntity)

  @Delete
  suspend fun delete(favoriteTrip: FavoriteTripEntity)

  @Update
  suspend fun update(favoriteTrip: FavoriteTripEntity)

  @Query("SELECT COUNT(*) FROM favoriteTrips WHERE tripGroupId == :tripGroupId")
  suspend fun countWaypointTripGroupsById(tripGroupId: String): Int

  @Query("SELECT COUNT(*) FROM favoriteTrips WHERE uuid == :tripGroupId")
  suspend fun countFavoriteTripById(tripGroupId: String): Boolean
}

@Dao
interface WaypointsDao {
  @Query("SELECT * from waypoints WHERE tripId == :tripId ORDER BY `order` ASC")
  suspend fun getWaypointsByFavoriteTrip(tripId: String): List<WaypointEntity>

  @Insert(onConflict = FAIL)
  fun insert(waypoint: WaypointEntity)
}