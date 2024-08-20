package com.skedgo.tripkit.ui.utils

import com.skedgo.tripkit.ui.BuildConfig
import io.reactivex.Observable
import java.io.IOException

fun <T> Observable<T>.ignoreNetworkErrors(): Observable<T> =
    onErrorResumeNext { throwable: Throwable ->
        if (BuildConfig.DEBUG) {
            throwable.printStackTrace()
        }
        when (throwable) {
            is IOException -> Observable.empty()
            else -> Observable.error(throwable)
        }
    }