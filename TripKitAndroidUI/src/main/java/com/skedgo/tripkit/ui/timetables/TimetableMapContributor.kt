package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.gojuno.koptional.Some
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.ServiceStop
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
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
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

    override fun safeToUseMap(context: Context, map: GoogleMap) {

        googleMap = map
        previousCameraPosition = map.cameraPosition

        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mStop!!.lat, mStop!!.lon), 15.0f))

        autoDisposable.add(viewModel.drawStops
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
                if (realTimeVehicleOptional is Some<*>) {
                    setRealTimeVehicle((realTimeVehicleOptional as Some<RealTimeVehicle>).value)
                } else {
                    setRealTimeVehicle(null)
                }
            })
    }

    override fun getInfoContents(marker: Marker): View? {
        return serviceStopCalloutAdapter.getInfoContents(marker)
    }

    override fun cleanup() {
        stopCodesToMarkerMap.forEach { it.value.remove() }
        serviceLines.forEach { it.remove() }
        autoDisposable.clear()
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
        realTimeVehicleMarker?.remove()
        if (realTimeVehicle == null) {
            return
        }
        googleMap?.let {
            if (realTimeVehicle.hasLocationInformation()) {
                if (service != null && TextUtils.equals(
                        realTimeVehicle.serviceTripId,
                        service!!.serviceTripId
                    )) {
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
            val snippet: String
            snippet = if (TextUtils.isEmpty(vehicle.label)) {
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

    fun getMapPreviousPosition(): CameraPosition? {
        return previousCameraPosition
    }
}