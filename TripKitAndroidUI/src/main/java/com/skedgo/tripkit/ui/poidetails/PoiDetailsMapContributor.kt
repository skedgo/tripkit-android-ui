package com.skedgo.tripkit.ui.poidetails

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.map.TripLocationMarkerCreator
import com.skedgo.tripkit.ui.map.home.TripKitMapContributor
import com.skedgo.tripkit.ui.map.home.getFromAndToMarkerBitmap
import javax.inject.Inject

class PoiDetailsMapContributor : TripKitMapContributor {

    @Inject
    lateinit var tripLocationMarkerCreator: TripLocationMarkerCreator

    private var map: GoogleMap? = null
    private var poiMarker: Marker? = null
    private var location: Location? = null
    private var markerOffsetPx: Int = 160
    private var markerBitmap: Bitmap? = null

    override fun initialize() {
        TripKitUI.getInstance().inject(this)
    }

    override fun setup() {
        // Make sure we display any pending location once the map becomes available.
        updateMarker()
    }

    override fun safeToUseMap(context: Context, map: GoogleMap) {
        this.map = map
        this.markerBitmap = context.getFromAndToMarkerBitmap(0)
        setup()
    }

    override fun getInfoContents(marker: Marker): View? = null

    override fun cleanup() {
        poiMarker?.remove()
        poiMarker = null
        map = null
    }

    fun setLocation(location: Location?) {
        this.location = location
        updateMarker()
    }

    fun setMarkerVerticalOffset(bottomSheetHeight: Int) {
        markerOffsetPx = bottomSheetHeight / 2
        val latLng = location?.let { LatLng(it.lat, it.lon) } ?: return
        map?.let { mapInstance ->
            getOffsetLatLng(mapInstance, latLng)?.let { adjusted ->
                mapInstance.moveCamera(CameraUpdateFactory.newLatLng(adjusted))
            }
        }
    }

    private fun updateMarker() {
        val map = this.map ?: return

        if (location == null) {
            poiMarker?.remove()
            poiMarker = null
            return
        }

        val markerPosition = LatLng(location!!.lat, location!!.lon)

        // Avoid unnecessary updates when the location hasn't changed.
        if (poiMarker != null && poiMarker!!.position == markerPosition) {
            return
        }

        poiMarker?.remove()

        val markerOptions: MarkerOptions = tripLocationMarkerCreator.call(location!!, markerBitmap)
        poiMarker = map.addMarker(markerOptions)

        val cameraTarget = getOffsetLatLng(map, markerPosition) ?: markerPosition
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraTarget, DEFAULT_ZOOM_LEVEL))
    }

    private fun getOffsetLatLng(map: GoogleMap, target: LatLng): LatLng? {
        val offset = markerOffsetPx
        if (offset == 0) {
            return target
        }

        val projection = map.projection ?: return null
        val point: Point = projection.toScreenLocation(target)
        point.y -= offset
        return projection.fromScreenLocation(point)
    }

    companion object {
        private const val DEFAULT_ZOOM_LEVEL = 15f
    }
}

