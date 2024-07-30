package com.skedgo.tripkit.ui.favorites.v2.data.network

import com.skedgo.TripKit
import com.skedgo.network.Resource
import com.skedgo.tripkit.Configs
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteDaoV2
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteType
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2.LocationFavorite
import com.skedgo.tripkit.ui.utils.safeCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Repository class to handle syncing of favorites to server and local storage
 */
interface FavoritesRepository {

    fun getFavorites(syncFromServer: Boolean = false): Flow<Resource<FavoriteResponse>>
    fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>>
    fun deleteFavorite(favoriteId: String): Flow<Resource<Unit>>
    fun deleteFavoriteWithStopCode(stopCode: String): Flow<Resource<Unit>>
    fun isFavorite(favoriteId: String): Flow<Resource<Boolean>>
    fun isFavoriteByStopCode(stopCode: String): Flow<Resource<Boolean>>
    fun isFavoriteByLocation(location: LocationFavorite): Flow<Resource<Boolean>>
    fun deleteFavoriteWithLocationAddress(locationAddress: String): Flow<Resource<Unit>>
    fun getFavoriteById(uuid: String): Flow<FavoriteV2?>
    fun getFavoriteByType(type: FavoriteType): Flow<FavoriteV2?>

    class FavoritesRepositoryImpl @Inject constructor(
        private val api: FavoritesApi,
        private val favoriteDao: FavoriteDaoV2,
    ) : FavoritesRepository {

        private val configs: Configs = TripKit.getInstance().configs()

        override fun getFavorites(syncFromServer: Boolean): Flow<Resource<FavoriteResponse>> =
            flow {
                val userId = configs.userIdentifier()?.call()
                safeCall<FavoriteResponse>(
                    errorHandlingCall = {
                        if (it is HttpException && it.code() in 400..499) {
                            emit(
                                Resource.success(
                                    data = FavoriteResponse(
                                        result = favoriteDao.getAllFavoritesWithEmptyUserId("")
                                    )
                                )
                            )
                        } else {
                            emit(
                                Resource.error(
                                    data = null,
                                    message = it.message ?: "Error Occurred!",
                                    -1
                                )
                            )
                        }
                    }
                ) {
                    if (configs.favoritesServerSyncEnabled() && syncFromServer && !userId.isNullOrEmpty()) {
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
                        val localFavorites =
                            favoriteDao.getAllFavoritesWithEmptyUserId(userId.orEmpty())

                        val localFavoritesToUpload = localFavorites.filterNot { localFavorite ->
                            response.result?.any { it.uuid == localFavorite.uuid } == true
                        }
                        localFavoritesToUpload.forEach { favorite -> api.addFavorite(favorite) }
                        emit(Resource.success(data = response))
                    } else {
                        val favorites =
                            favoriteDao.getAllFavoritesWithEmptyUserId(userId.orEmpty())
                        emit(Resource.success(data = FavoriteResponse(result = favorites)))
                    }
                }
            }.flowOn(Dispatchers.IO)

        override fun addFavorite(favoriteDto: FavoriteV2): Flow<Resource<FavoriteV2>> =
            flow {
                safeCall<FavoriteV2> {
                    val userId = configs.userIdentifier()?.call()
                    favoriteDto.userId = userId
                    val favorite =
                        if (configs.favoritesServerSyncEnabled() && !userId.isNullOrEmpty()) {
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
                    stopFavorite?.let {
                        favoriteDao.deleteFavoriteByObjectId(stopFavorite.uuid)
                    }
                    if (configs.favoritesServerSyncEnabled()) {
                        stopFavorite?.let { api.deleteFavorite(stopFavorite.uuid) }
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
                    val userId = configs.userIdentifier()?.call()
                    emit(Resource.success(data = userId?.let {
                        favoriteDao.favoriteStopExistsForUser(stopCode, userId)
                    } ?: run { favoriteDao.favoriteStopExists(stopCode) }))
                }
            }.flowOn(Dispatchers.IO)

        override fun isFavoriteByLocation(location: LocationFavorite): Flow<Resource<Boolean>> =
            flow {
                safeCall<Boolean> {
                    val userId = configs.userIdentifier()?.call()
                    emit(Resource.success(data = userId?.let {
                        favoriteDao.favoriteLocationExistsForUser(location.address, userId)
                    } ?: run { favoriteDao.favoriteLocationExists(location.address) }))
                }
            }.flowOn(Dispatchers.IO)

        override fun deleteFavoriteWithLocationAddress(locationAddress: String): Flow<Resource<Unit>> =
            flow {
                safeCall<Unit> {
                    val location = favoriteDao.getFavoriteByLocationAddress(locationAddress)
                    location?.let { favoriteDao.deleteFavoriteByObjectId(location.uuid) }
                    if (configs.favoritesServerSyncEnabled()) {
                        location?.let { api.deleteFavorite(location.uuid) }
                    }
                    emit(Resource.success(data = Unit))
                }
            }.flowOn(Dispatchers.IO)

        override fun getFavoriteById(uuid: String): Flow<FavoriteV2?> =
            flow<FavoriteV2?> {
                emit(favoriteDao.getFavoriteById(uuid))
            }.flowOn(Dispatchers.IO)

        override fun getFavoriteByType(type: FavoriteType): Flow<FavoriteV2?> =
            flow<FavoriteV2?> {
                emit(favoriteDao.getFavoriteOfType(type))
            }.flowOn(Dispatchers.IO)
    }

}