package com.skedgo.tripkit.ui.map.home

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.location.GeoPoint
import io.reactivex.Observable
import javax.inject.Inject

open class GetCellIdsFromViewPort @Inject constructor(val regionService: RegionService) {

    open fun execute(viewPort: ViewPort): Observable<List<String>> {
        return regionService.getRegionByLocationAsync(
            viewPort.visibleBounds.southwest.latitude,
            viewPort.visibleBounds.southwest.longitude
        )
            .ignoreOutOfRegionsException()
            .map { region ->
                val googleMapsBounds = LatLngBounds.builder()
                    .include(
                        LatLng(
                            viewPort.visibleBounds.southwest.latitude,
                            viewPort.visibleBounds.southwest.longitude
                        )
                    )
                    .include(
                        LatLng(
                            viewPort.visibleBounds.northeast.latitude,
                            viewPort.visibleBounds.northeast.longitude
                        )
                    )
                    .build()
                StopLoaderArgs.getCellIdsByCameraZoom(
                    region,
                    GeoPoint(googleMapsBounds.center.latitude, googleMapsBounds.center.longitude),
                    viewPort.zoom,
                    googleMapsBounds
                )
            }
    }
}