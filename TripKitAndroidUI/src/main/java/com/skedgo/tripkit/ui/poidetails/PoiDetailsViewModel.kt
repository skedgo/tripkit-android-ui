package com.skedgo.tripkit.ui.poidetails

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.ui.core.RxViewModel
import javax.inject.Inject


class PoiDetailsViewModel @Inject constructor(private val locationInfoService: LocationInfoService): RxViewModel() {
    var locationTitle = ObservableField<String>("")
    var showCloseButton = ObservableBoolean(false)

    var address = ObservableField<String>("")
    var website = ObservableField<String>("")
    var what3words = ObservableField<String>("")

    fun start(location: Location) {
        this.locationTitle.set(location.displayName)
        this.address.set(location.address)
        this.what3words.set(location.w3w)
    }
}