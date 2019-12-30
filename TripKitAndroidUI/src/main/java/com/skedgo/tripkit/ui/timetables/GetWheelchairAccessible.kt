package com.skedgo.tripkit.ui.timetables

import com.skedgo.tripkit.common.model.WheelchairAccessible
import javax.inject.Inject

open class GetWheelchairAccessible @Inject constructor() {

  operator fun invoke(wheelchairAccessible: WheelchairAccessible) =
      when (wheelchairAccessible.wheelchairAccessible) {
        null -> -1
        true -> 1
        false -> 0
      }
}