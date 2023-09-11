package com.skedgo.tripkit.ui.controller.homeviewcontroller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel

class TKUIHomeViewControllerViewModel : RxViewModel() {

    private val _state = MutableLiveData(TKUIHomeViewControllerUIState())
    val state: LiveData<TKUIHomeViewControllerUIState> = _state

    private val _centerPinVisible = MutableLiveData(false)
    val centerPinVisible: LiveData<Boolean> = _centerPinVisible

    private val _locationPointerFrameVisible = MutableLiveData(false)
    val locationPointerFrameVisible: LiveData<Boolean> = _locationPointerFrameVisible

    private val _myLocationButtonVisible = MutableLiveData(false)
    val myLocationButtonVisible: LiveData<Boolean> = _myLocationButtonVisible

    fun setCenterPinVisible(isVisible: Boolean) {
        _centerPinVisible.postValue(isVisible)
    }

    fun setLocationPointerFrameVisible(isVisible: Boolean) {
        _locationPointerFrameVisible.postValue(isVisible)
    }

    fun setMyLocationButtonVisible(isVisible: Boolean) {
        _myLocationButtonVisible.postValue(isVisible)
    }

    fun toggleChooseOnMap(isShow: Boolean) {
        _state.postValue(
            _state.value?.copy(isChooseOnMap = isShow)
        )
    }
}