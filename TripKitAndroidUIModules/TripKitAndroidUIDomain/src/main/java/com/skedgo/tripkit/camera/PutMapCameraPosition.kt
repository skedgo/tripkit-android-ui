package com.skedgo.tripkit.camera
import com.skedgo.tripkit.time.GetNow
import io.reactivex.Observable
import javax.inject.Inject

open class PutMapCameraPosition @Inject constructor(
        private val getNow: GetNow,
        private val putCachedMapCameraPosition: PutCachedMapCameraPosition
) {
  open fun execute(mapCameraPosition: MapCameraPosition): Observable<Unit> =
      putCachedMapCameraPosition.execute(CachedMapCameraPosition(getNow.execute(), mapCameraPosition))
}