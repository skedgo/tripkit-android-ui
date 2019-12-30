package com.skedgo.tripkit.ui.trip.options
import android.content.res.Resources
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trip.ArriveBy
import com.skedgo.tripkit.ui.trip.LeaveAfter
import com.skedgo.tripkit.ui.trip.Now
import com.skedgo.tripkit.ui.trip.RoutingTime
import io.reactivex.Single
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val ROUTING_TIME_PATTERN = "MMM dd, h:mm a"

open class RoutingTimeViewModelMapper @Inject internal constructor(
    private val resources: Resources
) {
  open fun toText(routingTime: RoutingTime): Single<String> = Single.fromCallable {
    when (routingTime) {
      Now -> resources.getString(R.string.leave_now)
      is LeaveAfter -> resources.getString(R.string.leave) + " ${routingTime.time.format()}"
      is ArriveBy -> resources.getString(R.string.arrive) + " ${routingTime.time.format()}"
    }
  }

  private fun DateTime.format(): String {
    val simpleDateFormat = SimpleDateFormat(ROUTING_TIME_PATTERN, Locale.US)
    simpleDateFormat.timeZone = zone.toTimeZone()
    return simpleDateFormat.format(Date(millis))
  }
}
