package com.skedgo.tripkit.ui.favorites.waypoints

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WaypointsDao {

  @Query("SELECT * from waypoints")
  suspend fun getAllWaypoints(): List<WaypointEntity>
  @Query("SELECT * from waypoints WHERE tripId = :tripId")
  fun getWaypointsByTrip(tripId: String): List<WaypointEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(waypoint: WaypointEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(waypoints: List<WaypointEntity>)

  @Query("DELETE from waypoints WHERE tripId = :tripId")
  fun deleteTripWaypoints(tripId: String)
}