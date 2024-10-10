package com.skedgo.tripkit.ui.core

import com.skedgo.tripkit.utils.OptionalCompat
import io.reactivex.Observable

fun <T : Any> Observable<out OptionalCompat<T>>.filterSome(): Observable<T> =
    this.filter { it.isPresent() } // Check if OptionalCompat contains a value
        .map { it.get() } // Extract the value from OptionalCompat

fun <T : Any> Observable<out OptionalCompat<T>>.filterNone(): Observable<Unit> =
    this.filter { !it.isPresent() } // Check if OptionalCompat is empty
        .map { Unit } // Emit Unit when OptionalCompat is empty
