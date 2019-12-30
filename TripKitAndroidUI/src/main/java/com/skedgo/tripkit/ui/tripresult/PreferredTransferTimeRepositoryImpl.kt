package com.skedgo.tripkit.ui.tripresult
import android.content.SharedPreferences
import android.content.res.Resources
import com.skedgo.tripkit.data.util.onChanged
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.routing.PreferredTransferTimeRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.joda.time.Minutes

/** TODO: Refactor to move this class to TripGoData. */
internal class PreferredTransferTimeRepositoryImpl constructor(
    private val resources: Resources,
    private val prefs: SharedPreferences
) : PreferredTransferTimeRepository {
  override fun putPreferredTransferTime(preferredTransferTime: Minutes): Completable {
    // FIXME: Should use this Repository to change preferred transfer time.
    TODO("Not implemented yet.")
  }

  override fun getPreferredTransferTime(
      defaultIfEmpty: () -> Minutes
  ): Observable<Minutes>
      = Observable
      .fromCallable {
        prefs.getString(resources.getString(R.string.pref_transfer_time), null)
      }
      .filter { !it.isNullOrEmpty() }
      .map { it.toInt() }
      .map { Minutes.minutes(it) }
      .switchIfEmpty(Observable.fromCallable { defaultIfEmpty() })

  override fun whenPreferredTransferTimeChanges(): Observable<Minutes> =
      prefs
          .onChanged()
          .filter { it == resources.getString(R.string.pref_transfer_time) }
          .switchMap { getPreferredTransferTime() }
          .subscribeOn(Schedulers.io())
}