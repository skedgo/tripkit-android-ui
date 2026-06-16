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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reactive wrapper around [FusedLocationProviderClient] exposing location updates as
 * RxJava [Flowable]s and Kotlin [Flow]s.
 *
 * [removeLocationUpdates] is async, so GMS may still deliver callbacks after disposal.
 * The RxJava path guards against this with an [AtomicBoolean] cancellation flag; the
 * [Flow] path relies on [callbackFlow]/[awaitClose] where [trySend] on a closed channel
 * is already a safe no-op.
 */
open class RxFusedLocationProviderClient(
    private val client: FusedLocationProviderClient
) {
    /**
     * Emits the most recent known location as [LastLocation.Available], or
     * [LastLocation.NotAvailable] when no cached fix exists.
     *
     * @see [The Tasks API](https://developers.google.com/android/guides/tasks)
     */
    @SuppressLint("MissingPermission")
    open fun getLastLocation(): Flowable<LastLocation> =
        Flowable.create<LastLocation>({ emitter ->
            client.lastLocation.addOnCompleteListener { task ->
                when {
                    task.isSuccessful && task.result != null ->
                        emitter.onNext(LastLocation.Available(task.result!!))
                    task.isSuccessful ->
                        emitter.onNext(LastLocation.NotAvailable)
                    else ->
                        emitter.onError(task.exception!!)
                }
            }
        }, BackpressureStrategy.BUFFER)

    /**
     * Streams [LocationUpdates] via [FusedLocationProviderClient.requestLocationUpdates].
     * Disposing the subscription calls [removeLocationUpdates] and sets an [AtomicBoolean]
     * cancellation flag *before* the removal request, so any in-flight GMS callbacks
     * arriving during the async removal window are silently dropped.
     *
     * @param request Frequency and accuracy parameters for location updates.
     * @see [The Tasks API](https://developers.google.com/android/guides/tasks)
     */
    @SuppressLint("MissingPermission")
    open fun requestLocationUpdates(request: LocationRequest): Flowable<LocationUpdates> =
        Flowable.create<LocationUpdates>({ emitter ->
            val isCancelled = AtomicBoolean(false)

            val callback = object : LocationCallback() {
                override fun onLocationAvailability(availability: LocationAvailability?) {
                    if (isCancelled.get()) return
                    availability?.let {
                        emitter.onNext(LocationUpdates.Availability(it))
                    }
                }

                override fun onLocationResult(result: LocationResult?) {
                    if (isCancelled.get()) return
                    result?.let { emitter.onNext(LocationUpdates.Result(it)) }
                }
            }

            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { error ->
                    if (!isCancelled.get()) emitter.onError(error)
                }

            emitter.setCancellable {
                isCancelled.set(true)
                client.removeLocationUpdates(callback)
                    .addOnFailureListener { Log.e(TAG, "removeLocationUpdates failed", it) }
            }
        }, BackpressureStrategy.BUFFER)

    /**
     * Projects [requestLocationUpdates] to raw [Location] objects, discarding availability
     * events. Subscribes on IO, observes on the main thread.
     *
     * @param request Frequency and accuracy parameters for location updates.
     */
    open fun requestLocationStream(request: LocationRequest): Observable<Location> =
        requestLocationUpdates(request).toObservable()
            .flatMap { update ->
                when (update) {
                    is LocationUpdates.Result ->
                        update.value.lastLocation?.let { Observable.just(it) }
                            ?: Observable.empty()
                    else -> Observable.empty()
                }
            }
            .subscribeOn(io())
            .observeOn(AndroidSchedulers.mainThread())

    /**
     * [Flow] counterpart of [requestLocationStream]; emits non-null [Location] values only.
     *
     * @param request Frequency and accuracy parameters for location updates.
     */
    open fun requestLocationStreamFlow(request: LocationRequest): Flow<Location> =
        requestLocationUpdatesFlow(request)
            .mapNotNull { update ->
                when (update) {
                    is LocationUpdates.Result -> update.value.lastLocation
                    else -> null
                }
            }

    /**
     * [Flow]-based equivalent of [requestLocationUpdates]. [awaitClose] ensures
     * [removeLocationUpdates] is called on cancellation; stale callbacks after removal
     * are silently dropped via [trySend] on a closed channel.
     *
     * @param request Frequency and accuracy parameters for location updates.
     */
    @SuppressLint("MissingPermission")
    open fun requestLocationUpdatesFlow(request: LocationRequest): Flow<LocationUpdates> =
        callbackFlow {
            val callback = object : LocationCallback() {
                override fun onLocationAvailability(availability: LocationAvailability) {
                    trySend(LocationUpdates.Availability(availability))
                }

                override fun onLocationResult(result: LocationResult) {
                    trySend(LocationUpdates.Result(result))
                }
            }

            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }

            awaitClose {
                client.removeLocationUpdates(callback)
                    .addOnFailureListener { Log.e(TAG, "removeLocationUpdates failed", it) }
            }
        }

    companion object {
        private const val TAG = "RxFusedLocationClient"
    }
}
