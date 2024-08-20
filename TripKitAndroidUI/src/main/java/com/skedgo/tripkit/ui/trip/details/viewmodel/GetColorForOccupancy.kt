package com.skedgo.tripkit.ui.trip.details.viewmodel

import androidx.annotation.ColorRes
import com.skedgo.tripkit.routing.Occupancy
import com.skedgo.tripkit.ui.R

object GetColorForOccupancy {

    @ColorRes
    fun execute(occupancy: Occupancy?): Int =
        when (occupancy) {
            Occupancy.Empty, Occupancy.ManySeatsAvailable -> R.color.occupancyManySeats
            Occupancy.FewSeatsAvailable -> R.color.occupancyFewSeats
            Occupancy.StandingRoomOnly, Occupancy.CrushedStandingRoomOnly -> R.color.occupancyStandingOnly
            Occupancy.Full -> android.R.color.white
            Occupancy.NotAcceptingPassengers -> android.R.color.white
            else -> android.R.color.white
        }
}