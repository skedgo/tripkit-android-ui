package com.skedgo.tripkit.ui.tripresult

import android.graphics.Color

data class RoadTagChartItem(
    val label: String,
    var length: Int,
    val color: Int,
    val textColor: Int = Color.WHITE,
    var progress: Int = 0,
    var maxProgress: Int = 0,
    var index: Int = 0
)
