package com.skedgo.tripkit.ui.utils

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

fun getISODateFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(tz)
}

fun getDisplayDateFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("MMM dd, yyyy").withZone(tz)
}

fun getDisplayTimeFormatter(tz: DateTimeZone? = null): DateTimeFormatter {
    return DateTimeFormat.forPattern("h:mm aa").withZone(tz)
}