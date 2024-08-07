package com.skedgo.tripkit.ui.interactor

//object TripKitEventBus {
//    private val publisher = PublishSubject.create<Any>()
//
//    fun publish(event: Any) {
//        publisher.onNext(event)
//    }
//
//    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
//}
//
//class TripKitEvent {
//    data class OnBookDrt(
//            val fragmentHashCode: Int,
//            val bookCallBack: (Boolean, PaymentData?) -> Unit //Boolean for if success or not
//    )
//    data class OnDrtConfirmPaymentUpdate(val updateUrl: String)
//    data class OnToggleDrtFooterVisibility(val isHide: Boolean)
//
//}