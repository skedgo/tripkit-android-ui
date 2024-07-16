package com.skedgo.tripkit.ui.favorites.v2.data.network

import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoriteV2
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FavoritesApi {

    @GET("data/user/favorite")
    suspend fun getFavorites(): FavoriteResponse

    @POST("data/user/favorite")
    suspend fun addFavorite(favoriteDto: FavoriteV2): FavoriteV2

    @PUT("data/user/favorite/{uuid}")
    fun updateFavorite(
        @Path("uuid") uuid: String,
        @Body favorite: FavoriteV2
    ): FavoriteV2

    @DELETE("v1/data/user/favorite/{uuid}")
    fun deleteFavorite(
        @Path("uuid") uuid: String
    ): ResponseBody
}