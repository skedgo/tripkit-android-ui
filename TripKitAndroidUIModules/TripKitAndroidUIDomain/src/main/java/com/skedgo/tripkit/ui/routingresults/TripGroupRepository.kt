package com.skedgo.tripkit.ui.routingresults

import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

typealias TripGroupId = String

interface TripGroupRepository {
    /**
     * @return An [Observable] which emits a list of [TripGroup]s
     * for an a2b routing request. There will be may lists emitted.
     */
    fun getTripGroupsByA2bRoutingRequestId(a2bRoutingRequestId: String): Observable<List<TripGroup>>

    /**
     * @return An [Observable] which emits [TripGroup] whose [TripGroup.uuid] is [tripGroupId].
     * This [Observable] also emits any of its future changes (e.g. from database change).
     * The [Observable] will never complete.
     */
    fun getTripGroup(tripGroupId: String): Observable<TripGroup>

    fun getTripSegmentByIdAndTripId(segmentId: Long, tripId: String): Single<TripSegment>

    fun addTripGroups(requestId: String?, groups: List<TripGroup>): Completable
    fun setTripGroup(tripGroup: TripGroup): Completable
    fun onManualTripChanges(): Observable<Unit>
    fun deletePastRoutesAsync(): Observable<Int>
    fun onNewTripGroupsAvailable(): Observable<String>
    fun whenTripGroupIsUpdated(): Observable<TripGroupId>
    fun updateTrip(tripGroupId: String, oldTripUuid: String, trip: Trip): Completable
    fun updateNotify(tripGroupId: String, isFavorite: Boolean): Completable
    fun addTripToTripGroup(tripGroupId: String, displayTrip: Trip): Completable
}
