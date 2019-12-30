package com.skedgo.tripkit.ui.data.places

data class GooglePlace(
    val name: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val placeId: String,
    val attribution: String?
)
