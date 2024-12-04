package com.skedgo.tripkit.ui.geocoding

import com.google.gson.annotations.SerializedName

class ReviewSummary {
    @SerializedName("averageRating")
    var averageRating: Float = 0f

    @SerializedName("reviewCount")
    var reviewCount: Int = 0

    @SerializedName("ratingImageURL")
    var ratingImageURL: String? = null
}