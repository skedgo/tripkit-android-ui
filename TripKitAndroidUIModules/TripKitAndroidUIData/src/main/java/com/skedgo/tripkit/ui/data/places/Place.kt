package com.skedgo.tripkit.ui.data.places

import android.graphics.drawable.Drawable
import com.skedgo.tripkit.common.model.location.Location

sealed class Place {
    abstract fun source(): String?
    abstract fun locationType(): Int
    abstract fun icon(): Drawable?

    data class TripGoPOI(val location: Location, val icon: Drawable?) : Place() {
        constructor(location: Location) : this(location, null)

        override fun source(): String? = location.source
        override fun locationType(): Int = location.locationType
        override fun icon(): Drawable? = icon
    }

    data class WithoutLocation(val prediction: GooglePlacePrediction, val icon: Drawable?) :
        Place() {
        constructor(prediction: GooglePlacePrediction) : this(prediction, null)

        override fun source(): String? = Location.GOOGLE
        override fun locationType(): Int = Location.TYPE_HISTORY
        override fun icon(): Drawable? = icon
    }
}
