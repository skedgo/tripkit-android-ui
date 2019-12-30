package com.skedgo.tripkit.ui.core.rxproperty

import androidx.databinding.ObservableField
import io.reactivex.Observable

/**
 * When we subscribe to an [Observable] created by this function,
 * the [Observable] will emit the current value of [ObservableField]
 * and also emit any future changes.
 */
fun<T> ObservableField<T>.asObservable(): Observable<T> {
    return Observable.create {
        // To emit the current value.
        get()?.let { it1 -> it.onNext(it1) }
        val callback = object : androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                get()?.let { it1 -> it.onNext(it1) }
            }
        }
        addOnPropertyChangedCallback(callback)
        it.setCancellable { removeOnPropertyChangedCallback(callback) }
    }
}