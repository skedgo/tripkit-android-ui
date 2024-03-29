package com.skedgo.tripkit.ui.data.cameraposition
import android.content.SharedPreferences
import android.content.res.Resources
import android.util.Log
import com.skedgo.tripkit.camera.LastCameraPositionRepository
import com.skedgo.tripkit.camera.MapCameraPosition
import com.skedgo.tripkit.ui.data.R
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
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
        private val resources: Resources,
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
          // There isn't a cached map position, so first see if this is being used by a whitelabel overriding
          // the default Locale-based algorithm.
          mapCameraPosition = getDefaultMapCameraPositionSync()
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

  private fun getDefaultMapCameraPositionSync(): MapCameraPosition {
    return if (resources.getBoolean(R.bool.trip_kit_map_override_default_latlon)) {
      getOverriddenMapCameraPosition()
    } else {
      getMapCameraPositionByLocaleSync()
    }
  }

  override fun getDefaultMapCameraPosition(): Observable<MapCameraPosition>
          = Observable.fromCallable { getDefaultMapCameraPositionSync() }
          .subscribeOn(Schedulers.computation())

  override fun hasMapCameraPosition(): Boolean
      = preferences.contains(KEY_MAP_LAST_TARGET_LAT)

  private fun getOverriddenMapCameraPosition(): MapCameraPosition {
    var lat = resources.getString(R.string.trip_kit_map_default_latitude)
    var lon  = resources.getString(R.string.trip_kit_map_default_longitude)
    var zoom = resources.getString(R.string.trip_kit_map_default_zoom)
    if (lat.isEmpty()) lat = LAT_US.toString()
    if (lon.isEmpty()) lon = LON_US.toString()
    if (zoom.isEmpty()) zoom = "3"

    return MapCameraPosition(lat.toDouble(), lon.toDouble(), zoom.toFloat(),0f, 0f)
  }

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
