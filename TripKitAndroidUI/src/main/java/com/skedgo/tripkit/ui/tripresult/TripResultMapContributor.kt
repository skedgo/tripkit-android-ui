package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.google.android.gms.maps.model.UrlTileProvider
import com.google.maps.android.collections.MarkerManager
import com.skedgo.rxtry.toTrySingle
import com.skedgo.tripkit.common.util.PolyUtil
import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.UnableToFetchBitmapError
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.data.location.toLatLng
import com.skedgo.tripkit.ui.map.*
import com.skedgo.tripkit.ui.map.adapter.SegmentInfoWindowAdapter
import com.skedgo.tripkit.ui.map.adapter.ServiceStopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.utils.correctItemType
import com.squareup.picasso.Picasso
import dagger.Lazy
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.inject.Inject


class TripResultMapContributor : TripKitMapContributor {
    private var travelledStopMarkers: MarkerManager.Collection? = null
    private var vehicleMarkers: MarkerManager.Collection? = null
    private var segmentMarkers: MarkerManager.Collection? = null
    private var nonTravelledStopMarkers: MarkerManager.Collection? = null
    private var alertMarkers: MarkerManager.Collection? = null

    private val marker2SegmentCache: HashMap<Marker, TripSegment> = LinkedHashMap()
    private val alertIdToMarkerCache: HashMap<Long, Marker> = LinkedHashMap()
    private val tripLines = Collections.synchronizedList(ArrayList<Polyline>())
    private val tripLinesTravelled = Collections.synchronizedList(ArrayList<Polyline>())

    @Inject
    lateinit var segmentStopMarkerMaker: SegmentStopMarkerMaker

    @Inject
    lateinit var alertMarkerMaker: ServiceAlertMarkerMaker

    @Inject
    lateinit var picasso: Picasso

    @Inject
    lateinit var serviceStopCalloutAdapter: ServiceStopInfoWindowAdapter

    @Inject
    lateinit var segmentCalloutAdapter: SegmentInfoWindowAdapter

    @Inject
    lateinit var vehicleMarkerCreatorLazy: Lazy<TripVehicleMarkerCreator>

    @Inject
    lateinit var vehicleMarkerIconFetcherLazy: Lazy<VehicleMarkerIconFetcher>

    @Inject
    lateinit var alertMarkerIconFetcherLazy: Lazy<AlertMarkerIconFetcher>

    @Inject
    lateinit var createSegmentMarkers: CreateSegmentMarkers

    @Inject
    lateinit var getTripLineLazy: Lazy<GetTripLine>

    @Inject
    lateinit var viewModel: TripResultMapViewModel

    @Inject
    lateinit var errorLogger: ErrorLogger

    @Inject
    lateinit var segmentMarkerIconMaker: SegmentMarkerIconMaker

    var markerManager: MarkerManager? = null
    protected val autoDisposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }


    private lateinit var map: GoogleMap
    private var tileProvider: TileProvider? = null
    private var tileOverlays = mutableListOf<TileOverlay>()

    private val geoFenceCircleMarkers = mutableListOf<Circle>()

    fun setTileProvide(url: String) {
        tileProvider = object : UrlTileProvider(256, 256) {
            override fun getTileUrl(x: Int, y: Int, zoom: Int): URL? {

                val updatedUrl =
                    url.replace("{x}", x.toString())
                        .replace("{y}", y.toString())
                        .replace("{z}", zoom.toString())

                return try {
                    URL(updatedUrl)
                } catch (e: MalformedURLException) {
                    throw AssertionError(e)
                }
            }
        }


        if(this::map.isInitialized) {
            tileOverlays
        }
    }

    fun setTileProvider(context: Context, urls: List<String>) {

        tileProvider = CustomUrlTileProvider(urls).apply {
            mapLoaded = {
                //drawSegmentMarkers(context) no need to re-draw
            }
        }

        if(this::map.isInitialized && tileOverlays.isEmpty()) {
            tileProvider?.let {
                tileOverlays.add(
                    map.addTileOverlay(
                        TileOverlayOptions()
                            .tileProvider(it)
                    )
                )
            }
        }
    }

    private fun removeTileOverlay() {
        tileOverlays.forEach { it.remove() }
        tileOverlays.clear()
    }

    override fun initialize() {
        TripKitUI.getInstance().tripDetailsComponent().inject(this)
        segmentCalloutAdapter!!.setSegmentCache(marker2SegmentCache)
    }

    override fun setup() {

    }

    override fun safeToUseMap(context: Context, map: GoogleMap) {
        this.map = map
        markerManager = MarkerManager(map).apply {
            travelledStopMarkers = newCollection("travelledStopMarkers")
            vehicleMarkers = newCollection("vehicleMarkers")
            segmentMarkers = newCollection("segmentMarkers")
            nonTravelledStopMarkers = newCollection("nonTravelledStopMarkers")
            alertMarkers = newCollection("alertMarkers")

            travelledStopMarkers!!.setInfoWindowAdapter(serviceStopCalloutAdapter)
            nonTravelledStopMarkers!!.setInfoWindowAdapter(serviceStopCalloutAdapter)
            segmentMarkers!!.setInfoWindowAdapter(segmentCalloutAdapter)
            val listener = GoogleMap.OnInfoWindowClickListener { marker: Marker -> val segment = marker.tag as TripSegment? }
            segmentMarkers!!.setOnInfoWindowClickListener(listener)
            alertMarkers!!.setOnInfoWindowClickListener(listener)
            map.setOnInfoWindowClickListener(markerManager)
            map.isIndoorEnabled = false
            map.uiSettings.isRotateGesturesEnabled = true

            drawSegmentMarkers(context)

            autoDisposable.add(viewModel!!.vehicleMarkerViewModels
                    .subscribe(
                            { it: List<VehicleMarkerViewModel> ->
                                showVehicleMarkers(context, it, vehicleMarkers!!)
                            }
                    ) { error: Throwable? ->
                        errorLogger!!.trackError(error!!)
                    }
            )
            autoDisposable.add(viewModel!!.alertMarkerViewModels
                    .subscribe(
                            { it: List<AlertMarkerViewModel> ->
                                showAlertMarkers(it, alertMarkers!!)
                            }
                    ) { error: Throwable? ->
                        errorLogger!!.trackError(error!!)
                    }
            )
            autoDisposable.add(viewModel!!.travelledStopMarkerViewModels
                    .subscribe(
                            { it: List<StopMarkerViewModel> ->
                                showStopMarkers(it, travelledStopMarkers!!)
                            }
                    ) { error: Throwable? ->
                        errorLogger!!.trackError(error!!)
                    }
            )
            autoDisposable.add(viewModel!!.nonTravelledStopMarkerViewModels
                    .subscribe(
                            { it: List<StopMarkerViewModel> ->
                                showStopMarkers(it, nonTravelledStopMarkers!!)
                            }
                    ) { error: Throwable? ->
                        errorLogger!!.trackError(error!!)
                    }
            )
            autoDisposable.add(viewModel!!.segments
                    .flatMap { segments: List<TripSegment> -> getTripLineLazy!!.get().execute(segments) }
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { polylineOptionsList: List<SegmentsPolyLineOptions> ->
                                showTripLinesWithTravelledChecking(map, polylineOptionsList)
                            }
                    ) { error: Throwable? ->
                        errorLogger!!.trackError(error!!)
                    }
            )
            autoDisposable.add(viewModel!!.onTripSegmentTapped()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ (first, second) ->
                        map.animateCamera(first)
                        showMarkerForSegment(map, second)
                    }) { error: Throwable? -> errorLogger!!.trackError(error!!) })

            autoDisposable.add(viewModel.tripCameraUpdate
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ cameraUpdate ->
                        map.animateCamera(cameraUpdate)
                    }, { Timber.e(it) }))
        }
    }

    private fun drawSegmentMarkers(context: Context) {
        autoDisposable.add(viewModel.segments
            .flatMap { it: List<TripSegment> -> createSegmentMarkers!!.execute(it) }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { it: List<Pair<TripSegment, MarkerOptions>> ->
                    showSegmentMarkers(context, it, segmentMarkers!!)
                }
            ) { error: Throwable? ->
                errorLogger.trackError(error!!)
            }
        )
    }

    override fun getInfoContents(marker: Marker): View? {
        return markerManager?.getInfoContents(marker)
    }

    override fun cleanup() {
        autoDisposable.clear()
        travelledStopMarkers?.clear()
        vehicleMarkers?.clear()
        segmentMarkers?.clear()
        nonTravelledStopMarkers?.clear()
        alertMarkers?.clear()
        marker2SegmentCache.clear()
        alertIdToMarkerCache.clear()
        tripLines.forEach { it.remove() }
        tripLines.clear()
        tripLinesTravelled.clear()
        removeTileOverlay()
        tileProvider?.let {
            if(it is CustomUrlTileProvider) {
                it.clear()
            }
        }
    }

    fun setTripGroupId(tripGroupId: String?, tripId: Long? = null) {
        viewModel!!.setTripGroupId(tripGroupId!!, tripId)
    }

    @Synchronized
    private fun showTripLinesWithTravelledChecking(
        map: GoogleMap,
        segmentsPolyLineOptions: List<SegmentsPolyLineOptions>
    ) {
        // To remove old lines before adding new ones.
        for (line in tripLines) {
            line.remove()
        }
        tripLines.clear()
        tripLinesTravelled.clear()
        segmentsPolyLineOptions.forEach { segment ->
            segment.polyLineOptions.forEach { polylineOption ->
                polylineOption.zIndex(2.0f)
                val polyLine = map.addPolyline(polylineOption)
                tripLines.add(polyLine)
                if (segment.isTravelled) {
                    tripLinesTravelled.add(polyLine)
                }
            }
        }
    }

    @Synchronized
    private fun showTripLines(map: GoogleMap, polylineOptionsList: List<PolylineOptions>) {
        // To remove old lines before adding new ones.
        for (line in tripLines) {
            line.remove()
        }
        tripLines.clear()
        for (polylineOption in polylineOptionsList) {
            polylineOption.zIndex(2.0f)
            tripLines.add(map.addPolyline(polylineOption))
        }
    }

    fun resetTripLineTravelled() {
        tripLinesTravelled.forEach {
            it.color = it.color.removeAlpha()
        }

        val builder = LatLngBounds.Builder()
        tripLinesTravelled.forEach {
            for (point in it.points) {
                builder.include(point)
            }
        }

        val bounds = builder.build()

        // Move the camera to focus on the bounds
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50)
        map.animateCamera(cameraUpdate)
    }

    /* Removed temporarily as still needs optimization, causes the app to lag.
    fun focusTripLine(segment: TripSegment) {
        val segmentPolyLines = segment.getPolyLines()

        updateTravelledPolyLinesHighlight(segmentPolyLines)

        if (segmentPolyLines.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            segmentPolyLines.forEach {
                for (point in it.points) {
                    builder.include(point)
                }
            }

            val bounds = builder.build()

            // Move the camera to focus on the bounds
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 50)
            map.animateCamera(cameraUpdate)
        } else if (segment.singleLocation != null) {
            val cameraUpdate =
                CameraUpdateFactory.newLatLngZoom(segment.singleLocation.toLatLng(), 20f)
            map.animateCamera(cameraUpdate)
        }
    }
    */

    private fun updateTravelledPolyLinesHighlight(segmentPolyLines: List<Polyline>) {
        tripLinesTravelled.forEach { polyLine ->
            if (segmentPolyLines.any { it == polyLine }) {
                polyLine.color = polyLine.color.removeAlpha()
            } else {
                polyLine.color = polyLine.color.adjustAlpha(0.25f)
            }
        }
    }

    private fun TripSegment.getPolyLines() =
        if (this.streets != null) {
            tripLinesTravelled.filter {
                it.points.any { point ->
                    this.streets?.filter { it.encodedWaypoints() != null }?.any { street ->
                        PolyUtil.decode(street.encodedWaypoints())
                            .zipWithNext()
                            .any { (start, end) ->
                                (point.latitude == start.latitude && point.longitude == start.longitude) ||
                                        (point.latitude == end.latitude && point.longitude == end.longitude)
                            }
                    } ?: false
                }
            }
        } else {
            tripLinesTravelled.filter {
                it.points.any { point ->
                    this.shapes?.filter { it.isTravelled }?.any { shape ->
                        PolyUtil.decode(shape.encodedWaypoints)
                            .orEmpty().zipWithNext()
                            .any { (start, end) ->
                                (point.latitude == start.latitude && point.longitude == start.longitude) ||
                                        (point.latitude == end.latitude && point.longitude == end.longitude)
                            }
                    } ?: false
                }
            }
        }

    private fun Int.adjustAlpha(alpha: Float): Int {
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        val alphaValue = (alpha * 255).toInt()
        return ColorUtils.setAlphaComponent(Color.rgb(red, green, blue), alphaValue)
    }

    private fun Int.removeAlpha(): Int {
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        return Color.rgb(red, green, blue)
    }

    @Synchronized
    private fun showVehicleMarkers(
            context: Context,
            vehicleMarkerViewModels: List<VehicleMarkerViewModel>,
            vehicleMarkers: MarkerManager.Collection) {
        vehicleMarkers.clear()
        for ((segment) in vehicleMarkerViewModels) {
            createVehicleMarker(context, segment, vehicleMarkers)
        }
    }

    private fun createVehicleMarker(context: Context, segment: TripSegment, vehicleMarkers: MarkerManager.Collection) {
        val vehicleMarkerOptions = vehicleMarkerCreatorLazy!!.get().call(context.resources, segment)
        val marker = vehicleMarkers.addMarker(vehicleMarkerOptions)
        vehicleMarkerIconFetcherLazy!!.get().call(marker, segment.realTimeVehicle)
    }

    private fun showAlertMarkers(alertMarkerViewModels: List<AlertMarkerViewModel>, alertMarkers: MarkerManager.Collection) {
        alertMarkers.clear()
        alertIdToMarkerCache.clear()
        for ((alert, segment) in alertMarkerViewModels) {
            val marker = alertMarkers.addMarker(alertMarkerMaker!!.make(alert))
            marker.tag = segment
            alertMarkerIconFetcherLazy!!.get().call(marker, alert)
            alertIdToMarkerCache[alert.remoteHashCode()] = marker
        }
    }


    private fun setMyLocationEnabled() {
//    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
    }

    @Synchronized
    private fun showSegmentMarkers(
            context: Context,
            segmentMarkerViewModels: List<Pair<TripSegment, MarkerOptions>>,
            segmentMarkers: MarkerManager.Collection) {
        segmentMarkers.clear()
        for (viewModel in segmentMarkerViewModels) {
            showSegmentMarker(context, viewModel, segmentMarkers)
        }
    }

    private fun showSegmentMarker(
            context: Context,
            segmentMarkerViewModel: Pair<TripSegment, MarkerOptions>,
            segmentMarkers: MarkerManager.Collection) {
        val segment = segmentMarkerViewModel.first
        val marker = segmentMarkers.addMarker(segmentMarkerViewModel.second)
        marker.tag = segment
        val url = TransportModeUtils.getIconUrlForModeInfo(context.resources, segment.modeInfo)
        if (url != null) {
            autoDisposable.add(picasso.fetchAsync(url)
                    .map { it: Bitmap? -> BitmapDrawable(context.resources, it) }
                    .map { it: BitmapDrawable? -> segmentMarkerIconMaker!!.make(segment, it) }
                    .compose(toTrySingle { error: Throwable? -> error is UnableToFetchBitmapError })
                    .toObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(SetSegmentMarkerIcon(marker), Consumer { error: Throwable? -> errorLogger!!.trackError(error!!) }))
        }
    }

    private fun showStopMarkers(
            stopMarkerViewModels: List<StopMarkerViewModel>,
            stopMarkers: MarkerManager.Collection) {
        // To clear old stop markers before adding new ones.
        stopMarkers.clear()

        // To add new stop markers.
        for (viewModel in stopMarkerViewModels) {
            val marker = stopMarkers.addMarker(segmentStopMarkerMaker!!.make(viewModel))
            marker.tag = viewModel
        }
    }

    private fun showMarkerForSegment(map: GoogleMap, segmentId: Long) {
        val entrySet: Set<Map.Entry<Marker, TripSegment>> = marker2SegmentCache.entries
        for ((marker, markerSegment) in entrySet) {
            if (markerSegment.id == segmentId) {
                marker.showInfoWindow()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15.0f))
            }
        }
    }

    /**
     * Added for for showing and debugging geofences in map.
     */
    fun addCircleToMap(circle: CircleOptions) {
        if (!this::map.isInitialized) {
            return
        }
        map.addCircle(circle)?.apply {
            geoFenceCircleMarkers.add(this)
        }
    }

    fun clearMapCircles() {
        geoFenceCircleMarkers.forEach { it.remove() }
    }

}