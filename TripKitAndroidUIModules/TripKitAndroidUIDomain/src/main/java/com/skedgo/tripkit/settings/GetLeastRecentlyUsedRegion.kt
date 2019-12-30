package com.skedgo.tripkit.settings
import com.skedgo.tripkit.common.model.Region
import com.skedgo.tripkit.camera.LastCameraPositionRepository
import com.skedgo.tripkit.data.regions.RegionService
import io.reactivex.Observable
import javax.inject.Inject

open class GetLeastRecentlyUsedRegion @Inject internal constructor(
        private val lastCameraPositionStore: LastCameraPositionRepository,
        private val regionService: RegionService
) {
  open fun execute(): Observable<Region> =
      lastCameraPositionStore.getMapCameraPosition()
          .flatMap { (lat, lng) -> regionService.getRegionByLocationAsync(-34.0, 151.0) }
}