package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.common.util.DateTimeFormats
import com.skedgo.tripkit.common.util.StringUtils
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.map.TimeLabelMaker
import com.skedgo.tripkit.ui.map.VehicleMarkerIconCreator
import com.skedgo.tripkit.ui.map.adapter.ServiceStopInfoWindowAdapter
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMapViewModel
import com.skedgo.tripkit.ui.map.servicestop.ServiceStopMarkerCreator
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import com.skedgo.tripkit.ui.realtime.RealTimeViewModelFactory
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText
import com.skedgo.tripkit.ui.utils.MapUtils.animateMarkerToPosition
import com.skedgo.tripkit.ui.utils.MapUtils.animatePulseOverlay
import com.skedgo.tripkit.ui.utils.MapUtils.calculateAgeFactor
import com.skedgo.tripkit.ui.utils.MapUtils.calculateFadeFromAgeFactor
import com.skedgo.tripkit.ui.utils.MapUtils.formatElapsedTime
import com.skedgo.tripkit.ui.utils.MapUtils.getBitmapFromDrawable
import com.skedgo.tripkit.ui.utils.MapUtils.hidePulseOverlay
import com.skedgo.tripkit.ui.utils.MapUtils.updateMarkerOpacity
import com.skedgo.tripkit.ui.utils.MapUtils.updateOverlayTransparency
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject


class TimetableMapContributor(val fragment: Fragment) : TripKitMapContributor {
    protected val autoDisposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    @Inject
    lateinit var regionService: RegionService

    @Inject
    lateinit var vehicleMarkerIconCreatorLazy: Lazy<VehicleMarkerIconCreator>

    @Inject
    lateinit var realTimeViewModelFactory: RealTimeViewModelFactory

    @Inject
    lateinit var getStopDisplayText: GetStopDisplayText

    @Inject
    lateinit var errorLogger: ErrorLogger

    @Inject
    lateinit var viewModel: ServiceStopMapViewModel

    @Inject
    lateinit var serviceStopCalloutAdapter: ServiceStopInfoWindowAdapter

    private var mStop: ScheduledStop? = null
    private var service: TimetableEntry? = null
    private var realTimeVehicleMarker: Marker? = null
    private val stopCodesToMarkerMap = HashMap<String, Marker>()
    private val serviceLines = Collections.synchronizedList(ArrayList<Polyline>())
    private var googleMap: GoogleMap? = null

    private var previousCameraPosition: CameraPosition? = null

    private var pulseOverlay: GroundOverlay? = null

    private val handler = android.os.Handler()

    override fun initialize() {
        TripKitUI.getInstance()
            .serviceStopMapComponent()
            .inject(this)

        val realTimeViewModel: RealTimeChoreographerViewModel =
            ViewModelProviders.of(fragment, realTimeViewModelFactory)
                .get(RealTimeChoreographerViewModel::class.java)

        viewModel.realtimeViewModel = realTimeViewModel
        val timeTextView =
            fragment.layoutInflater.inflate(R.layout.view_time_label, null) as TextView
        val timeLabelMaker = TimeLabelMaker(timeTextView)
        val serviceStopMarkerCreator =
            ServiceStopMarkerCreator(fragment.requireContext(), timeLabelMaker)
        viewModel.serviceStopMarkerCreator = serviceStopMarkerCreator
    }

    override fun setup() {

    }

    private val fadeRunnable = object : Runnable {
        override fun run() {
            updateVehicleMarkerAppearance()
            handler.postDelayed(this, 1000) // Schedule next update after 3 seconds
        }
    }

    override fun safeToUseMap(context: Context, map: GoogleMap) {

        googleMap = map
        previousCameraPosition = map.cameraPosition

        googleMap?.setOnCameraIdleListener {
            val zoomLevel = googleMap?.cameraPosition?.zoom ?: return@setOnCameraIdleListener
            animatePulseOverlay(pulseOverlay, zoomLevel)
        }

        // Start periodic updates
        startMarkerUpdateInterval()

        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mStop!!.lat, mStop!!.lon), 15.0f))

        autoDisposable.add(
            viewModel.drawStops
                .subscribe({ (newMarkerOptions, removedStopIds) ->
                    for (id in removedStopIds) {
                        stopCodesToMarkerMap[id]!!.remove()
                        stopCodesToMarkerMap.remove(id)
                    }
                    for ((first, second) in newMarkerOptions) {
                        val marker = map.addMarker(first)
                        stopCodesToMarkerMap[second!!] = marker
                    }
                }, {})
        )


        autoDisposable.add(viewModel.viewPort
            .subscribe { coordinates: List<LatLng>? -> this.centerMapOver(map, coordinates) })

        autoDisposable.add(viewModel.drawServiceLine
            .subscribe { polylineOptions: List<PolylineOptions?> ->
                for (line in serviceLines) {
                    line.remove()
                }
                serviceLines.clear()
                val builder = LatLngBounds.Builder()
                for (polylineOption in polylineOptions) {
                    serviceLines.add(map.addPolyline(polylineOption))

                    polylineOption?.let {
                        for (point in it.points) {
                            builder.include(point)
                        }
                    }
                }

                val bounds = builder.build()
                val padding = 50 // Optional padding around the bounds
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                map.animateCamera(cameraUpdate)
            })

        autoDisposable.add(viewModel.realtimeVehicle
            .subscribe { realTimeVehicleOptional ->
                if (realTimeVehicleOptional.isPresent()) { // Check if the value is present
                    setRealTimeVehicle(realTimeVehicleOptional.get()) // Get the value from OptionalCompat
                } else {
                    setRealTimeVehicle(null) // Handle empty OptionalCompat
                }
            })
    }

    override fun getInfoContents(marker: Marker): View? {
        return serviceStopCalloutAdapter.getInfoContents(marker)
    }

    override fun cleanup() {
        stopMarkerUpdateInterval() // Stop periodic updates
        stopCodesToMarkerMap.forEach { it.value.remove() }
        serviceLines.forEach { it.remove() }
        autoDisposable.clear()
        cleanupServiceDetailVehicleUpdates()
    }

    private fun cleanupServiceDetailVehicleUpdates() {
        // Stop real-time updates
        viewModel.stopRealtimeUpdates()

        // Cleanup pulse animation
        hidePulseOverlay(pulseOverlay)
        pulseOverlay = null

        // Safely remove the real-time vehicle marker if it exists
        realTimeVehicleMarker?.let { marker ->
            marker.remove()
            realTimeVehicleMarker = null // Clear the reference to avoid memory leaks
            Timber.d("Real-time vehicle marker removed")
        }
    }

    fun setService(service: TimetableEntry?) {
        viewModel.service.accept(service)
        this.service = service
    }

    fun setStop(stop: ScheduledStop?) {
        mStop = stop
        viewModel.stop.accept(stop)
    }

    private fun centerMapOver(map: GoogleMap, coordinates: List<LatLng>?) {
        if (coordinates != null && coordinates.size > 0) {
            val builder = LatLngBounds.Builder()
            for (coordinate in coordinates) {
                builder.include(coordinate)
            }
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 320))
        }
    }

    fun serviceStopClick(stop: ServiceStop) {
        if (!TextUtils.isEmpty(stop.code)) {
            val marker = stopCodesToMarkerMap[stop.code]
            if (marker != null) {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                marker.showInfoWindow()
            }
        }
    }

    private fun setRealTimeVehicle(realTimeVehicle: RealTimeVehicle?) {
        googleMap?.let { map ->
            realTimeVehicleMarker?.let { marker ->
                // Animate existing marker if it already exists
                if (realTimeVehicle != null && realTimeVehicle.hasLocationInformation()) {
                    val newLatLng =
                        LatLng(realTimeVehicle.location.lat, realTimeVehicle.location.lon)

                    animateMarkerToPosition(marker, newLatLng)
                    marker.rotation = realTimeVehicle.location.bearing.toFloat()
                    pulseOverlay?.position = newLatLng

                    // Check if the location has changed
                    if (marker.position != newLatLng) {
                        // Update independent last known update time
                        service?.realtimeVehicle?.lastUpdateTime = System.currentTimeMillis()
                    }
                }
                return
            }

            // Create a new marker and pulse overlay if it doesn't exist
            if (realTimeVehicle != null && realTimeVehicle.hasLocationInformation()) {
                if (service != null && TextUtils.equals(
                        realTimeVehicle.serviceTripId,
                        service!!.serviceTripId
                    )
                ) {
                    realTimeVehicle.lastUpdateTime = System.currentTimeMillis()
                    service!!.realtimeVehicle = realTimeVehicle
                    createVehicleMarker(realTimeVehicle)
                }
            }
        }
    }

    private fun createVehicleMarker(vehicle: RealTimeVehicle) {
        var title: String? = null
        if (TextUtils.isEmpty(service!!.serviceNumber)) {
            title = "Your upcoming service"
        } else {
            if (mStop != null && mStop!!.type != null) {
                title =
                    StringUtils.capitalizeFirst(mStop!!.type.toString()) + " " + service!!.serviceNumber
            }
            if (TextUtils.isEmpty(title)) {
                title = "Service " + service!!.serviceNumber
            }
        }
        val bearing = if (vehicle.location == null) 0 else vehicle.location.bearing
        val color =
            if (service!!.serviceColor == null || service!!.serviceColor.color == Color.BLACK) fragment.resources.getColor(
                R.color.v4_color
            ) else service!!.serviceColor.color
        val text =
            (if (TextUtils.isEmpty(service!!.serviceNumber)) if (mStop == null || mStop!!.type == null) "" else StringUtils.capitalizeFirst(
                mStop!!.type.toString()
            ) else service!!.serviceNumber)!!
        val icon = vehicleMarkerIconCreatorLazy.get().call(bearing, color, text)
        val markerTitle = title
        googleMap?.let { map: GoogleMap ->
            val millis = vehicle.lastUpdateTime * 1000
            val time = DateTimeFormats.printTime(fragment.context, millis, null)
            val location = LatLng(vehicle.location.lat, vehicle.location.lon)
            val snippet: String = if (TextUtils.isEmpty(vehicle.label)) {
                "Real-time location as at $time"
            } else {
                "Vehicle " + vehicle.label + " location as at " + time
            }
            realTimeVehicleMarker = map.addMarker(
                MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(icon))
                    .rotation(bearing.toFloat())
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .title(markerTitle)
                    .snippet(snippet)
                    .position(location)
                    .draggable(false)
            )

            // Cleanup pulse animation
            hidePulseOverlay(pulseOverlay)
            pulseOverlay = null

            showPulseOverlay(color, location, map)

            // Get the current zoom level
            val zoomLevel = map.cameraPosition.zoom

            // Start the pulse animation with zoom level
            animatePulseOverlay(pulseOverlay, zoomLevel)
        }
    }

    private fun showPulseOverlay(color: Int, location: LatLng, map: GoogleMap) {
        // Create the pulse overlay
        val bitmap = getBitmapFromDrawable(
            fragment.requireContext(),
            R.drawable.pulse_circle,
            125,
            125,
            color
        ) // Convert drawable to Bitmap
        val overlayOptions = GroundOverlayOptions()
            .position(location, 100f) // Initial size in meters
            .image(BitmapDescriptorFactory.fromBitmap(bitmap))
            .transparency(0.5f)

        pulseOverlay = map.addGroundOverlay(overlayOptions)
    }

    private fun updateVehicleMarkerAppearance() {
        val realTimeVehicle = service?.realtimeVehicle ?: return

        realTimeVehicleMarker?.let { marker ->
            pulseOverlay?.let { overlay ->
                // Calculate time since the last known update
                val currentTimeMillis = System.currentTimeMillis()
                val lastUpdateTimeMillis = realTimeVehicle.lastUpdateTime // Already in milliseconds
                val ageInSeconds =
                    ((currentTimeMillis - lastUpdateTimeMillis) / 1000).coerceAtLeast(1) // Start from 1 second

                // Calculate age factor and fade level
                val ageFactor = calculateAgeFactor(ageInSeconds)
                val fadeLevel = calculateFadeFromAgeFactor(ageFactor)

                // Update marker opacity and snippet
                updateMarkerOpacity(marker, fadeLevel)
                marker.snippet = formatElapsedTime(ageInSeconds, realTimeVehicle)

                if (ageFactor < 0.1f) {
                    pulseOverlay?.isVisible = false
                } else {
                    pulseOverlay?.isVisible = true
                    // Update overlay transparency using age factor directly
                    updateOverlayTransparency(overlay, fadeLevel)
                }
            }
        }
    }


    /**
     * Starts the periodic updates for marker fading and snippet updates.
     */
    private fun startMarkerUpdateInterval() {
        // Delay the first execution to avoid immediate update showing "1 second ago" twice
        handler.postDelayed(fadeRunnable, 1000) // 1-second delay
    }

    /**
     * Stops the periodic updates for marker fading and snippet updates.
     */
    private fun stopMarkerUpdateInterval() {
        handler.removeCallbacks(fadeRunnable)
    }


    fun getMapPreviousPosition(): CameraPosition? {
        return previousCameraPosition
    }
}