package com.skedgo.tripkit.ui.map.adapter

import com.google.android.gms.maps.model.Marker

interface OnInfoWindowClose {
    fun onInfoWindowClosed(marker: Marker)
}