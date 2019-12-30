package com.skedgo.tripkit.ui.utils

import io.reactivex.Observable
import java.io.IOException

fun <T> Observable<T>.ignoreNetworkErrors(): Observable<T> =
    onErrorResumeNext { throwable: Throwable ->
      when (throwable) {
        is IOException -> Observable.empty()
        else -> Observable.error(throwable)
      }
    }