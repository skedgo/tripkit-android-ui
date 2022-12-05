package com.skedgo.tripkit.ui.tripresult

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import javax.inject.Inject


class TripSegmentGetOffAlertsViewModel @Inject internal constructor() : RxViewModel() {

    private val _getOffAlertStateOn = MutableLiveData<Boolean>()
    val getOffAlertStateOn: LiveData<Boolean> = _getOffAlertStateOn

    private var tripUuid: String = ""

    val items = DiffObservableList<TripSegmentGetOffAlertDetailViewModel>(TripSegmentGetOffAlertDetailViewModel.diffCallback())
    val itemBinding = ItemBinding.of<TripSegmentGetOffAlertDetailViewModel>(BR.viewModel, R.layout.item_alert_detail)

    fun setup(tripUuid: String, details: List<TripSegmentGetOffAlertDetailViewModel>) {
        this.tripUuid = tripUuid
        items.clear()
        items.update(details)
    }

    fun onAlertChange(isOn: Boolean) {
        _getOffAlertStateOn.postValue(isOn)
        GetOffAlertCache.setTripAlertOnState(tripUuid, isOn)

        Log.e("GetOffAlertCache", "state: $tripUuid ~ ${GetOffAlertCache.isTripAlertStateOn(tripUuid)}")
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