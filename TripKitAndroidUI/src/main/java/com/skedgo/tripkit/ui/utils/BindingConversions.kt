package com.skedgo.tripkit.ui.utils

import androidx.annotation.DrawableRes
import androidx.databinding.BindingConversion
import com.skedgo.tripkit.common.model.location.Location
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.common.model.stop.StopType.BUS
import com.skedgo.tripkit.common.model.stop.StopType.CABLECAR
import com.skedgo.tripkit.common.model.stop.StopType.FERRY
import com.skedgo.tripkit.common.model.stop.StopType.MONORAIL
import com.skedgo.tripkit.common.model.stop.StopType.PARKING
import com.skedgo.tripkit.common.model.stop.StopType.SUBWAY
import com.skedgo.tripkit.common.model.stop.StopType.TAXI
import com.skedgo.tripkit.common.model.stop.StopType.TRAIN
import com.skedgo.tripkit.common.model.stop.StopType.TRAM
import com.skedgo.tripkit.ui.R

object BindingConversions {
    @DrawableRes
    fun convertStopTypeToMapIconRes(stopType: StopType?): Int {
        return when (stopType) {
            BUS -> {
                R.drawable.ic_map_stop_bus
            }
            TRAIN -> {
                R.drawable.ic_map_stop_train
            }
            FERRY -> {
                R.drawable.ic_map_stop_ferry
            }
            MONORAIL -> {
                R.drawable.ic_map_stop_monorail
            }
            SUBWAY -> {
                R.drawable.ic_map_stop_subway
            }
            TAXI -> {
                R.drawable.ic_map_stop_taxi
            }
            PARKING -> {
                R.drawable.ic_map_stop_parking
            }
            TRAM -> {
                R.drawable.ic_map_stop_tram
            }
            CABLECAR -> {
                R.drawable.ic_map_stop_cablecar
            }
            else -> {
                0
            }
        }
    }
}

@BindingConversion
fun convertLocationToAlpha(location: Location?): Int {
    return if (location == null) 1 else 0
}