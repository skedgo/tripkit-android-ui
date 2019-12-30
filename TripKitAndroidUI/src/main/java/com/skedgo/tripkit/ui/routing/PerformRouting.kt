package com.skedgo.tripkit.ui.routing

import android.content.Context
import com.skedgo.tripkit.common.model.Query
import com.skedgo.tripkit.TransitModeFilter
import com.skedgo.tripkit.a2brouting.RouteService
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeed
import com.skedgo.tripkit.ui.routing.settings.CyclingSpeedRepository
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeed
import com.skedgo.tripkit.ui.routing.settings.WalkingSpeedRepository
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import org.joda.time.Minutes
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routingstatus.RoutingStatusRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FIXME: Should move this into TripGoDomainLegacy module.
 */
@Singleton
open class PerformRouting @Inject internal constructor(
        private val queryLocationResolverLazy: Lazy<QueryLocationResolver>,
        private val tripGroupRepository: TripGroupRepository,
        private val routeService: RouteService,
        private val routingStatusRepositoryLazy: Lazy<RoutingStatusRepository>,
        private val preferredTransferTimeRepositoryLazy: Lazy<PreferredTransferTimeRepository>,
        private val cyclingSpeedRepositoryLazy: Lazy<CyclingSpeedRepository>,
        private val walkingSpeedRepositoryLazy: Lazy<WalkingSpeedRepository>,
        private val transitModeFilter: TransitModeFilter,
        private val errorLogger: ErrorLogger,
        private val getRoutingConfig: GetRoutingConfig,
        private val context: Context
) {

  fun buildRoutingConfigAndExecuteQuery(query: Query): Observable<List<TripGroup>> {
    return Observables
        .zip(
            Observable.just(query),
            preferredTransferTimeRepositoryLazy.get().getPreferredTransferTime(),
            cyclingSpeedRepositoryLazy.get().getCyclingSpeed(),
            walkingSpeedRepositoryLazy.get().getWalkingSpeed(),
            getRoutingConfig.execute()
        ) { _query: Query, transferTime: Minutes, cyclingSpeed:CyclingSpeed, walkingSpeed:WalkingSpeed, routingConfig:RoutingConfig ->
          _query.transferTime = transferTime.minutes
          _query.cyclingSpeed = cyclingSpeed.value
          _query.walkingSpeed = walkingSpeed.value
          _query.budgetWeight = routingConfig.weightingProfile.budgetPriority.value
          _query.timeWeight = routingConfig.weightingProfile.timePriority.value
          _query.environmentWeight = routingConfig.weightingProfile.environmentPriority.value
          _query.hassleWeight = routingConfig.weightingProfile.conveniencePriority.value
          _query
        }
        .flatMap {
          queryLocationResolverLazy.get()
              .apply(it)
              .flatMap { executeQuery(it) }
        }
  }

//  open fun execute(query: Query) {
//    Observable
//        .defer {
//          when (context.isNetworkConnected()) {
//            false -> Observable.error(NoConnectionError(context.getString(R.string.connect_to_internet_to_get_trips)))
//            else -> buildRoutingConfigAndExecuteQuery(query)
//          }
//        }
//        .doOnSubscribe {
//          routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
//              query.uuid(),
//              Status.InProgress()
//          )).subscribe()
//        }
//        .doOnError {
//          routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
//              query.uuid(),
//              Status.Error(
//                  when (it) {
//                    is RoutingError -> it.message
//                    else -> context.getString(R.string.error_encountered)
//                  }
//              )
//          )).subscribe()
//        }
//        .doOnComplete {
//          routingStatusRepositoryLazy.get().putRoutingStatus(RoutingStatus(
//              query.uuid(),
//              Status.Completed()
//          )).subscribe()
//        }
//        .subscribeOn(io())
//        .subscribe({}, { errorLogger.logError(it) })
//  }

  fun executeQuery(query: Query): Observable<List<TripGroup>> {
    val requestId = query.uuid()
    return routeService.routeAsync(query = query, transitModeFilter = transitModeFilter)
        .flatMap {
          tripGroupRepository.addTripGroups(requestId, it).toObservable<List<TripGroup>>()
        }
  }
}