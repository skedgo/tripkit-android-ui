package com.skedgo.tripkit.ui.data.waypoints

import android.content.res.Resources
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.skedgo.tripkit.configuration.ServerManager
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypoints
import com.skedgo.tripkit.ui.favorites.GetTripFromWaypointsImpl
import com.skedgo.tripkit.ui.favorites.waypoints.Waypoint
import dagger.Module
import dagger.Provides
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

@Module
class WaypointsModule {

    @Provides
    internal fun waypointsApi(httpClient: OkHttpClient): WaypointsApi {
        val gson = GsonConverterFactory.create(
            GsonBuilder()
                .registerTypeAdapter(Waypoint::class.java, WaypointsAdapter())
                .create()
        )
        return Retrofit.Builder()
            .baseUrl(ServerManager.configuration.apiTripGoUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
            .addConverterFactory(gson)
            .client(httpClient)
            .build()
            .create(WaypointsApi::class.java)
    }

    @Provides
    fun getTripFromWaypoints(
        resources: Resources,
        gson: Gson,
        waypointsApi: WaypointsApi
    ): GetTripFromWaypoints =
        GetTripFromWaypointsImpl(resources, gson, waypointsApi)

}