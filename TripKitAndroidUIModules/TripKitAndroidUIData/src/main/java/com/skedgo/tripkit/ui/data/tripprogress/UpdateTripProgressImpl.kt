package com.skedgo.tripkit.ui.data.tripprogress
import android.util.Log
import com.skedgo.rxtry.Failure
import com.skedgo.rxtry.Success
import com.skedgo.rxtry.Try
import com.skedgo.rxtry.toTry
import com.skedgo.tripkit.location.LocationSample
import com.skedgo.tripkit.ui.tripprogress.UpdateTripProgress
import io.reactivex.Observable
import timber.log.Timber

class UpdateTripProgressImpl(val api: UpdateProgressApi
) : UpdateTripProgress {
  override fun execute(progressUrl: String, samples: List<LocationSample>): Observable<Try<Unit>> {
    val body = UpdateProgressBody
        .builder()
        .samples(samples.map { it.toLocationSampleDto() })
        .build()
    return api.execute(progressUrl, body)
        .map { Unit }
        .toTry()
        // For debugging purpose only.
        .doOnNext {
          when (it) {
            is Success -> Timber.d("Executed $progressUrl")
            is Failure -> Timber.e("Error executing $progressUrl", it())
          }
        }
  }
}
