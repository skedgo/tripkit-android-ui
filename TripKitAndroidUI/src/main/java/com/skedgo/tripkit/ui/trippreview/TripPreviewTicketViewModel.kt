package com.skedgo.tripkit.ui.trippreview

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.payment.PaymentData
import javax.inject.Inject

class TripPreviewTicketViewModel @Inject constructor() : RxViewModel() {

    /*
    private val _tickets = MutableLiveData<List<Ticket>>()
    val tickets: LiveData<List<Ticket>> = _tickets
    */

    private val _labelTicketQuantity = MutableLiveData<String>()
    val labelTicketQuantity: LiveData<String> = _labelTicketQuantity

    private val _labelTicketTotal = MutableLiveData<String>()
    val labelTicketTotal: LiveData<String> = _labelTicketTotal

    private val _showView = MutableLiveData<Boolean>()
    val showView: LiveData<Boolean> = _showView

    private val _goToPayment = MutableLiveData<Unit>()
    val goToPayment: LiveData<Unit> = _goToPayment

    /*
    private val _drtItems = MutableLiveData<List<DrtItemViewModel>>()
    val drtItems: LiveData<List<DrtItemViewModel>> = _drtItems

    private val _drtTickets = MutableLiveData<List<DrtTicketViewModel>>()
    val drtTickets: LiveData<List<DrtTicketViewModel>> = _drtTickets

    fun setDrtItems(items: List<DrtItemViewModel>) {
        _drtItems.value = items
    }

    fun setDrtTickets(tickets: List<DrtTicketViewModel>) {
        _drtTickets.value = tickets
    }
    */

    private val _paymentData = MutableLiveData<PaymentData>()
    val paymentData: LiveData<PaymentData> = _paymentData

    fun setPaymentData(data: PaymentData) {
        _paymentData.value = data
    }

    fun setTotalTickets(value: Double, currency: String) {
        _labelTicketTotal.value = String.format("%s%.2f", currency, value)
    }

    fun setNumberTickets(value: Int) {
        _labelTicketQuantity.value =
                String.format("%s %s", value.toString(), if (value > 1) "tickets" else "ticket")
    }

    fun setShowView(value: Boolean) {
        _showView.value = value
    }

    /*
    fun setTickets(value: List<Ticket>) {
        _tickets.value = value
    }
    */

    fun onContinue() {
        _goToPayment.value = Unit
    }
}