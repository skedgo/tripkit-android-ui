package com.skedgo.tripkit.ui.data.places

import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers.io
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider


class PlaceSearchRepositoryImpl
@Inject constructor(private val geoDataClient: Provider<PlacesClient>) : PlaceSearchRepository {
    override fun searchForPlaces(
        query: String,
        latLngBounds: LatLngBounds
    ): Observable<GooglePlacePrediction> {
        // TODO This should be a Flowable
        val bounds = RectangularBounds.newInstance(
            com.google.android.gms.maps.model.LatLng(
                latLngBounds.southwest.latitude,
                latLngBounds.southwest.longitude
            ),
            com.google.android.gms.maps.model.LatLng(
                latLngBounds.northeast.latitude,
                latLngBounds.northeast.longitude
            )
        )

        return Observable
            .create<List<AutocompletePrediction>> {
                geoDataClient.get()
                    .findAutocompletePredictions(
                        FindAutocompletePredictionsRequest.builder()
                            .setLocationBias(bounds)
                            .setQuery(query)
                            .build()
                    )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val result = task.result
                            val list = result?.autocompletePredictions.orEmpty()
                            it.onNext(list)
                            it.onComplete()
                        } else {
                            Timber.e(task.exception)
                            it.tryOnError(GooglePlacesException(task.toString(), task.exception!!))
                        }

                    }
            }
            .flatMap { Observable.fromIterable(it) }
            .flatMap {
                getPlaceDetails(it.placeId).map { details ->
                    GooglePlacePrediction(
                        primaryText = it.getPrimaryText(null).toString(),
                        secondaryText = it.getSecondaryText(null).toString(),
                        fullText = it.getFullText(null).toString(),
                        placeId = it.placeId,
                        latitude = details.lat,
                        longitude = details.lng
                    )
                }
            }
            .subscribeOn(io())
    }

    override fun getPlaceDetails(placeId: String): Observable<GooglePlace> {
        return Observable
            .create<com.google.android.libraries.places.api.model.Place> {
                val placeFields = listOf(
                    com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
                    com.google.android.libraries.places.api.model.Place.Field.WEBSITE_URI,
                    com.google.android.libraries.places.api.model.Place.Field.LAT_LNG
                )

                geoDataClient.get()
                    .fetchPlace(FetchPlaceRequest.builder(placeId, placeFields).build())
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val result = task.result
                            result?.let { response ->
                                it.onNext(response.place)
                            }
                            it.onComplete()
                        } else {
                            Timber.e(task.exception)
                            it.tryOnError(task.exception!!)
                        }
                    }
            }
            .map {
                GooglePlace(
                    name = it.name.toString(),
                    lat = it.latLng!!.latitude,
                    lng = it.latLng!!.longitude,
                    address = it.address.toString(),
                    placeId = it.id!!,
                    website = it.websiteUri,
                    attribution = it.attributions?.toString()
                )
            }
    }
}