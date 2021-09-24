package com.skedgo.tripkit.ui.core

import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class RxViewModel() : ViewModel() {
    private val clearRelay = PublishRelay.create<Unit>()
    protected val compositeSubscription: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    fun <T> Observable<T>.autoClear(): Observable<T> = this.takeUntil(clearRelay)
    fun Disposable.autoClear() {
        compositeSubscription.add(this)
    }

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     */
    public override fun onCleared() {
        super.onCleared()
        clearRelay.accept(Unit)
        compositeSubscription.clear()
    }
}
