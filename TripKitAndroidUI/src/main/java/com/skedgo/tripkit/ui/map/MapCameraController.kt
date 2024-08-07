package com.skedgo.tripkit.ui.map

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolygonOptions
import com.skedgo.tripkit.account.data.Polygon
import com.skedgo.tripkit.ui.map.home.ZoomLevel

import javax.inject.Inject

class MapCameraController @Inject constructor() {

    companion object {
        const val DELAY_REGION_POLYGON_REMOVE = 4000L
        const val STROKE_DASH_SIZE_REGION_POLYGON = 20f
        const val STROKE_GAP_SIZE_REGION_POLYGON = 5f
        const val PADDING_REGION_POLYGON = 50
    }

    fun moveTo(map: GoogleMap, marker: Marker) {
        val zoomLevel = computeZoomLevel(map.cameraPosition.zoom)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.position, zoomLevel))
        marker.showInfoWindow()
    }

    fun moveToLatLng(map: GoogleMap, latLng: LatLng) {
        val zoomLevel = computeZoomLevel(map.cameraPosition.zoom)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
    }

    private fun computeZoomLevel(cameraZoom: Float): Float = when {
        cameraZoom > ZoomLevel.INNER.level -> cameraZoom
        else -> ZoomLevel.INNER.level.toFloat()
    }

    fun moveToPolygonBounds(map: GoogleMap, multiPolygon: Polygon) {
        val builder = LatLngBounds.builder()
        val polygons = mutableListOf<com.google.android.gms.maps.model.Polygon>()

        for (polygon in multiPolygon.coordinates) {
            for (polygonPath in polygon) {
                val polygonOptions = PolygonOptions()

                for (coordinate in polygonPath) {
                    val point = LatLng(coordinate[1], coordinate[0])
                    polygonOptions.add(point)
                    builder.include(point)
                }
                polygonOptions.strokeColor(Color.GRAY)
                polygonOptions.strokePattern(
                    listOf(
                        Dash(STROKE_DASH_SIZE_REGION_POLYGON),
                        Gap(STROKE_GAP_SIZE_REGION_POLYGON),
                        Dash(STROKE_DASH_SIZE_REGION_POLYGON),
                        Gap(STROKE_GAP_SIZE_REGION_POLYGON)
                    )
                )
                polygons.add(map.addPolygon(polygonOptions))
            }
        }

        val bounds = builder.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, PADDING_REGION_POLYGON))
        polygons.removePolygons()
    }

    private fun List<com.google.android.gms.maps.model.Polygon>.removePolygons() {
        Handler(Looper.getMainLooper()).postDelayed({
            forEach { it.remove() }
        }, DELAY_REGION_POLYGON_REMOVE)
    }
}
