package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.TripKit
import com.skedgo.tripkit.ServiceResponse
import com.skedgo.tripkit.common.model.realtimealert.RealtimeAlert
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.ServiceStop
import com.skedgo.tripkit.data.regions.RegionService
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.*
import com.skedgo.tripkit.servicedetail.ServiceDetailRepository
import com.skedgo.tripkit.ui.BR
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.model.TimetableEntry
import com.skedgo.tripkit.ui.timetables.GetRealtimeText
import com.skedgo.tripkit.ui.timetables.GetServiceTertiaryText
import com.skedgo.tripkit.ui.timetables.GetServiceTitleText
import com.skedgo.tripkit.ui.trip.details.viewmodel.OccupancyViewModel
import com.skedgo.tripkit.ui.trip.details.viewmodel.ServiceAlertViewModel
import com.skedgo.tripkit.ui.utils.TapAction
import io.reactivex.android.schedulers.AndroidSchedulers
import me.tatarka.bindingcollectionadapter2.ItemBinding
import org.joda.time.DateTimeZone
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class ServiceDetailViewModel @Inject constructor(
    private val context: Context,
    private val regionService: RegionService,
    private val serviceDetailRepository: ServiceDetailRepository,
    val occupancyViewModel: OccupancyViewModel,
    private val serviceViewModelProvider: Provider<ServiceDetailItemViewModel>,
    private val getServiceTertiaryText: GetServiceTertiaryText,
    private val getRealtimeText: GetRealtimeText,
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
    val isExpandableMenuExpanded = ObservableBoolean(false)
    val onExpandMenuClick = TapAction.create {
        isExpandableMenuExpanded.set(!isExpandableMenuExpanded.get())
    }
    val modeInfo = ObservableField<ModeInfo>()

    val wheelchairIcon = ObservableField<Drawable?>()

    val showOccupancyInfo = ObservableBoolean(false)

    val lastUpdatedText = ObservableField<String>()

    val itemBinding: ItemBinding<ServiceDetailItemViewModel> by lazy {
        ItemBinding.of<ServiceDetailItemViewModel>(
            BR.viewModel,
            R.layout.service_detail_fragment_list_item
        )
    }

    val items: ObservableField<List<ServiceDetailItemViewModel>> = ObservableField(emptyList())
    val onItemClicked = PublishRelay.create<ServiceStop>()
    var showCloseButton = ObservableBoolean(false)

    private val _alerts = MutableLiveData<List<RealtimeAlert>>()
    val alerts: LiveData<List<RealtimeAlert>> = _alerts

    var alertClickListener: AlertClickListener? = null

    private val _showBicycleAccessible = MutableLiveData(false)
    val showBicycleAccessible: LiveData<Boolean> = _showBicycleAccessible

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun setAlerts(alerts: List<RealtimeAlert>?) {
        _alerts.postValue(alerts.orEmpty())
    }

    fun setup(
        region: String,
        regionUrls: List<String>? = null,
        serviceId: String,
        serviceName: String?,
        serviceNumber: String?,
        serviceColor: ServiceColor?,
        operator: String?,
        startStopCode: String?,
        endStopCode: String?,
        embarkation: Long,
        realTimeVehicle: RealTimeVehicle?,
        wheelchairAccessible: Boolean?,
        bicycleAccessible: Boolean?,
        schedule: Pair<String, Int>? = null,
        modeInfo: ModeInfo? = null,
        travelledBoundaryStopCode: String? = null
    ) {
        this.stationName.set(serviceName)
        this.serviceNumber.set(serviceNumber)

        schedule?.let {
            secondaryText.set(it.first)
            secondaryTextColor.set(ContextCompat.getColor(context, it.second))
        }

        modeInfo?.let {
            this.modeInfo.set(modeInfo)
        }

        val globalConfigs = TripKit.getInstance().configs()
        if (globalConfigs.showOperatorNames()) {
            tertiaryText.set(operator)
        }

        realTimeVehicle?.let { occupancyViewModel.setOccupancy(it, false) }
        showOccupancyInfo.set(occupancyViewModel.hasInformation())

        showWheelchairAccessible.set(wheelchairAccessible == true)
        wheelchairAccessible?.let {
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

        _showBicycleAccessible.postValue(bicycleAccessible ?: false)

        showExpandableMenu.set(
            showOccupancyInfo.get() ||
                showWheelchairAccessible.get() ||
                (showBicycleAccessible.value ?: false)
        )
        if (!showExpandableMenu.get()) {
            isExpandableMenuExpanded.set(false)
        }

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

        val request = if (regionUrls != null) {
            serviceDetailRepository.getService(
                baseUrls = regionUrls,
                region = region,
                serviceTripId = serviceId,
                operator = operator,
                startStopCode = startStopCode,
                endStopCode = endStopCode,
                embarkationTimeInSecs = embarkation,
                encode = true
            )
        } else {
            serviceDetailRepository.getService(
                region = region,
                serviceTripId = serviceId,
                operator = operator,
                startStopCode = startStopCode,
                endStopCode = endStopCode,
                embarkationTimeInSecs = embarkation,
                encode = true
            )
        }

        request.doOnSubscribe {
            _isLoading.postValue(true)
        }.subscribe(
            {
                _isLoading.postValue(false)
                processResponse(it, travelledBoundaryStopCode)
            }, {
                _isLoading.postValue(false)
                Timber.e(it)
            }
        )
            .autoClear()
    }

    fun setup(segment: TripSegment) {
        regionService.getRegionByLocationAsync(segment.from)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val serviceName = if (segment.serviceName != segment.serviceDirection) {
                    segment.serviceDirection
                } else {
                    segment.serviceName
                }
                setup(
                    region = it.name.orEmpty(),
                    regionUrls = it.getURLs(),
                    serviceId = segment.serviceTripId.orEmpty(),
                    serviceName = serviceName,
                    serviceNumber = segment.serviceNumber,
                    serviceColor = segment.serviceColor,
                    operator = segment.serviceOperator,
                    startStopCode = segment.startStopCode.orEmpty(),
                    endStopCode = segment.endStopCode,
                    embarkation = if(segment.timetableStartTime == 0L) {
                        segment.startTimeInSecs
                    } else {
                        segment.timetableStartTime
                    },
                    realTimeVehicle = segment.realTimeVehicle,
                    wheelchairAccessible = segment.wheelchairAccessible,
                    bicycleAccessible = segment.bicycleAccessible
                )
            }, {
                Timber.e(it)
            })
            .autoClear()

        _alerts.postValue(segment.alerts)
    }

    fun setup(_stop: ScheduledStop, _entry: TimetableEntry) {
        regionService.getRegionByLocationAsync(_stop)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                val serviceName = if (_entry.serviceName.isNullOrEmpty()) {
                    getServiceTertiaryText.execute(_entry)
                } else if (_entry.serviceName != _entry.serviceDirection) {
                    _entry.serviceDirection
                } else {
                    _entry.serviceName
                }
                setup(
                    region = it.name.orEmpty(),
                    regionUrls = it.getURLs(),
                    serviceId = _entry.serviceTripId.orEmpty(),
                    serviceName = serviceName,
                    serviceNumber = _entry.serviceNumber,
                    serviceColor = _entry.serviceColor,
                    operator = _entry.operator,
                    // A stop-based service detail should show the complete service. Supplying the
                    // selected stop code makes service.json return only a partial set of shapes.
                    startStopCode = null,
                    endStopCode = null,
                    embarkation = _entry.startTimeInSecs,
                    realTimeVehicle = _entry.realtimeVehicle,
                    wheelchairAccessible = _entry.wheelchairAccessible,
                    bicycleAccessible = _entry.bicycleAccessible,
                    schedule = getRealtimeText.execute(
                        it.timezone?.let(DateTimeZone::forID) ?: _stop.dateTimeZone,
                        _entry,
                        _entry.realtimeVehicle
                    ),
                    modeInfo = _entry.modeInfo,
                    travelledBoundaryStopCode = _entry.startStopCode
                )
            }, {
                it.printStackTrace()
            }).autoClear()
    }

    fun processResponse(response: ServiceResponse, travelledBoundaryStopCode: String? = null) {
        val stops = response.shapes().flatMap { shape ->
            shape.stops.orEmpty().map { stop -> shape to stop }
        }
        val travelledBoundaryIndex = travelledBoundaryStopCode?.let { stopCode ->
            stops.indexOfFirst { (_, stop) -> stop.code == stopCode }.takeIf { it >= 0 }
        }
        val list = stops.mapIndexed { index, (shape, stop) ->
            serviceViewModelProvider.get().apply {
                // A complete service response can contain a single shape with one travelled value
                // for the whole route. In that case, use the selected timetable stop as the local
                // boundary so earlier stops remain visually travelled without trimming the list.
                val isTravelled = travelledBoundaryIndex?.let { index < it } ?: !shape.isTravelled
                this.setStop(context, stop, shape.serviceColor.color, isTravelled)
                this.setDrawable(context, ServiceDetailItemViewModel.LineDirection.MIDDLE)
                this.onItemClick.observable.subscribe { stopInfo ->
                    stopInfo?.let { onItemClicked.accept(it) }
                }.autoClear()
            }
        }

        items.get()?.forEach { vm ->
            vm.onCleared()
        }
        list.firstOrNull()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.START)
        list.lastOrNull()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.END)
        items.set(list)
    }

    override fun onCleared() {
        super.onCleared()
        items.get()?.forEach {
            it.onCleared()
        }
    }
}
