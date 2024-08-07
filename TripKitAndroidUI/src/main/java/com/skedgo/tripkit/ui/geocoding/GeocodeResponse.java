package com.skedgo.tripkit.ui.geocoding;

import com.google.gson.annotations.SerializedName;
import com.skedgo.tripkit.common.model.Location;

import java.util.List;

public class GeocodeResponse {
    @SerializedName("query")
    private String mQuery;

    @SerializedName("choices")
    private List<Location> mChoiceList;

    public List<Location> getChoiceList() {
        return mChoiceList;
    }

    public String getQuery() {
        return mQuery;
    }
}