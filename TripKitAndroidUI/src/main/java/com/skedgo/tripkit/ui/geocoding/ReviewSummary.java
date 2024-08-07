package com.skedgo.tripkit.ui.geocoding;

import com.google.gson.annotations.SerializedName;

public class ReviewSummary {

    @SerializedName("averageRating")
    public float averageRating;

    @SerializedName("reviewCount")
    public int reviewCount;

    @SerializedName("ratingImageURL")
    public String ratingImageURL;
}