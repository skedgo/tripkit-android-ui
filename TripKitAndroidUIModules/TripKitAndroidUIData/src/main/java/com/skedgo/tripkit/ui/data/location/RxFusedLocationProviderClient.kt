package com.skedgo.tripkit.ui.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers.io
import java.lang.ref.WeakReference

open class RxFusedLocationProviderClient(
    private val client: FusedLocationProviderClient
) {
    /**
     * @see [The Tasks API](https://developers.google.com/android/guides/tasks).
     */
    @SuppressLint("MissingPermission")
    open fun getLastLocation(): Flowable<LastLocation> =
        Flowable.create<LastLocation>({
            val emitterWeakRef = WeakReference(it)
            client.lastLocation.addOnCompleteListener {
                when (it.isSuccessful) {
                    true -> when (it.result) {
                        null -> emitterWeakRef.get()?.onNext(LastLocation.NotAvailable)
                        else -> emitterWeakRef.get()?.onNext(LastLocation.Available(it.result!!))
                    }
                    false -> emitterWeakRef.get()?.onError(it.exception!!)
                }
            }
        }, BackpressureStrategy.BUFFER)

    /**
     * A RxJava-based version of [FusedLocationProviderClient.requestLocationUpdates].
     *
     * @see [The Tasks API](https://developers.google.com/android/guides/tasks).
     */
    @SuppressLint("MissingPermission")
    open fun requestLocationUpdates(request: LocationRequest): Flowable<LocationUpdates> =
        Flowable.create<LocationUpdates>({ emitter ->
            // FIXME: The `client` still references to `callback` even after calling `removeLocationUpdates`.
            // This is why we gotta make this weak as a temporary fix.
            val emitterWeakRef = WeakReference(emitter)
            val callback = object : LocationCallback() {
                override fun onLocationAvailability(availability: LocationAvailability?) {
                    availability?.let {
                        emitterWeakRef.get()?.onNext(LocationUpdates.Availability(it))
                    }
                }

                override fun onLocationResult(result: LocationResult?) {
                    result?.let { emitterWeakRef.get()?.onNext(LocationUpdates.Result(it)) }
                }
            }
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { emitter.onError(it) }
            emitter.setCancellable() {
                client.removeLocationUpdates(callback)
                    .addOnFailureListener { Log.e("removeLocationUpdates", it.message, it) }
            }
        }, BackpressureStrategy.BUFFER)

    open fun requestLocationStream(request: LocationRequest): Observable<Location> =
        requestLocationUpdates(request).toObservable()
            .flatMap {
                when (it) {
                    is LocationUpdates.Result -> {
                        it.value.lastLocation?.let {
                            Observable.just(it)
                        } ?: Observable.empty()
                    }
                    else -> Observable.empty()
                }
            }
            .subscribeOn(io())
            .observeOn(AndroidSchedulers.mainThread())
}
