package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.common.model.BicycleAccessible
import com.skedgo.tripkit.common.model.WheelchairAccessible
import javax.inject.Inject

open class GetModeAccessibility @Inject constructor() {

    fun wheelchair(wheelchairAccessible: WheelchairAccessible) =
        when (wheelchairAccessible.wheelchairAccessible) {
            null -> -1
            true -> 1
            false -> 0
        }

    fun bicycle(bicycleAccessible: BicycleAccessible) =
        when (bicycleAccessible.bicycleAccessible) {
            null -> -1
            true -> 1
            false -> 0
        }

}