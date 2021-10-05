package com.skedgo.tripkit.ui.trippreview.drt

import android.content.res.Resources
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.haroldadmin.cnradapter.NetworkResponse
import com.skedgo.tripkit.booking.quickbooking.*
import com.skedgo.tripkit.common.model.BookingConfirmation
import com.skedgo.tripkit.common.model.BookingConfirmationAction
import com.skedgo.tripkit.common.model.BookingConfirmationStatusValue
import com.skedgo.tripkit.routing.TripSegment
import com.skedgo.tripkit.ui.R
import com.skedgo.tripkit.ui.booking.apiv2.BookingV2TrackingService
import com.skedgo.tripkit.ui.core.RxViewModel
import com.skedgo.tripkit.ui.core.logError
import com.skedgo.tripkit.ui.utils.updateFields
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.tatarka.bindingcollectionadapter2.BR
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject


class DrtViewModel @Inject constructor(
        private val quickBookingService: QuickBookingService,
        private val resources: Resources,
        private val bookingService: BookingV2TrackingService
) : RxViewModel() {

    private val _quickBooking = MutableLiveData<QuickBooking>()

    private val _inputs = MutableLiveData<List<Input>>()

    private val _segment = MutableLiveData<TripSegment>()
    val segment: LiveData<TripSegment> = _segment

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _areInputsValid = MutableLiveData<Boolean>()
    val areInputsValid: LiveData<Boolean> = _areInputsValid

    val error = ObservableField<String>()

    val onItemChangeActionStream = MutableSharedFlow<DrtItemViewModel>()

    val items = DiffObservableList<DrtItemViewModel>(DrtItemViewModel.diffCallback())
    val itemBinding = ItemBinding.of<DrtItemViewModel>(BR.viewModel, R.layout.item_drt)

    private val _bookingConfirmation = MutableLiveData<BookingConfirmation>()
    val bookingConfirmation: LiveData<BookingConfirmation> = _bookingConfirmation

    private val _confirmationActions = MutableLiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>>()
    val confirmationActions: LiveData<List<com.skedgo.tripkit.ui.generic.action_list.Action>> = _confirmationActions

    private val stopPollingUpdate = AtomicBoolean()

    private val quickBookingObserver = Observer<QuickBooking> {
        generateDrtItems(it.input)
        getBookingUpdate(it.tripUpdateURL)
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
                                            listOf(input.value() ?: "")
                                        } else {
                                            input.values() ?: emptyList()
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

            result.add(
                    DrtItemViewModel().apply {
                        setIcon(getIconById(null))
                        setLabel("Notes from operator")
                        setType("Notes")
                        setValue(
                                listOf(String.format("You have %d note", it.notes().size))
                        )
                        setRequired(false)
//                        setItemId(input.id())
                        it.notes()?.let { opt ->
                            setOptions(
                                    opt.map { note ->
                                        Option.parseBookingConfirmationInputOptions(note)
                                    }
                            )
                        }
                        setViewMode(true)
                        onChangeStream = onItemChangeActionStream
                    }
            )

            items.update(result)

            _confirmationActions.value = it.actions().map { confirmationAction ->
                com.skedgo.tripkit.ui.generic.action_list.Action(
                        confirmationAction.title(),
                        if (confirmationAction.isDestructive) {
                            R.color.tripKitError
                        } else {
                            R.color.colorPrimary
                        },
                        confirmationAction
                )
            }
        }
    }

    init {
        //getDrtItems()
        _quickBooking.observeForever(quickBookingObserver)
        _inputs.observeForever(inputsObserver)
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

    private fun getIconById(id: String?): Int {
        return when (id) {
            "mobilityOptions" -> R.drawable.ic_person
            "purpose" -> R.drawable.ic_flag
            "notes" -> R.drawable.ic_edit
            else -> R.drawable.ic_note
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
        if (segment.booking.quickBookingsUrl.isNullOrEmpty()) {
            segment.trip.temporaryURL?.let { getBookingUpdate(it) }
        } else {
            segment.booking.quickBookingsUrl?.let { fetchQuickBooking(it) }
        }
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

                            try {
                                val errorString = (it as HttpException).response()?.errorBody()?.string()
                                errorString?.let {
                                    val json = JSONObject(errorString)
                                    error.set(json.getString("error"))
                                }
                            } catch (e: Exception) {}
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
        _quickBooking.value?.let { quickBooking ->
            logAction(quickBooking.bookingURL)
        }
    }

    private fun logAction(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = bookingService.logTrip(url)
            if (result !is NetworkResponse.Success) {
                result.logError()
            }
            viewModelScope.launch(Dispatchers.Main) {
                proceedBooking()
            }
        }
    }


    private fun proceedBooking() {
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
        getBookingUpdate(response.refreshURLForSourceObject)
    }

    private fun setBookingUpdatePolling(url: String) {
        //For the initial call before the polling
        stopPollingUpdate.set(false)
        Observable.interval(30L, TimeUnit.SECONDS, Schedulers.io())
                .takeWhile { !stopPollingUpdate.get() }
                .flatMapSingle {
                    quickBookingService.getBookingUpdate(url)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSuccess { response ->
                                response.processRawData(resources, Gson())
                                val segment = response.tripGroupList.firstOrNull()?.trips
                                        ?.firstOrNull()?.segments?.firstOrNull { it.booking != null }

                                updateSegment(segment)

                                val confirmation = segment?.booking?.confirmation
                                confirmation?.let {
                                    _bookingConfirmation.postValue(it)
                                    if (it.status().value() != BookingConfirmationStatusValue.PROCESSING) {
                                        stopPollingUpdate.set(true)
                                    }
                                }
                            }
                }.subscribeBy(
                        onError = {
                            it.printStackTrace()
                        }
                ).autoClear()
    }

    private fun getBookingUpdate(url: String) {
        quickBookingService.getBookingUpdate(url)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                        onSuccess = { response ->
                            response.processRawData(resources, Gson())
                            val segment = response.tripGroupList.firstOrNull()?.trips
                                    ?.firstOrNull()?.segments?.firstOrNull { it.booking != null }

                            updateSegment(segment)

                            val confirmation = segment?.booking?.confirmation
                            confirmation?.let {
                                _bookingConfirmation.postValue(it)

                                if (it.status().value() == BookingConfirmationStatusValue.PROCESSING) {
                                    setBookingUpdatePolling(url)
                                } else {
                                    stopPollingUpdate.set(true)
                                }
                            }
                        },
                        onError = {
                            it.printStackTrace()
                        }
                ).autoClear()
    }


    private fun updateSegment(segment: TripSegment?) {
        if (segment?.id == _segment.value?.id) {
            _segment.updateFields {
                it.value?.booking = segment?.booking
            }
        }
    }

    fun processAction(action: BookingConfirmationAction?) {
        action?.let {
            it.internalURL()?.let {
                quickBookingService.executeBookingAction(it)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { _loading.value = true }
                        .subscribeBy(
                                onError = { e ->
                                    e.printStackTrace()
                                    _loading.value = false
                                },
                                onSuccess = { response ->
                                    getBookingUpdate(response.refreshURLForSourceObject)
                                    _loading.value = false
                                }
                        ).autoClear()
            }
        }
    }

}