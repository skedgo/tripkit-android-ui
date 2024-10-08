package com.skedgo.tripkit.ui.core.module

import android.content.Context
import com.google.gson.GsonBuilder
import com.skedgo.routepersistence.LocationTypeAdapterFactory
import com.skedgo.routepersistence.RouteDatabaseHelper
import com.skedgo.routepersistence.RouteStore
import com.skedgo.routepersistence.RoutingStatusStore
import com.skedgo.tripkit.common.model.booking.GsonAdaptersBooking
import com.skedgo.tripkit.common.model.realtimealert.GsonAdaptersRealtimeAlert
import com.skedgo.tripkit.common.util.LowercaseEnumTypeAdapterFactory
import com.skedgo.tripkit.data.routingstatus.RoutingStatusRepositoryImpl
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class RouteStoreModule {

    @Provides
    @Singleton
    internal fun routeStore(routeDatabaseHelper: RouteDatabaseHelper): RouteStore {
        val gson = GsonBuilder()
            .registerTypeAdapterFactory(LocationTypeAdapterFactory())
            .registerTypeAdapterFactory(LowercaseEnumTypeAdapterFactory())
            .registerTypeAdapterFactory(GsonAdaptersBooking())
            .registerTypeAdapterFactory(GsonAdaptersRealtimeAlert())
            .create()
        return RouteStore(routeDatabaseHelper, gson)
    }

    @Provides
    @Singleton
    fun routeDatabaseHelper(context: Context): RouteDatabaseHelper =
        RouteDatabaseHelper(context, "routes.db")


    @Provides
    @Singleton
    internal fun routingStatusStore(
        routeDatabaseHelper: RouteDatabaseHelper
    ): RoutingStatusStore = RoutingStatusStore(routeDatabaseHelper)

    @Provides
    @Singleton
    internal fun routingStatusRepository(
        impl: RoutingStatusRepositoryImpl
    ): RoutingStatusRepository = impl

}