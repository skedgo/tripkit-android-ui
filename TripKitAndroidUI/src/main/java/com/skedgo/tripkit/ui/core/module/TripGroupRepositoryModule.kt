package com.skedgo.tripkit.ui.core.module

import com.skedgo.routepersistence.RouteStore
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.data.routingresults.TripGroupRepositoryImpl
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Not `@InstallIn`: the annotation added in #25836 was reverted in #25837.
 *
 * `TripGroupRepository` is backed by the `RouteStore` that owns `routes.db`, so it has to come
 * from the one canonical graph rather than be rebuilt per DI root. `:app`'s
 * `LegacyDaggerBridge` bridges `TripKitUI.getInstance().tripGroupRepository()` into the Hilt
 * `SingletonComponent`.
 */
@Module
class TripGroupRepositoryModule {
    @Provides
    @Singleton
    fun tripGroupRepository(
        routeStore: RouteStore,
        getNow: GetNow
    ): TripGroupRepository = TripGroupRepositoryImpl(
        routeStore,
        getNow
    )

//  @Provides
//  @Singleton
//  fun excludedStopsRepository(): ExcludedStopsRepository = ExcludedStopsRepositoryImpl()
}