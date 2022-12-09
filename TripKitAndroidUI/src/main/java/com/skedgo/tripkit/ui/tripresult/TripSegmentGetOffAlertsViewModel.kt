package com.skedgo.tripkit.ui.tripresult

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import com.araujo.jordan.excuseme.ExcuseMe
import com.google.gson.Gson
import com.skedgo.tripkit.routing.GeoLocation
import com.skedgo.tripkit.routing.GetOffAlertCache
import com.skedgo.tripkit.routing.Trip
import com.skedgo.tripkit.routing.TripAlarmBroadcastReceiver
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import java.util.concurrent.TimeUnit
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

        cancelStartTripAlarms(context) //this will cancel previous alarm that was setup

        if (isOn) {
            ExcuseMe.couldYouGive(context).permissionFor(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) {

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                var pendingIntent: PendingIntent? = null
                var startSegmentStartTimeInSecs = 0L

                trip?.segments?.minByOrNull { it.startTimeInSecs }?.let { startSegment ->
                    startSegmentStartTimeInSecs = startSegment.startTimeInSecs
                    val alarmIntent = Intent(context, TripAlarmBroadcastReceiver::class.java)
                    alarmIntent.putExtra(TripAlarmBroadcastReceiver.ACTION_START_TRIP_EVENT, true)
                    alarmIntent.putExtra(TripAlarmBroadcastReceiver.EXTRA_START_TRIP_EVENT_TRIP, Gson().toJson(trip))
                    pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0)
                }

                if (it.denied.isNotEmpty()) {
                    _getOffAlertStateOn.postValue(false)
                    alarmManager.cancel(pendingIntent)
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            (TimeUnit.SECONDS.toMillis(startSegmentStartTimeInSecs) - TimeUnit.MINUTES.toMillis(5)),
                            pendingIntent
                    )
                    trip?.segments?.mapNotNull { it.geofences }?.flatten()?.let { geofences ->
                        GeoLocation.createGeoFences(geofences)
                    }
                }
            }
        } else {
            GeoLocation.clearGeofences()
        }
    }

    private fun cancelStartTripAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, TripAlarmBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
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