package com.skedgo.tripkit.ui.geocoding

import com.skedgo.tripkit.ui.data.places.LatLng
import com.skedgo.tripkit.ui.data.places.LatLngBounds
import com.skedgo.tripkit.ui.data.places.Place
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import io.reactivex.Observable

class GoogleGeocoderLive(private val placeSearchRepository: PlaceSearchRepository) {
  private val halfSpan = 0.2

  fun query(
      locationName: String,
      maxResult: Int,
      swLat: Double,
      swLon: Double,
      neLat: Double,
      neLon: Double): Observable<Place.WithoutLocation> {
    val lowerLeftLat = swLat - halfSpan
    val upperRightLat = neLat + halfSpan
    val lowerLeftLon = swLon - halfSpan
    val upperRightLon = neLon + halfSpan
    return placeSearchRepository.searchForPlaces(locationName, LatLngBounds(LatLng(lowerLeftLat, lowerLeftLon), LatLng(upperRightLat, upperRightLon)))
        .take(maxResult.toLong())
        .map { Place.WithoutLocation(it) }
  }
}