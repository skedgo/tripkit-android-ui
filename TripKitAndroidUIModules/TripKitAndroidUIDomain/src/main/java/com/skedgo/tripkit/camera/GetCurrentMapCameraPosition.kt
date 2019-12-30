package com.skedgo.tripkit.camera

import com.skedgo.tripkit.location.UserGeoPointRepository
import io.reactivex.Observable
import javax.inject.Inject

open class GetCurrentMapCameraPosition @Inject internal constructor(
    private val userGeoPointRepository: UserGeoPointRepository
) {
  open fun execute(): Observable<MapCameraPosition> = userGeoPointRepository.getFirstCurrentGeoPoint()
      .map {
        MapCameraPosition(
            lat = it.latitude,
            lng = it.longitude,
            zoom = 15f,
            tilt = 0f,
            bearing = 0f
        )
      }
}