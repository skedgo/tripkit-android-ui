package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.text.TextUtils
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.skedgo.tripkit.agenda.ConfigRepository
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.region.Region
import com.skedgo.tripkit.common.util.Gsons.createForLowercaseEnum
import com.skedgo.tripkit.common.util.ListUtils
import com.skedgo.tripkit.routing.RoutingResponse
import com.skedgo.tripkit.routing.SegmentType.ARRIVAL
import com.skedgo.tripkit.routing.SegmentType.DEPARTURE
import com.skedgo.tripkit.routing.SegmentType.STATIONARY
import com.skedgo.tripkit.routing.TripGroup
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.tripresult.WayPointTaskParam.ForChangingService
import com.skedgo.tripkit.ui.tripresult.WayPointTaskParam.ForChangingStop
import com.skedgo.tripkit.ui.utils.HttpUtils
import com.skedgo.tripkit.ui.utils.isWheelchairModeSelected
import io.reactivex.SingleEmitter
import io.reactivex.SingleOnSubscribe
import java.io.IOException

/**
 * https://redmine.buzzhives.com/projects/buzzhives/wiki/Routing_API#Trips-from-waypoint
 */
class WaypointTask(
    private val context: Context,
    private val configCreator: ConfigRepository,
    private val param: WayPointTaskParam
) : SingleOnSubscribe<List<TripGroup>> {
    @Throws(Exception::class)
    override fun subscribe(singleSubscriber: SingleEmitter<List<TripGroup>>) {
        val region: Region

        val postData: String
        try {
            region = param.region
            if (param is ForChangingService) {
                val segments = param.segments
                val prototypeSegment = param.prototypeSegment
                val service = param.service
                postData = createPostDataForChangingService(
                    region,
                    segments,
                    prototypeSegment,
                    service
                )
            } else {
                val segments = (param as ForChangingStop).segments
                val prototypeSegment = param.prototypeSegment
                val waypoint = param.waypoint
                val isGetOn = param.isGetOn

                postData = createPostDataForChangingStop(
                    routingConfig(),
                    createJsonSegments(
                        segments,
                        prototypeSegment,
                        waypoint,
                        isGetOn
                    )
                ).toString()
            }
        } catch (e: Exception) {
            singleSubscriber.onError(e)
            return
        }

        val serverURLs: List<String>? = region.getURLs()
        if (serverURLs != null) {
            for (serverURL in serverURLs) {
                try {
                    val waypointResponseBody = HttpUtils.post(
                        serverURL + context.getString(R.string.api_waypoint),
                        postData
                    )
                    val gson = createForLowercaseEnum()
                    val waypointResponse =
                        gson.fromJson(waypointResponseBody, RoutingResponse::class.java)
                    if (waypointResponse.hasError()) {
                        singleSubscriber.onError(RuntimeException(waypointResponse.errorMessage))
                        return
                    }

                    waypointResponse.processRawData(context.resources, gson)
                    val tripGroups: ArrayList<TripGroup>? = waypointResponse.tripGroupList
                    if (ListUtils.isEmpty(tripGroups) || ListUtils.isEmpty(
                            tripGroups!![0]!!.trips
                        )
                    ) {
                        singleSubscriber.onError(RuntimeException("No groups found"))
                        return
                    }

                    singleSubscriber.onSuccess(tripGroups!!)
                    return
                } catch (e: IOException) {
                    singleSubscriber.onError(e)
                }
            }
        } else {
            singleSubscriber.onError(RuntimeException("No urls"))
        }
    }

    fun createPostDataForChangingService(
        region: Region,
        segments: List<TripSegment>,
        prototypeSegment: TripSegment,
        service: TimetableEntry
    ): String {
        val jsonSegments = JsonArray()
        for (segment in segments) {
            if (segment.segmentId == prototypeSegment.segmentId) {
                val jsonSegment = convertServiceToJson(region, service)
                jsonSegments.add(jsonSegment)
            } else if ((segment.getType() != STATIONARY)
                && (segment.getType() != ARRIVAL)
                && (segment.getType() != DEPARTURE)
            ) {
                val jsonSegment = convertSegmentToJson(segment)
                jsonSegments.add(jsonSegment)
            }
        }

        val jsonPostData = JsonObject()
        jsonPostData.add(KEY_CONFIG, routingConfig())
        jsonPostData.add(KEY_SEGMENTS, jsonSegments)

        return jsonPostData.toString()
    }

    /**
     * The config for a re-plan must describe how the user asked to *travel*, so the wheelchair
     * flag follows the wheelchair transport-mode selection — the same source the A2B routing
     * request uses. [ConfigRepository] derives it from the separate "wheelchair information"
     * option, which would otherwise turn a walking trip into a wheelchair trip here.
     */
    private fun routingConfig(): JsonObject = configCreator.call().apply {
        addProperty(KEY_WHEELCHAIR, context.isWheelchairModeSelected())
    }

    private fun convertServiceToJson(region: Region, service: TimetableEntry): JsonObject {
        val jsonSegment = JsonObject()
        jsonSegment.addProperty(KEY_START, service.stopCode)
        jsonSegment.addProperty(KEY_END, service.endStopCode)

        val jsonModes = JsonArray()
        jsonModes.add(JsonPrimitive("pt_pub"))
        jsonModes.add(JsonPrimitive("pt_sch"))
        jsonSegment.add(KEY_MODES, jsonModes)

        jsonSegment.addProperty(KEY_START_TIME, service.startTimeInSecs)
        jsonSegment.addProperty(KEY_END_TIME, service.endTimeInSecs)
        jsonSegment.addProperty(KEY_SERVICE_TRIP_ID, service.serviceTripId)
        jsonSegment.addProperty(KEY_OPERATOR, service.operator)
        jsonSegment.addProperty(KEY_REGION, region.name)
        return jsonSegment
    }

    companion object {
        const val KEY_REGION: String = "region"
        const val KEY_SEGMENTS: String = "segments"
        const val KEY_OPERATOR: String = "operator"
        const val KEY_SERVICE_TRIP_ID: String = "serviceTripID"
        const val KEY_END_TIME: String = "endTime"
        const val KEY_START_TIME: String = "startTime"
        const val KEY_MODES: String = "modes"
        const val KEY_END: String = "end"
        const val KEY_START: String = "start"
        const val KEY_CONFIG: String = "config"
        const val KEY_WHEELCHAIR: String = "wheelchair"
        const val FORMAT_COORDINATES: String = "(%f,%f)"

        fun createJsonSegments(
            segments: List<TripSegment>,
            prototypeSegment: TripSegment,
            waypoint: Location,
            isGetOn: Boolean
        ): JsonArray {
            var changeNextDeparture = false
            var isTimeAdded = false

            val jsonSegments = JsonArray()
            for (segment in segments) {
                if (segment.segmentId == prototypeSegment.segmentId) {
                    val jsonSegment = JsonObject()
                    if (isGetOn) {
                        // The waypoint now becomes the departure.
                        jsonSegment.addProperty(KEY_START, waypoint.coordinateString)
                        jsonSegment.addProperty(KEY_END, segment.to!!.coordinateString)
                    } else {
                        // Get-off case.
                        jsonSegment.addProperty(KEY_START, segment.from!!.coordinateString)

                        // The waypoint now becomes the arrival.
                        jsonSegment.addProperty(KEY_END, waypoint.coordinateString)

                        // This case we have to change next segment's departure.
                        changeNextDeparture = true
                    }

                    if (!TextUtils.isEmpty(segment.transportModeId)) {
                        val jsonModes = JsonArray()
                        jsonModes.add(JsonPrimitive(segment.transportModeId))
                        jsonSegment.add(KEY_MODES, jsonModes)
                    }

                    if (!isTimeAdded) {
                        jsonSegment.addProperty(KEY_START_TIME, segment.startTimeInSecs)

                        // We only add once.
                        isTimeAdded = true
                    }

                    jsonSegments.add(jsonSegment)
                } else if ((segment.getType() != STATIONARY)
                    && (segment.getType() != ARRIVAL)
                    && (segment.getType() != DEPARTURE)
                ) {
                    val jsonSegment = convertSegmentToJson(segment)
                    if (changeNextDeparture) {
                        // We've iterated at the segment following the prototype segment.
                        jsonSegment.addProperty(KEY_START, waypoint.coordinateString)

                        // We only change once.
                        changeNextDeparture = true
                    }

                    if (!isTimeAdded) {
                        jsonSegment.addProperty(KEY_START_TIME, segment.startTimeInSecs)

                        // We only add once.
                        isTimeAdded = true
                    }

                    jsonSegments.add(jsonSegment)
                }
            }

            return jsonSegments
        }

        fun convertSegmentToJson(segment: TripSegment): JsonObject {
            val jsonSegment = JsonObject()
            jsonSegment.addProperty(KEY_START, segment.from!!.coordinateString)
            jsonSegment.addProperty(KEY_END, segment.to!!.coordinateString)

            if (!TextUtils.isEmpty(segment.transportModeId)) {
                val jsonModes = JsonArray()
                jsonModes.add(JsonPrimitive(segment.transportModeId))
                jsonSegment.add(KEY_MODES, jsonModes)
            }

            return jsonSegment
        }

        /**
         * TODO: Handle 'vehicleUUID'.
         */
        fun createPostDataForChangingStop(
            configParams: JsonObject,
            segments: JsonArray
        ): JsonObject {
            val jsonPostData = JsonObject()
            jsonPostData.add(KEY_CONFIG, configParams)
            jsonPostData.add(KEY_SEGMENTS, segments)
            return jsonPostData
        }
    }
}