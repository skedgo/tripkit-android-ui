package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import android.webkit.URLUtil
import androidx.databinding.ObservableArrayList
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
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.trippreview.Action
import com.skedgo.tripkit.ui.trippreview.TripPreviewPagerItemViewModel
import com.skedgo.tripkit.ui.trippreview.external.ExternalActionViewModel
import com.skedgo.tripkit.ui.trippreview.handleExternalAction
import com.skedgo.tripkit.ui.utils.checkUrl
import com.skedgo.tripkit.ui.utils.getPackageNameFromStoreUrl
import com.skedgo.tripkit.ui.utils.isAppInstalled
import me.tatarka.bindingcollectionadapter2.ItemBinding
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class SharedNearbyTripPreviewItemViewModel @Inject constructor(private val regionService: RegionService,
                                                               private val locationsApi: LocationsApi) : TripPreviewPagerItemViewModel() {

    val externalActions = ObservableArrayList<ExternalActionViewModel>()
    val externalActionsBinding = ItemBinding.of<ExternalActionViewModel>(BR.viewModel, R.layout.trip_preview_external_action_pager_list_item)
            .bindExtra(BR.parentViewModel, this)

    var locationDetails = BehaviorRelay.create<NearbyLocation>()
    var locationList = BehaviorRelay.create<List<NearbyLocation>>()
    var bookingForm = BehaviorRelay.create<BookingForm>()

    var loadedSegment: TripSegment? = null

    val hasExternalActions = ObservableBoolean(false)
    val showActions = ObservableBoolean(false)
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
        var deepLink: String? = null
        if (loadedSegment?.booking?.externalActions != null) {
            loadedSegment?.booking?.externalActions?.forEach {
                deepLink = it
            }
        } else if (loadedSegment?.sharedVehicle?.operator()?.appInfo != null) {
            deepLink = loadedSegment?.sharedVehicle?.operator()?.appInfo!!.deepLink
        }

        if (isAppInstalled && !deepLink.isNullOrEmpty()) {
            action = deepLink!!
            var label = loadedSegment!!.booking?.title
            if (label.isNullOrEmpty()) {
                label = "Open App"
            }
            buttonText.set(label)
        } else {
            action = "getApp"
            buttonText.set("Get App")
        }
        showActions.set(loadedSegment?.sharedVehicle != null && action.isNotEmpty())
    }

    fun withAction(context: Context) {
        externalActions.clear()
        loadedSegment?.booking?.externalActions?.forEachIndexed { index, action ->
            addExternalActionItem(context, action, index)
        } ?: kotlin.run {
            //For handling action when there's no external actions
            handleNonExternalAction(context)
        }

    }

    private fun addExternalActionItem(context: Context, action: String, index: Int) {
        hasExternalActions.set(true)
        val vm = ExternalActionViewModel()
        val externalAction = context.handleExternalAction(action)
        vm.action = action
        vm.externalAction = externalAction
        if (index == 0 && (loadedSegment?.booking?.externalActions?.size ?: 0) > 1) {
            externalAction?.drawable = R.drawable.ic_open
        }
        if(!URLUtil.isValidUrl(externalAction?.data) && externalAction?.data?.contains("://") == true){
            externalAction.fallbackUrl = generateFallbackUrl()
        }
        vm.title.set(
                when {
                    index == 0 -> {
                        loadedSegment?.booking?.title
                    }
                    URLUtil.isNetworkUrl(externalAction?.data) -> {
                        context.getString(R.string.show_website)
                    }
                    else -> {
                        context.getString(R.string.open_app)
                    }
                }
        )
        externalActions.add(vm)
        showActions.set(false)
    }

    private fun handleNonExternalAction(context: Context) {
        hasExternalActions.set(false)
        loadedSegment?.sharedVehicle?.operator()?.appInfo?.appURLAndroid?.let {
            buttonText.set(
                    if (it.isAppInstalled(context.packageManager)) {
                        context.getString(R.string.open_app)
                    } else {
                        context.getString(R.string.get_app)
                    }
            )
            action = if (it.isAppInstalled(context.packageManager)) {
                "openApp"
            } else {
                "getApp"
            }

            showActions.set(true)
        } ?: kotlin.run { showActions.set(false) }
    }

    //in case url is a deep link, app is not installed and no way to get app package name from externalActions
    private fun generateFallbackUrl(): String? {
        return loadedSegment?.booking?.externalActions?.singleOrNull { it.getPackageNameFromStoreUrl() != null }
                ?: loadedSegment?.sharedVehicle?.operator()?.appInfo?.appURLAndroid
                ?: loadedSegment?.booking?.externalActions?.singleOrNull { URLUtil.isNetworkUrl(it) }
    }
}