package com.skedgo.tripkit.ui.trippreview

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.booking.quickbooking.Ticket
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject

class TripPreviewTicketViewModel @Inject constructor(): RxViewModel() {

    private val _tickets = MutableLiveData<List<Ticket>>()
    val tickets: LiveData<List<Ticket>> = _tickets

    private val _labelTicketQuantity = MutableLiveData<String>()
    val labelTicketQuantity: LiveData<String> = _labelTicketQuantity

    private val _labelTicketTotal = MutableLiveData<String>()
    val labelTicketTotal: LiveData<String> = _labelTicketTotal

    private val _showView = MutableLiveData<Boolean>()
    val showView: LiveData<Boolean> = _showView

    fun setTotalTickets(value: Double, currency: String) {
        _labelTicketTotal.value = String.format("%s%.2f", currency, value)
    }

    fun setNumberTickets(value: Long) {
        _labelTicketQuantity.value =
            String.format("%s %s", value.toString(), if (value > 1) "tickets" else "ticket")
    }

    fun setShowView(value: Boolean) {
        _showView.value = value
    }

    fun setTickets(value: List<Ticket>) {
        _tickets.value = value
    }

    fun onContinue() {
        // TODO
        Log.i("TripPreviewTicketViewModel", "onChange")
    }
}