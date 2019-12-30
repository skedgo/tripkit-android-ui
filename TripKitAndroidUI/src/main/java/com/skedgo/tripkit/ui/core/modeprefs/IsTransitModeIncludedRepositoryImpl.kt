package com.skedgo.tripkit.ui.core.modeprefs
import android.content.SharedPreferences
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class IsTransitModeIncludedRepositoryImpl @Inject constructor(
    private val prefs: SharedPreferences
) : IsTransitModeIncludedRepository {
  override fun getAll(): Observable<IsTransitModeIncluded> {
      val tmp =Observable
          .fromCallable {
            prefs.all
                .map {
                    IsTransitModeIncluded(it.key, it.value as Boolean)
                }
          }
          .flatMapIterable { it }
          .subscribeOn(Schedulers.io())
      return tmp
  }
  override fun isTransitModeIncluded(modeId: String): Single<Boolean> =
      Single.fromCallable {
          prefs.getBoolean(modeId, true)
      }
          .subscribeOn(Schedulers.io())

  override fun setIsTransitModeIncluded(modeId: String, isIncluded: Boolean): Completable =
      Completable
          .fromAction {
            prefs.edit()
                .putBoolean(modeId, isIncluded)
                .apply()
          }
          .subscribeOn(Schedulers.io())
}
