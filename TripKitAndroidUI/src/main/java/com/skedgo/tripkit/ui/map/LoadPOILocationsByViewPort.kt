package com.skedgo.tripkit.ui.map

import com.skedgo.tripkit.ui.map.home.ViewPort
import io.reactivex.Observable

interface LoadPOILocationsByViewPort {
  fun execute(viewPort: ViewPort): Observable<List<IMapPoiLocation>>
}