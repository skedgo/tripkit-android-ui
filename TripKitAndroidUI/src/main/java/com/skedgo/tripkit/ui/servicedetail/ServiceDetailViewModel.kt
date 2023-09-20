package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.TripKit
import com.skedgo.tripkit.ServiceApi
import com.skedgo.tripkit.ServiceResponse
import com.skedgo.tripkit.common.model.Location
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.ServiceStop
import com.skedgo.tripkit.common.model.WheelchairAccessible
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import com.skedgo.tripkit.ui.timetables.GetServiceSubTitleText
import com.skedgo.tripkit.ui.timetables.GetServiceTertiaryText
import com.skedgo.tripkit.ui.timetables.GetServiceTitleText
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.trippreview.service.ServiceTripActionViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import me.tatarka.bindingcollectionadapter2.ItemBinding
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class ServiceDetailViewModel @Inject constructor(
    private val context: Context,
    private val regionService: RegionService,
    private val serviceApi: ServiceApi,
    val occupancyViewModel: OccupancyViewModel,
    private val serviceViewModelProvider: Provider<ServiceDetailItemViewModel>,
    val serviceAlertViewModel: ServiceAlertViewModel,
    private val loadServices: LoadServices,
    private val getServiceTitleText: GetServiceTitleText,
    private val getServiceSubTitleText: GetServiceSubTitleText,
    private val getServiceTertiaryText: GetServiceTertiaryText,
    private val getRealtimeText: GetRealtimeText,
    private val errorLogger: ErrorLogger
) : RxViewModel() {
    val stationName = ObservableField<String>()
    val serviceColor: ObservableInt = ObservableInt()
    val serviceNumber = ObservableField<String>()

    val secondaryText = ObservableField<String>()
    val secondaryTextColor: ObservableInt = ObservableInt()
    val tertiaryText = ObservableField<String>()
    val showWheelchairAccessible = ObservableBoolean(false)
    val wheelchairAccessibleText = ObservableField<String>()
    val showExpandableMenu = ObservableBoolean(false)
    val modeInfo = ObservableField<ModeInfo>()

    val wheelchairIcon = ObservableField<Drawable?>()

    val showOccupancyInfo = ObservableBoolean(false)

    val itemBinding = ItemBinding.of<ServiceDetailItemViewModel>(
        BR.viewModel,
        R.layout.service_detail_fragment_list_item
    )
    val items: ObservableField<List<ServiceDetailItemViewModel>> = ObservableField(emptyList())
    val onItemClicked = PublishRelay.create<ServiceStop>()
    var showCloseButton = ObservableBoolean(false)

    fun setup(
        region: String,
        serviceId: String,
        serviceName: String?,
        serviceNumber: String?,
        serviceColor: ServiceColor?,
        operator: String?,
        startStopCode: String,
        endStopCode: String?,
        embarkation: Long,
        realTimeVehicle: RealTimeVehicle?,
        wheelchairAccessible: Boolean?,
        schedule: Pair<String, Int>? = null,
        modeInfo: ModeInfo? = null
    ) {
        this.stationName.set(serviceName)
        this.serviceNumber.set(serviceNumber)

//        val (secondaryMessage, color) = getRealtimeText.execute(_stop.dateTimeZone, _entry, _entry.realtimeVehicle)
//        secondaryText.set(secondaryMessage)
//        secondaryTextColor.set(ContextCompat.getColor(context, color))

        schedule?.let {
            secondaryText.set(it.first)
            secondaryTextColor.set(ContextCompat.getColor(context, it.second))
        }

        modeInfo?.let {
            this.modeInfo.set(modeInfo)
        }

        val globalConfigs = TripKit.getInstance().configs()
        if (globalConfigs.showOperatorNames())
            tertiaryText.set(operator)

        realTimeVehicle?.let { occupancyViewModel.setOccupancy(it, false) }
        showOccupancyInfo.set(occupancyViewModel.hasInformation())

        wheelchairAccessible?.let {
            showWheelchairAccessible.set(true)
            if (it) {
                wheelchairAccessibleText.set(context.getString(R.string.wheelchair_accessible))
                wheelchairIcon.set(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair))
            } else {
                wheelchairAccessibleText.set(context.getString(R.string.not_wheelchair_accessible))
                wheelchairIcon.set(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_wheelchair_not_accessible
                    )
                )
            }
        }

        showExpandableMenu.set(showOccupancyInfo.get() && showWheelchairAccessible.get())

        serviceColor?.let {
            when (it.color) {
                Color.BLACK, Color.WHITE -> {
                    this.serviceColor.set(Color.BLACK)
                }

                else -> {
                    this.serviceColor.set(it.color)
                }
            }
        }

        serviceApi.getServiceAsync(
            region, serviceId, operator, startStopCode, endStopCode,
            embarkation, true
        )
            .subscribe({ processResponse(it) }, { Timber.e(it) })
            .autoClear()

    }

    fun setup(segment: TripSegment) {
        regionService.getRegionByLocationAsync(segment.from)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                setup(
                    it.name!!,
                    segment.serviceTripId,
                    segment.serviceName,
                    segment.serviceNumber,
                    segment.serviceColor,
                    segment.serviceOperator,
                    segment.startStopCode,
                    segment.endStopCode,
                    segment.timetableStartTime,
                    segment.realTimeVehicle,
                    segment.wheelchairAccessible
                )
            }, {
                Timber.e(it)
            })
            .autoClear()
    }

    fun setup(_stop: ScheduledStop, _entry: TimetableEntry) {
        regionService.getRegionByLocationAsync(_stop)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                setup(
                    it.name ?: "",
                    _entry.serviceTripId,
                    if (!_entry.serviceName.isNullOrEmpty())
                        _entry.serviceName!!
                    else
                        getServiceTertiaryText.execute(_entry),
                    _entry.serviceNumber,
                    _entry.serviceColor,
                    _entry.operator,
                    _entry.startStopCode,
                    null,
                    _entry.startTimeInSecs,
                    _entry.realtimeVehicle,
                    _entry.wheelchairAccessible,
                    getRealtimeText.execute(_stop.dateTimeZone, _entry, _entry.realtimeVehicle),
                    _entry.modeInfo
                )
            }, {
                it.printStackTrace()
            }).autoClear()
    }

    fun processResponse(response: ServiceResponse) {
        var list = mutableListOf<ServiceDetailItemViewModel>()
        response.shapes().forEach { shape ->
            shape.stops?.forEach { stop ->
                list.add(serviceViewModelProvider.get().apply {
                    // isTravelled indicates whether or not the *traveller* has travelled the stop, which will always
                    // be false for stops on a line prior to the displayed station, and true for stops after. So for our purposes here,
                    // invert it.
                    this.setStop(context, stop, shape.serviceColor.color, !shape.isTravelled)
                    this.setDrawable(context, ServiceDetailItemViewModel.LineDirection.MIDDLE)
                    this.onItemClick.observable.subscribe { stopInfo ->
                        stopInfo?.let { onItemClicked.accept(it) }
                    }
                })
            }
        }

        items.get()!!.forEach { vm ->
            vm.onCleared()
        }
        list.firstOrNull()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.START)
        list.lastOrNull()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.END)
        items.set(list)
    }

    override fun onCleared() {
        super.onCleared()
        items.get()!!.forEach {
            it.onCleared()
        }
    }
}
