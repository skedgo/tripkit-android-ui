package com.skedgo.tripkit.ui.trip.details.viewmodel

import androidx.annotation.DrawableRes
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.routing.Occupancy

object GetDrawableForOccupancy {

  @DrawableRes
  fun execute(occupancy: Occupancy?): Int =
      when (occupancy) {
        Occupancy.Empty, Occupancy.ManySeatsAvailable -> R.drawable.ic_occupancy_25
        Occupancy.FewSeatsAvailable -> R.drawable.ic_occupancy_50
        Occupancy.StandingRoomOnly, Occupancy.CrushedStandingRoomOnly -> R.drawable.ic_occupancy_75
        Occupancy.Full -> R.drawable.ic_occupancy_100
        Occupancy.NotAcceptingPassengers -> R.drawable.ic_occupancy_100
        else -> R.drawable.ic_occupancy_0
      }
}