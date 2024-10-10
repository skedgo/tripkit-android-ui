package com.skedgo.tripkit.ui.map.home

import android.annotation.SuppressLint
import android.content.res.Resources
import android.util.Log
import androidx.databinding.ObservableBoolean
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.tripkit.camera.GetInitialMapCameraPosition
import com.skedgo.tripkit.camera.PutMapCameraPosition
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.GoToMyLocationRepository
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.tripplanner.DiffTransformer
import com.skedgo.tripkit.tripplanner.PinUpdate
import com.skedgo.tripkit.tripplanner.PinUpdateRepository
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.data.cameraposition.toCameraPosition
import com.skedgo.tripkit.ui.data.cameraposition.toMapCameraPosition
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.map.IMapPoiLocation
import com.skedgo.tripkit.ui.map.LoadPOILocationsByViewPort
import com.squareup.picasso.Picasso
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
class MapViewModel @Inject internal constructor(
    private val putMapCameraPosition: PutMapCameraPosition,
    private val getInitialMapCameraPosition: GetInitialMapCameraPosition,
    private val pinUpdateRepository: PinUpdateRepository,
    private val resources: Resources,
    private val picasso: Picasso,
    private val goToMyLocationRepository: GoToMyLocationRepository,
    private val fetchStopsByViewport: FetchStopsByViewport,
    private val getCellIdsFromViewPort: GetCellIdsFromViewPort,
    private val loadPOILocationsByViewPort: LoadPOILocationsByViewPort,
    private val errorLogger: ErrorLogger
) : RxViewModel() {
    private val _myLocationError: PublishRelay<Throwable> = PublishRelay.create()
    val myLocationError: Observable<Throwable>
        get() = _myLocationError.hide()

    private val _myLocation: PublishRelay<Location> = PublishRelay.create()
    val myLocation: Observable<Location>
        get() = _myLocation.hide()

    var showMarkers = ObservableBoolean(true)

    var transportModes: List<TransportMode>? = null

    private val viewportChanged = PublishRelay.create<ViewPort>()
    val markers = viewportChanged.hide()
        .debounce(500, TimeUnit.MILLISECONDS)
        .flatMap { viewPort ->
            getCellIdsFromViewPort.execute(viewPort)
                .map { viewPort to it }
        }
        .distinctUntilChanged { a, b -> a.second == b.second }
        .map { it.first }
        .observeOn(Schedulers.io())
        .switchMap {
            if (showMarkers.get()) {
                loadPOILocationsByViewPort.execute(it)
            } else {
                Observable.empty()
            }
        }
        .switchMap {
            hidePoi(it.toMutableList())
        }
        .compose(
            DiffTransformer<IMapPoiLocation, MarkerOptions>({ it.identifier },
                { it.createMarkerOptions(resources, picasso) })
        )
        .autoClear()

    private fun hidePoi(list: MutableList<IMapPoiLocation>): Observable<MutableList<IMapPoiLocation>> {
        val toRemove: MutableList<IMapPoiLocation> = ArrayList()
        list.forEach {
            if (hidePoi(it.identifier)) {
                toRemove.add(it)
            }
        }

        for (poi in toRemove) {
            list.remove(poi)
        }

        return if (list.isNullOrEmpty()) {
            Observable.empty()
        } else {
            Observable.just(list)
        }
    }

    private fun hidePoi(identifier: String): Boolean {
        var _toRemove = false
        transportModes?.forEach {
            if (identifier.contains(it.id ?: "") && !_toRemove) {
                _toRemove = true
            }
        }
        return _toRemove
    }

    init {
        goToMyLocationRepository.myLocation
            .map<Try<Location>> { result: Try<GeoPoint> ->
                when (result) {
                    is Success -> Success(createMyLocationViewModel(result))
                    is Failure -> Failure<Location>(result())
                    else -> null
                }
            }
            .subscribeOn(Schedulers.io())
            .subscribe({
                when (it) {
                    is Success -> _myLocation.accept(it())
                    is Failure -> _myLocationError.accept(it())
                }
            }, errorLogger::trackError)
            .autoClear()

        viewportChanged.hide()
            .debounce(500, TimeUnit.MILLISECONDS)!!
            .distinctViewPortUntilChanged(getCellIdsFromViewPort)
            .switchMapDelayError {
                fetchStopsByViewport.execute(it).toObservable<Unit>()
            }
            .subscribe({}, { errorLogger.logError(it) })
            .autoClear()
    }

    fun clearCarPods() {
        fetchStopsByViewport.clearData(FetchStopsByViewport.ClearDataType.CAR_PODS)?.subscribe({
            Log.e("MapViewModel", "cleared")
        }, {
            it.printStackTrace()
        })?.autoClear()
    }

    fun goToMyLocation() = goToMyLocationRepository.goToMyLocation()

    fun getInitialCameraUpdate(): Observable<CameraUpdate> =
        getInitialMapCameraPosition.execute()
            .map { it.toCameraPosition() }
            .map { CameraUpdateFactory.newCameraPosition(it) }
            .observeOn(AndroidSchedulers.mainThread())

    fun putCameraPosition(cameraPosition: CameraPosition?): Completable =
        Observable.just<CameraPosition>(cameraPosition)
            .map { it.toMapCameraPosition() }
            .flatMap { putMapCameraPosition.execute(it) }
            .ignoreElements()

    fun getOriginPinUpdate(): Observable<PinUpdate> =
        pinUpdateRepository.getOriginPinUpdate()

    fun getDestinationPinUpdate(): Observable<PinUpdate> =
        pinUpdateRepository.getDestinationPinUpdate()

    private fun createMyLocationViewModel(result: Success<GeoPoint>): Location =
        Location(
            result().latitude,
            result().longitude
        ).also {
            it.name = resources.getString(R.string.current_location)
        }

    fun onViewPortChanged(viewPort: ViewPort) = viewportChanged.accept(viewPort)

}

private fun Observable<ViewPort>.distinctViewPortUntilChanged(
    getCellIdsFromViewPort: GetCellIdsFromViewPort
): Observable<ViewPort> {
    return this
        .flatMap { viewPort ->
            getCellIdsFromViewPort.execute(viewPort)
                .map { viewPort to it }
        }
        .distinctUntilChanged { a, b -> a.second == b.second }
        .map { it.first }
}

sealed class ViewPort(val zoom: Float, val visibleBounds: LatLngBounds) {
    class CloseEnough(zoom: Float, visibleBounds: LatLngBounds) : ViewPort(zoom, visibleBounds)
    class NotCloseEnough(zoom: Float, visibleBounds: LatLngBounds) : ViewPort(zoom, visibleBounds)

    fun isInner(): Boolean = ZoomLevel.fromLevel(zoom) == ZoomLevel.INNER
}