package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.booking.quickbooking.Ticket
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.payment.PaymentSummaryDetails
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

    private val _ticket = MutableLiveData<Ticket>()
    val ticket: LiveData<Ticket> = _ticket

    private val _itemId = MutableLiveData<String>()
    val itemId: LiveData<String> = _itemId

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

    fun setTicket(value: Ticket) {
        _ticket.value = value
    }

    fun setValue(value: Long) {
        _value.value = value
    }

    fun setItemId(value: String) {
        _itemId.value = value
    }

    fun onIncrementValue() {
        val result = ((value.value)?.toLong() ?: 0) + 1
        _value.value = result
        updateTicket(result)
        onChange()
    }

    fun onDecrementValue() {
        val result = ((value.value)?.toLong() ?: 0) - 1
        if (result >= 0) _value.value = result
        else _value.value = 0
        updateTicket(_value.value ?: 0)
        onChange()
    }

    private fun updateTicket(value: Long) {
        val ticket = ticket.value
        ticket?.value = value
        ticket?.let {
            _ticket.value = it
        }
    }

    fun onSelect() {
        _value.value = 1
        onChange()
    }

    @Deprecated("Will be replaced by parsing details from Review")
    fun generateSummaryDetails(): PaymentSummaryDetails {
        return PaymentSummaryDetails(
                hashCode().toString(),
                R.drawable.ic_person,
                label.value ?: "",
                breakdown = value.value,
                price = price.value,
                currency = currency.value
        )
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