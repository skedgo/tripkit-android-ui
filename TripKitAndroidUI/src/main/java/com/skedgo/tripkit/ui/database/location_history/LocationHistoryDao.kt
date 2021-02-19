package com.skedgo.tripkit.ui.database.location_history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skedgo.tripkit.ui.database.location_history.LocationHistoryEntity
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface LocationHistoryDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insert(entities: List<LocationHistoryEntity>): Completable

  @Query("SELECT * from location_history")
  fun getAllLocationInHistory(): Single<List<LocationHistoryEntity>>

  @Query("SELECT * from location_history WHERE createdAt > :startTimestamp")
  fun getLocationInHistory(startTimestamp: Long): Single<List<LocationHistoryEntity>>

  @Query("DELETE FROM location_history WHERE createdAt < :startTimestamp")
  fun deleteOldHistory(startTimestamp: Long): Completable
}