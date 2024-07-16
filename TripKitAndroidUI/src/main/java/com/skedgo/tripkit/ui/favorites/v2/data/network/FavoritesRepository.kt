package com.skedgo.tripkit.ui.favorites.v2.data.network

import com.skedgo.TripKit
import com.skedgo.network.Resource
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteDaoV2
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2
import com.skedgo.tripkit.ui.utils.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import javax.inject.Inject

/**
 * Repository class to handle syncing of favorites to server and local storage
 */
interface FavoritesRepository {

    fun getFavorites(): Flow<Resource<FavoriteResponse>>
    fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>>
    fun deleteFavorite(favoriteId: String): Flow<Resource<ResponseBody>>

    class FavoritesRepositoryImpl @Inject constructor(
        private val api: FavoritesApi,
        private val favoriteDao: FavoriteDaoV2
    ) : FavoritesRepository {

        private val configs: Configs by lazy { TripKit.getInstance().configs() }

        override fun getFavorites(): Flow<Resource<FavoriteResponse>> =
            flow {
                safeCall<FavoriteResponse> {
                    val response = api.getFavorites()
                    favoriteDao.insertAllFavorites(response.favorites)

                    // Check for local favorites not present in API response and upload them
                    val localFavorites = favoriteDao.getAllFavorites()
                    val localFavoritesToUpload = localFavorites.filterNot { localFavorite ->
                        response.favorites.any { it.id == localFavorite.id }
                    }
                    localFavoritesToUpload.forEach { favorite -> api.addFavorite(favorite) }

                    emit(Resource.success(data = response))
                }
            }.flowOn(Dispatchers.IO)

        override fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>> =
            flow {
                safeCall<FavoriteV2> {
                    val favorite = api.addFavorite(favoriteDto)
                    favoriteDao.insertFavorite(favorite)
                    emit(Resource.success(data = favorite))
                }
            }.flowOn(Dispatchers.IO)

        override fun deleteFavorite(favoriteId: String): Flow<Resource<ResponseBody>> =
            flow {
                safeCall<ResponseBody> {
                    favoriteDao.deleteFavoriteByObjectId(favoriteId)
                    emit(Resource.success(data = api.deleteFavorite(favoriteId)))
                }
            }.flowOn(Dispatchers.IO)
    }

}