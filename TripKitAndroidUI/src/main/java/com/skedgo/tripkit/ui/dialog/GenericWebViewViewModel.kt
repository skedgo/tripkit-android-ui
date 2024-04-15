package com.skedgo.tripkit.ui.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject

class GenericWebViewViewModel @Inject constructor() : RxViewModel() {

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _acceptShown = MutableLiveData<Boolean>()
    val acceptShown: LiveData<Boolean> = _acceptShown
    fun setAcceptShown(value: Boolean) {
        _acceptShown.value = value
    }

    fun setTitle(value: String) {
        _title.value = value
    }
}