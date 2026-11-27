package com.skedgo.tripkit.ui.timetables

import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import org.joda.time.DateTimeZone

abstract class ServiceViewModel() : RxViewModel() {
    abstract val occupancyViewModel: OccupancyViewModel
    abstract val serviceAlertViewModel: ServiceAlertViewModel
    abstract val serviceNumber: MutableLiveData<String>
    abstract val secondaryText: MutableLiveData<String>
    abstract val secondaryTextColor: MutableLiveData<Int>
    abstract val tertiaryText: MutableLiveData<String>
    abstract val quaternaryText: MutableLiveData<String>
    abstract val countDownTimeText: MutableLiveData<String>
    abstract val countDownTimeTextColor: MutableLiveData<Int>
    abstract val alpha: MutableLiveData<Float>
    abstract val countDownTimeTextBack: MutableLiveData<Drawable>
    abstract val serviceColor: MutableLiveData<Int>
    abstract val showOccupancyInfo: MutableLiveData<Boolean>
    abstract val showBicycleAccessible: MutableLiveData<Boolean>
    abstract val isCurrentTrip: MutableLiveData<Boolean>
    abstract val isOnTime: MutableLiveData<Boolean>

//    abstract val wheelchairAccessible: ObservableBoolean
//    abstract val wheelchairInaccessible: ObservableBoolean

    abstract val wheelchairIcon: MutableLiveData<Drawable?>
    abstract val wheelchairTint: MutableLiveData<Int>
    abstract val wheelchairBackgroundTint: MutableLiveData<Drawable>

    abstract val modeInfo: MutableLiveData<ModeInfo>
    abstract val onItemClick: TapAction<TimetableEntry>
    abstract val onAlertsClick: TapAction<List<RealtimeAlert>?>

    abstract var service: TimetableEntry
    abstract var dateTimeZone: DateTimeZone

    abstract fun getRealTimeDeparture(): Long
    abstract fun setService(
        _currentTripId: String,
        _service: TimetableEntry,
        _dateTimeZone: DateTimeZone
    )

}