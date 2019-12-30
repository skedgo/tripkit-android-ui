package com.skedgo.tripkit.ui.core.modeprefs
import android.content.SharedPreferences
import com.skedgo.tripkit.data.util.onChanged
import com.skedgo.tripkit.ui.routingresults.IsModeMinimizedRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal class IsModeMinimizedRepositoryImpl constructor(
    private val prefs: SharedPreferences
) : IsModeMinimizedRepository {
  override fun isModeMinimized(modeId: String): Single<Boolean> =
      Single.fromCallable {
        prefs.getBoolean(modeId, false)
      }

  override fun setModeMinimized(modeId: String, isMinimized: Boolean): Completable =
      Completable.fromCallable {
        prefs.edit()
            .putBoolean(modeId, isMinimized)
            .apply()
      }

  override fun onChanged(): Observable<String> = prefs.onChanged()
}

