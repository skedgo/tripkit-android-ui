package com.skedgo.tripkit.ui.servicedetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.jakewharton.rxrelay2.PublishRelay
import com.skedgo.tripkit.common.model.ScheduledStop
import com.skedgo.tripkit.common.model.ServiceStop
import com.skedgo.tripkit.logging.ErrorLogger
import com.skedgo.tripkit.routing.dateTimeZone
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
import io.reactivex.android.schedulers.AndroidSchedulers
import me.tatarka.bindingcollectionadapter2.ItemBinding
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

class ServiceDetailViewModel  @Inject constructor(private val context: Context,
                                                  val occupancyViewModel: OccupancyViewModel,
                                                  private val serviceViewModelProvider: Provider<ServiceDetailItemViewModel>,
                                                  val serviceAlertViewModel: ServiceAlertViewModel,
                                                  private val loadServices: LoadServices,
                                                  private val getServiceTitleText: GetServiceTitleText,
                                                  private val getServiceSubTitleText: GetServiceSubTitleText,
                                                  private val getServiceTertiaryText: GetServiceTertiaryText,
                                                  private val getRealtimeText: GetRealtimeText,
                                                  private val errorLogger: ErrorLogger): RxViewModel() {

    var stop: ScheduledStop? = null
    var timetableEntry: TimetableEntry? = null

    val stationName = ObservableField<String>()
    val serviceColor: ObservableInt = ObservableInt()
    val serviceNumber = ObservableField<String>()

    val secondaryText = ObservableField<String>()
    val secondaryTextColor: ObservableInt = ObservableInt()
    val tertiaryText = ObservableField<String>()
    val showWheelchairAccessible = ObservableBoolean(false)
    val wheelchairAccessibleText = ObservableField<String>()

    val wheelchairIcon = ObservableField<Drawable?>()

    val showOccupancyInfo = ObservableBoolean(false)

    val itemBinding = ItemBinding.of<ServiceDetailItemViewModel>(BR.viewModel, R.layout.service_detail_fragment_list_item)
    val items: ObservableField<List<ServiceDetailItemViewModel>> = ObservableField(emptyList())
    val onItemClicked = PublishRelay.create<ServiceStop>()
    var showCloseButton = ObservableBoolean(false)

    fun setup(_stop: ScheduledStop, _entry: TimetableEntry) {
        stop = _stop
        timetableEntry = _entry

        stationName.set(_entry.serviceName)
        serviceNumber.set(_entry.serviceNumber)

        val (secondaryMessage, color) = getRealtimeText.execute(_stop.dateTimeZone, _entry, _entry.realtimeVehicle)
        secondaryText.set(secondaryMessage)
        secondaryTextColor.set(ContextCompat.getColor(context, color))
        tertiaryText.set(getServiceTertiaryText.execute(_entry))


        _entry.realtimeVehicle?.let { occupancyViewModel.setOccupancy(it, false) }
        showOccupancyInfo.set(occupancyViewModel.hasInformation())

        _entry.wheelchairAccessible?.let {
            showWheelchairAccessible.set(true)
            if (it) {
                wheelchairAccessibleText.set(context.getString(R.string.wheelchair_accessible))
                wheelchairIcon.set(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair))
            } else {
                wheelchairAccessibleText.set(context.getString(R.string.not_wheelchair_accessible))
                wheelchairIcon.set(ContextCompat.getDrawable(context, R.drawable.ic_wheelchair_not_accessible))
            }
        }

        setServiceColor()
        loadServices.fetch(timetableEntry!!, stop!!).observeOn(AndroidSchedulers.mainThread()).subscribe ({
            loadServices.execute(timetableEntry!!, stop!!)
                .map { listListPair -> listListPair.first }
                .map {
                    var travelled = true
                    it.map {
                        serviceViewModelProvider.get().apply {
                            if (_entry.stopCode == it.stop.code) travelled = false
                            this.setStop(context, it, serviceColor.get(), travelled)
                            this.setDrawable(context, ServiceDetailItemViewModel.LineDirection.MIDDLE)
                            this.onItemClick.observable.subscribe {
                                stopInfo -> stopInfo?.let { onItemClicked.accept(it.stop) }
                            }
                        }
                    }
                }
                .subscribe ({
                    list ->
                        items.get()!!.forEach {
                            vm -> vm.onCleared()
                        }
                        list.first()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.START)
                        list.last()?.setDrawable(context, ServiceDetailItemViewModel.LineDirection.END)
                        items.set(list)
                }, { Timber.e(it) })
        }, { Timber.e(it) }).autoClear()
    }

    private fun setServiceColor() {
        timetableEntry?.serviceColor?.let {
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

    override fun onCleared() {
        super.onCleared()
        items.get()!!.forEach {
            it.onCleared()
        }
    }
}
