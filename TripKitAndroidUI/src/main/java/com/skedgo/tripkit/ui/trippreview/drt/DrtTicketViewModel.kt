package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.ui.utils.toIntSafe
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class DrtTicketViewModel : ViewModel() {

    var onChangeStream: MutableSharedFlow<DrtTicketViewModel>? = null

    private val _label = MutableLiveData<String>()
    val label: LiveData<String> = _label

    private val _currency = MutableLiveData<String>()
    val currency: LiveData<String> = _currency

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description

    private val _price = MutableLiveData<Double>()
    val price: LiveData<Double> = _price

    private val _value = MutableLiveData<Long>()
    val value: LiveData<Long> = _value

    private val _contentDescription = MutableLiveData<String>()
    val contentDescription: LiveData<String> = _contentDescription

    private val _required = MutableLiveData<Boolean>()
    val required: LiveData<Boolean> = _required

    private val _viewMode = MutableLiveData<Boolean>()
    val viewMode: LiveData<Boolean> = _viewMode

    private val _enableIncrement = MutableLiveData(true)
    val enableIncrement: LiveData<Boolean> = _enableIncrement

    private val _enableDecrement = MutableLiveData(false)
    val enableDecrement: LiveData<Boolean> = _enableDecrement

    fun onChange() {
        viewModelScope.launch {
            onChangeStream?.emit(this@DrtTicketViewModel)
        }
    }

    fun setLabel(value: String) {
        _label.value = value
    }

    fun setCurrency(value: String) {
        _currency.value = value
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setPrice(value: Double) {
        _price.value = value
    }

    fun onIncrementValue() {
        _value.value = ((value.value)?.toLong() ?: 0) + 1
        onChange()
    }

    fun onDecrementValue() {
        val result = ((value.value)?.toLong() ?: 0) - 1
        if (result >= 0) _value.value = result
        else _value.value = 0
        onChange()
    }

    fun onSelect() {
        _value.value = 1
        onChange()
    }

    companion object {
        fun diffCallback() = object : DiffUtil.ItemCallback<DrtTicketViewModel>() {
            override fun areItemsTheSame(
                oldItem: DrtTicketViewModel,
                newItem: DrtTicketViewModel
            ): Boolean =
                oldItem.label == newItem.label

            override fun areContentsTheSame(
                oldItem: DrtTicketViewModel,
                newItem: DrtTicketViewModel
            ): Boolean =
                oldItem.label.value == newItem.label.value
        }
    }
}