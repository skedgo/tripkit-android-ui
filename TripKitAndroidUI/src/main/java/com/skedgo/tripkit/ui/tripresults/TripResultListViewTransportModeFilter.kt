package com.skedgo.tripkit.ui.tripresults

import android.os.Parcel
import android.os.Parcelable
import com.skedgo.tripkit.TransportModeFilter
import com.skedgo.tripkit.common.model.TransportMode
import com.skedgo.tripkit.ui.model.UserMode
import com.skedgo.tripkit.ui.routing.SimpleTransportModeFilter


// This TransportModeFilter is used to combine the user's transport mode filter, which broadly says what transport modes
// they want in general, with a view filter, which makes it easier to only load the routes from the network
// which will be displayed.
class TripResultListViewTransportModeFilter(
    private val transportModeFilter: TransportModeFilter,
    private val transportViewFilter: TripResultTransportViewFilter
) : TransportModeFilter {

    private var replacementModes: List<UserMode> = listOf()

    override fun useTransportMode(mode: String): Boolean {
        val includedByInjectedMode = InjectedTransportModes.shouldIncludeBackendMode(mode) { injectedId ->
            transportViewFilter.isSelected(injectedId)
        }
        return transportModeFilter.useTransportMode(mode)
            && (
                transportViewFilter.isSelected(mode) ||
                    transportViewFilter.isMinimized(mode) ||
                    includedByInjectedMode
                )
    }

    override fun avoidTransportMode(mode: String): Boolean {
        return transportModeFilter.avoidTransportMode(mode)
    }

    fun replaceTransportModes(mode: List<UserMode>) {
        replacementModes = mode
    }

    override fun getFilteredMode(originalModes: List<String>): List<String> {
        if (isParkRideOnlySelection()) {
            val hasCarOrPt =
                originalModes.contains(TransportMode.ID_CAR) ||
                    originalModes.contains(TransportMode.ID_PUBLIC_TRANSPORT)
            if (hasCarOrPt) {
                // Collapse Park & Ride into one combined backend request only.
                return listOf(TransportMode.ID_CAR, TransportMode.ID_PUBLIC_TRANSPORT)
            }
        }

        val modeArray = ArrayList(originalModes)
        replacementModes.forEach {
            if (modeArray.contains(it.mode)) {
                modeArray.remove(it.mode)
                it.rules?.replaceWith?.let { list ->
                    modeArray.addAll(list)
                }
            }
        }
        return modeArray
    }

    private fun isParkRideOnlySelection(): Boolean {
        val parkRideSelected = transportViewFilter.isSelected(InjectedTransportModes.ID_PARK_RIDE)
        if (!parkRideSelected) return false

        val ptSelectedStandalone = transportViewFilter.isSelected(TransportMode.ID_PUBLIC_TRANSPORT)
        val carSelectedStandalone = transportViewFilter.isSelected(TransportMode.ID_CAR)
        return !ptSelectedStandalone && !carSelectedStandalone
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TripResultListViewTransportModeFilter> {
        override fun createFromParcel(parcel: Parcel): TripResultListViewTransportModeFilter {
            return TripResultListViewTransportModeFilter(
                SimpleTransportModeFilter(),
                PermissiveTransportViewFilter()
            )
        }

        override fun newArray(size: Int): Array<TripResultListViewTransportModeFilter?> {
            return arrayOfNulls(size)
        }
    }
}