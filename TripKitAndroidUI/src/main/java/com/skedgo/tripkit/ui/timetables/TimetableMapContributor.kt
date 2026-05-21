package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
import com.skedgo.rxtry.printThrowableStackTrace
import com.skedgo.rxtry.subscribeWithErrorHandling
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

    private val _formattedElapsedTime = MutableLiveData<String>()
    val formattedElapsedTime: LiveData<String> get() = _formattedElapsedTime

    private var mStop: ScheduledStop? = null
    private var service: TimetableEntry? = null
    private var realTimeVehicleMarker: Marker? = null
    private val stopCodesToMarkerMap = HashMap<String, Marker>()
    private val serviceLines = Collections.synchronizedList(ArrayList<Polyline>())
    private var googleMap: GoogleMap? = null

    private var previousCameraPosition: CameraPosition? = null

    private var pulseOverlay: GroundOverlay? = null

    private val handler = Handler(Looper.getMainLooper())

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
            handler.postDelayed(this, 1000) // Schedule next update after 1 second
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

        // Ensure viewModel is initialized before accessing it
        // This can happen when safeToUseMap is called before fragment is attached (e.g., during contributor switching)
        // If not initialized, defer the viewModel subscriptions - they will be set up when safeToUseMap is called again
        // after the fragment is attached (via onAttachFragment callback)
        if (!::viewModel.isInitialized) {
            Timber.w("TimetableMapContributor - viewModel not initialized yet, deferring viewModel subscriptions. Map reference saved, will setup subscriptions after fragment attachment.")
            return
        }

        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mStop!!.lat, mStop!!.lon), 15.0f))

        autoDisposable.add(
            viewModel.drawStops
                .subscribeWithErrorHandling { (newMarkerOptions, removedStopIds) ->
                    for (id in removedStopIds) {
                        stopCodesToMarkerMap[id]!!.remove()
                        stopCodesToMarkerMap.remove(id)
                    }
                    for ((first, second) in newMarkerOptions) {
                        val marker = map.addMarker(first)
                        stopCodesToMarkerMap[second!!] = marker
                    }
                    fitAllMapElementsToBounds()
                }
        )


//        autoDisposable.add(viewModel.viewPort
//            .subscribeWithErrorHandling { coordinates: List<LatLng>? ->
//                this.centerMapOver(map, coordinates) }
//        )

        autoDisposable.add(viewModel.drawServiceLine
            .subscribeWithErrorHandling { polylineOptions: List<PolylineOptions?> ->
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

                fitAllMapElementsToBounds()
            })

        autoDisposable.add(viewModel.realtimeVehicle
            .subscribeWithErrorHandling { realTimeVehicleOptional ->
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
        // Stop real-time updates - only if viewModel is initialized
        // This can happen when cleanup is called before fragment is attached (e.g., during contributor switching)
        if (::viewModel.isInitialized) {
            viewModel.stopRealtimeUpdates()
        }

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
        this.service = service
        // Only set viewModel if it's initialized (will be set in onActivityCreated after fragment attachment)
        if (::viewModel.isInitialized) {
            viewModel.service.accept(service)
        }
    }

    fun setStop(stop: ScheduledStop?) {
        mStop = stop
        // Only set viewModel if it's initialized (will be set in onActivityCreated after fragment attachment)
        if (::viewModel.isInitialized) {
            viewModel.stop.accept(stop)
        }
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
            if (service!!.serviceColor == null || service!!.serviceColor?.color == Color.BLACK) fragment.resources.getColor(
                R.color.v4_color
            ) else service!!.serviceColor?.color!!
        val maxLength = 3
        val text =
            (if (TextUtils.isEmpty(service!!.serviceNumber)) {
                if (mStop == null || mStop!!.type == null) "" else StringUtils.capitalizeFirst(mStop!!.type.toString())
            } else service!!.serviceNumber)!!.let {
                if (it.length > maxLength) it.take(maxLength - 1) + "…" else it
            }
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
                    .infoWindowAnchor(0.5f, 0.0f)
                    .title(markerTitle)
                    .snippet(snippet)
                    .position(location)
                    .draggable(false)
            )

            // Cleanup pulse animation
            hidePulseOverlay(pulseOverlay)
            pulseOverlay = null

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

            // Get the current zoom level
            val zoomLevel = map.cameraPosition.zoom

            // Start the pulse animation with zoom level
            animatePulseOverlay(pulseOverlay, zoomLevel)
        }
    }

    private fun updateVehicleMarkerAppearance() {
        val realTimeVehicle = service?.realtimeVehicle ?: return

        realTimeVehicleMarker?.let { marker ->
            pulseOverlay?.let { overlay ->
                // Calculate time since the last known update
                val currentTimeMillis = System.currentTimeMillis()
                val lastUpdateTimeMillis = realTimeVehicle.lastUpdateTime // Already in milliseconds
                val ageInSeconds =
                    ((currentTimeMillis - lastUpdateTimeMillis) / 1000) // Start from 1 second

                // Calculate age factor and fade level
                val ageFactor = calculateAgeFactor(ageInSeconds)
                val fadeLevel = calculateFadeFromAgeFactor(ageFactor)

                // Update marker opacity and snippet
                updateMarkerOpacity(marker, fadeLevel)

                // Post the formatted elapsed time
                _formattedElapsedTime.postValue(formatElapsedTime(ageInSeconds))

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

    private fun fitAllMapElementsToBounds(
        paddingPx: Int = 160,
        includeRealtimeVehicle: Boolean = true,
        singlePointZoom: Float = 16f
    ) {
        val map = googleMap ?: return

        val builder = LatLngBounds.Builder()
        var count = 0
        var firstPoint: LatLng? = null

        // 1) All stop markers
        for (marker in stopCodesToMarkerMap.values) {
            val p = marker.position
            builder.include(p)
            if (count == 0) firstPoint = p
            count++
        }

        // 2) All polyline points
        for (poly in serviceLines) {
            for (p in poly.points) {
                builder.include(p)
                if (count == 0) firstPoint = p
                count++
            }
        }

        // 3) Realtime vehicle marker
        if (includeRealtimeVehicle) {
            realTimeVehicleMarker?.position?.let { p ->
                builder.include(p)
                if (count == 0) firstPoint = p
                count++
            }
        }

        if (count == 0) return // nothing to show

        try {
            if (count == 1) {
                // Only one point -> just zoom to it
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstPoint!!, singlePointZoom))
            } else {
                // Multiple points -> fit bounds
                val bounds = builder.build()
                try {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
                } catch (_: IllegalStateException) {
                    // Fallback if called before map has size; post to the view to retry
                    fragment.view?.post {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
                    }
                }
            }
        } catch (e: Exception) {
            e.printThrowableStackTrace()
        }
    }

}