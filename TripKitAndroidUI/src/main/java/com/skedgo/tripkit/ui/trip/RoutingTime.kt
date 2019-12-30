package com.skedgo.tripkit.ui.trip
import org.joda.time.DateTime

sealed class RoutingTime
object Now : RoutingTime()
data class LeaveAfter(val time: DateTime) : RoutingTime()
data class ArriveBy(val time: DateTime) : RoutingTime()
