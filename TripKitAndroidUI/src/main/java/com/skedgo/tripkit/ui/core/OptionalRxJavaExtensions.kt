package com.skedgo.tripkit.ui.core

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
import io.reactivex.Observable

@Suppress("UNCHECKED_CAST")
fun <T : Any> Observable<out Optional<T>>.filterSome(): Observable<T> =
    ofType(Some::class.java).map { it.value as T }

fun <T : Any> Observable<out Optional<T>>.filterNone(): Observable<Unit> =
    ofType(None::class.java).map { Unit }
