package com.skedgo.tripkit.ui.map.servicestop

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.data.location.toLatLng
import com.skedgo.tripkit.ui.model.StopInfo
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import com.skedgo.tripkit.ui.servicedetail.FetchAndLoadServices
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText
import com.skedgo.tripkit.ui.utils.ServiceLineOverlayTask
import com.skedgo.tripkit.utils.OptionalCompat
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("CheckResult")
class ServiceStopMapViewModel @Inject constructor(
    val context: Context,
    val fetchAndLoadServices: FetchAndLoadServices,
    val regionService: RegionService,
    val getStopDisplayText: GetStopDisplayText
) : RxViewModel() {

    val service = BehaviorRelay.create<TimetableEntry>()
    val stop = BehaviorRelay.create<ScheduledStop>()

    private val stopRealtimeRelay = PublishRelay.create<Unit>() // To stop real-time updates

    private val serviceStop = Observable
        .combineLatest(
            stop.hide(),
            service.hide(),
            BiFunction { stop: ScheduledStop, service: TimetableEntry ->
                getStopForService(stop, service)
            }
        )

    lateinit var realtimeViewModel: RealTimeChoreographerViewModel
    lateinit var serviceStopMarkerCreator: ServiceStopMarkerCreator

    fun stopRealtimeUpdates() {
        stopRealtimeRelay.accept(Unit)
    }

    /**
     * Vehicle locations are independent of whether a service has real-time arrival predictions.
     * A service can be labelled "Scheduled" and still have a tracked vehicle, so always ask the
     * latest endpoint while service details are visible.
     *
     * Keeping the request and selected-vehicle lookup in one replaying stream also prevents the
     * first response from being lost between the old fire-and-forget request and PublishRelay
     * subscriptions.
     */
    private val realtimeVehicleUpdates = Observables.combineLatest(
        service,
        serviceStop.hide().switchMap { regionService.getRegionByLocationAsync(it) }
    ) { service, region -> service to region }
        .distinctUntilChanged()
        .observeOn(Schedulers.io())
        .switchMap { (service, region) ->
            realtimeViewModel.getRealTimeVehicles(region, listOf(service))
                .startWith(service.realtimeVehicle?.let(::listOf).orEmpty())
                .takeUntil(stopRealtimeRelay)
                .doOnNext { vehicles ->
                    Timber.d("Fetched real-time vehicles: $vehicles")
                }
                .onErrorResumeNext { throwable: Throwable ->
                    Timber.e(throwable, "Error fetching real-time vehicles")
                    Observable.empty()
                }
                .map { vehicles -> service to vehicles }
        }
        .replay(1)
        .refCount()

    // Share the existing request with detail presentation, including predictions without locations.
    internal val realtimeServiceUpdates = realtimeVehicleUpdates.autoClear()

    private val scopedServiceStopsAndLines =
        Observable.combineLatest(
            service,
            serviceStop,
            BiFunction { service: TimetableEntry, stop: ScheduledStop -> service to stop })
            .distinctUntilChanged()
            .observeOn(Schedulers.io())
            .switchMap { (service, stop) ->
                fetchAndLoadServices.load(service, stop).map { (service to stop) to it }.toObservable()
            }
            .replay(1)
            .refCount()

    val realtimeVehicles = realtimeVehicleUpdates
        .map { (service, vehicles) ->
            vehicles.filter { vehicle ->
                vehicle.serviceTripId == service.serviceTripId && vehicle.hasLocationInformation()
            }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .autoClear()

    val realtimeVehicle = realtimeVehicles
        .map { vehicles -> OptionalCompat.ofNullable(vehicles.firstOrNull()) }
        .autoClear()

    val region by lazy {
        serviceStop.hide()
            .flatMap { regionService.getRegionByLocationAsync(it) }
            .replay(1)
            .autoConnect()
    }

    private val serviceStopsAndLines = scopedServiceStopsAndLines.map { it.second }

    private data class StopMarkerState(val entry: TimetableEntry, val info: StopInfo)
    private data class StopMarkerDiff(
        val selection: Pair<TimetableEntry, ScheduledStop>?,
        val current: Map<String, StopMarkerState>,
        val changed: List<StopInfo>,
        val removed: Set<String>
    )

    private fun effectiveSelectedStop(
        info: StopInfo, entry: TimetableEntry, selected: ScheduledStop, vehicles: List<RealTimeVehicle>
    ): StopInfo {
        if (info.stop.code != selected.code || entry.startStopCode != selected.code) return info
        val vehicle = vehicles.firstOrNull {
            it.serviceTripId == entry.serviceTripId &&
                (it.startStopCode == null || it.startStopCode == selected.code)
        }
        val departure = vehicle?.arriveAtStartStopTime?.takeIf { it > 0 }
            ?: entry.realTimeDeparture.toLong().takeIf { it > 0 }
            ?: info.stop.departureSecs()
        // Keep service.json schedule and coordinates intact; only the selected pin's presentation changes.
        val presentation = ServiceStop().apply {
            fillFrom(info.stop)
            setDepartureSecs(departure)
        }
        return info.copy(stop = presentation)
    }

    val drawStops = scopedServiceStopsAndLines
        .switchMap { (selection, details) ->
            val (entry, selected) = selection
            realtimeServiceUpdates
                .filter { it.first === entry }
                .map { it.second }
                .startWith(entry.realtimeVehicle?.let(::listOf).orEmpty())
                .map { vehicles ->
                    selection to details.first.map { effectiveSelectedStop(it, entry, selected, vehicles) }
                }
        }
        .scan(StopMarkerDiff(null, emptyMap(), emptyList(), emptySet())) { previous, (selection, stops) ->
            val current = stops.associate { it.stop.code to StopMarkerState(selection.first, it) }
            val changed = current.filter { (code, state) ->
                val old = previous.current[code]
                old == null || old.entry !== state.entry || old.info.id != state.info.id ||
                    old.info.stop.departureSecs() != state.info.stop.departureSecs()
            }.values.map { it.info }
            StopMarkerDiff(selection, current, changed, previous.current.keys - current.keys)
        }
        .filter { it.selection != null && (it.changed.isNotEmpty() || it.removed.isNotEmpty()) }
        .concatMap { diff ->
            Observable.fromIterable(diff.changed)
                .flatMapSingle { stopInfo ->
                    Observable.combineLatest(
                        getStopDisplayText.execute(stopInfo.stop), region,
                        BiFunction { text: String, region: Region -> text to region }
                    )
                        .firstOrError()
                        .map { (text, region) ->
                            serviceStopMarkerCreator.toMarkerOptions(stopInfo, text, region.timezone) to stopInfo.stop.code
                        }
                }
                .toList().toObservable()
                .map { diff.selection!! to (it to diff.removed) }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .filter { (selection, _) ->
            service.value === selection.first && stop.value?.let {
                getStopForService(it, selection.first).code == selection.second.code
            } == true
        }
        .map { it.second }
        .autoClear()

    val drawServiceLine = serviceStopsAndLines.map {
        it.second
    }.map {
        ServiceLineOverlayTask().apply(it)
    }.observeOn(AndroidSchedulers.mainThread()).autoClear()

    val viewPort by lazy {
        Observables
            .combineLatest(realtimeVehicle, serviceStop)
            { realtimeVehicle: OptionalCompat<RealTimeVehicle>, stop: ScheduledStop ->
                if (realtimeVehicle.isPresent()) {
                    with(realtimeVehicle.get().location) {
                        if (this != null) {
                            return@combineLatest listOf(this.toLatLng(), stop.toLatLng())
                        }
                    }
                }
                return@combineLatest listOf(stop.toLatLng())
            }
            .observeOn(AndroidSchedulers.mainThread())
            .autoClear()
    }

    @VisibleForTesting
    fun getStopForService(stop: ScheduledStop, service: TimetableEntry): ScheduledStop {
        if (stop.code == service.stopCode || stop.children == null) {
            return stop
        }
        for (child in stop.children.orEmpty()) {
            if (child.code == service.stopCode) {
                return child
            }
        }
        return stop
    }

}
