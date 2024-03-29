package com.skedgo.tripkit.ui.core.rxproperty

import androidx.databinding.ObservableList
import androidx.databinding.ObservableList.OnListChangedCallback
import io.reactivex.Observable

fun <T> ObservableList<T>.asObservable(): Observable<ObservableList<T>> {
    return Observable.create {
        it.onNext(this)
        val callback = object : OnListChangedCallback<ObservableList<T>>() {
            override fun onItemRangeRemoved(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
                it.onNext(sender!!)
            }

            override fun onItemRangeInserted(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
                it.onNext(sender!!)
            }

            override fun onItemRangeMoved(sender: ObservableList<T>?, fromPosition: Int, toPosition: Int, itemCount: Int) {
                it.onNext(sender!!)
            }

            override fun onItemRangeChanged(sender: ObservableList<T>?, positionStart: Int, itemCount: Int) {
                it.onNext(sender!!)
            }

            override fun onChanged(sender: ObservableList<T>?) {
                it.onNext(sender!!)
            }
        }
        addOnListChangedCallback(callback)
        it.setCancellable { removeOnListChangedCallback(callback) }
    }
}