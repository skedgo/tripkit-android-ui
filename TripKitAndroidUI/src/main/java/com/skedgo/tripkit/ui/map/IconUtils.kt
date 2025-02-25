package com.skedgo.tripkit.ui.map

import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.VisibleForTesting

object IconUtils {
    fun asUrl(resources: Resources, icon: String, urlTemplate: String): String {
        val densityDpiName = getDensityDpiName(resources.displayMetrics.densityDpi)
        return String.format(urlTemplate, densityDpiName, icon)
    }

    @VisibleForTesting
    fun getDensityDpiName(densityDpi: Int): String {
        return when (densityDpi) {
            DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
            DisplayMetrics.DENSITY_HIGH -> "hdpi"
            DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
            DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
            else -> "xxxhdpi"
        }
    }
}
