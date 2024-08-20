package com.skedgo.tripkit.ui.tripresult

import com.skedgo.tripkit.ui.utils.DistanceFormatter

data class RoadTagChart(
    val max: Int,
    val middle: Int,
    val items: List<RoadTagChartItem>
) {
    fun getMaxDistance(): String =
        if (max > 999) {
            DistanceFormatter.format(max)
        } else {
            "${max}m"
        }

    fun getMiddleDistance(): String =
        if (middle > 999) {
            DistanceFormatter.format(middle)
        } else {
            "${middle}m"
        }
}
