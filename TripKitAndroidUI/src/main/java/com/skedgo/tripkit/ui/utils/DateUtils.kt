package com.skedgo.tripkit.ui.utils

import android.content.Context
import com.skedgo.tripkit.ui.R
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.*

fun getISODateFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(tz)
}

fun getDisplayDateFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("MMM dd, yyyy").withZone(tz)
}

fun getDisplayTimeFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("h:mm aa").withZone(tz)
}

fun Date.checkDateForStringLabel(context: Context): String? {
    val newTime = Date()
    try {
        val cal = Calendar.getInstance()
        cal.time = newTime
        val oldCal = Calendar.getInstance()
        oldCal.time = this
        val oldYear = oldCal[Calendar.YEAR]
        val year = cal[Calendar.YEAR]
        val oldDay = oldCal[Calendar.DAY_OF_YEAR]
        val day = cal[Calendar.DAY_OF_YEAR]
        if (oldYear == year) {
            when (oldDay - day) {
                -1 -> {
                    return context.getString(R.string.yesterday)
                }
                0 -> {
                    return context.getString(R.string.today)
                }
                1 -> {
                    return context.getString(R.string.tomorrow)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}