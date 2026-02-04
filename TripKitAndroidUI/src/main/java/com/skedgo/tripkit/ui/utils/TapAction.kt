package com.skedgo.tripkit.ui.utils

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TapAction<TSender> internal constructor(
    private val getSender: () -> TSender
) {
    companion object Factory {
        fun <TSender> create(getSender: () -> TSender): TapAction<TSender> = TapAction(getSender)
    }

    private val onTap = PublishSubject.create<TSender>()
    val observable: Observable<TSender>
        get() = onTap.hide()

        @Suppress("UNCHECKED_CAST")
    fun perform() {
        val sender = getSender()
        // Kotlin 2.0 requires non-nullable type for RxJava onNext
        // Cast to Any to satisfy Kotlin 2.0's stricter type checking
        @Suppress("UNCHECKED_CAST")
        (onTap as PublishSubject<Any>).onNext(sender as Any)
    }
}