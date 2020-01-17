package com.skedgo.tripkit.ui.data.places

import com.google.android.gms.maps.model.LatLngBounds.builder
import com.google.android.libraries.places.compat.AutocompletePrediction
import com.google.android.libraries.places.compat.GeoDataClient
import com.google.android.libraries.places.compat.PlaceBufferResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers.io
import javax.inject.Inject
import javax.inject.Provider

class PlaceSearchRepositoryImpl
@Inject constructor(private val geoDataClient: Provider<GeoDataClient>) : PlaceSearchRepository {
  override fun searchForPlaces(query: String, latLngBounds: LatLngBounds): Observable<GooglePlacePrediction> {
      // TODO This should be a Flowable
    return Observable
        .create<List<AutocompletePrediction>> {
            geoDataClient.get()
                    .getAutocompletePredictions(query, builder()
                            .include(com.google.android.gms.maps.model.LatLng(latLngBounds.southwest.latitude, latLngBounds.southwest.longitude))
                            .include(com.google.android.gms.maps.model.LatLng(latLngBounds.northeast.latitude, latLngBounds.northeast.longitude))
                            .build()
                            , null)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val result = task.result
                            val list = result?.toList().orEmpty()
                            it.onNext(list)
                            result?.release()
                            it.onComplete()
                        } else {
                            it.onError(GooglePlacesException(task.toString(), task.exception!!))
                        }
                    }
        }
            .flatMap { Observable.fromIterable(it) }
        .map {
          GooglePlacePrediction(
              it.getPrimaryText(null).toString(),
              it.getSecondaryText(null).toString(),
              it.getFullText(null).toString(),
              it.placeId!!)
        }
        .subscribeOn(io())
  }

  override fun getPlaceDetails(placeId: String): Observable<GooglePlace> {
    return Observable
        .create<com.google.android.libraries.places.compat.Place> {
            geoDataClient.get().getPlaceById(placeId)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val result: PlaceBufferResponse? = task.result
                            if (result != null) {
                                it.onNext(result.get(0))
                            }
                            result?.release()
                            it.onComplete()
                        } else {
                            it.onError(task.exception!!)
                        }
                    }
        }
            .map {
          GooglePlace(
              name = it.name.toString(),
              lat = it.latLng.latitude,
              lng = it.latLng.longitude,
              address = it.address.toString(),
              placeId = it.id,
              attribution = it.attributions?.toString())
        }
  }
}