package com.skedgo.tripkit.ui.data.cameraposition
import android.content.SharedPreferences
import com.skedgo.tripkit.camera.LastCameraPositionRepository
import com.skedgo.tripkit.camera.MapCameraPosition
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

internal const val KEY_MAP_LAST_BEARING = "lastBearing"
internal const val KEY_MAP_LAST_TARGET_LAT = "lastTargetLat"
internal const val KEY_MAP_LAST_TARGET_LON = "lastTargetLon"
internal const val KEY_MAP_LAST_TILT = "lastTilt"
internal const val KEY_MAP_LAST_ZOOM = "lastZoom"
internal const val LAT_AUS = -15.229080156614616f
internal const val LON_AUS = 135.15893429517746f
internal const val LAT_US = 51.353938388852946f
internal const val LON_US = -97.3880223557353f

open class LastCameraPositionRepositoryImpl(
    private val preferences: SharedPreferences,
    private val locale: Locale
) : LastCameraPositionRepository {
  override fun putMapCameraPosition(mapCameraPosition: MapCameraPosition?): Observable<MapCameraPosition?> {
    // FIXME: Remove nullity of `mapCameraPosition`.
    if (mapCameraPosition == null) {
      return Observable.empty<MapCameraPosition>()
    }

    return Observable.fromCallable {
      preferences.edit()
          .putFloat(KEY_MAP_LAST_TARGET_LAT, mapCameraPosition.lat.toFloat())
          .putFloat(KEY_MAP_LAST_TARGET_LON, mapCameraPosition.lng.toFloat())
          .putFloat(KEY_MAP_LAST_ZOOM, mapCameraPosition.zoom)
          .putFloat(KEY_MAP_LAST_TILT, mapCameraPosition.tilt)
          .putFloat(KEY_MAP_LAST_BEARING, mapCameraPosition.bearing)
          .apply()
      mapCameraPosition
    }.subscribeOn(Schedulers.io())
  }

  override fun getMapCameraPosition(): Observable<MapCameraPosition>
      = Observable
      .fromCallable {
        val mapCameraPosition: MapCameraPosition
        if (!preferences.contains(KEY_MAP_LAST_TARGET_LAT)) {
          // TODO: An improvement would be to check device's timezone against region timezones.
          // If any region is matched, picks its first city as the target for camera.
          // Otherwise, fallback to the following Locale-based algorithm.
          mapCameraPosition = getMapCameraPositionByLocaleSync()
        } else {
          val targetLat = preferences.getFloat(KEY_MAP_LAST_TARGET_LAT, LAT_AUS)
          val targetLon = preferences.getFloat(KEY_MAP_LAST_TARGET_LON, LON_AUS)
          val zoom = preferences.getFloat(KEY_MAP_LAST_ZOOM, 3f)
          val tilt = preferences.getFloat(KEY_MAP_LAST_TILT, 0f)
          val bearing = preferences.getFloat(KEY_MAP_LAST_BEARING, 0f)
          mapCameraPosition = MapCameraPosition(
              targetLat.toDouble(), targetLon.toDouble(),
              zoom,
              tilt,
              bearing
          )
        }

        mapCameraPosition
      }
      .subscribeOn(Schedulers.io())

  override fun getMapCameraPositionByLocale(): Observable<MapCameraPosition>
      = Observable.fromCallable { getMapCameraPositionByLocaleSync() }
      .subscribeOn(Schedulers.computation())

  override fun hasMapCameraPosition(): Boolean
      = preferences.contains(KEY_MAP_LAST_TARGET_LAT)

  private fun getMapCameraPositionByLocaleSync(): MapCameraPosition = when (locale) {
    Locale.US ->
      // USA.
      MapCameraPosition(
          LAT_US.toDouble(), LON_US.toDouble(),
          3f, 0f, 0f
      )
    else ->
      // Australia.
      MapCameraPosition(
          LAT_AUS.toDouble(), LON_AUS.toDouble(),
          3f, 0f, 0f
      )
  }
}
