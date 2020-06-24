package com.skedgo.tripkit.ui.trippreview.nearby

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.ui.core.RxViewModel


class ModeLocationTripPreviewViewModel : RxViewModel() {
    var address = ObservableField<String>("")
    var showAddress = ObservableBoolean(false)
    var website = ObservableField<String>("")
    var showWebsite = ObservableBoolean(false)

    var what3words = ObservableField<String>("")
    var showWhat3words = ObservableBoolean(false)

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