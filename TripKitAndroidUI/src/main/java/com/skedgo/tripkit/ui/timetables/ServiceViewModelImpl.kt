package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import com.skedgo.tripkit.ui.utils.TimeSpanUtils
import org.joda.time.DateTimeZone
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.ModeInfo
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class ServiceViewModelImpl @Inject constructor(
        private val context: Context,
        override val occupancyViewModel: OccupancyViewModel,
        override val serviceAlertViewModel: ServiceAlertViewModel,
        private val getServiceTitleText: GetServiceTitleText,
        private val getServiceSubTitleText: GetServiceSubTitleText,
        private val getServiceTertiaryText: GetServiceTertiaryText,
        private val getRealtimeText: GetRealtimeText,
        private val errorLogger: ErrorLogger
) : ServiceViewModel() {
//  override val wheelchairAccessible = ObservableBoolean(false)
  override val wheelchairIcon = ObservableField<Drawable?>()
  override val wheelchairTint = ObservableField<Int>(Color.BLACK)

  override val serviceNumber = ObservableField<String>()
  override val secondaryText = ObservableField<String>()
  override val secondaryTextColor: ObservableInt = ObservableInt()
  override val showOccupancyInfo = ObservableBoolean(false)

  override val tertiaryText = ObservableField<String>()
  override val countDownTimeText = ObservableField<String>()
  override val alpha = ObservableFloat(1f)

  override val serviceColor: ObservableInt = ObservableInt()

  override val countDownTimeTextBack: ObservableField<Drawable> = ObservableField()
  override val modeInfo: ObservableField<ModeInfo> = ObservableField()
  override val onItemClick = TapAction.create { service }

  override val onAlertsClick = TapAction.create { service.alerts }

  override lateinit var service: TimetableEntry
  override lateinit var dateTimeZone: DateTimeZone

  override fun getRealTimeDeparture() = realTimeDeparture(service, service.realtimeVehicle)

  override fun setService(_service: TimetableEntry,
                          _dateTimeZone: DateTimeZone) {
    service = _service
    dateTimeZone = _dateTimeZone
    updateInfo()
  }

  private fun updateInfo() {

    modeInfo.set(service.modeInfo)
    if (service.serviceNumber.isNullOrBlank()) {
      serviceNumber.set(service.serviceName)
    } else {
      serviceNumber.set(service.serviceNumber)
    }
    val (secondaryMessage, color) = getRealtimeText.execute(dateTimeZone, service, service.realtimeVehicle)
    secondaryText.set(secondaryMessage)
    secondaryTextColor.set(ContextCompat.getColor(context, color))
    tertiaryText.set(getServiceTertiaryText.execute(service))

    presentOccupancy()
    presentCountDownTimeForFrequency()
    presentServiceColor(service)
    initHelpersVMs(service)
  }

  private fun presentOccupancy() {
    service.realtimeVehicle?.let { occupancyViewModel.setOccupancy(it, false) }
    showOccupancyInfo.set(occupancyViewModel.hasInformation())
  }

  private fun presentCountDownTimeForFrequency() {
    if (!service.isFrequencyBased) {
      service.getTimeLeftToDepartInterval(30, TimeUnit.SECONDS)
          .subscribe({ presentCountDownTime(it) }, errorLogger::logError)
          .autoClear()
    }
  }

  private fun presentCountDownTime(departureCountDownTimeInMins: Long) {
    countDownTimeText.set(TimeSpanUtils.getRelativeTimeSpanString(departureCountDownTimeInMins))

    if (departureCountDownTimeInMins < 0) {
      countDownTimeTextBack.set(ContextCompat.getDrawable(context, R.drawable.v4_shape_rect_cancelled))
      alpha.set(0.5f)
    } else {
      countDownTimeTextBack.set(ContextCompat.getDrawable(context, R.drawable.v4_shape_btn_positive_normal))
      alpha.set(1f)
    }
  }

  private fun presentServiceColor(service: TimetableEntry) {
    service.serviceColor?.let {
      when (it.color) {
        Color.BLACK, Color.WHITE -> {
          serviceColor.set(Color.BLACK)
        }
        else -> {
          serviceColor.set(it.color)
        }
      }
    }
  }

  private fun initHelpersVMs(service: TimetableEntry) {

    service.wheelchairAccessible?.let {
      if (it) {
        wheelchairIcon.set(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair))
        wheelchairTint.set(ContextCompat.getColor(context, R.color.black2))
      } else {
        wheelchairIcon.set(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair_not_accessible))
        wheelchairTint.set(ContextCompat.getColor(context, R.color.tripKitWarning))
      }
    }
    serviceAlertViewModel.setAlerts(service.alerts)
    serviceAlertViewModel.showAlertsObservable
        .subscribe { onAlertsClick.perform() }
        .autoClear()
  }

}