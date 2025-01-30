package com.skedgo.tripkit.ui.map.servicestop

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds.Builder
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.common.util.DateTimeFormats
import com.skedgo.tripkit.common.util.StringUtils.capitalizeFirst
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.RealTimeVehicle
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.map.LocationEnhancedMapFragment
import com.skedgo.tripkit.ui.map.SimpleCalloutView
import com.skedgo.tripkit.ui.map.TimeLabelMaker
import com.skedgo.tripkit.ui.map.VehicleMarkerIconCreator
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.realtime.RealTimeChoreographerViewModel
import com.skedgo.tripkit.ui.realtime.RealTimeViewModelFactory
import com.skedgo.tripkit.ui.servicedetail.GetStopDisplayText
import com.skedgo.tripkit.ui.servicedetail.ServiceDetailFragment.OnScheduledStopClickListener
import com.skedgo.tripkit.ui.timetables.TimetableFragment.OnTimetableEntrySelectedListener
import com.skedgo.tripkit.utils.OptionalCompat
import com.squareup.otto.Bus
import dagger.Lazy
import javax.inject.Inject

class ServiceStopMapFragment : LocationEnhancedMapFragment(), OnInfoWindowClickListener, InfoWindowAdapter,
    OnTimetableEntrySelectedListener, OnScheduledStopClickListener {
    @JvmField
    @Inject
    var regionService: RegionService? = null

    @JvmField
    @Inject
    var vehicleMarkerIconCreatorLazy: Lazy<VehicleMarkerIconCreator>? = null

    @JvmField
    @Inject
    var realTimeViewModelFactory: RealTimeViewModelFactory? = null

    @JvmField
    @Inject
    var getStopDisplayText: GetStopDisplayText? = null

    lateinit var viewModel: ServiceStopMapViewModel

    /* TODO: Replace with RxJava-based approach. */
    @JvmField
    @Deprecated("")
    @Inject
    var bus: Bus? = null
    private var mStop: ScheduledStop? = null
    private var service: TimetableEntry? = null
    private var realTimeVehicleMarker: Marker? = null
    private val stopCodesToMarkerMap = HashMap<String, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TripKitUI.getInstance()
            .serviceStopMapComponent()
            .inject(this)

        val realTimeViewModel = ViewModelProviders.of(
            requireActivity(), realTimeViewModelFactory
        )
            .get(RealTimeChoreographerViewModel::class.java)

        viewModel.realtimeViewModel = realTimeViewModel
        val timeTextView =
            requireActivity().layoutInflater.inflate(R.layout.view_time_label, null) as TextView
        val timeLabelMaker = TimeLabelMaker(timeTextView)
        val serviceStopMarkerCreator = ServiceStopMarkerCreator(requireActivity(), timeLabelMaker)
        viewModel.serviceStopMarkerCreator = serviceStopMarkerCreator

        setMyLocationEnabled()
        throw RuntimeException("error here")
    }

    override fun onStart() {
        super.onStart()
        bus!!.register(this)
        autoDisposable.add(
            viewModel!!.drawStops
                .subscribe { newMarkerOptionsAndRemovedStopIdsPair: Pair<List<Pair<MarkerOptions, String>>, Set<String>> ->
                    val newMarkerOptions = newMarkerOptionsAndRemovedStopIdsPair.first
                    val removedStopIds = newMarkerOptionsAndRemovedStopIdsPair.second
                    for (id in removedStopIds) {
                        stopCodesToMarkerMap[id]!!.remove()
                        stopCodesToMarkerMap.remove(id)
                    }
                    whenSafeToUseMap { googleMap: GoogleMap ->
                        for ((first, second) in newMarkerOptions) {
                            val marker = googleMap.addMarker(first)
                            stopCodesToMarkerMap[second] = marker
                        }
                    }
                })

        autoDisposable.add(
            viewModel!!.viewPort
                .subscribe { coordinates: List<LatLng>? -> this.centerMapOver(coordinates) })

        val serviceLines: MutableList<Polyline> = ArrayList()

        autoDisposable.add(
            viewModel!!.drawServiceLine
                .subscribe { polylineOptions: List<PolylineOptions?> ->
                    whenSafeToUseMap { googleMap: GoogleMap ->
                        for (line in serviceLines) {
                            line.remove()
                        }
                        serviceLines.clear()
                        for (polylineOption in polylineOptions) {
                            serviceLines.add(googleMap.addPolyline(polylineOption))
                        }
                    }
                })

        autoDisposable.add(
            viewModel.realtimeVehicle
                .subscribeWithErrorHandling { realTimeVehicleOptional: OptionalCompat<RealTimeVehicle> ->
                    if (realTimeVehicleOptional.isPresent()) { // Check if the value is present
                        setRealTimeVehicle(realTimeVehicleOptional.get()) // Get the value from OptionalCompat
                    } else {
                        setRealTimeVehicle(null) // Handle empty OptionalCompat
                    }
                }
        )
    }

    override fun onStop() {
        super.onStop()
        bus!!.unregister(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupMap()
        whenSafeToUseMap { map: GoogleMap ->
            if (mStop != null) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            mStop!!.lat, mStop!!.lon
                        ), 15.0f
                    )
                )
            }
        }
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun getInfoContents(marker: Marker): View {
        val view = SimpleCalloutView.create(LayoutInflater.from(activity))
        view.setTitle(marker.title)
        view.setSnippet(marker.snippet)
        return view
    }

    override fun onInfoWindowClick(marker: Marker) {
//    startActivity(StreetViewActivity.Intents.viewLocation(
//        getActivity(),
//        marker.getPosition().latitude,
//        marker.getPosition().longitude,
//        0
//    ));
    }

    private fun setRealTimeVehicle(realTimeVehicle: RealTimeVehicle?) {
        if (realTimeVehicleMarker != null) {
            realTimeVehicleMarker!!.remove()
        }

        if (realTimeVehicle == null) {
            return
        }

        whenSafeToUseMap { map: GoogleMap? ->
            if (realTimeVehicle.hasLocationInformation()) {
                if (service != null && TextUtils.equals(
                        realTimeVehicle.serviceTripId,
                        service!!.serviceTripId
                    )
                ) {
                    service!!.realtimeVehicle = realTimeVehicle
                    createVehicleMarker(realTimeVehicle)
                }
            }
        }
    }

    fun setService(service: TimetableEntry?) {
        viewModel!!.service.accept(service)
        this.service = service
    }

    fun setStop(stop: ScheduledStop?) {
        this.mStop = stop
        viewModel!!.stop.accept(stop)
    }

    private fun createVehicleMarker(vehicle: RealTimeVehicle) {
        var title: String? = null
        if (TextUtils.isEmpty(service!!.serviceNumber)) {
            title = "Your upcoming service"
        } else {
            if (mStop != null && mStop!!.type != null) {
                title = capitalizeFirst(mStop!!.type.toString()) + " " + service!!.serviceNumber
            }

            if (TextUtils.isEmpty(title)) {
                title = "Service " + service!!.serviceNumber
            }
        }

        val bearing = if (vehicle.location == null) 0 else vehicle.location.bearing
        val color =
            if (service!!.serviceColor == null || service!!.serviceColor.color == Color.BLACK) resources.getColor(
                R.color.v4_color
            ) else service!!.serviceColor.color
        val text =
            if (TextUtils.isEmpty(service!!.serviceNumber)) (if (mStop == null || mStop!!.type == null) "" else capitalizeFirst(
                mStop!!.type.toString()
            )) else service!!.serviceNumber!!

        val icon = vehicleMarkerIconCreatorLazy!!.get().call(bearing, color, text)
        val markerTitle = title

        val context = requireActivity().applicationContext
        whenSafeToUseMap { map: GoogleMap ->
            val millis = vehicle.lastUpdateTime * 1000
            val time = DateTimeFormats.printTime(context, millis, null)
            val snippet = if (TextUtils.isEmpty(vehicle.label)) {
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
                    .position(LatLng(vehicle.location.lat, vehicle.location.lon))
                    .draggable(false)
            )
        }
    }

    /**
     * To zoom in/out to view the whole trip or the whole service line.
     */
    private fun centerMapOver(coordinates: List<LatLng>?) {
        if (coordinates != null && coordinates.size > 0) {
            whenSafeToUseMap { map: GoogleMap ->
                val builder = Builder()
                for (coordinate in coordinates) {
                    builder.include(coordinate)
                }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 320))
            }
        }
    }

    private fun setMyLocationEnabled() {
//    ((BaseActivity) getActivity())
//        .checkSelfPermissionReactively(Manifest.permission.ACCESS_FINE_LOCATION)
//        .filter(result -> result)
//        .subscribe(__ -> whenSafeToUseMap(map -> map.setMyLocationEnabled(true)));
    }

    @SuppressLint("MissingPermission")
    private fun setupMap() {
        whenSafeToUseMap { map: GoogleMap ->
            map.setOnInfoWindowClickListener(this@ServiceStopMapFragment)
            map.setInfoWindowAdapter(this@ServiceStopMapFragment)
            map.setIndoorEnabled(false)

            map.uiSettings.isRotateGesturesEnabled = false
            requireActivity().supportInvalidateOptionsMenu()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel!!.onCleared()
    }

    override fun onTimetableEntrySelected(
        segment: TripSegment?,
        service: TimetableEntry,
        stop: ScheduledStop,
        minStartTime: Long
    ) {
        setService(service)
    }

    override fun onScheduledStopClicked(stop: ServiceStop) {
        if (!TextUtils.isEmpty(stop.code)) {
            val marker = stopCodesToMarkerMap[stop.code]
            if (marker != null) {
                whenSafeToUseMap { googleMap: GoogleMap ->
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                    marker.showInfoWindow()
                }
            }
        }
    }
}