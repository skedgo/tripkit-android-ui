package com.skedgo.tripkit.ui.controller.routeviewcontroller

import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.core.RxViewModel


class TKUIRouteViewModel : RxViewModel() {
    enum class FocusedField {
        NONE,
        START,
        DESTINATION
    }

    var focusedField = FocusedField.NONE

    val start = MutableLiveData<String>("")

    val destination = MutableLiveData<String>("")

    var destinationLocation: Location? = null
        set(value) {
            field = value
            destination.postValue(destinationLocation?.displayName ?: "")
        }

    var startLocation: Location? = null
        set(value) {
            field = value
            start.postValue(startLocation?.displayName ?: "")
        }

    var swap = PublishRelay.create<Unit>()

    fun swap() {
        swap.accept(Unit)
    }

    fun bothLocationsAreValid(): Boolean {
        return (destinationLocation != null
            && startLocation != null
            && destinationLocation != startLocation
            && (startLocation!!.lat != 0.0 && startLocation!!.lon != 0.0)
            && (destinationLocation!!.lat != 0.0 && destinationLocation!!.lon != 0.0))
    }

    fun swapLocations() {
        val startLoc = startLocation
        val destLoc = destinationLocation

        destinationLocation = startLoc
        startLocation = destLoc
    }

    fun setStart(value: String) {
        start.postValue(value)
    }

    fun setDestination(value: String) {
        destination.postValue(value)
    }
}