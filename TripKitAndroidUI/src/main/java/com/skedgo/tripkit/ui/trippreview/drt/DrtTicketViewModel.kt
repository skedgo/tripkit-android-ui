package com.skedgo.tripkit.ui.trippreview.drt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.booking.quickbooking.Fare
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

    private val _name = MutableLiveData<String?>()
    val name: LiveData<String?> = _name

    private val _description = MutableLiveData<String?>()
    val description: LiveData<String?> = _description

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

    private val _fare = MutableLiveData<Fare>()
    val fare: LiveData<Fare> = _fare

    private val _itemId = MutableLiveData<String>()
    val itemId: LiveData<String> = _itemId

    private val _accessibilityFocusIncrementAction = MutableLiveData<Boolean>()
    val accessibilityFocusIncrementAction: LiveData<Boolean> = _accessibilityFocusIncrementAction

    private val _accessibilityFocusSelectAction = MutableLiveData<Boolean>()
    val accessibilityFocusSelectAction: LiveData<Boolean> = _accessibilityFocusSelectAction

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

    fun setDescription(value: String?) {
        // Removed due to new design no longer showing this
//        _description.value = value
    }

    fun setName(value: String?) {
        _name.postValue(value)
    }

    fun setPrice(value: Double, currency: String) {
        _description.value = if ((value / 100.0) > 0) {
            String.format("%s%.2f", currency, value / 100.0)
        } else {
            "FREE"
        }
        _price.value = value
    }

    fun setTicket(value: Fare) {
        _fare.value = value
        setValue(value.value ?: _value.value ?: 0L)
    }

    fun setValue(value: Long) {
        val quantity = _fare.value?.quantityAtLeastMinimum(value) ?: value
        _value.value = quantity
        updateTicket(quantity)
        updateQuantityControls()
    }

    private fun updateQuantityControls() {
        val quantity = _value.value ?: 0L
        _enableIncrement.value = _fare.value?.canIncreaseQuantity(quantity) ?: (quantity < Long.MAX_VALUE)
        _enableDecrement.value = _fare.value?.let { it.decreasedQuantity(quantity) < quantity } ?: (quantity > 0L)
    }

    fun setItemId(value: String) {
        _itemId.value = value
    }

    fun setViewMode(value: Boolean) {
        _viewMode.value = value
    }

    fun onIncrementValue() {
        _accessibilityFocusIncrementAction.postValue(false)
        val current = value.value ?: 0L
        val result = _fare.value?.increasedQuantity(current)
            ?: if (current == Long.MAX_VALUE) current else current + 1
        setValue(result)
        updateTicket(result)
        onChange()
    }

    fun onDecrementValue() {
        val current = value.value ?: 0L
        setValue(_fare.value?.decreasedQuantity(current) ?: (current - 1).coerceAtLeast(0))

        _accessibilityFocusSelectAction.postValue((_value.value ?: 0) == 0L)

        updateTicket(_value.value ?: 0)
        onChange()
    }

    private fun updateTicket(value: Long) {
        val ticket = fare.value
        ticket?.value = value
        ticket?.let {
            _fare.value = it
        }
    }

    fun onSelect() {
        if (_fare.value?.hasValidQuantityBounds() == false) return
        setValue(_fare.value?.minimumSelectedQuantity ?: 1L)
        updateTicket(_value.value ?: 0L)
        _accessibilityFocusSelectAction.postValue(false)
        _accessibilityFocusIncrementAction.postValue(true)
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