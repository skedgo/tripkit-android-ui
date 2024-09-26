package com.skedgo.tripkit.ui.map

import com.gojuno.koptional.None
import com.gojuno.koptional.Some
import com.google.android.gms.maps.CameraUpdate
import com.jakewharton.rxrelay2.BehaviorRelay
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.datetime.PrintTime
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.SchedulerFactory
import com.skedgo.tripkit.ui.routing.GetStopsByTravelType
import com.skedgo.tripkit.ui.routing.SegmentCameraUpdateMapper
import com.skedgo.tripkit.ui.routing.SegmentCameraUpdateRepository
import com.skedgo.tripkit.ui.routing.getVisibleGeoPointsOnMap
import com.skedgo.tripkit.ui.routingresults.TripGroupRepository
import com.skedgo.tripkit.ui.tripresult.toCameraUpdate
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TripResultMapViewModel @Inject internal constructor(
    private val printTimeLazy: Lazy<PrintTime>,
    private val tripGroupRepository: TripGroupRepository,
    private val getStopsByTravelTypeLazy: Lazy<GetStopsByTravelType>,
    private val segmentCameraUpdateRepository: SegmentCameraUpdateRepository,
    private val segmentCameraUpdateMapper: SegmentCameraUpdateMapper,
    private val schedulers: SchedulerFactory
) : RxViewModel() {

    companion object {
        const val DELAY_SELECTED_TRIP = 700L
        const val DELAY_MAP_CAMERA_UPDATE = 400L
    }

    private val selectedTrip: BehaviorRelay<Trip> = BehaviorRelay.create()

    private var currentTrip: Trip? = null

    // Having a lot of observer on selectedTrip might be the cause of unable
    // to update map when users swiped on the trips overview.
    // So updated the approach to only have one observer to selectedTrip
    // then do the processing of the data for updating the map from there
    // and send updates to TripResultMapContributor via event bus (PublishSubject)
    val tripCameraUpdateStream = PublishSubject.create<CameraUpdate>()
    val segmentsStream = PublishSubject.create<List<TripSegment>>()
    val travelledStopMarkerViewModelsStream = PublishSubject.create<List<StopMarkerViewModel>>()
    val nonTravelledStopMarkerViewModelsStream = PublishSubject.create<List<StopMarkerViewModel>>()
    val alertMarkerViewModelsStream = PublishSubject.create<List<AlertMarkerViewModel>>()
    val vehicleMarkerViewModelsStream = PublishSubject.create<List<VehicleMarkerViewModel>>()
    val mapTilesStream = PublishSubject.create<List<String>>()

    private val stopMarkerViewModelsDisposable = CompositeDisposable()

    // This is to ensure the previous observable is cleared when a new tripGroup is selected
    private val tripGroupDisposable = CompositeDisposable()

    init {
        selectedTrip
            .debounce(DELAY_SELECTED_TRIP, TimeUnit.MILLISECONDS)
            .observeOn(mainThread())
            .subscribe(
                { trip ->
                    if (currentTrip != trip) {
                        currentTrip = trip
                        with(trip) {
                            processCameraUpdate()
                            processSegments()
                            processTravelledStopMarkerViewModels()
                            processMarkerViewModels()
                            processMapTiles()
                        }
                    }
                }, {
                    it.printStackTrace()
                }
            ).autoClear()
    }

    private fun Trip.processCameraUpdate() {
        this.segments?.let { tripSegments ->
            Observable.timer(DELAY_MAP_CAMERA_UPDATE, TimeUnit.MILLISECONDS, Schedulers.io())
                .subscribe({
                    // Run after the delay to make sure markers and segments will be drawn first
                    val cameraUpdate = tripSegments.getVisibleGeoPointsOnMap().toCameraUpdate()
                    tripCameraUpdateStream.onNext(cameraUpdate)
                }, {
                    // Handle any potential error (although `timer` usually doesn't throw)
                    Timber.e(it, "Error while processing delayed camera update")
                })
        }
    }

    private fun Trip.processSegments() {
        segmentsStream.onNext(this.segments)
    }

    private fun Trip.processTravelledStopMarkerViewModels() {
        // to ensure previous observer is cleared before executing a new one
        stopMarkerViewModelsDisposable.clear()

        // Define Singles for both travelled and non-travelled StopMarkerViewModels
        val travelledSingle = toStopMarkerViewModels(this, travelled = true)
            .toList()
            .subscribeOn(Schedulers.io()) // Execute on IO scheduler for concurrency

        val nonTravelledSingle = toStopMarkerViewModels(this, travelled = false)
            .toList()
            .subscribeOn(Schedulers.io())

        Single.zip(
            travelledSingle,
            nonTravelledSingle
        ) { travelledList, nonTravelledList ->
            Pair(travelledList, nonTravelledList) // Pair both lists together
        }.subscribe({
            travelledStopMarkerViewModelsStream.onNext(it.first)
            nonTravelledStopMarkerViewModelsStream.onNext(it.second)
        }, {
            Timber.e(it)
        }).addTo(stopMarkerViewModelsDisposable)
    }

    private fun Trip.processMarkerViewModels() {
        val tripSegments = this.segments
        alertMarkerViewModelsStream.onNext(
            tripSegments.flatMap { segment ->
                (segment.alerts ?: emptyList<RealtimeAlert>())
                    .filter { it.location() != null }
                    .map { alert -> AlertMarkerViewModel(alert, segment) }
            }
        )
        vehicleMarkerViewModelsStream.onNext(
            tripSegments.filter { it.realTimeVehicle != null }.map {
                VehicleMarkerViewModel(it)
            }
        )
    }

    private fun Trip.processMapTiles() {
        val tripSegments = this.segments
        val segmentWithMapTiles = tripSegments.firstOrNull { it.mapTiles != null }
        mapTilesStream.onNext(segmentWithMapTiles?.mapTiles?.urlTemplates ?: emptyList())
    }

    fun setTripGroupId(tripGroupId: String, tripId: Long? = null) {
        tripGroupDisposable.clear()
        tripGroupRepository.getTripGroup(tripGroupId)
            .subscribe { tripGroup ->
                val trip = tripId?.let { id ->
                    tripGroup.trips?.firstOrNull { it.id == id } ?: tripGroup.displayTrip
                } ?: tripGroup.displayTrip
                trip?.let { selectedTrip.accept(it) }
            }.addTo(tripGroupDisposable)
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

    override fun onCleared() {
        super.onCleared()
        tripGroupDisposable.clear()
        stopMarkerViewModelsDisposable.clear()
    }
}

data class AlertMarkerViewModel(
    val alert: RealtimeAlert,
    val segment: TripSegment
)

data class VehicleMarkerViewModel(
    val segment: TripSegment
)
