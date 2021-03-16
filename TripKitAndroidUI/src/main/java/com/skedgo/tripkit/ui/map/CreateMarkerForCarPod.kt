package com.skedgo.tripkit.ui.map
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.TripGoStyleKit
import com.skedgo.tripkit.ui.core.fetchAsyncWithSize
import com.skedgo.tripkit.ui.utils.StopMarkerUtils
import com.squareup.picasso.Picasso
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import com.skedgo.tripkit.locations.CarPod
import io.reactivex.android.schedulers.AndroidSchedulers

object CreateMarkerForCarPod {

  fun execute(resources: Resources, picasso: Picasso, carPod: CarPod): Single<MarkerOptions> {
    val carPodEntity = carPod
    return StopMarkerUtils.getMapIconUrlForModeInfo(resources, carPodEntity.remoteIcon)
        .let { Observable.fromIterable(it.orEmpty()) }
        .filter { it != null }
        .concatMap {
          picasso.fetchAsyncWithSize(it, R.dimen.map_icon_size)
              .map { BitmapDescriptorFactory.fromBitmap(it) }
              .toObservable()
              .observeOn(Schedulers.io())
                  .subscribeOn(AndroidSchedulers.mainThread())
              .onErrorResumeNext(Observable.empty())
        }
        .switchIfEmpty(Observable.fromCallable {
          val iconSize = resources.getDimensionPixelSize(R.dimen.map_icon_size)
          val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
          val canvas = Canvas(bitmap)
          val availableVehicles = carPodEntity.realTimeInfo?.availableVehicles
          val totalSpaces = carPodEntity.realTimeInfo?.totalSpaces
          val fraction = if (availableVehicles != null && totalSpaces != null) {
            availableVehicles.toFloat() / totalSpaces
          } else {
            1f
          }
          TripGoStyleKit.drawCarShareMap(canvas, fraction, 0.toFloat(), iconSize.toFloat())
          BitmapDescriptorFactory.fromBitmap(bitmap)
        })
        .firstOrError()
        .map {
          MarkerOptions()
              .title(carPodEntity.name)
              .position(LatLng(carPodEntity.lat, carPodEntity.lng))
              .draggable(false)
              .snippet(carPodEntity.address)
              .icon(it)
        }
  }
}