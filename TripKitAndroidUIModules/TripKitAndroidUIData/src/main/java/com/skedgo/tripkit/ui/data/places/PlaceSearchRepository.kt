package com.skedgo.tripkit.ui.data.places

import io.reactivex.Observable

interface PlaceSearchRepository {
  fun searchForPlaces(query: String, latLngBounds: LatLngBounds): Observable<GooglePlacePrediction>
  fun getPlaceDetails(placeId: String): Observable<GooglePlace>
}
