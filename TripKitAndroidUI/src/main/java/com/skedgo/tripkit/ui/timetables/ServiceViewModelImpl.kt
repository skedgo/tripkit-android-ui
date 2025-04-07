package com.skedgo.tripkit.ui.timetables

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.skedgo.TripKit
import com.skedgo.rxtry.subscribeWithErrorHandling
import com.skedgo.tripkit.common.model.realtimealert.RealTimeStatus
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import com.skedgo.tripkit.ui.utils.TimeSpanUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class ServiceViewModelImpl @Inject constructor(
    private val context: Context,
    override val occupancyViewModel: OccupancyViewModel,
    override val serviceAlertViewModel: ServiceAlertViewModel,
    private val getServiceTitleText: GetServiceTitleText,
    private val getServiceTertiaryText: GetServiceTertiaryText,
    private val getRealtimeText: GetRealtimeText,
    private val errorLogger: ErrorLogger
) : ServiceViewModel() {
    //  override val wheelchairAccessible = ObservableBoolean(false)
    override val wheelchairIcon = MutableLiveData<Drawable?>()
    override val wheelchairTint = MutableLiveData<Int>(Color.BLACK)

    override val serviceNumber = MutableLiveData<String>()
    override val secondaryText = MutableLiveData<String>()
    override val secondaryTextColor: MutableLiveData<Int> = MutableLiveData()
    override val showOccupancyInfo = MutableLiveData(false)
    override val showBicycleAccessible = MutableLiveData(false)

    override val tertiaryText = MutableLiveData<String>()
    override val quaternaryText = MutableLiveData<String>()
    override val countDownTimeText = MutableLiveData<String>()
    override val countDownTimeTextColor = MutableLiveData<Int>(R.color.tripKitSuccess)
    override val alpha = MutableLiveData(1f)

    override val serviceColor: MutableLiveData<Int> = MutableLiveData()
    override val isCurrentTrip = MutableLiveData(false)

    override val countDownTimeTextBack: MutableLiveData<Drawable> = MutableLiveData()
    override val modeInfo: MutableLiveData<ModeInfo> = MutableLiveData()
    override val onItemClick = TapAction.create { service }

    override val onAlertsClick = TapAction.create { service.alerts }

    override lateinit var service: TimetableEntry
    override lateinit var dateTimeZone: DateTimeZone

    override fun getRealTimeDeparture() = realTimeDeparture(service, service.realtimeVehicle)

    override fun setService(
        _currentTripId: String,
        _service: TimetableEntry,
        _dateTimeZone: DateTimeZone
    ) {
        service = _service
        this.isCurrentTrip.postValue(_currentTripId == service.serviceTripId)
        dateTimeZone = _dateTimeZone
        updateInfo()
    }

    private fun updateInfo() {

        modeInfo.postValue(service.modeInfo)
        if (service.serviceNumber.isNullOrBlank()) {
            serviceNumber.postValue(service.serviceName)
        } else {
            serviceNumber.postValue(service.serviceNumber)
        }
        val (secondaryMessage, color) = getRealtimeText.execute(
            dateTimeZone,
            service,
            service.realtimeVehicle
        )
        secondaryText.postValue(secondaryMessage)
        secondaryTextColor.postValue(ContextCompat.getColor(context, color))
        tertiaryText.postValue(getServiceTertiaryText.execute(service))

        val globalConfigs = TripKit.getInstance().configs()
        if (globalConfigs.showOperatorNames()) {
            quaternaryText.postValue(service.operator)
        }

        setBicycleAccessible()
        presentOccupancy()
        presentCountDownTimeForFrequency()
        presentServiceColor(service)
        initHelpersVMs(service)
    }

    private fun setBicycleAccessible() {
        showBicycleAccessible.postValue(service.bicycleAccessible == true)
    }

    private fun presentOccupancy() {
        service.realtimeVehicle?.let { occupancyViewModel.setOccupancy(it, false) }
        showOccupancyInfo.postValue(occupancyViewModel.hasInformation())
    }

    private fun presentCountDownTimeForFrequency() {
        if (!service.isFrequencyBased) {
            service.getTimeLeftToDepartInterval(30, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ presentCountDownTime(it) }, errorLogger::logError)
                .autoClear()
        }
    }

    private fun presentCountDownTime(departureCountDownTimeInMins: Long) {
        if (service.realTimeStatus == RealTimeStatus.CANCELLED || service.isCancelled) {
            countDownTimeText.postValue(context.getString(R.string.cancelled))
            countDownTimeTextColor.postValue(
                ContextCompat.getColor(context, R.color.tripKitError)
            )
        } else {
            countDownTimeTextColor.postValue(
                ContextCompat.getColor(context, R.color.tripKitSuccess)
            )
            countDownTimeText.postValue(
                TimeSpanUtils.getRelativeTimeSpanString(departureCountDownTimeInMins)
            )
        }


        if (departureCountDownTimeInMins < 0 || service.isCancelled) {
            countDownTimeTextBack.value =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.v4_shape_rect_cancelled
                )
            alpha.value = 0.5f
        } else {
            countDownTimeTextBack.value =
                ContextCompat.getDrawable(
                    context,
                    R.drawable.v4_shape_btn_positive_normal
                )
            alpha.value = 1f
        }
    }

    private fun presentServiceColor(service: TimetableEntry) {
        service.serviceColor?.let {
            when (it.color) {
                Color.BLACK, Color.WHITE -> {
                    serviceColor.postValue(Color.BLACK)
                }
                else -> {
                    serviceColor.postValue(it.color)
                }
            }
        }
    }

    private fun initHelpersVMs(service: TimetableEntry) {

        service.wheelchairAccessible?.let {
            if (it) {
                wheelchairIcon.postValue(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair))
                wheelchairTint.postValue(ContextCompat.getColor(context, R.color.black2))
            } else {
                wheelchairIcon.postValue(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_wheelchair_not_accessible
                    )
                )
                wheelchairTint.postValue(ContextCompat.getColor(context, R.color.tripKitWarning))
            }
        }
        serviceAlertViewModel.setAlerts(service.alerts)
        serviceAlertViewModel.showAlertsObservable
            .subscribeWithErrorHandling { onAlertsClick.perform() }
            .autoClear()
    }
}