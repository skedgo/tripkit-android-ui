package com.skedgo.tripkit.ui.data.routingresults

import android.util.Log
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.routepersistence.GroupQueries
import com.skedgo.routepersistence.RouteStore
import com.skedgo.routepersistence.WhereClauses
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.time.GetNow
import com.skedgo.tripkit.ui.routingresults.TripGroupId
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.io
import io.reactivex.subjects.PublishSubject
import org.joda.time.Days
import java.util.concurrent.ConcurrentHashMap

typealias A2bRoutingRequestId = String

class TripGroupRepositoryImpl(
        private val routeStore: RouteStore,
        private val getNow: GetNow
) : TripGroupRepository {
    private val onNewTripGroupsAvailable = PublishSubject.create<A2bRoutingRequestId>()
    private val _whenTripGroupIsUpdated = PublishRelay.create<TripGroupId>()
    private val whenManualTripChanges = PublishRelay.create<Unit>()
    private val map = ConcurrentHashMap<String, Observable<TripGroup>>()

    override fun getTripGroupsByA2bRoutingRequestId(
            a2bRoutingRequestId: String
    ): Observable<List<TripGroup>> =
            routeStore.queryTripGroupIdsByRequestIdAsync(a2bRoutingRequestId)
                    .map { getTripGroup(it) }
                    .toList()
                    .toObservable()
                    .repeatWhen {
                        onNewTripGroupsAvailable()
                                .filter { a2bRoutingRequestId == it }
                                .map { Unit }
                                .observeOn(io())
                    }
                    .switchMap {
                        Observable.combineLatest(it) { it.map { it as TripGroup }.toList() }
                                .switchIfEmpty(Observable.just(emptyList()))
                    }

    override fun whenTripGroupIsUpdated(): Observable<TripGroupId> =
            _whenTripGroupIsUpdated.hide()

    override fun getTripGroup(tripGroupId: String): Observable<TripGroup> {
//        return map[tripGroupId] ?: createQuery(tripGroupId)
        return createQuery(tripGroupId)
    }

    private fun createQuery(tripGroupId: String): Observable<TripGroup> {
        val query = routeStore.queryAsync(GroupQueries.hasUuid(tripGroupId))
                .repeatWhen { _whenTripGroupIsUpdated.hide().filter { it == tripGroupId } }
                .subscribeOn(io())
                .replay(1)
                .autoConnect()
        map[tripGroupId] = query
        return query
    }

    override fun getTripSegmentByIdAndTripId(segmentId: Long, tripId: String): Single<TripSegment> {
        return routeStore.querySegmentByIdAndTripId(segmentId = segmentId, tripId = tripId)
    }

    override fun updateTrip(tripGroupId: String, oldTripUuid: String, trip: Trip): Completable {
        val done = map.remove(tripGroupId)
        return routeStore.updateTripAsync(oldTripUuid, trip)
                .andThen(
                        Completable.fromAction {
                            _whenTripGroupIsUpdated.accept(tripGroupId)
                        })
                .subscribeOn(io())
    }

    override fun updateNotify(tripGroupId: String, isFavorite: Boolean): Completable =
            routeStore.updateNotifiability(tripGroupId, isFavorite)
                    .andThen(
                            Completable.fromAction {
                                _whenTripGroupIsUpdated.accept(tripGroupId)
                                whenManualTripChanges.accept(Unit)
                            }
                    )

    override fun addTripToTripGroup(tripGroupId: String, displayTrip: Trip): Completable {
        return routeStore.addTripToTripGroup(tripGroupId, displayTrip)
                .andThen(
                        Completable.fromAction {
                            _whenTripGroupIsUpdated.accept(tripGroupId)
                        }
                )
    }

    override fun setTripGroup(tripGroup: TripGroup): Completable {
        return Observable.just(tripGroup)
                .flatMap {
                    routeStore.updateAlternativeTrips(mutableListOf(it))
                }.singleOrError()
                .doOnSuccess {
                    _whenTripGroupIsUpdated.accept(it.first().uuid())
                }
                .toCompletable()
                .subscribeOn(io())
    }

    override fun deletePastRoutesAsync(): Observable<Int> = routeStore.deleteAsync(
            WhereClauses.happenedBefore(
                    Days.days(30).toPeriod().toStandardHours().hours.toLong(), /* months */
                    getNow.execute().millis
            ))

    override fun addTripGroups(requestId: String?, groups: List<TripGroup>): Completable {
        return routeStore.saveAsync(requestId, groups)
                .ignoreElements()
                .andThen(Completable.fromAction {
                    groups.forEach {
                        _whenTripGroupIsUpdated.accept(it.uuid())
                    }
                    onNewTripGroupsAvailable.onNext(requestId!!)
                })
    }

    override fun onManualTripChanges(): Observable<Unit> =
            whenManualTripChanges.hide()

    override fun onNewTripGroupsAvailable(): Observable<String> =
            onNewTripGroupsAvailable.hide()
}
