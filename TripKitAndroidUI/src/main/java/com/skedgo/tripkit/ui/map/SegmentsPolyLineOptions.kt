package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.PolylineOptions

data class SegmentsPolyLineOptions(
    val polyLineOptions: List<PolylineOptions>,
    val isTravelled: Boolean
)
