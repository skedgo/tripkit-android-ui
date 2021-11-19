package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.booking.quickbooking.Option
import com.skedgo.tripkit.booking.quickbooking.QuickBookingType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class DrtItemViewModel : ViewModel() {

    var onChangeStream: MutableSharedFlow<DrtItemViewModel>? = null

    private val _icon = MutableLiveData<Int>()
    val icon: LiveData<Int> = _icon

    private val _label = MutableLiveData<String>()
    val label: LiveData<String> = _label

    private val _values = MutableLiveData<List<String>>()
    val values: LiveData<List<String>> = _values

    private val _required = MutableLiveData<Boolean>()
    val required: LiveData<Boolean> = _required

    private val _viewMode = MutableLiveData<Boolean>()
    val viewMode: LiveData<Boolean> = _viewMode

    private val _itemId = MutableLiveData<String>()
    val itemId: LiveData<String> = _itemId

    private val _options = MutableLiveData<List<Option>>()
    val options: LiveData<List<Option>> = _options

    private val _type = MutableLiveData<String>()
    val type: LiveData<String> = _type

    private val _showChangeButton = MutableLiveData(true)
    val showChangeButton: LiveData<Boolean> = _showChangeButton

    private val _isReturnTrip = MutableLiveData(false)
    val isReturnTrip: LiveData<Boolean> = _isReturnTrip

    private val _rawDate = MutableLiveData<String>()
    val rawDate: LiveData<String> = _rawDate

    fun setIcon(value: Int) {
        _icon.value = value
    }

    fun setLabel(value: String) {
        _label.value = value
    }

    fun setValue(values: List<String>) {
        _values.value = values
    }

    fun setRequired(value: Boolean) {
        _required.value = value
    }

    fun onChange() {
        viewModelScope.launch {
            onChangeStream?.emit(this@DrtItemViewModel)
        }
    }

    fun setViewMode(value: Boolean) {
        _viewMode.value = value
    }

    fun setItemId(value: String) {
        _itemId.value = value
    }

    fun setOptions(value: List<Option>) {
        _options.value = value
    }

    fun setType(value: String) {
        _type.value = value
        if (value.equals(QuickBookingType.RETURN_TRIP, true)) {
            setIsReturnTrip(true)
        }
    }

    fun setShowChangeButton(value: Boolean) {
        _showChangeButton.value = value
    }

    fun setIsReturnTrip(value: Boolean) {
        _isReturnTrip.value = value
    }

    fun setRawDate(value: String) {
        _rawDate.value = value
    }

    companion object {
        fun diffCallback() = object : DiffUtil.ItemCallback<DrtItemViewModel>() {
            override fun areItemsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                    oldItem.label == newItem.label

            override fun areContentsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                    oldItem.label.value == newItem.label.value
                            && oldItem.values.value == newItem.values.value
        }
    }
}