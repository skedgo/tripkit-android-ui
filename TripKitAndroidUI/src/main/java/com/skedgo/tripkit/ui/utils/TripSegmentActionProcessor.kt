package com.skedgo.tripkit.ui.utils

import android.content.Context
import android.content.res.Resources
import com.skedgo.tripkit.common.util.DateTimeFormats
import com.skedgo.tripkit.common.util.TimeUtils
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.endDateTime
import com.skedgo.tripkit.routing.startDateTime
import javax.inject.Inject


class TripSegmentActionProcessor @Inject constructor() {
    val numberRegex = "<NUMBER>".toRegex()
    val lineNameRegex = "<LINE_NAME>".toRegex()
    val directionRegex = "<DIRECTION>".toRegex()
    val locationsRegex = "<LOCATIONS>".toRegex()
    val platformRegex = "<PLATFORM>".toRegex()
    val stopsRegex = "<STOPS>".toRegex()
    val timeRegex = "<TIME>".toRegex()
    val durationRegex = "<DURATION>".toRegex()
    val trafficRegex = "<TRAFFIC>".toRegex()
    val emptyRemoveRegex = "(^: )|(\\s*⋅\\s*$)|(^\\n*)|(\\n*$)".toRegex()
    val singleDotReplace = "⋅\\s*⋅".toRegex()
    val doubleSpaceReplace = "\\s\\s".toRegex()


    fun hasTime(segment: TripSegment): Boolean {
        segment.action?.let { return timeRegex.containsMatchIn(it) }
        return false
    }

    fun processText(context: Context, segment: TripSegment, text: String, withTime: Boolean): String {
        var out = text
        segment.serviceNumber?.let {
            numberRegex.replace(text, it)
        }

        segment.serviceName?.let {
            out = lineNameRegex.replace(out, it)
        }

        if (directionRegex.containsMatchIn(out)) {
            out = if (segment.serviceDirection.isNullOrEmpty()) {
                directionRegex.replace(out, "")
            } else {
                directionRegex.replace(out,  context.resources.getString(R.string.direction) + ": " + segment.direction)
            }
        }
        out = locationsRegex.replace(out, "")

        if (stopsRegex.matches(out)) {
            out = stopsRegex.replace(out, context.resources.getQuantityString(R.plurals.number_of_stops, segment.stopCount, segment.stopCount))
        }

        out = if (segment.platform != null) {
            platformRegex.replace(out, segment.platform!!)
        } else {
            platformRegex.replace(out, "")
        }

        out = if (withTime && segment.from != null) {
            val timeText = DateTimeFormats.printTime(context, segment.startDateTime.millis, segment.timeZone)
            timeRegex.replace(out, timeText)
        } else {
            timeRegex.replace(out, "")
        }

        if (durationRegex.containsMatchIn(out)) {
            val minutes = (segment.endDateTime.millis - segment.startDateTime.millis) / 1000 / 60
            out = if (minutes < 0) {
                durationRegex.replace(out, "")
            } else {
                durationRegex.replace(out, " " + context.resources.getString(com.skedgo.tripkit.common.R.string.for__pattern, TimeUtils.getDurationInHoursMins((segment.endTimeInSecs - segment.startTimeInSecs).toInt())))
            }
        }

        if (trafficRegex.containsMatchIn(out)) {
            val durationWithTraffic = (segment.endDateTime.millis - segment.startDateTime.millis) / 1000
            out = if (segment.durationWithoutTraffic < durationWithTraffic + 60 /* secs */) {
                // Plus 60 secs since we show both duration types in minutes.
                // For instance, if durationWithTraffic is 65 secs, and durationWithoutTraffic is 60 secs,
                // they will be both shown as '1min'. Thus, no need to show this difference.
                trafficRegex.replace(out, getDurationWithoutTrafficText(context.resources, segment.durationWithoutTraffic))
            } else {
                trafficRegex.replace(out, "")
            }
        }

        out = emptyRemoveRegex.replace(out, "")
        out = singleDotReplace.replace(out, "⋅")
        out = doubleSpaceReplace.replace(out, "\\ ")

        return out
    }

    private fun getDurationWithoutTrafficText(resources: Resources, durationWithoutTraffic: Long): String {
        val durationText = TimeUtils.getDurationInDaysHoursMins(durationWithoutTraffic.toInt())
        return resources.getString(R.string._pattern_w_slasho_traffic, durationText)
    }

}