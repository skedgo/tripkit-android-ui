package com.skedgo.tripkit.ui.routing

import android.location.Location
import com.skedgo.tripkit.common.model.Query
import io.reactivex.Observable
import io.reactivex.functions.Function
import javax.inject.Inject
import javax.inject.Provider

open class QueryLocationResolver @Inject constructor(
    private val provider: Provider<Observable<Location>>
) : Function<Query, Observable<Query>> {
    override fun apply(query: Query): Observable<Query> {
        if (query.originIsCurrentLocation() && query.destinationIsCurrentLocation().not()) {
            return provider.get()
                .firstOrError()
                .map { location ->
                    query.fromLocation = com.skedgo.tripkit.common.model.Location(
                        location.latitude,
                        location.longitude
                    )
                    query
                }.toObservable()
        } else if (query.originIsCurrentLocation().not() && query.destinationIsCurrentLocation()) {
            return provider.get()
                .firstOrError()
                .map { location ->
                    query.toLocation = com.skedgo.tripkit.common.model.Location(
                        location.latitude,
                        location.longitude
                    )
                    query
                }.toObservable()
        } else if (!query.originIsCurrentLocation() && !query.destinationIsCurrentLocation()) {
            return Observable.just(query)
        } else {
            return Observable.error(NullPointerException("Departure and arrival are missing!"))
        }
    }
}
