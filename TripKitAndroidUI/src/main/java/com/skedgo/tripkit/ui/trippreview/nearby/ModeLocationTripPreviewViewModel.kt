package com.skedgo.tripkit.ui.trippreview.nearby

import android.webkit.URLUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.SharedVehicleType
import com.skedgo.tripkit.common.model.getSharedVehicleType
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.utils.DistanceFormatter
import com.skedgo.tripkit.ui.utils.checkUrl
import me.tatarka.bindingcollectionadapter2.ItemBinding
import timber.log.Timber
import javax.inject.Inject

class ModeLocationTripPreviewViewModel @Inject constructor(private val locationInfoService: LocationInfoService)
    : RxViewModel() {
    val infoGroups = ObservableArrayList<InfoGroupViewModel>()
    val infoGroupBinding: ItemBinding<InfoGroupViewModel> = ItemBinding.of(BR.viewModel, R.layout.trip_preview_pager_nearby_info_group_item)

    var address = ObservableField<String>("")
    var showAddress = ObservableBoolean(false)
    var website = ObservableField<String>("")
    var showWebsite = ObservableBoolean(false)

    var what3words = ObservableField<String>("")
    var showWhat3words = ObservableBoolean(false)

    fun set(segment: TripSegment) {
        infoGroups.clear()
        locationInfoService.getLocationInfoAsync(segment.singleLocation)
                .take(1)
                .subscribe({
                    val w3w = it.details()?.w3w()
                    if (!w3w.isNullOrBlank()) {
                        this.what3words.set(w3w)
                        this.showWhat3words.set(true)
                    }
                }, { Timber.e(it) })
                .autoClear()
        if (segment.sharedVehicle != null) {
            val vehicle = segment.sharedVehicle
            val vehicleVm = InfoGroupViewModel()
            vehicleVm.title.set(vehicle.vehicleType()?.title()
                    ?: getSharedVehicleType(vehicle.vehicleTypeInfo()?.formFactor ?: "")?.title()
                    ?: R.string.car)
            vehicleVm.value.set(vehicle.name())
            vehicleVm.icon.set(vehicle.vehicleType()?.iconId
                    ?: getSharedVehicleType(vehicle.vehicleTypeInfo()?.formFactor ?: "")?.iconId)
            infoGroups.add(vehicleVm)

            if (vehicle.batteryRange() != null) {
                val batteryVm = InfoGroupViewModel()
                batteryVm.title.set(R.string.battery)
                batteryVm.value.set(DistanceFormatter.format(vehicle.batteryRange()!! * 1000))
                batteryVm.icon.set(R.drawable.ic_battery)
                infoGroups.add(batteryVm)
            } else if (vehicle.batteryLevel() != null) {
                val batteryLevelVm = InfoGroupViewModel()
                batteryLevelVm.title.set(R.string.battery)
                batteryLevelVm.value.set(String.format("%d%%", vehicle.batteryLevel()))
                batteryLevelVm.icon.set(
                        when {
                            vehicle.batteryLevel()!! < 12 -> R.drawable.ic_battery_0
                            vehicle.batteryLevel()!! < 37 -> R.drawable.ic_battery_25
                            vehicle.batteryLevel()!! < 62 -> R.drawable.ic_battery_50
                            vehicle.batteryLevel()!! < 87 -> R.drawable.ic_battery_75
                            else -> R.drawable.ic_battery_100
                        }
                )
                infoGroups.add(batteryLevelVm)
            }

            (segment.sharedVehicle.operator()?.website ?: segment.sharedVehicle.deepLink())?.let {
                website.set(it.checkUrl())
                showWebsite.set(true)
            }


        }
    }

    fun set(location: NearbyLocation) {
        if (!location.address.isNullOrEmpty()) {
            address.set(location.address)
            showAddress.set(true)
        }

        if (!location.website.isNullOrEmpty()) {
            website.set(location.website)
            showWebsite.set(true)
        }


    }
}