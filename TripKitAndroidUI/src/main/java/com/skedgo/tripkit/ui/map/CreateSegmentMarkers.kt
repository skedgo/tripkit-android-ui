package com.skedgo.tripkit.ui.map
import com.google.android.gms.maps.model.MarkerOptions
import io.reactivex.Observable
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.routing.Visibilities
import javax.inject.Inject

open class CreateSegmentMarkers @Inject constructor(
    private val segmentMarkerMaker: SegmentMarkerMaker
) {
  open fun execute(segments: List<TripSegment>): Observable<List<Pair<TripSegment, MarkerOptions>>>
      = Observable.fromIterable(segments)
      .filter { it.isVisibleInContext(Visibilities.VISIBILITY_ON_MAP) }
      .flatMap {
        val markerOptions: MarkerOptions? = segmentMarkerMaker.make(it)
        when (markerOptions) {
          null -> Observable.empty()
          else -> Observable.just(Pair(it, markerOptions))
        }
      }
      .toList().toObservable()
}
