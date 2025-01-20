package com.skedgo.tripkit.ui.utils

import android.content.Context
import com.skedgo.tripkit.common.util.TransportModeUtils

object DeviceInfo {
    var densityDpi: Int = 0
        private set

    fun init(context: Context) {
        val metrics = context.resources.displayMetrics
        densityDpi = metrics.densityDpi
    }

    fun getDensityDpiName(): String = TransportModeUtils.getDensityDpiName(densityDpi)
}