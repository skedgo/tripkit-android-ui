package com.skedgo.tripkit.ui.tripresult

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.gson.Gson
import com.skedgo.tripkit.routing.GeoLocation
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import javax.inject.Inject


class TripSegmentGetOffAlertsViewModel @Inject internal constructor() : RxViewModel() {

    private val _getOffAlertStateOn = MutableLiveData<Boolean>()
    val getOffAlertStateOn: LiveData<Boolean> = _getOffAlertStateOn

    private var trip: Trip? = null

    val items = DiffObservableList<TripSegmentGetOffAlertDetailViewModel>(TripSegmentGetOffAlertDetailViewModel.diffCallback())
    val itemBinding = ItemBinding.of<TripSegmentGetOffAlertDetailViewModel>(BR.viewModel, R.layout.item_alert_detail)

    fun setup(trip: Trip?, details: List<TripSegmentGetOffAlertDetailViewModel>) {
        this.trip = trip
        items.clear()
        items.update(details)

        trip?.uuid()?.let {
            _getOffAlertStateOn.postValue(GetOffAlertCache.isTripAlertStateOn(it))
        }
    }

    fun onAlertChange(context: Context, isOn: Boolean) {
        _getOffAlertStateOn.postValue(isOn)
        trip?.uuid()?.let { GetOffAlertCache.setTripAlertOnState(it, isOn) }

        if (isOn) {
            ExcuseMe.couldYouGive(context).permissionFor(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) {
                if (it.denied.isNotEmpty()) {
                    _getOffAlertStateOn.postValue(false)
                } else {
                    trip?.segments?.mapNotNull { it.geofences }?.flatten()?.let { geofences ->
                        GeoLocation.createGeoFences(geofences)
                    }
                }
            }
        } else {
            GeoLocation.clearGeofences()
        }
    }

}

class TripSegmentGetOffAlertDetailViewModel @Inject internal constructor(
        val icon: Drawable?,
        val title: String
) : RxViewModel() {
    companion object {
        fun diffCallback() = object : DiffUtil.ItemCallback<TripSegmentGetOffAlertDetailViewModel>() {
            override fun areItemsTheSame(oldItem: TripSegmentGetOffAlertDetailViewModel, newItem: TripSegmentGetOffAlertDetailViewModel): Boolean =
                    oldItem.title == newItem.title

            override fun areContentsTheSame(oldItem: TripSegmentGetOffAlertDetailViewModel, newItem: TripSegmentGetOffAlertDetailViewModel): Boolean =
                    oldItem.icon == newItem.icon
                            && oldItem.title == newItem.title
        }
    }
}