package com.skedgo.tripkit.ui.payment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.booking.quickbooking.PaymentIntent
import com.skedgo.tripkit.booking.quickbooking.QuickBookingService
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.routing.SegmentType
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripKitUI
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.core.fetchAsync
import com.skedgo.tripkit.ui.trippreview.drt.DrtItem
import com.skedgo.tripkit.ui.trippreview.drt.DrtItemViewModel
import com.skedgo.tripkit.ui.trippreview.drt.DrtTicketViewModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("StaticFieldLeak")
class PaymentSummaryViewModel @Inject constructor(
        private val quickBookingService: QuickBookingService
) : RxViewModel() {

    private val _paymentData = MutableLiveData<PaymentData>()
    val paymentData: LiveData<PaymentData> = _paymentData

    private val _summaryDetails = MutableLiveData<List<PaymentSummaryDetails>>()
    val summaryDetails: LiveData<List<PaymentSummaryDetails>> = _summaryDetails

    private val _modeIcon = MutableLiveData<Drawable>()
    val modeIcon: LiveData<Drawable> = _modeIcon

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Throwable>()
    val error: LiveData<Throwable> = _error

    private val _paymentIntent = MutableLiveData<PaymentIntent>()
    val paymentIntent: LiveData<PaymentIntent> = _paymentIntent

    fun setData(context: Context, data: PaymentData) {
        _paymentData.value = data
        _summaryDetails.value = data.paymentSummaryDetails
        generateModeIcon(context, data.modeIcon, data.modeDarkVehicleIcon ?: 0)
    }

    private fun generateModeIcon(context: Context, url: String?, darkVehicleIcon: Int) {
        var remoteIcon = Observable.empty<Drawable>()
        if (url != null) {
            remoteIcon = TripKitUI.getInstance().picasso().fetchAsync(url).toObservable()
                    .map { bitmap -> BitmapDrawable(context.resources, bitmap) }
        }
        Observable
                .just(ContextCompat.getDrawable(context, darkVehicleIcon))
                .concatWith(remoteIcon)
                .map { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ drawable:
                             Drawable ->
                    _modeIcon.postValue(drawable)
                }, { e -> Timber.e(e) }).autoClear()
    }

    fun onContinueToPayment() {
        _paymentData.value?.let {
            it.paymentOptions?.first()?.let { paymentOption ->
                quickBookingService.getPaymentIntent(paymentOption.url)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            _loading.postValue(true)
                        }.doAfterTerminate {
                            _loading.postValue(false)
                        }
                        .subscribeBy(
                                onError = {
                                    _error.postValue(it)
                                },
                                onSuccess = {
                                    _paymentIntent.postValue(it)
                                }
                        ).autoClear()
            }
        }
    }
}