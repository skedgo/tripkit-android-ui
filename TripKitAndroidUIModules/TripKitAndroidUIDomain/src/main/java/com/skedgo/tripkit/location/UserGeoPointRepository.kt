package com.skedgo.tripkit.location
import io.reactivex.Observable
import io.reactivex.Single
import com.skedgo.tripkit.location.GeoPoint

/**
 * Retrieves [GeoPoint] which indicates user location.
 */
interface UserGeoPointRepository {
  fun getFirstCurrentGeoPoint(): Observable<GeoPoint>
  fun getCurrentGeoPoint(): Single<GeoPoint>
  fun getPeriodicLocationSample(intervalSecs: Long): Observable<LocationSample>
}
