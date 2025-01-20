package com.skedgo.tripkit.ui.map.servicestop

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.tripplanner.DiffTransformer
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
import io.reactivex.Single
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

    init {
        Observables.combineLatest(
            service,
            serviceStop.hide().flatMap { regionService.getRegionByLocationAsync(it) }
        ) { service, region -> service to region }
            .distinctUntilChanged()
            .observeOn(Schedulers.io())
            .switchMap { (service, region) ->
                if (service.realTimeStatus in listOf(
                        RealTimeStatus.IS_REAL_TIME,
                        RealTimeStatus.CAPABLE
                    )
                ) {
                    realtimeViewModel.getRealTimeVehicles(region, listOf(service))
                        .takeUntil(stopRealtimeRelay) // Stop when stopRealtimeRelay emits
                        .doOnNext { vehicles ->
                            Timber.d("Fetched real-time vehicles: $vehicles")
                        }
                        .onErrorResumeNext { throwable: Throwable ->
                            Timber.e(throwable, "Error fetching real-time vehicles")
                            Observable.empty() // Emit nothing in case of an error
                        }
                } else {
                    Timber.d("Service not real-time capable")
                    Observable.just(service to region)
                }
            }
            .replay(1)
            .refCount()
            .subscribe()
            .autoClear()
    }

    fun stopRealtimeUpdates() {
        stopRealtimeRelay.accept(Unit)
    }

    private val serviceStopsAndLines =
        Observable.combineLatest(
            service,
            serviceStop,
            BiFunction { service: TimetableEntry, stop: ScheduledStop -> service to stop })
            .distinctUntilChanged()
            .observeOn(Schedulers.io())
            .switchMap { (service, stop) ->
                fetchAndLoadServices.load(service, stop).toObservable()
            }
            .replay(1)
            .refCount()

    val realtimeVehicle = service
        .observeOn(Schedulers.io())
        .switchMap {
            if (it.realTimeStatus in listOf(RealTimeStatus.IS_REAL_TIME, RealTimeStatus.CAPABLE)) {
                realtimeViewModel.realTimeVehicleObservable(it)
                    .map { OptionalCompat.ofNullable(it) }
            } else {
                Observable.just(OptionalCompat.empty())
            }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .autoClear()

    val region by lazy {
        serviceStop.hide()
            .flatMap { regionService.getRegionByLocationAsync(it) }
            .replay(1)
            .autoConnect()
    }

    val drawStops = serviceStopsAndLines
        .map { it.first }
        .compose(DiffTransformer<StopInfo, MarkerOptions>({ it.stop.code }, { stopInfo ->
            getStopDisplayText.execute(stopInfo.stop)
                .withLatestFrom(
                    region,
                    BiFunction { text: String, region: Region -> text to region })
                .firstOrError()
                .flatMap {
                    Single.just(
                        serviceStopMarkerCreator.toMarkerOptions(
                            stopInfo,
                            it.first,
                            it.second.timezone
                        )
                    )
                }
        }))
        .map { it.first.map { it.first to it.second.stop.code } to it.second }
        .observeOn(AndroidSchedulers.mainThread())
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

    private fun getStopForService(stop: ScheduledStop, service: TimetableEntry): ScheduledStop {
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
