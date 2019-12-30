package com.skedgo.tripkit.ui.map

import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.ui.data.places.LatLng

fun LatLngBounds.convertToDomainLatLngBounds(): com.skedgo.tripkit.ui.data.places.LatLngBounds {
  return com.skedgo.tripkit.ui.data.places.LatLngBounds(
      LatLng(this.southwest.latitude, this.southwest.longitude),
      LatLng(this.northeast.latitude, this.northeast.longitude))
}