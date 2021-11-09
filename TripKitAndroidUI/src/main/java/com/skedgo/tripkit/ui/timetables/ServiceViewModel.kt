package com.skedgo.tripkit.ui.timetables

import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.common.model.RealtimeAlert
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import org.joda.time.DateTimeZone
import com.skedgo.tripkit.routing.ModeInfo

abstract class ServiceViewModel() : RxViewModel() {
    abstract val occupancyViewModel: OccupancyViewModel
    abstract val serviceAlertViewModel: ServiceAlertViewModel
    abstract val serviceNumber: ObservableField<String>
    abstract val secondaryText: ObservableField<String>
    abstract val secondaryTextColor: ObservableInt
    abstract val tertiaryText: ObservableField<String>
    abstract val countDownTimeText: ObservableField<String>
    abstract val alpha: ObservableFloat
    abstract val countDownTimeTextBack: ObservableField<Drawable>
    abstract val serviceColor: ObservableInt
    abstract val showOccupancyInfo: ObservableBoolean
    abstract val isCurrentTrip: ObservableBoolean

//    abstract val wheelchairAccessible: ObservableBoolean
//    abstract val wheelchairInaccessible: ObservableBoolean

    abstract val wheelchairIcon: ObservableField<Drawable?>
    abstract val wheelchairTint: ObservableField<Int>

    abstract val modeInfo: ObservableField<ModeInfo>
    abstract val onItemClick: TapAction<TimetableEntry>
    abstract val onAlertsClick: TapAction<ArrayList<RealtimeAlert>>

    abstract var service: TimetableEntry
    abstract var dateTimeZone: DateTimeZone

    abstract fun getRealTimeDeparture(): Long
    abstract fun setService(_currentTripId: String, _service: TimetableEntry, _dateTimeZone: DateTimeZone)

}