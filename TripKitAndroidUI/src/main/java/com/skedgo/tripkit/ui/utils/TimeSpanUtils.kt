package com.skedgo.tripkit.ui.utils

import com.skedgo.tripkit.common.StyleManager

object TimeSpanUtils {
    const val FORMAT_TIME_SPAN = "%s %s"
    const val HOURS_IN_DAY = 24 * 60
    fun getRelativeTimeSpanString(minutes: Long): String {
        val timeUnit: String
        val sign = if (minutes < 0) -1 else 1
        var timeNumber = Math.abs(minutes)
        when {
            timeNumber == 0L -> {
                return StyleManager.FORMAT_TIME_SPAN_NOW
            }
            timeNumber < 60 -> {
                // Less than a hour
                timeUnit = StyleManager.FORMAT_TIME_SPAN_MIN
            }
            timeNumber >= HOURS_IN_DAY -> {
                // More than a day
                timeNumber = Math.round(timeNumber.toFloat() / HOURS_IN_DAY).toLong()
                timeUnit = StyleManager.FORMAT_TIME_SPAN_DAY
            }
            else -> {
                // Less than a day
                timeNumber = Math.round(timeNumber.toFloat() / 60).toLong()
                timeUnit = StyleManager.FORMAT_TIME_SPAN_HOUR
            }
        }
        return String.format(FORMAT_TIME_SPAN, sign * timeNumber, timeUnit)
    }
}