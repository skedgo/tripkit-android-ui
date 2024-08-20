package com.skedgo.tripkit.ui.core

import io.reactivex.Observable

fun <T> Observable<T>.isExecuting(f: (Boolean) -> Unit): Observable<T> {
    return this.doOnSubscribe { f(true) }
        .doOnTerminate { f(false) }
        .doOnDispose { f(false) }
}