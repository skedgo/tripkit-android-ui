package com.skedgo.tripkit.ui.utils

import android.content.Context
import com.skedgo.tripkit.common.model.TimeTag
import com.skedgo.tripkit.ui.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit


fun TimeTag.formatString(context: Context, timezone: String?): String {
    val stringBuilder = StringBuilder(50)
    val millis = TimeUnit.SECONDS.toMillis(this.timeInSecs)
    val prefix = if (type == TimeTag.TIME_TYPE_LEAVE_AFTER)
        context.getString(R.string.leave)
    else
        context.getString(R.string.arrive)

    if (isDynamic && type == TimeTag.TIME_TYPE_LEAVE_AFTER) {
        stringBuilder.setLength(0)
        stringBuilder.append(context.getString(R.string.leave_now))
    } else {
        stringBuilder.setLength(0)
        stringBuilder.trimToSize()
        stringBuilder.append(prefix)
        stringBuilder.append(" ")
        val date = Date(millis)
        val dateFormat = SimpleDateFormat("MMM dd, h:mm a", Locale.US)
        dateFormat.timeZone = if (timezone != null) {
            TimeZone.getTimeZone(timezone)
        } else {
            TimeZone.getDefault()
        }
        val timeText = dateFormat.format(date)
        stringBuilder.append(timeText)
    }

    return stringBuilder.toString()
}
