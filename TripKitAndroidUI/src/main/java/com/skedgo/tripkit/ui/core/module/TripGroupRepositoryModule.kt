package com.skedgo.tripkit.ui.core.module
import com.skedgo.routepersistence.RouteStore
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.data.routingresults.TripGroupRepositoryImpl
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.routingresults.WhenToRefreshRoutingResults
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class TripGroupRepositoryModule {
  @Provides
  @Singleton
  fun tripGroupRepository(
          routeStore: RouteStore,
          getNow: GetNow,
          whenToRefreshRoutingResults: WhenToRefreshRoutingResults
  ): TripGroupRepository = TripGroupRepositoryImpl(
      routeStore,
      getNow,
      whenToRefreshRoutingResults
  )

//  @Provides
//  @Singleton
//  fun excludedStopsRepository(): ExcludedStopsRepository = ExcludedStopsRepositoryImpl()
}