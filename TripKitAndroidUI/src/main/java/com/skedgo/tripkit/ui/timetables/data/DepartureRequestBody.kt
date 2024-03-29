package com.skedgo.tripkit.ui.timetables.data

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.immutables.gson.Gson
import org.immutables.value.Value

@Value.Immutable
@Gson.TypeAdapters
@Value.Style(passAnnotations = [JsonAdapter::class])
@JsonAdapter(GsonAdaptersDepartureRequestBody::class)
interface DepartureRequestBody {

    fun embarkationStops(): List<String>

    fun disembarkationStops(): List<String>?

    @SerializedName("region")
    fun regionName(): String

    @SerializedName("timeStamp")
    fun timeInSecs(): Long

    fun limit(): Int

    fun filters(): List<DepartureFilter>?

    fun includeStops(): Boolean = false
}
