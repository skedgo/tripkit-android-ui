package com.skedgo.tripkit.ui.controller.locationsearchcontroller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel

class TKUILocationSearchViewControllerViewModel: RxViewModel() {

    private val _withHeaders = MutableLiveData(false)
    val withHeaders: LiveData<Boolean> = _withHeaders

    fun setWithHeaders(value: Boolean) {
        _withHeaders.postValue(value)
    }
}