package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class TKUIDateTimePickerDialogViewModel @Inject constructor() : ViewModel() {

    private val _config = MutableLiveData<DateTimePickerConfig>()
    val config: LiveData<DateTimePickerConfig> = _config

    private val _state = MutableLiveData<DateTimePickerState>()
    val state: LiveData<DateTimePickerState> = _state

    fun setup(config: DateTimePickerConfig) {
        _config.postValue(config)
    }
}