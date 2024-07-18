package com.skedgo.tripkit.ui.favorites.v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteType.home
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteType.work

@Dao
interface FavoriteDaoV2 {

    @Query("SELECT * FROM favorites_v2 ORDER BY CASE WHEN type = :homeType THEN 0 WHEN TYPE = :workType THEN 1 ELSE 2 END")
    suspend fun getFavoritesSorted(
        homeType: FavoriteType = home,
        workType: FavoriteType = work
    ): FavoriteV2

    @Query("SELECT * FROM favorites_v2 WHERE type != :homeType AND type != :workType ORDER BY uuid")
    suspend fun getFavoritesExcludingWorkAndHome(
        homeType: FavoriteType = home,
        workType: FavoriteType = work
    ): List<FavoriteV2>

    @Query("SELECT * FROM favorites_v2 WHERE type = :type")
    suspend fun getFavoriteOfType(type: FavoriteType): FavoriteV2?

    @Query("DELETE FROM favorites_v2 WHERE uuid = :id")
    suspend fun deleteFavoriteByObjectId(id: String)

    @Query("DELETE FROM favorites_v2 WHERE type = :type")
    suspend fun deleteFavoriteType(type: FavoriteType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFavorites(favorites: List<FavoriteV2>)

    @Query("SELECT * FROM favorites_v2 WHERE uuid = :id")
    suspend fun getFavoriteById(id: String): FavoriteV2

    @Query("SELECT * FROM favorites_v2 WHERE stopCode = :code")
    suspend fun getFavoriteByStopCode(code: String): FavoriteV2

    @Query("SELECT * from favorites_v2")
    suspend fun getAllFavorites(): List<FavoriteV2>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites_v2 WHERE uuid = :uuid)")
    suspend fun favoriteExists(uuid: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites_v2 WHERE stopCode = :code)")
    suspend fun favoriteStopExists(code: String): Boolean

    @Query(
        "SELECT * from favorites_v2 WHERE name LIKE :query OR location LIKE :query OR " +
            "start LIKE :query OR `end` LIKE :query"
    )
    suspend fun getFavoritesByTerm(query: String): List<FavoriteV2>

    @Transaction
    suspend fun deleteAndInsertFavorite(type: FavoriteType, entity: FavoriteV2) {
        deleteFavoriteType(type)
        insertFavorite(entity)
    }

}

