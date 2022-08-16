package com.skedgo.tripkit.ui.map

import com.gojuno.koptional.None
import com.gojuno.koptional.Some
import com.google.android.gms.maps.CameraUpdate
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.routing.GetStopsByTravelType
import com.skedgo.tripkit.ui.routing.SegmentCameraUpdateMapper
import com.skedgo.tripkit.ui.routing.SegmentCameraUpdateRepository
import com.skedgo.tripkit.ui.routing.getVisibleGeoPointsOnMap
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresult.MapCameraUpdate
import com.skedgo.tripkit.ui.tripresult.toCameraUpdate
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.functions.BiFunction
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

class TripResultMapViewModel @Inject internal constructor(
        private val printTimeLazy: Lazy<PrintTime>,
        private val tripGroupRepository: TripGroupRepository,
        private val getStopsByTravelTypeLazy: Lazy<GetStopsByTravelType>,
        private val segmentCameraUpdateRepository: SegmentCameraUpdateRepository,
        private val segmentCameraUpdateMapper: SegmentCameraUpdateMapper,
        private val schedulers: SchedulerFactory
) : RxViewModel() {
    private val selectedTrip: BehaviorRelay<Trip> = BehaviorRelay.create()

    val tripCameraUpdate: Observable<CameraUpdate>
        get() = selectedTrip
                .distinctUntilChanged { x, y ->
                    x.uuid() == y.uuid()
                }
                .map { it.segments }
                .flatMap {
                    Observable.fromCallable { it.getVisibleGeoPointsOnMap() }
                            .map { it.toCameraUpdate() }
                }

    val segments: Observable<List<TripSegment>>
        get() = selectedTrip.map {
            it.segments
        }

    val travelledStopMarkerViewModels: Observable<List<StopMarkerViewModel>>
        get() = selectedTrip
                .flatMap { trip ->
                    toStopMarkerViewModels(trip, travelled = true).toList().toObservable()
                }
                .observeOn(mainThread())

    val nonTravelledStopMarkerViewModels: Observable<List<StopMarkerViewModel>>
        get() = selectedTrip
                .flatMap { trip ->
                    toStopMarkerViewModels(trip, travelled = false).toList().toObservable()
                }
                .observeOn(mainThread())

    val alertMarkerViewModels: Observable<List<AlertMarkerViewModel>>
        get() = selectedTrip
                .flatMap {
                    Observable.just(
                            it.segments.flatMap { segment ->
                                (segment.alerts ?: emptyList<RealtimeAlert>())
                                        .filter { it.location() != null }
                                        .map { alert -> AlertMarkerViewModel(alert, segment) }
                            }
                    )
                }
                .observeOn(mainThread())

    val vehicleMarkerViewModels: Observable<List<VehicleMarkerViewModel>>
        get() = selectedTrip
                .flatMap {
                    Observable.fromIterable(it.segments)
                            .filter { it.realTimeVehicle != null }
                            .map { VehicleMarkerViewModel(it) }
                            .toList().toObservable()
                }
                .observeOn(mainThread())

    fun setTripGroupId(tripGroupId: String, tripId: Long? = null) {
        tripGroupRepository.getTripGroup(tripGroupId)
                .subscribe {
                    var trip = it.displayTrip!!
                    tripId?.let { id ->
                        if (trip.id != id) {
                            trip = it.trips?.firstOrNull { it.id == id } ?: it.displayTrip!!
                        }
                    }
                    selectedTrip.accept(trip)
                }.autoClear()
    }

    fun onTripSegmentTapped(): Observable<Pair<CameraUpdate, Long>> =
            segmentCameraUpdateRepository.getSegmentCameraUpdate()
                    .flatMap {
                        val x = segmentCameraUpdateMapper.toCameraUpdate(it)
                        when (x) {
                            is Some -> Observable.just(Pair(x.value, it.tripSegmentId()))
                            is None -> Observable.empty()
                        }
                    }
                    .subscribeOn(schedulers.ioScheduler)

    /**
     * This function is to deal with some design flaws from legacy code
     * which doesn't adhere well Clean Architecture.
     */
    @Deprecated("")
    fun getSelectedTripByBlocking(): Trip = selectedTrip.blockingFirst()

    private fun toStopMarkerViewModels(
            trip: Trip,
            travelled: Boolean
    ): Observable<StopMarkerViewModel> = Observable.fromIterable(trip.segments)
            .flatMap { segment ->
                getStopsByTravelTypeLazy.get()
                        .execute(segment, travelled)
                        .flatMap { stop ->
                            val title = arrayOf(stop.arrivalDateTime, stop.departureDateTime)
                                    .firstOrNull { it != null }
                                    ?.let { printTimeLazy.get().execute(it).toObservable() }
                                    ?: Observable.just("")
                            title
                                    .map {
                                        StopMarkerViewModel(trip, stop, it, segment, travelled)
                                    }
                                    .cast(StopMarkerViewModel::class.java)
                        }
            }
}

data class AlertMarkerViewModel(
        val alert: RealtimeAlert,
        val segment: TripSegment
)

data class VehicleMarkerViewModel(
        val segment: TripSegment
)
