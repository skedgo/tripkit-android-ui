package com.skedgo.tripkit.ui.model

import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.common.model.location.Location

class PodLocation(lat: Double, lon: Double) : Location(lat, lon) {
    @SerializedName("identifier")
    var podIdentifier: String? = null
}