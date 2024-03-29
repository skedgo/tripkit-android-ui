package com.skedgo.tripkit.ui.trip.details.viewmodel

import androidx.annotation.StringRes
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.routing.Occupancy

object GetTextForOccupancy {

  @StringRes
  fun execute(occupancy: Occupancy?): Int =
      when (occupancy) {
        Occupancy.Empty, Occupancy.ManySeatsAvailable ->  R.string.many_seats_available
        Occupancy.FewSeatsAvailable -> R.string.few_seats_available
        Occupancy.StandingRoomOnly, Occupancy.CrushedStandingRoomOnly -> R.string.standing_room_only
        Occupancy.Full ->  R.string.almost_full
        Occupancy.NotAcceptingPassengers ->  R.string.full
        else -> throw Error("Wrong occupancy status: " + occupancy.toString())
      }
}