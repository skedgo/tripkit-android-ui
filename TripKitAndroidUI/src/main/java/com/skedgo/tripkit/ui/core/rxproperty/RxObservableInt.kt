package com.skedgo.tripkit.ui.core.rxproperty

import androidx.databinding.ObservableInt
import io.reactivex.Observable

/**
 * When we subscribe to an [Observable] created by this function,
 * the [Observable] will emit the current value of [ObservableInt]
 * and also emit any future changes.
 */
fun ObservableInt.asObservable(): Observable<Int> {
    return Observable.create {
        // To emit the current value.
        it.onNext(get())
        val callback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                it.onNext(get())
            }
        }
        addOnPropertyChangedCallback(callback)
        it.setCancellable { removeOnPropertyChangedCallback(callback) }
    }
}