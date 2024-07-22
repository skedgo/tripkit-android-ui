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
import javax.inject.Inject

/**
 * Repository class to handle syncing of favorites to server and local storage
 */
interface FavoritesRepository {

    fun getFavorites(): Flow<Resource<FavoriteResponse>>
    fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>>
    fun deleteFavorite(favoriteId: String): Flow<Resource<Unit>>
    fun deleteFavoriteWithStopCode(stopCode: String): Flow<Resource<Unit>>
    fun isFavorite(favoriteId: String): Flow<Resource<Boolean>>
    fun isFavoriteByStopCode(stopCode: String): Flow<Resource<Boolean>>

    class FavoritesRepositoryImpl @Inject constructor(
        private val api: FavoritesApi,
        private val favoriteDao: FavoriteDaoV2,
    ) : FavoritesRepository {

        private val configs: Configs = TripKit.getInstance().configs()

        override fun getFavorites(): Flow<Resource<FavoriteResponse>> =
            flow {
                safeCall<FavoriteResponse> {
                    val userId = configs.userIdentifier()?.call()
                    if (configs.favoritesServerSyncEnabled()) {
                        val response = api.getFavorites()
                        response.result?.let {
                            favoriteDao.insertAllFavorites(
                                it.map { favorite ->
                                    favorite.userId = userId
                                    favorite
                                }
                            )
                        }

                        // Check for local favorites not present in API response and upload them
                        val localFavorites = userId?.let {
                            favoriteDao.getUserFavorites(userId)
                        } ?: favoriteDao.getAllFavorites()

                        val localFavoritesToUpload = localFavorites.filterNot { localFavorite ->
                            response.result?.any { it.uuid == localFavorite.uuid } == true
                        }
                        localFavoritesToUpload.forEach { favorite -> api.addFavorite(favorite) }
                        emit(Resource.success(data = response))
                    } else {
                        val favorites = userId?.let {
                            favoriteDao.getUserFavorites(userId)
                        } ?: favoriteDao.getAllFavorites()
                        emit(Resource.success(data = FavoriteResponse(result = favorites)))
                    }
                }
            }.flowOn(Dispatchers.IO)

        override fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>> =
            flow {
                safeCall<FavoriteV2> {
                    val userId = configs.userIdentifier()?.call()
                    favoriteDto.userId = userId
                    val favorite = if (configs.favoritesServerSyncEnabled()) {
                        val result = api.addFavorite(favoriteDto)
                        result.userId = userId
                        result
                    } else {
                        favoriteDto
                    }
                    favoriteDao.insertFavorite(favorite)
                    emit(Resource.success(data = favorite))
                }
            }.flowOn(Dispatchers.IO)

        override fun deleteFavorite(favoriteId: String): Flow<Resource<Unit>> =
            flow {
                safeCall<Unit> {
                    favoriteDao.deleteFavoriteByObjectId(favoriteId)
                    if (configs.favoritesServerSyncEnabled()) {
                        api.deleteFavorite(favoriteId)
                    }
                    emit(Resource.success(data = Unit))
                }
            }.flowOn(Dispatchers.IO)

        override fun deleteFavoriteWithStopCode(stopCode: String): Flow<Resource<Unit>> =
            flow {
                safeCall<Unit> {
                    val stopFavorite = favoriteDao.getFavoriteByStopCode(stopCode)
                    favoriteDao.deleteFavoriteByObjectId(stopFavorite.uuid)
                    if (configs.favoritesServerSyncEnabled()) {
                        api.deleteFavorite(stopFavorite.uuid)
                    }
                    emit(Resource.success(data = Unit))
                }
            }.flowOn(Dispatchers.IO)

        override fun isFavorite(favoriteId: String): Flow<Resource<Boolean>> = flow {
            safeCall<Boolean> {
                val userId = configs.userIdentifier()?.call()
                emit(Resource.success(data = userId?.let {
                    favoriteDao.favoriteExistsForUser(favoriteId, userId)
                } ?: kotlin.run { favoriteDao.favoriteExists(favoriteId) }))
            }
        }.flowOn(Dispatchers.IO)

        override fun isFavoriteByStopCode(stopCode: String): Flow<Resource<Boolean>> =
            flow {
                safeCall<Boolean> {
                    emit(Resource.success(data = favoriteDao.favoriteStopExists(stopCode)))
                }
            }.flowOn(Dispatchers.IO)
    }

}