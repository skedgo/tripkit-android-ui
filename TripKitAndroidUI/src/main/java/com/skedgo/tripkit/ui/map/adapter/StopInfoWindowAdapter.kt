package com.skedgo.tripkit.ui.map.adapter

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

interface StopInfoWindowAdapter : GoogleMap.InfoWindowAdapter, OnInfoWindowClose {
    fun windowInfoHeightInPixel(marker: Marker): Int
}