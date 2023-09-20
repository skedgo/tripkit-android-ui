package com.skedgo.tripkit.ui.controller.routeviewcontroller

import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.core.RxViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


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