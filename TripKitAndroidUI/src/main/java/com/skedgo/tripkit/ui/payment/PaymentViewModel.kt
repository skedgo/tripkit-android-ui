package com.skedgo.tripkit.ui.payment

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.trippreview.drt.DrtItem
import com.skedgo.tripkit.ui.trippreview.drt.DrtItemViewModel
import com.skedgo.tripkit.ui.trippreview.drt.DrtTicketViewModel
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
class PaymentViewModel @Inject constructor() : RxViewModel() {

    private val _drtItems = MutableLiveData<List<PaymentSummaryDetails>>()
    val drtItems: LiveData<List<PaymentSummaryDetails>> = _drtItems

    private val _drtTickets = MutableLiveData<List<PaymentSummaryDetails>>()
    val drtTickets: LiveData<List<PaymentSummaryDetails>> = _drtTickets

    fun setData(items: List<PaymentSummaryDetails>, tickets: List<PaymentSummaryDetails>) {

    }
}