package com.skedgo.tripkit.ui.data.location

import android.location.Location
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.location.LocationSample
import com.skedgo.tripkit.location.UserGeoPointRepository
import com.skedgo.tripkit.routing.toSeconds
import com.skedgo.tripkit.time.GetNow
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

open class UserGeoPointRepositoryImpl constructor(
    private val getLocationUpdates: () -> Observable<Location>,
    private val getNow: GetNow
) : UserGeoPointRepository {
    override fun getFirstCurrentGeoPoint(): Observable<GeoPoint> =
        getLocationUpdates()
            .firstOrError().toObservable()
            .map { GeoPoint(it.latitude, it.longitude) }

    override fun getCurrentGeoPoint(): Single<GeoPoint> =
        getLocationUpdates()
            .map { GeoPoint(it.latitude, it.longitude) }
            .firstOrError()

    override fun getPeriodicLocationSample(intervalSecs: Long): Observable<LocationSample> =
        Observable.interval(intervalSecs, TimeUnit.SECONDS, Schedulers.io())
            .flatMap { getLocationUpdates().firstOrError().toObservable() }
            .map {
                LocationSample(
                    getNow.execute().toSeconds(),
                    it.latitude,
                    it.longitude,
                    it.bearing.toInt(),
                    it.speed.toInt()
                )
            }
}
