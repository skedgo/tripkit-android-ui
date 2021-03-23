package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.common.util.SphericalUtil
import com.skedgo.tripkit.data.database.stops.toModeInfo
import com.skedgo.tripkit.data.locations.LocationsApi
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class SharedNearbyTripPreviewItemViewModel @Inject constructor(private val regionService: RegionService,
                                                               private val locationsApi: LocationsApi) : TripPreviewPagerItemViewModel() {
    var locationDetails = BehaviorRelay.create<NearbyLocation>()
    var locationList = BehaviorRelay.create<List<NearbyLocation>>()
    var bookingForm = BehaviorRelay.create<BookingForm>()

    var loadedSegment: TripSegment? = null

    val enableButton = ObservableBoolean(true)
    val showButton = ObservableBoolean(false)
    val buttonText = ObservableField<String>()
    val actionChosen = PublishRelay.create<String>()
    var action = ""

    override fun setSegment(context: Context, segment: TripSegment) {
        super.setSegment(context, segment)
        if (segment != loadedSegment) {
            loadedSegment = segment

            val details = NearbyLocation(lat = segment.singleLocation.lat,
                    lng = segment.singleLocation.lon,
                    title = segment.operator,
                    address = segment.singleLocation.address,
                    website = null,
                    modeInfo = segment.modeInfo)
            locationDetails.accept(details)
            regionService.getRegionByLocationAsync(segment.singleLocation)
                    .subscribe({ region ->
                        val baseUrl = region.urLs!![0]
                        val url = baseUrl.toHttpUrlOrNull()!!
                                .newBuilder()
                                .addPathSegment("locations.json")
                                .build()
                        val mode = when {
                            (segment.modeInfo?.id?.startsWith("stationary_parking")!!) -> "stationary_parking-offstreet"
                            (segment.transportModeId?.indexOf('_') != segment.transportModeId?.lastIndexOf('_')) -> segment.transportModeId?.substringBeforeLast('_')
                            else -> segment.transportModeId
                        }

                        locationsApi.fetchLocationsAsync(url.toString(),
                                segment.singleLocation.lat,
                                segment.singleLocation.lon,
                                1000, // Limit
                                1124, // Radius
                                listOf(mode))
                                .subscribe({
                                    val newList = mutableListOf<NearbyLocation>()
                                    it.groups.forEach {
                                        it.bikePods?.forEach {
                                            newList.add(NearbyLocation(lat = it.lat,
                                                    lng = it.lng,
                                                    title = it.bikePod.operator.name,
                                                    address = it.address,
                                                    website = it.bikePod.operator.website,
                                                    modeInfo = it.modeInfo?.toModeInfo()))
                                        }
                                        it.freeFloating?.forEach {
                                            newList.add(NearbyLocation(lat = it.lat,
                                                    lng = it.lng,
                                                    title = it.vehicle.operator.name,
                                                    address = it.address,
                                                    website = it.vehicle.operator.website,
                                                    modeInfo = it.modeInfo?.toModeInfo()))

                                        }
                                        it.carRentals?.forEach {
                                            newList.add(NearbyLocation(lat = it.lat(),
                                                    lng = it.lng(),
                                                    title = it.name(),
                                                    address = it.address(),
                                                    website = null,
                                                    modeInfo = it.modeInfo()))

                                        }
                                        it.carPods?.forEach {
                                            newList.add(NearbyLocation(lat = it.lat,
                                                    lng = it.lng,
                                                    title = it.name,
                                                    address = it.address,
                                                    website = it.carPod.operator.website,
                                                    modeInfo = it.modeInfo))

                                        }
                                        it.carParks?.forEach {
                                            newList.add(NearbyLocation(lat = it.lat(),
                                                    lng = it.lng(),
                                                    title = it.name(),
                                                    address = it.address(),
                                                    website = it.carPark().operator().website(),
                                                    modeInfo = it.modeInfo()))

                                        }
                                    }
                                    val compareLat = segment.singleLocation.lat
                                    val compareLng = segment.singleLocation.lon

                                    val comparator = compareBy<NearbyLocation> {
                                        SphericalUtil.computeDistanceBetween(compareLat, compareLng, it.lat, it.lng)
                                    }
                                    newList.sortWith(comparator)
                                    locationList.accept(newList)
                                }, { loadedSegment = null }).autoClear()
                    }, { loadedSegment = null }).autoClear()

        }
    }

    fun withAction(isAppInstalled: Boolean) {
        loadedSegment!!.booking?.externalActions?.forEach {
            when {
                isAppInstalled -> {
                    action = it
                    var label = loadedSegment!!.booking.title
                    if (label.isNullOrEmpty()) {
                        label = "Open App"
                    }
                    buttonText.set(label)
                }
                !isAppInstalled -> {
                    action = "getApp"
                    buttonText.set("Get App")
                }
            }
        }
        showButton.set(loadedSegment?.sharedVehicle != null && action.isNotEmpty())
    }
}