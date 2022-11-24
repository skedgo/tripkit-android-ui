package com.skedgo.tripkit.ui.timetables.data

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.immutables.gson.Gson
import org.immutables.value.Value

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersDepartureFilter::class)
interface DepartureFilter {

    @SerializedName("operatorID")
    fun operatorId(): String

    @SerializedName("routeID")
    fun routeId(): String? = null

    @SerializedName("directionID")
    fun directionId(): String? = null
}
