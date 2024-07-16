package com.skedgo.tripkit.ui.core.settings

import android.content.SharedPreferences
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable

/**
 * An extension function to observe future changes on value of certain key.
 * Note that the [Observable] returned by this function is not intended to emit the current value.
 */
fun SharedPreferences.onChange(observedKey: String): Observable<Pair<SharedPreferences, String>> {
  return Flowable
      .create<String>({
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key != null) {
                it.onNext(key)
            }
        }
        registerOnSharedPreferenceChangeListener(listener)
        it.setCancellable { unregisterOnSharedPreferenceChangeListener(listener) }
      }, BackpressureStrategy.BUFFER)
      .toObservable()
      .filter { it == observedKey }
      .map { Pair<SharedPreferences, String>(this, it) }
}
