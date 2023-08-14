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


class TKUIRouteViewModel constructor(/*private val favoriteRepository: FavoriteRepository*/) : RxViewModel() {
    enum class FocusedField {
        NONE,
        START,
        DESTINATION
    }

    var focusedField = FocusedField.NONE

    private val _start = MutableLiveData<String>("")
    val start: LiveData<String> = _start

    private val _destination = MutableLiveData<String>("")
    val destination: LiveData<String> = _destination

    /*
    var home = ObservableField<Home>()
    var work = ObservableField<Work>()
    */

    private val _accessibilityFocusStartField = MutableLiveData<Boolean>()
    val accessibilityFocusStartField: LiveData<Boolean> = _accessibilityFocusStartField

    private val _accessibilityFocusDestinationField = MutableLiveData<Boolean>()
    val accessibilityFocusDestinationField: LiveData<Boolean> = _accessibilityFocusDestinationField

    var destinationLocation: Location? = null
        set(value) {
            field = value
            _destination.postValue(destinationLocation?.displayName ?: "")
        }

    var startLocation: Location? = null
        set(value) {
            field = value
            _start.postValue(startLocation?.displayName ?: "")
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

    /*
    fun getLocation(type: Int) {
        if (type == Location.TYPE_WORK) {
            favoriteRepository.work()
                    .onEach { work ->
                        this.work.set(work)
                    }.launchIn(viewModelScope)
        } else if (type == Location.TYPE_HOME) {
            favoriteRepository.home()
                    .onEach { home ->
                        this.home.set(home)
                    }.launchIn(viewModelScope)
        }
    }
    */

    fun swapLocations() {
        val startLoc = startLocation
        val destLoc = destinationLocation

        destinationLocation = startLoc
        startLocation = destLoc
    }

    fun setStartAccessibilityFocus() {
        _accessibilityFocusStartField.postValue(true)
        _accessibilityFocusDestinationField.postValue(false)
    }

    fun setDestinationAccessibilityFocus() {
        _accessibilityFocusStartField.postValue(false)
        _accessibilityFocusDestinationField.postValue(true)
    }
}