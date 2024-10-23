package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.map.GetTripLine
import com.skedgo.tripkit.ui.map.PolylineConfig
import com.skedgo.tripkit.ui.map.SegmentsPolyLineOptions
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.home.getFromAndToMarkerBitmap
import com.skedgo.tripkit.ui.tripresults.TripResultListViewModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import java.util.Collections
import javax.inject.Inject

class TripResultListMapContributor(
    val viewModel: TripResultListViewModel
) : TripKitMapContributor {

    @Inject
    lateinit var getTripLineLazy: GetTripLine

    private var context: Context? = null
    private lateinit var map: GoogleMap
    private var isSafeToUse = false
    private val autoDisposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }
    private val tripLines = Collections.synchronizedList(ArrayList<Polyline>())
    private val tripLinesOptions = Collections.synchronizedList(ArrayList<PolylineOptions>())
    private var origin: Location? = null
    private var destination: Location? = null
    private var originMarker: Marker? = null
    private var originMarkerOptions: MarkerOptions? = null
    private var destinationMarker: Marker? = null
    private var destinationMarkerOptions: MarkerOptions? = null
    private var lastTripUuid: String? = null
    // Indicator when this contributor was initialize from the fragment that's using it.
    // If this is false, it means that it's either not yet initialized or the user
    // moved to another screen
    private var isFromInitialization = false

    override fun initialize() {
        TripKitUI.getInstance().routesComponent().inject(this)
    }

    fun setup(context: Context) {
        this.context = context
        setup()
    }

    override fun setup() {
        if (isSafeToUse) {
            context?.let {
                setupObservers(it)
            }
        }
    }

    override fun safeToUseMap(context: Context, map: GoogleMap) {
        this.context = context
        this.map = map
        isSafeToUse = true
        isFromInitialization = true
        setup()
    }

    override fun getInfoContents(marker: Marker): View? = null

    override fun cleanup() {
        autoDisposable.clear()
        originMarker?.remove()
        destinationMarker?.remove()
        tripLines.forEach { it.remove() }
        tripLines.clear()
        context = null
        isFromInitialization = false
    }

    private fun setupObservers(context: Context) {
        if(!isFromInitialization) {
            showCachedMapElements()
        }
        viewModel.tripResultListStream
            .map {
                if(!isFromInitialization) {
                    clearLinesCache()
                }
                if(it.isNotEmpty()) {
                    lastTripUuid = it.last { it.group.displayTrip != null}.trip.uuid
                }
                it.flatMap {
                    it.group.displayTrip?.segmentList.orEmpty()
                }
            }
            .flatMap { segments ->
                getTripLineLazy.executeForTravelledLine(
                    PolylineConfig(
                        ContextCompat.getColor(context, R.color.trip_line_inactive),
                        ContextCompat.getColor(context, R.color.colorPrimary),
                        lastTripUuid
                    ),
                    segments
                )
            }
            .onErrorResumeNext(Observable.empty())
            .subscribe({ polylineOptionsList ->
                showTripLines(polylineOptionsList)
            }, {
                if (BuildConfig.DEBUG) {
                    it.printStackTrace()
                }
            }).addTo(autoDisposable)
    }

    private fun showCachedMapElements() {
        if(tripLinesOptions.isNotEmpty()) {
            tripLinesOptions.forEach {
                tripLines.add(map.addPolyline(it))
            }
        }
        originMarkerOptions?.let {
            originMarker = map.addMarker(it)
        }
        destinationMarkerOptions?.let {
            destinationMarker = map.addMarker(it)
        }
    }

    private fun clearLinesCache() {
        tripLines.forEach { it.remove() }
        tripLines.clear()
        tripLinesOptions.clear()
    }

    @Synchronized
    private fun showTripLines(
        segmentsPolyLineOptions: List<SegmentsPolyLineOptions>
    ) {
        // To remove old lines before adding new ones.

        tripLines.forEach {
            context?.let { context ->
                it.color = ContextCompat.getColor(context, R.color.trip_line_inactive)
            }
        }

        val boundsBuilder = LatLngBounds.Builder()
        var hasPoints = false
        segmentsPolyLineOptions.forEach { segment ->
            segment.polyLineOptions.forEachIndexed { index, polylineOption ->
                if(polylineOption.zIndex == 0f) {
                    polylineOption.zIndex(2.0f)
                }
                val polyLine = map.addPolyline(polylineOption)
                tripLinesOptions.add(polylineOption)
                tripLines.add(polyLine)
                for (point in polyLine.points) {
                    boundsBuilder.include(point)
                    hasPoints = true
                }
            }
        }

        // Move the camera to focus on the bounds
        if (hasPoints) {
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 30)
            map.moveCamera(cameraUpdate)
        }

        if(originMarker == null && destinationMarker == null) {
            setOriginDestinationLocations(origin, destination)
        }
    }

    fun setOriginDestinationLocations(from: Location?, to: Location?) {
        if(!isSafeToUse || context == null) {
            origin = from
            destination = to
            return
        }
        from?.let { location ->
            val bitmap = BitmapDescriptorFactory.fromBitmap(
                context?.getFromAndToMarkerBitmap(0)
            )
            bitmap?.let {
                originMarkerOptions = MarkerOptions()
                    .position(LatLng(location.lat, location.lon))
                    .icon(bitmap)
                originMarker = map.addMarker(originMarkerOptions)
            }
        }

        to?.let { location ->
            val bitmap = BitmapDescriptorFactory.fromBitmap(
                context?.getFromAndToMarkerBitmap(1)
            )
            bitmap?.let {
                destinationMarkerOptions = MarkerOptions()
                    .position(LatLng(location.lat, location.lon))
                    .icon(bitmap)
                destinationMarker = map.addMarker(destinationMarkerOptions)
            }
        }
    }
}