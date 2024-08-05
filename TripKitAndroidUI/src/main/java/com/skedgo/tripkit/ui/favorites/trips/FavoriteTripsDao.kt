package com.skedgo.tripkit.ui.favorites.trips

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.FAIL
import androidx.room.OnConflictStrategy.Companion.REPLACE
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteTripsDao {

  /**
   * Updated and removed `suspend` on all functions since getting error on the generated
   * Java file. e.g. the [getWaypointsByFavoriteTrip] method is defined as an abstract method returning java.lang.Object.
   * This is a typical pattern in Kotlin when using coroutines with [Room].
   * The method is expected to be a suspend function, returning a [List<WaypointEntity>],
   * but due to the coroutine's Continuation parameter, it appears to the Java type system as returning an Object.
   * When you call this method from Kotlin, it should behave as if it's returning a List<WaypointEntity>.
   * However, if you're trying to call it from Java, or expecting it to behave like a normal Java method, that could be causing confusion.
   */

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