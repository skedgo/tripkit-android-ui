package com.skedgo.tripkit.location

import io.reactivex.Observable
import io.reactivex.Single

/**
 * Retrieves [GeoPoint] which indicates user location.
 */
interface UserGeoPointRepository {
    fun getFirstCurrentGeoPoint(): Observable<GeoPoint>
    fun getCurrentGeoPoint(): Single<GeoPoint>
    fun getPeriodicLocationSample(intervalSecs: Long): Observable<LocationSample>
}
