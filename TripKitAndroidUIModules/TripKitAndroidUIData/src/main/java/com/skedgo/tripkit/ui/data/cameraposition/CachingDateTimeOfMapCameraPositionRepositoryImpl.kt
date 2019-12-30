package com.skedgo.tripkit.ui.data.cameraposition
import android.content.SharedPreferences
import com.skedgo.tripkit.camera.CachingDateTimeOfMapCameraPositionRepository
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

open class CachingDateTimeOfMapCameraPositionRepositoryImpl(
    private val preferences: SharedPreferences
) : CachingDateTimeOfMapCameraPositionRepository {
  val cachingDateTimeKey = "cachingDateTime"

  override fun putCachingDateTime(dateTime: DateTime): Observable<Unit>
      = Observable.just(dateTime)
      .map { ISODateTimeFormat.dateTime().print(it) }
      .doOnNext {
        preferences.edit()
            .putString(cachingDateTimeKey, it)
            .apply()
      }
      .map { Unit }

  override fun getCachingDateTime(): Observable<DateTime>
      = Observable.fromCallable { preferences.getString(cachingDateTimeKey, null) }
      .filter { !it.isNullOrEmpty() }
      .map { ISODateTimeFormat.dateTime().parseDateTime(it) }
      .subscribeOn(Schedulers.io())
}
