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


/**
 * Owns `routes.db`.
 *
 * Deliberately **not** `@InstallIn`: the annotation added in #25836 was reverted in #25837
 * because this module is already installed in `TripKitUI` and in every per-flavor
 * `@Component`, so letting Hilt install it as well would open a further `SQLiteOpenHelper`
 * on the same database file. `TripKitUI` is the canonical owner, and `:app`'s
 * `LegacyDaggerBridge` hands that one instance to the Hilt `SingletonComponent`.
 */
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