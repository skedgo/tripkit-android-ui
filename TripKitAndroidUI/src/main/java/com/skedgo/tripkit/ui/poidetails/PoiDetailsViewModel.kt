package com.skedgo.tripkit.ui.poidetails

import android.content.Context
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skedgo.TripKit
import com.skedgo.tripkit.LocationInfoService
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.PoiLocation
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.data.places.PlaceSearchRepository
import com.skedgo.tripkit.ui.utils.isAppInstalled
import timber.log.Timber
import javax.inject.Inject


class PoiDetailsViewModel @Inject constructor(
    private val locationInfoService: LocationInfoService,
    private val placeSearchRepository: PlaceSearchRepository
) : RxViewModel() {
    val locationTitle = ObservableField<String>("")
    val showCloseButton = ObservableBoolean(false)
    val favoriteText = ObservableField<String>("")
    val address = ObservableField<String>("")
    val showAddress = ObservableBoolean(false)
    val website = ObservableField<String>("")
    val showWebsite = ObservableBoolean(false)
    val type = ObservableField(Location.TYPE_UNKNOWN)
    val withExternalApp = ObservableBoolean(false)
    val openAppButtonText = ObservableField("")
    val goButtonText = ObservableField("")

    var what3words = ObservableField<String>("")
    var showWhat3words = ObservableBoolean(false)

    val location = ObservableField<Location>()

    private val _favoriteVisible = MutableLiveData(true)
    val favoriteVisible: LiveData<Boolean> = _favoriteVisible

    init {
        val globalConfigs = TripKit.getInstance().configs()
        _favoriteVisible.postValue(!globalConfigs.hideFavorites())
    }

    fun setFavorite(context: Context, favorite: Boolean) {
        if (favorite) {
            favoriteText.set(context.getString(R.string.remove_favourite))
        } else {
            favoriteText.set(context.getString(R.string.favourite))
        }
    }

    fun start(
        context: Context,
        location: Location,
        isRouting: Boolean = false,
        isDeparture: Boolean = false
    ) {
        this.location.set(location)
        this.locationTitle.set(location.displayName)
        this.address.set(location.address)
        this.website.set(location.url)
        this.type.set(location.locationType)
        this.withExternalApp.set(location.isWithExternalApp)

        openAppButtonText.set(
            if (location.appUrl?.isAppInstalled(context.packageManager) == true) {
                context.getString(R.string.open_app)
            } else {
                context.getString(R.string.get_app)
            }
        )

        goButtonText.set(
            if (isRouting) {
                if (isDeparture) {
                    context.getString(R.string.start_here)
                } else {
                    context.getString(R.string.end_here)
                }
            } else {
                context.getString(R.string.go)
            }
        )

        locationInfoService.getLocationInfoAsync(location)
            .take(1)
            .subscribe({
                val w3w = it.details()?.w3w()
                if (!w3w.isNullOrBlank()) {
                    this.what3words.set(w3w)
                    this.showWhat3words.set(true)
                }
            }, { Timber.e(it) })
            .autoClear()
        showAddress.set(!location.address.isNullOrBlank())
        showWebsite.set(!location.url.isNullOrBlank())

        if (location is PoiLocation) {
            location.placeId?.let { placeId ->
                placeSearchRepository.getPlaceDetails(placeId)
                    .take(1)
                    .subscribe({
                        if (it.address.isNotBlank()) {
                            this.address.set(it.address)
                            this.showAddress.set(true)
                        }
                        var website = it.website?.toString()
                        if (!website.isNullOrBlank()) {
                            this.website.set(website)
                            this.showWebsite.set(true)
                        }
                    }, { Timber.e(it) }).autoClear()
            }
        }
    }

    fun refresh(context: Context) {
        location.get()?.let { location ->
            openAppButtonText.set(
                if (location.appUrl?.isAppInstalled(context.packageManager) == true) {
                    context.getString(R.string.open_app)
                } else {
                    context.getString(R.string.get_app)
                }
            )
        }
    }
}