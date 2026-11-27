package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import com.skedgo.tripkit.ui.R

/** Presentation of the API vehicle's lastUpdate timestamp, independent of map/occupancy. */
internal object ServiceInformationAge {
    fun format(context: Context, timestamp: Long, nowMillis: Long): String {
        if (timestamp <= 0) return ""
        val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
        // TripKit UI is also published for API 23; android.icu starts at API 24.
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.N) {
            return context.getString(R.string.label_last_updated) + " " +
                android.text.format.DateUtils.getRelativeTimeSpanString(
                    millis.coerceAtMost(nowMillis), nowMillis,
                    android.text.format.DateUtils.SECOND_IN_MILLIS
                )
        }
        val seconds = ((nowMillis - millis) / 1000).coerceAtLeast(0)
        val (amount, unit) = when {
            seconds < 60 -> seconds to RelativeDateTimeFormatter.RelativeUnit.SECONDS
            seconds < 3600 -> seconds / 60 to RelativeDateTimeFormatter.RelativeUnit.MINUTES
            seconds < 86400 -> seconds / 3600 to RelativeDateTimeFormatter.RelativeUnit.HOURS
            else -> seconds / 86400 to RelativeDateTimeFormatter.RelativeUnit.DAYS
        }
        val formatter = RelativeDateTimeFormatter.getInstance(context.resources.configuration.locales[0])
        return context.getString(R.string.label_last_updated) + " " +
            formatter.format(amount.toDouble(), RelativeDateTimeFormatter.Direction.LAST, unit)
    }
}
