package com.skedgo.tripkit.ui.routing
import com.skedgo.tripkit.ui.map.ServiceStop
import io.reactivex.Observable
import com.skedgo.tripkit.location.GeoPoint
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.startDateTime
import javax.inject.Inject

open class GetStopsByTravelType @Inject internal constructor() {
  open fun execute(segment: TripSegment, travelled: Boolean): Observable<ServiceStop> =
      Observable
          .fromCallable {
            (segment.shapes ?: emptyList())
                .filter { it.isTravelled == travelled }
                .map { it.stops ?: emptyList() }
                .flatten()
                .map {
                  ServiceStop(
                      code = it.code,
                      position = GeoPoint(it.lat, it.lon),
                      name = it.name,
                      platform = it.shortName,
                      departureDateTime = it.relativeArrival?.let {
                        segment.startDateTime.plusSeconds(it.toInt())
                      },
                      arrivalDateTime = it.relativeDeparture?.let {
                        segment.startDateTime.plusSeconds(it.toInt())
                      },
                      isWheelchairAccessible = it.wheelchairAccessible
                  )
                }
          }
          .flatMapIterable { it }
}
