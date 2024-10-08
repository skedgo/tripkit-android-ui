package com.skedgo.tripkit.ui.trip

import com.skedgo.tripkit.common.model.time.TimeTag
import com.skedgo.tripkit.routing.toSeconds
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit.SECONDS

fun RoutingTime.toTimeTag(): TimeTag = when (this) {
    is Now -> TimeTag.createForLeaveNow()
    is LeaveAfter -> TimeTag.createForLeaveAfter(time.toSeconds())
    is ArriveBy -> TimeTag.createForArriveBy(time.toSeconds())
}

fun TimeTag.toRoutingTime(tz: DateTimeZone): RoutingTime = when {
    isDynamic -> Now
    type == TimeTag.TIME_TYPE_LEAVE_AFTER -> LeaveAfter(
        time = DateTime(SECONDS.toMillis(timeInSecs), tz)
    )
    else -> ArriveBy(time = DateTime(SECONDS.toMillis(timeInSecs), tz))
}
