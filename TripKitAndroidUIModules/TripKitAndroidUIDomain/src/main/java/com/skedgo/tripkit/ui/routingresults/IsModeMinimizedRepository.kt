package com.skedgo.tripkit.ui.routingresults

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface IsModeMinimizedRepository {
  fun isModeMinimized(modeId: String): Single<Boolean>
  fun setModeMinimized(modeId: String, isMinimized: Boolean): Completable
  fun onChanged(): Observable<String>
}