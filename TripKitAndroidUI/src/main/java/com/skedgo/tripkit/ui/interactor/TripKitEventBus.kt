package com.skedgo.tripkit.ui.interactor

import com.skedgo.tripkit.ui.payment.PaymentData
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object TripKitEventBus {
    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class TripKitEvent {
    data class OnBookDrt(
            val fragmentHashCode: Int,
            val bookCallBack: (Boolean, PaymentData?) -> Unit //Boolean for if success or not
    )
    data class onDrtConfirmPaymentUpdate(val updateUrl: String)
}