package com.skedgo.tripkit.ui.trippreview.nearby

import com.skedgo.tripkit.common.model.SharedVehicleType
import com.skedgo.tripkit.ui.R


fun SharedVehicleType.title(): Int {
    return when (this) {
        SharedVehicleType.BIKE -> R.string.bicycle
        SharedVehicleType.BICYCLE -> R.string.bicycle
        SharedVehicleType.CAR -> R.string.car
        SharedVehicleType.KICK_SCOOTER, SharedVehicleType.SCOOTER-> R.string.kick_scooter
        SharedVehicleType.MOTO_SCOOTER -> R.string.moto_scooter
        SharedVehicleType.PEDELEC -> R.string.e_minusbike
    }
}