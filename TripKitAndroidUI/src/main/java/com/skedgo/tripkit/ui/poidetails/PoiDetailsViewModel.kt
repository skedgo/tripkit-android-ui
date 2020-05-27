package com.skedgo.tripkit.ui.poidetails

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.PoiLocation
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import javax.inject.Inject


class PoiDetailsViewModel @Inject constructor(private val locationInfoService: LocationInfoService,
                                              private val placeSearchRepository: PlaceSearchRepository): RxViewModel() {
    var locationTitle = ObservableField<String>("")
    var showCloseButton = ObservableBoolean(false)

    var address = ObservableField<String>("")
    var showAddress = ObservableBoolean(false)
    var website = ObservableField<String>("")
    var showWebsite = ObservableBoolean(false)

    var what3words = ObservableField<String>("")
    var showWhat3words = ObservableBoolean(false)

    fun start(location: Location) {
        this.locationTitle.set(location.displayName)
        this.address.set(location.address)
        this.website.set(location.url)

        locationInfoService.getLocationInfoAsync(location)
                .take(1)
                .subscribe {
                    val w3w = it.details()?.w3w()
                    if (!w3w.isNullOrBlank()) {
                        this.what3words.set(w3w)
                        this.showWhat3words.set(true)
                    }
                }
                .autoClear()
        showAddress.set(!location.address.isNullOrBlank())
        showWebsite.set(!location.url.isNullOrBlank())

        if (location is PoiLocation) {
            location.placeId?.let {placeId ->
                placeSearchRepository.getPlaceDetails(placeId)
                        .take(1)
                        .subscribe {
                            if (it.address.isNotBlank()) {
                                this.address.set(it.address)
                                this.showAddress.set(true)
                            }
                            var website = it.website?.toString()
                            if (!website.isNullOrBlank()) {
                                this.website.set(website)
                                this.showWebsite.set(true)
                            }
                        }.autoClear()
            }
        }
    }


}