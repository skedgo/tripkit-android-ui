package com.skedgo.tripkit.ui.trippreview.nearby

import android.content.Context
import android.webkit.URLUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.booking.BookingForm
import com.skedgo.tripkit.common.model.location.Location.Companion.ZERO_LAT
import com.skedgo.tripkit.common.model.location.Location.Companion.ZERO_LON
import com.skedgo.tripkit.common.util.SphericalUtil
import com.skedgo.tripkit.data.database.stops.toModeInfo
import com.skedgo.tripkit.data.locations.LocationsApi
import com.skedgo.tripkit.data.locations.LocationsFetchCoordinator
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.ui.core.module.NearbyLocationsFetch
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
import com.skedgo.tripkit.ui.utils.isAppInstalledById
import me.tatarka.bindingcollectionadapter2.ItemBinding
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

class SharedNearbyTripPreviewItemViewModel @Inject constructor(
    private val regionService: RegionService,
    private val locationsApi: LocationsApi,
    @NearbyLocationsFetch private val fetchCoordinator: LocationsFetchCoordinator
) : TripPreviewPagerItemViewModel() {

    val externalActions = ObservableArrayList<ExternalActionViewModel>()
    val externalActionsBinding = ItemBinding.of<ExternalActionViewModel>(
        BR.viewModel,
        R.layout.trip_preview_external_action_pager_list_item
    )
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

            val details = NearbyLocation(
                lat = segment.singleLocation?.lat ?: ZERO_LAT,
                lng = segment.singleLocation?.lon ?: ZERO_LON,
                title = segment.operator,
                address = segment.singleLocation?.address,
                website = null,
                modeInfo = segment.modeInfo
            )
            locationDetails.accept(details)
            regionService.getRegionByLocationAsync(segment.singleLocation)
                .subscribe({ region ->
                    val baseUrl = region.getURLs()!![0]
                    val url = baseUrl.toHttpUrlOrNull()!!
                        .newBuilder()
                        .addPathSegment("locations.json")
                        .build()
                    val mode = when {
                        (segment.modeInfo?.id?.startsWith("stationary_parking")!!) -> "stationary_parking-offstreet"
                        (segment.transportModeId?.indexOf('_') != segment.transportModeId?.lastIndexOf(
                            '_'
                        )) -> segment.transportModeId?.substringBeforeLast('_')
                        else -> segment.transportModeId
                    }

                    val lat = segment.singleLocation?.lat ?: ZERO_LAT
                    val lng = segment.singleLocation?.lon ?: ZERO_LON

                    // ViewPager2 keeps up to three preview pages alive (offscreenPageLimit 1-2)
                    // and this ViewModel is shared across them via requireParentFragment(), so
                    // the single `loadedSegment` guard above cannot stop the same segment being
                    // requested again while its previous request is still in flight. Share the
                    // call instead of issuing a duplicate (#25936).
                    fetchCoordinator.shareInFlight(
                        nearbyRequestKey(url.toString(), lat, lng, mode)
                    ) {
                        locationsApi.fetchLocationsAsync(
                            url.toString(),
                            lat,
                            lng,
                            NEARBY_LIMIT,
                            NEARBY_RADIUS_METRES,
                            listOf(mode)
                        ).map { response -> response.groups }
                    }
                        .subscribe({ groups ->
                            val newList = mutableListOf<NearbyLocation>()
                            groups.forEach {
                                it.bikePods?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat,
                                            lng = it.lng,
                                            title = it.bikePod.operator.name,
                                            address = it.address,
                                            website = it.bikePod.operator.website,
                                            modeInfo = it.modeInfo?.toModeInfo()
                                        )
                                    )
                                }
                                it.freeFloating?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat,
                                            lng = it.lng,
                                            title = it.vehicle.operator.name,
                                            address = it.address,
                                            website = it.vehicle.operator.website,
                                            modeInfo = it.modeInfo?.toModeInfo()
                                        )
                                    )

                                }
                                it.carRentals?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat(),
                                            lng = it.lng(),
                                            title = it.name(),
                                            address = it.address(),
                                            website = null,
                                            modeInfo = it.modeInfo()
                                        )
                                    )

                                }
                                it.carPods?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat,
                                            lng = it.lng,
                                            title = it.name,
                                            address = it.address,
                                            website = it.carPod.operator.website,
                                            modeInfo = it.modeInfo
                                        )
                                    )

                                }
                                it.carParks?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat(),
                                            lng = it.lng(),
                                            title = it.name(),
                                            address = it.address(),
                                            website = it.carPark().operator().website(),
                                            modeInfo = it.modeInfo()
                                        )
                                    )

                                }
                                it.facilities?.forEach {
                                    newList.add(
                                        NearbyLocation(
                                            lat = it.lat,
                                            lng = it.lng,
                                            title = it.name,
                                            address = it.address,
                                            website = null,
                                            modeInfo = null
                                        )
                                    )
                                }
                            }
                            val compareLat = segment.singleLocation?.lat ?: ZERO_LAT
                            val compareLng = segment.singleLocation?.lon ?: ZERO_LON

                            val comparator = compareBy<NearbyLocation> {
                                SphericalUtil.computeDistanceBetween(
                                    compareLat,
                                    compareLng,
                                    it.lat,
                                    it.lng
                                )
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
            /*
            if (!loadedSegment?.booking?.accessibilityLabel.isNullOrEmpty()) {
                label = loadedSegment?.booking?.accessibilityLabel
            }
            */
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
        if (!URLUtil.isValidUrl(externalAction?.data) && externalAction?.data?.contains("://") == true) {
            externalAction.fallbackUrl = generateFallbackUrl()
        }

        /*
        //Since we do not hard code checking what kind of app the external action is for, there are cases,
        //like goget, that you have to check if its app installed but in the external action url there
        //is no package name included that we can use to check if the app is installed. So we'll
        //look on sharedVehicle field if package name is available
        if (loadedSegment?.booking?.externalActions?.none { it.getPackageNameFromStoreUrl() != null } == true) {
            checkAppIdOnSharedVehicleAndIfInstalled(context)?.let {
                externalAction?.data = it
                externalAction?.appInstalled = true
            }
        }
        */

        vm.title.set(
            when {
                index == 0 -> {
                    var label = loadedSegment?.booking?.title
                    /*
                    if (!loadedSegment?.booking?.accessibilityLabel.isNullOrEmpty()) {
                        label = loadedSegment?.booking?.accessibilityLabel
                    }
                    */
                    label
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

    private fun checkAppIdOnSharedVehicleAndIfInstalled(context: Context): String? {
        loadedSegment?.sharedVehicle?.operator()?.appInfo?.appURLAndroid?.getPackageNameFromStoreUrl()
            ?.let {
                if (it.isAppInstalledById(context.packageManager)) {
                    return it
                }
            }

        return null
    }

    companion object {
        private const val NEARBY_LIMIT = 1000
        private const val NEARBY_RADIUS_METRES = 1124

        /**
         * Identity of one Nearby radius query, used as the in-flight de-duplication key.
         *
         * Every input that can change the response is included: the region base URL, the
         * segment's coordinates, the radius/limit, and the mode filter.
         *
         * The coordinates are deliberately used at full precision. They are copied straight
         * from `TripSegment.singleLocation` rather than derived from a map gesture, so the same
         * segment always produces an identical key while two genuinely different segments never
         * collide. Rounding would risk serving one segment's nearby list for another.
         *
         * Note this key is only used for in-flight sharing, never for TTL suppression. Nearby
         * results are free-floating vehicle and parking availability, which is time-sensitive,
         * and this ViewModel has no persistence layer — suppressing a call without caching the
         * payload would leave the previously shown segment's list on screen.
         */
        internal fun nearbyRequestKey(
            url: String,
            lat: Double,
            lng: Double,
            mode: String?
        ): String = "$url|$lat|$lng|$NEARBY_RADIUS_METRES|$NEARBY_LIMIT|${mode.orEmpty()}"
    }
}