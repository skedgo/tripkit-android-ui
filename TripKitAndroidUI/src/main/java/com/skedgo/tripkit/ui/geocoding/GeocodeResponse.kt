package com.skedgo.tripkit.ui.geocoding

import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.common.model.location.Location

data class GeocodeResponse(
    @SerializedName("query")
    val query: String,
    @SerializedName("choices")
    val choiceList: List<Location>
)