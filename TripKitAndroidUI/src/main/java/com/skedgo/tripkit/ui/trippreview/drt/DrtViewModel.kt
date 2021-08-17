package com.skedgo.tripkit.ui.trippreview.drt

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import com.google.gson.Gson
import com.skedgo.tripkit.booking.BookingService
import com.skedgo.tripkit.booking.quickbooking.*
import com.skedgo.tripkit.common.model.BookingConfirmation
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.addTo
import com.skedgo.tripkit.ui.utils.updateFields
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DrtViewModel @Inject constructor(
        private val quickBookingService: QuickBookingService,
        private val resources: Resources
) : RxViewModel() {

    private val _quickBooking = MutableLiveData<QuickBooking>()
    //val quickBooking: LiveData<QuickBooking> = _quickBooking

    private val _inputs = MutableLiveData<List<Input>>()
    //val inputs: LiveData<List<Input>> = _inputs

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _areInputsValid = MutableLiveData<Boolean>()
    val areInputsValid: LiveData<Boolean> = _areInputsValid

    val onItemChangeActionStream = MutableSharedFlow<DrtItemViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemsDiffCallBack)
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    private val _bookingConfirmation = MutableLiveData<BookingConfirmation>()
    val bookingConfirmation: LiveData<BookingConfirmation> = _bookingConfirmation

    object DrtItemsDiffCallBack : DiffUtil.ItemCallback<DrtItemViewModel>() {
        override fun areItemsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label == newItem.label

        override fun areContentsTheSame(oldItem: DrtItemViewModel, newItem: DrtItemViewModel): Boolean =
                oldItem.label.value == newItem.label.value
                        && oldItem.values.value == newItem.values.value
    }

    private val quickBookingObserver = Observer<QuickBooking> {
        if (it.input.isNotEmpty()) {
            generateDrtItems(it.input)
        }
    }

    private val inputsObserver = Observer<List<Input>> { inputs ->
        _areInputsValid.value = inputs.none {
            it.required &&
                    ((it.type == QuickBookingType.MULTIPLE_CHOICE && !it.options.isNullOrEmpty() && it.values.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.SINGLE_CHOICE && !it.options.isNullOrEmpty() && it.value.isNullOrEmpty()) ||
                            (it.type == QuickBookingType.LONG_TEXT && it.value.isNullOrEmpty()))
        }
    }

    private val bookingConfirmationObserver = Observer<BookingConfirmation> {
        it?.let {
            items.clear()

            val result = mutableListOf<DrtItemViewModel>()

            it.input().forEach { input ->
                if (!input.value().isNullOrEmpty() || !input.values().isNullOrEmpty()) {
                    result.add(
                            DrtItemViewModel().apply {
                                setIcon(getIconById(input.id()))
                                setLabel(input.title())
                                setType(input.type())
                                setValue(
                                        if (!input.value().isNullOrEmpty()) {
                                            listOf(input.value())
                                        } else {
                                            input.values()
                                        }
                                )
                                setRequired(input.required())
                                setItemId(input.id())
                                input.options()?.let { opt ->
                                    setOptions(
                                            opt.map {
                                                Option.parseBookingConfirmationInputOptions(it)
                                            }
                                    )
                                }
                                setViewMode(true)
                                onChangeStream = onItemChangeActionStream
                            }
                    )
                }
            }
        }
    }

    /*
    private val bookingResponseObserver = Observer<QuickBookResponse> { response ->
        items.forEach { it.setViewMode(response != null) }
        response?.let {
            setBookingUpdatePolling(it.refreshURLForSourceObject)
        }
    }
    */

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
        _inputs.observeForever(inputsObserver)
        //_bookingResponse.observeForever(bookingResponseObserver)
        _bookingConfirmation.observeForever(bookingConfirmationObserver)
    }

    private fun generateDrtItems(inputs: List<Input>) {

        val result = mutableListOf<DrtItemViewModel>()

        inputs.forEach {
            if (it.type != QuickBookingType.LONG_TEXT && it.options.isNullOrEmpty()) {
                return@forEach
            }
            result.add(
                    DrtItemViewModel().apply {
                        setIcon(getIconById(it.id))
                        setLabel(it.title)
                        setType(it.type)
                        setValue(listOf(getDefaultValueByType(it.type, it.title)))
                        setRequired(it.required)
                        setItemId(it.id)
                        it.options?.let { opt -> setOptions(opt) }
                        onChangeStream = onItemChangeActionStream
                    }
            )
        }

        _inputs.value = inputs

        items.clear()
        items.update(result)

    }

    fun updateInputValue(item: DrtItemViewModel) {
        _inputs.updateFields {
            it.value?.let { list ->
                list.filter { input -> input.id == item.itemId.value }.map { input ->
                    if (input.type == QuickBookingType.MULTIPLE_CHOICE) {
                        input.values = item.values.value
                    } else {
                        input.value = item.values.value?.firstOrNull()
                    }
                }
            }
        }
    }

    private fun getIconById(id: String): Int {
        return when (id) {
            "mobilityOptions" -> R.drawable.ic_person
            "purpose" -> R.drawable.ic_flag
            "notes" -> R.drawable.ic_edit
            else -> R.drawable.ic_car
        }
    }

    fun getDefaultValueByType(@QuickBookingType type: String, title: String): String {
        return if (type == QuickBookingType.LONG_TEXT) {
            "Tap to $title"
        } else {
            "Tap Change to make selections"
        }
    }

    fun setTripSegment(segment: TripSegment) {
        _segment.value = segment
        segment.booking.quickBookingsUrl?.let { fetchQuickBooking(it) }
    }

    private fun fetchQuickBooking(url: String) {
        quickBookingService.getQuickBooking(url)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    _loading.value = true
                }
                .subscribeBy(
                        onError = {
                            it.printStackTrace()
                            _loading.value = false
                        },
                        onSuccess = {
                            it.firstOrNull()?.let {
                                _quickBooking.value = it
                            }
                            _loading.value = false
                        }
                ).autoClear()
    }

    fun book() {
        _inputs.value?.let { inputs ->
            //inputs.singleOrNull { it.id == "mobilityOptions" }?.values = listOf("Guest")
            _quickBooking.value?.let { quickBooking ->
                quickBookingService.quickBook(
                        quickBooking.bookingURL,
                        QuickBookRequest(
                                inputs.filter {
                                    !it.value.isNullOrEmpty() || !it.values.isNullOrEmpty()
                                }
                        )
                ).observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe {
                            _loading.value = true
                        }
                        .subscribeBy(
                                onError = {
                                    it.printStackTrace()
                                    _loading.value = false
                                },
                                onSuccess = {
                                    _loading.value = false
                                    processBookingResponse(it)
                                }
                        ).autoClear()
            }
        }
    }

    private fun processBookingResponse(response: QuickBookResponse) {
        setBookingUpdatePolling(response.refreshURLForSourceObject)
    }

    private fun setBookingUpdatePolling(url: String) {
        val stopUpdate = AtomicBoolean()
        Observable.interval(2L, TimeUnit.SECONDS, Schedulers.io())
                .takeWhile { !stopUpdate.get() }
                .flatMapSingle {
                    quickBookingService.getBookingUpdate(url)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { response ->
                                response.processRawData(resources, Gson())
                                val confirmation = response.tripGroupList.firstOrNull()?.trips
                                        ?.firstOrNull()?.segments?.firstOrNull()
                                        ?.booking?.confirmation
                                confirmation?.let {
                                    _bookingConfirmation.value = it
                                }
                            }
                }.subscribe().autoClear()
    }

}