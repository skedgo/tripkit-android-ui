package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.google.gson.GsonBuilder
import com.skedgo.tripkit.ui.data.waypoints.WaypointsAdapter
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints
import com.skedgo.tripkit.ui.favorites.v2.data.local.FavoritesDatabase
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesApi
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository
import com.skedgo.tripkit.ui.favorites.v2.data.network.FavoritesRepository.FavoritesRepositoryImpl
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointRepository
import com.skedgo.tripkit.ui.favorites.waypoints.WaypointRepository.WaypointRepositoryImpl
import com.skedgo.tripkit.ui.routing.GetRoutingConfig
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
class FavoritesModule {
    @Provides
    @Singleton
    internal fun favoritesDatabase(context: Context): FavoritesDatabase {
        return FavoritesDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    internal fun favoritesApi(builder: Retrofit.Builder, httpClient: OkHttpClient): FavoritesApi {
        val gson = GsonConverterFactory.create(
            GsonBuilder()
                .registerTypeAdapter(Waypoint::class.java, WaypointsAdapter())
                .create()
        )
        return builder
            .addConverterFactory(gson)
            .client(httpClient)
            .build().create(FavoritesApi::class.java)
    }

    @Provides
    fun favoritesRepository(
        favoritesApi: FavoritesApi,
        db: FavoritesDatabase
    ): FavoritesRepository =
        FavoritesRepositoryImpl(favoritesApi, db.favoriteDao())


    @Provides
    fun wayPointsRepository(
        getRoutingConfig: GetRoutingConfig,
        getTripFromWaypoints: GetTripFromWaypoints,
        tripGroupRepository: TripGroupRepository,
        db: FavoritesDatabase
    ): WaypointRepository =
        WaypointRepositoryImpl(
            getRoutingConfig,
            getTripFromWaypoints,
            tripGroupRepository,
            db.waypointDao()
        )
}