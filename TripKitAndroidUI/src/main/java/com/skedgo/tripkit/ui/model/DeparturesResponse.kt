package com.skedgo.tripkit.ui.model

import com.google.gson.annotations.SerializedName
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.model.stop.ScheduledStop

/**
 * @see [departures.json API](https://redmine.buzzhives.com/projects/buzzhives/wiki/RealTime_API.DepartureServlet-new-servlet-departuresjson)
 */
class DeparturesResponse {
    /**
     * (Optional)
     */
    @SerializedName("error")
    var error: String? = null

    /**
     * (Optional)
     */
    @SerializedName("usererror")
    var hasError: Boolean = false

    @SerializedName("embarkationStops")
    var embarkationStopList: List<ServicesResponse>? = null

    /**
     * (Optional)
     */
    @SerializedName("stops")
    var stopList: List<ScheduledStop>? = null

    /**
     * (Optional)
     */
    @SerializedName("parentInfo")
    val parentInfo: ScheduledStop? = null

    /**
     * (Optional)
     */
    @SerializedName("alerts")
    var alerts: List<RealtimeAlert>? = null

    private var mServiceList: List<TimetableEntry>? = null

    /**
     * Assigns stop code into corresponding service
     *
     *
     * NOTE: Must call this method after parsing the response
     */
    fun processEmbarkationStopList() {
        embarkationStopList?.forEach { servicesResponse ->
            servicesResponse.serviceList?.forEach { service ->
                service.stopCode = servicesResponse.stopCode
            }
        }
    }

    val serviceList: List<TimetableEntry>?
        get() {
            if (mServiceList == null) {
                mServiceList = extractServiceList()
            }

            return mServiceList
        }

    /**
     * Extracts services from the embarkation stops
     */
    private fun extractServiceList(): List<TimetableEntry>? {
        if (embarkationStopList == null) {
            return null
        }

        val serviceList: MutableList<TimetableEntry> = ArrayList()
        for (servicesResponse in embarkationStopList!!) {
            if (servicesResponse.serviceList != null) {
                serviceList.addAll(servicesResponse.serviceList!!)
            }
        }

        return serviceList
    }

    data class ServicesResponse(
        @SerializedName("stopCode") val stopCode: String,
        @SerializedName("services") val serviceList: List<TimetableEntry>?
    )
}
