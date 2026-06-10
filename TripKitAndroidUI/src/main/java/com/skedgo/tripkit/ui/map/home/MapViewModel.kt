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
import com.skedgo.tripkit.ui.data.extensions.withBuffer
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.map.IMapPoiLocation
import com.skedgo.tripkit.ui.map.LoadPOILocationsByViewPort
import com.skedgo.tripkit.ui.map.ScheduledStopRepository
import com.squareup.picasso.Picasso
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber

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
    private val scheduledStopRepository: ScheduledStopRepository,
    private val errorLogger: ErrorLogger
) : RxViewModel() {
    private val _myLocationError: PublishRelay<Throwable> = PublishRelay.create()
    val myLocationError: Observable<Throwable>
        get() = _myLocationError.hide()

    private val _myLocation: PublishRelay<Location> = PublishRelay.create()
    val myLocation: Observable<Location>
        get() = _myLocation.hide()

    var showMarkers = ObservableBoolean(true)

    var notIncludedTransportModes: List<TransportMode>? = null

    private val viewportChanged = PublishRelay.create<ViewPort>()
    private var lastViewPort: ViewPort? = null

    private data class VisibleCellState(
        val viewPort: ViewPort,
        val cellHashes: Map<String, Long?>
    )

    private companion object {
        private const val DEBUG_CELL_MARKER_RETENTION = false
    }

    val markers = viewportChanged.hide()
        .debounce(400, TimeUnit.MILLISECONDS)
        .flatMap { viewPort ->
            getCellIdsFromViewPort.fetch(viewPort)
                .flatMap { cellIds ->
                    scheduledStopRepository.getCellHashCodes(cellIds)
                        .map { hashByCell ->
                            VisibleCellState(
                                viewPort = viewPort,
                                cellHashes = hashByCell
                            )
                        }
                }
        }
        .distinctUntilChanged { previousState, newState ->
            val decision = shouldSuppressMarkerReload(
                previousVisibleCellHashes = previousState.cellHashes,
                newVisibleCellHashes = newState.cellHashes,
                previousBounds = previousState.viewPort.visibleBounds,
                newBounds = newState.viewPort.visibleBounds
            )
            val delta = decision.cellDelta
            if (DEBUG_CELL_MARKER_RETENTION) {
                Timber.d(
                    "cell-retention: mode=%s zoom=%.2f prevCells=%d newCells=%d entered=%d exited=%d stable=%d changed=%d unchanged=%d missingHash=%d prevBounds=%s newBounds=%s boundsChanged=%b boundsExpanded=%b suppress=%b reason=%s sampleCells=%s",
                    markerModeForLog(newState.viewPort.zoom),
                    newState.viewPort.zoom,
                    previousState.cellHashes.size,
                    newState.cellHashes.size,
                    delta.enteredCells.size,
                    delta.exitedCells.size,
                    delta.stableCells.size,
                    delta.changedCells.size,
                    delta.unchangedCells.size,
                    delta.missingHashCount,
                    summarizeBounds(previousState.viewPort.visibleBounds),
                    summarizeBounds(newState.viewPort.visibleBounds),
                    decision.boundsDelta.changed,
                    decision.boundsDelta.expanded,
                    decision.suppressReload,
                    decision.reason,
                    newState.cellHashes.keys.sorted().take(3).joinToString(",")
                )
            }
            decision.suppressReload
        }
        .map {
            it.viewPort
        }
        .observeOn(Schedulers.io())
        .switchMap { viewPort ->
            if (showMarkers.get()) {
                loadPOILocationsByViewPort.fetch(viewPort)
            } else {
                Observable.empty()
            }
        }
        .switchMap {
            hidePoi(it.toMutableList())
        }
        .compose(
            DiffTransformer<IMapPoiLocation, MarkerOptions>(
                { it.identifier },
                { it.createMarkerOptions(resources, picasso) }
            )
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
        notIncludedTransportModes?.forEach {
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
            .switchMapDelayError { viewPort ->
                // Fetch only the actually-visible viewport. The previous 3x buffer prefetch
                // was firing a 9x-area POST on every distinct viewport change and amplified
                // the API volume (#25753). The data-layer TTL cache + in-flight de-dup
                // (LocationsFetchCoordinator) now make adjacent panning cheap on its own.
                fetchStopsByViewport.fetch(viewPort).toObservable<Unit>()
            }
            .subscribe({
            }, { errorLogger.logError(it) })
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

    fun onViewPortChanged(viewPort: ViewPort) {
        lastViewPort = viewPort
        viewportChanged.accept(viewPort)
    }

    fun prefetchMarkersForRegion(zoom: Float, bounds: LatLngBounds) {
        // Previously this method fired four /satapp/locations.json POSTs per invocation
        // (1.5x direct + 5.0x direct + 1.5x via relay + 4.5x via relay's broader prefetch),
        // which compounded the API explosion (#25753) every time the user picked a city
        // or the app reached the home screen.
        //
        // Now we just nudge the viewport pipeline with a small 1.5x buffer so the visible
        // area + an immediate margin is loaded. Cache TTL + in-flight de-dup in
        // LocationsFetchCoordinator prevent duplicate calls if the camera-change listener
        // emits a near-identical viewport at the same time.
        val primed = bounds.withBuffer(1.5)
        val primedViewport = ViewPort.CloseEnough(zoom, primed)
        viewportChanged.accept(primedViewport)
    }

    private fun summarizeBounds(bounds: LatLngBounds): String {
        return "[sw=(%.4f,%.4f),ne=(%.4f,%.4f)]".format(
            bounds.southwest.latitude,
            bounds.southwest.longitude,
            bounds.northeast.latitude,
            bounds.northeast.longitude
        )
    }

    private fun markerModeForLog(zoom: Float): String {
        return when {
            zoom <= ZoomLevel.ZOOM_VALUE_TO_SHOW_CITIES -> "CITY_ONLY"
            zoom > ZoomLevel.ZOOM_START_VALUE_FOR_LOCAL -> "REGION_AND_LOCAL"
            else -> "REGION_ONLY"
        }
    }
}

sealed class ViewPort(val zoom: Float, val visibleBounds: LatLngBounds) {
    class CloseEnough(zoom: Float, visibleBounds: LatLngBounds) : ViewPort(zoom, visibleBounds)
    class NotCloseEnough(zoom: Float, visibleBounds: LatLngBounds) : ViewPort(zoom, visibleBounds)

    fun isInner(): Boolean = zoom >= ZoomLevel.ZOOM_START_VALUE_FOR_LOCAL
}