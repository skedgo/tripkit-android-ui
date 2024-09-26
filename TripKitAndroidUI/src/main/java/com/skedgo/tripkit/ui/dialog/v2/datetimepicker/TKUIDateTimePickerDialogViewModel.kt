package com.skedgo.tripkit.ui.dialog.v2.datetimepicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class TKUIDateTimePickerDialogViewModel @Inject constructor() : ViewModel() {

    private val _config = MutableLiveData<DateTimePickerConfig>()
    val config: LiveData<DateTimePickerConfig> = _config

    private val _state = MutableLiveData<DateTimePickerState>()
    val state: LiveData<DateTimePickerState> = _state

    private val _showTimePicker = MutableLiveData(false)
    val showTimePicker: LiveData<Boolean> = _showTimePicker

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _time = MutableLiveData<String>()
    val time: LiveData<String> = _time

    val isOneWayOnly = MutableLiveData(false)

    private val _selectedDate = MutableLiveData<Long>()
    val selectedDate: LiveData<Long> = _selectedDate

    init {
        _state.observeForever { state ->
            _error.postValue(null)
            _showTimePicker.postValue(state is DateTimePickerState.OnShowTimePicker)
            if (state is DateTimePickerState.OnError) {
                _error.postValue(state.error.message)
            }
        }
    }

    fun setup(config: DateTimePickerConfig) {
        _config.postValue(config)
    }

    fun setState(state: DateTimePickerState) {
        _state.postValue(state)
    }

    fun setTime(time: String) {
        _time.postValue(time)
    }

    fun setSelectedDate(dateInMillis: Long) {
        _selectedDate.postValue(dateInMillis)
    }

    fun combineDateTime(dateInMillis: Long, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(config.value?.timeZone ?: TimeZone.getDefault()).apply {
            timeInMillis = dateInMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
}