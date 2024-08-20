package com.skedgo.tripkit.ui.utils

import android.content.res.Resources

import com.skedgo.tripkit.common.util.TransportModeUtils
import com.skedgo.tripkit.configuration.ServerManager

import com.skedgo.tripkit.routing.ModeInfo

object StopMarkerUtils {
    private val MAP_ICON_URL_TEMPLATE_PRODUCTION =
        ServerManager.configuration.staticTripGoUrl + "icons/android/%s/ic_map_%s.png"
    private val MAP_ICON_URL_TEMPLATE_PRODUCTION2 =
        ServerManager.configuration.staticTripGoUrl + "icons/android/%s/ic_map_marker_%s.png"
    private val MAP_ICON_URL_TEMPLATE_BETA =
        ServerManager.configuration.bigBangUrl + "modeicons/android/%s/ic_map_%s.png"

    fun getMapIconUrlForModeInfo(resources: Resources, modeInfo: ModeInfo?): String? {
        if (modeInfo == null || modeInfo.remoteIconName == null) {
            return null
        }

        val densityDpiName =
            TransportModeUtils.getDensityDpiName(resources.displayMetrics.densityDpi)
        return String.format(
            MAP_ICON_URL_TEMPLATE_PRODUCTION,
            densityDpiName,
            modeInfo.remoteIconName
        )
    }

    fun getMapIconUrlForModeInfo(resources: Resources, remoteIcon: String?): List<String>? {
        if (remoteIcon == null) {
            return null
        }
        val densityDpiName =
            TransportModeUtils.getDensityDpiName(resources.displayMetrics.densityDpi)
        return listOf(
            MAP_ICON_URL_TEMPLATE_PRODUCTION,
            MAP_ICON_URL_TEMPLATE_PRODUCTION2,
            MAP_ICON_URL_TEMPLATE_BETA
        )
            .map { String.format(it, densityDpiName, remoteIcon) }
    }
}