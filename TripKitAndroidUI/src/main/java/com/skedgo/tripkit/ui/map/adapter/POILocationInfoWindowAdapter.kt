package com.skedgo.tripkit.ui.map.adapter

import android.content.Context
import android.view.View
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.skedgo.tripkit.ui.map.IMapPoiLocation
import javax.inject.Inject

class POILocationInfoWindowAdapter @Inject constructor(private val context: Context) :
    StopInfoWindowAdapter {

    private val locationToInfoWindowAdapter: MutableMap<String, StopInfoWindowAdapter?> =
        mutableMapOf()

    override fun getInfoContents(marker: Marker): View? {
        val poiLocation = marker.tag as IMapPoiLocation
        createInfoWindowAdapterIfNeeded(poiLocation)
        return locationToInfoWindowAdapter[poiLocation.javaClass.simpleName]?.getInfoContents(marker)
    }

    override fun getInfoWindow(marker: Marker): View? {
        val poiLocation = marker.tag as IMapPoiLocation
        createInfoWindowAdapterIfNeeded(poiLocation)
        return locationToInfoWindowAdapter[poiLocation.javaClass.simpleName]?.getInfoWindow(marker)
    }

    override fun onInfoWindowClosed(marker: Marker) {
        val poiLocation = marker.tag as IMapPoiLocation
        val infoWindowAdapter: GoogleMap.InfoWindowAdapter? =
            locationToInfoWindowAdapter[poiLocation.javaClass.simpleName]
        if (infoWindowAdapter is OnInfoWindowClose) {
            return infoWindowAdapter.onInfoWindowClosed(marker)
        }
    }

    override fun windowInfoHeightInPixel(marker: Marker): Int {
        val poiLocation = marker.tag as IMapPoiLocation
        createInfoWindowAdapterIfNeeded(poiLocation)
        return locationToInfoWindowAdapter[poiLocation.javaClass.simpleName]!!.windowInfoHeightInPixel(
            marker
        )
    }

    private fun createInfoWindowAdapterIfNeeded(poiLocation: IMapPoiLocation) {
        if (!locationToInfoWindowAdapter.containsKey(poiLocation.javaClass.simpleName)) {
            locationToInfoWindowAdapter[poiLocation.javaClass.simpleName] =
                poiLocation.getInfoWindowAdapter(context)
        }
    }
}