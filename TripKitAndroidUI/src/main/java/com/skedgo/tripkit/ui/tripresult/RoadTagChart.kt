package com.skedgo.tripkit.ui.tripresult

import android.graphics.Color
import com.skedgo.tripkit.routing.RoadTag

data class RoadTagChart(
    val max: Int,
    val middle: Int,
    val items: List<RoadTagChartItem>
)
